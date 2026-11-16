package users

import (
	"encoding/json"
	"fmt"
	"health/helper"
	"net/http"
	"net/url"
	"os"
	"strings"
	"sync"
	"time"
)

const (
	internalTokenHeader = "X-Internal-Token"
	defaultCacheTTL     = 60 * time.Second
)

type Profile struct {
	ID       string   `json:"id"`
	Username string   `json:"username"`
	Email    string   `json:"email"`
	Roles    []string `json:"roles"`
	Status   string   `json:"status"`
}

func (p Profile) HasRole(role string) bool {
	for _, current := range p.Roles {
		if current == role {
			return true
		}
	}
	return false
}

// Active reports whether the account can still use the platform.
func (p Profile) Active() bool {
	return p.Status == "OK"
}

type envelope[T any] struct {
	Message string `json:"message"`
	Data    T      `json:"data"`
}

type cacheEntry struct {
	profile Profile
	found   bool
	expires time.Time
}

type Client struct {
	baseURL string
	token   string
	http    *http.Client

	mu    sync.RWMutex
	cache map[string]cacheEntry
}

var (
	clientOnce sync.Once
	client     *Client
)

func getClient() *Client {
	clientOnce.Do(func() {
		client = &Client{
			baseURL: strings.TrimRight(os.Getenv("USERS_SERVICE_URL"), "/"),
			token:   os.Getenv("INTERNAL_TOKEN"),
			http:    &http.Client{Timeout: 3 * time.Second},
			cache:   make(map[string]cacheEntry),
		}
	})
	return client
}

func Enabled() bool {
	return getClient().baseURL != ""
}

// Get returns the profile of a user by id. The second value is false when the
// account does not exist.
func Get(id string) (Profile, bool, error) {
	if id == "" {
		return Profile{}, false, nil
	}

	if entry, ok := getClient().fromCache(id); ok {
		return entry.profile, entry.found, nil
	}

	var response envelope[[]Profile]
	if err := getClient().get("/internal/profiles?ids="+url.QueryEscape(id), &response); err != nil {
		return Profile{}, false, err
	}

	profile, found := Profile{}, false
	if len(response.Data) > 0 {
		profile, found = response.Data[0], true
		getClient().store(id, profile, true)
	} else {
		getClient().store(id, Profile{}, false)
	}

	return profile, found, nil
}

// GetByEmail returns the profile registered with the e-mail, if any.
func GetByEmail(email string) (Profile, bool, error) {
	email = strings.TrimSpace(strings.ToLower(email))
	if email == "" {
		return Profile{}, false, nil
	}

	if entry, ok := getClient().fromCache("email:" + email); ok {
		return entry.profile, entry.found, nil
	}

	var response envelope[Profile]
	status, err := getClient().getStatus("/internal/profiles/by-email?email="+url.QueryEscape(email), &response)
	if err != nil {
		return Profile{}, false, err
	}
	if status == http.StatusNotFound {
		getClient().store("email:"+email, Profile{}, false)
		return Profile{}, false, nil
	}

	getClient().store("email:"+email, response.Data, true)
	return response.Data, true, nil
}

// List resolves several profiles at once, using the cache for ids already seen.
func List(ids []string) (map[string]Profile, error) {
	profiles := make(map[string]Profile, len(ids))
	missing := make([]string, 0, len(ids))

	for _, id := range ids {
		if id == "" {
			continue
		}
		if entry, ok := getClient().fromCache(id); ok {
			if entry.found {
				profiles[id] = entry.profile
			}
			continue
		}
		missing = append(missing, id)
	}

	if len(missing) == 0 {
		return profiles, nil
	}

	var response envelope[[]Profile]
	query := "/internal/profiles?ids=" + url.QueryEscape(strings.Join(missing, ","))
	if err := getClient().get(query, &response); err != nil {
		return nil, err
	}

	found := make(map[string]bool, len(missing))
	for _, profile := range response.Data {
		profiles[profile.ID] = profile
		found[profile.ID] = true
		getClient().store(profile.ID, profile, true)
	}
	for _, id := range missing {
		if !found[id] {
			getClient().store(id, Profile{}, false)
		}
	}

	return profiles, nil
}

func (c *Client) get(path string, target any) error {
	status, err := c.getStatus(path, target)
	if err != nil {
		return err
	}
	if status != http.StatusOK {
		return fmt.Errorf("users service returned %d", status)
	}
	return nil
}

func (c *Client) getStatus(path string, target any) (int, error) {
	if c.baseURL == "" {
		return 0, fmt.Errorf("USERS_SERVICE_URL not configured")
	}

	request, err := http.NewRequest(http.MethodGet, c.baseURL+path, nil)
	if err != nil {
		return 0, err
	}
	request.Header.Set(internalTokenHeader, c.token)
	request.Header.Set("Accept", "application/json")

	response, err := c.http.Do(request)
	if err != nil {
		return 0, err
	}
	defer response.Body.Close()

	if response.StatusCode == http.StatusNotFound {
		return response.StatusCode, nil
	}
	if response.StatusCode != http.StatusOK {
		return response.StatusCode, fmt.Errorf("users service returned %d", response.StatusCode)
	}
	if err := json.NewDecoder(response.Body).Decode(target); err != nil {
		return response.StatusCode, err
	}

	return response.StatusCode, nil
}

func (c *Client) fromCache(key string) (cacheEntry, bool) {
	c.mu.RLock()
	defer c.mu.RUnlock()

	entry, ok := c.cache[key]
	if !ok || time.Now().After(entry.expires) {
		return cacheEntry{}, false
	}
	return entry, true
}

func (c *Client) store(key string, profile Profile, found bool) {
	c.mu.Lock()
	defer c.mu.Unlock()

	c.cache[key] = cacheEntry{profile: profile, found: found, expires: time.Now().Add(defaultCacheTTL)}
}

// StudentEligible checks the account is active and still has the student role.
func StudentEligible(profile Profile) bool {
	return profile.Active() && profile.HasRole(helper.RoleStudent)
}
