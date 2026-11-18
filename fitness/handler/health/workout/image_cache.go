package workout

import (
	"encoding/json"
	"net/http"
	"os"
	"sync"
	"time"

	"health/schemas/workout"
)

const (
	imageCacheTTL         = 24 * time.Hour
	imageNegativeCacheTTL = 10 * time.Minute
	imageWorkers          = 6
)

type imageCacheEntry struct {
	url     string
	expires time.Time
}

var (
	imageCache    sync.Map
	imageLocks    sync.Map
	imageInflight sync.Map
	imageWorkersC = make(chan struct{}, imageWorkers)
	imageClient   = &http.Client{Timeout: 3 * time.Second}
)

func lockImage(externalID string) *sync.Mutex {
	lock, _ := imageLocks.LoadOrStore(externalID, &sync.Mutex{})
	return lock.(*sync.Mutex)
}

func imageFromCache(externalID string) (string, bool) {
	value, ok := imageCache.Load(externalID)
	if !ok {
		return "", false
	}
	entry, ok := value.(imageCacheEntry)
	if !ok || time.Now().After(entry.expires) {
		imageCache.Delete(externalID)
		return "", false
	}
	return entry.url, true
}

func storeImage(externalID, url string) {
	ttl := imageCacheTTL
	if url == "" {
		ttl = imageNegativeCacheTTL
	}
	imageCache.Store(externalID, imageCacheEntry{url: url, expires: time.Now().Add(ttl)})
}

// resolveExerciseImage returns the cached image and fetches it synchronously
// when missing. Used by plan reads, which have only a handful of exercises.
func resolveExerciseImage(exercise *workout.Exercise) string {
	if exercise.ImageURL != "" {
		return exercise.ImageURL
	}
	if exercise.ExternalID == "" || exercise.AuthorID != "" {
		return ""
	}

	lock := lockImage(exercise.ExternalID)
	lock.Lock()
	defer lock.Unlock()

	if url, ok := imageFromCache(exercise.ExternalID); ok {
		persistExerciseImage(exercise, url)
		return url
	}

	url := fetchExerciseImage(exercise.ExternalID)
	storeImage(exercise.ExternalID, url)
	persistExerciseImage(exercise, url)
	return url
}

// cachedExerciseImage never touches the network.
func cachedExerciseImage(exercise workout.Exercise) string {
	if exercise.ImageURL != "" {
		return exercise.ImageURL
	}
	if url, ok := imageFromCache(exercise.ExternalID); ok {
		return url
	}
	return ""
}

// scheduleExerciseImages warms the cache in the background so the catalog
// request is not blocked by hundreds of upstream calls.
func scheduleExerciseImages(exercises []workout.Exercise) {
	for i := range exercises {
		exercise := exercises[i]
		if exercise.ExternalID == "" || exercise.AuthorID != "" || cachedExerciseImage(exercise) != "" {
			continue
		}
		if _, loaded := imageInflight.LoadOrStore(exercise.ExternalID, struct{}{}); loaded {
			continue
		}
		go func() {
			defer imageInflight.Delete(exercise.ExternalID)
			imageWorkersC <- struct{}{}
			defer func() { <-imageWorkersC }()
			resolveExerciseImage(&exercise)
		}()
	}
}

func persistExerciseImage(exercise *workout.Exercise, url string) {
	if url == "" || exercise.ID == 0 {
		return
	}
	exercise.ImageURL = url
	_ = db.Model(&workout.Exercise{}).Where("id = ?", exercise.ID).Update("image_url", url).Error
}

func fetchExerciseImage(externalID string) string {
	host := os.Getenv("EXERCISES_API_HOST")
	token := os.Getenv("EXERCISES_API_KEY")
	if host == "" || token == "" {
		return ""
	}

	request, err := http.NewRequest(http.MethodGet,
		"https://exercisedb.p.rapidapi.com/exercises/exercise/"+externalID, nil)
	if err != nil {
		return ""
	}
	request.Header.Set("x-rapidapi-host", host)
	request.Header.Set("x-rapidapi-key", token)

	response, err := imageClient.Do(request)
	if err != nil {
		logger.ErrF("exercise image request failed: %s", err.Error())
		return ""
	}
	defer response.Body.Close()

	if response.StatusCode != http.StatusOK {
		logger.ErrF("exercise image request returned %d", response.StatusCode)
		return ""
	}

	var data struct {
		GifURL string `json:"gifUrl"`
	}
	if err := json.NewDecoder(response.Body).Decode(&data); err != nil {
		logger.ErrF("exercise image decode failed: %s", err.Error())
		return ""
	}

	return data.GifURL
}
