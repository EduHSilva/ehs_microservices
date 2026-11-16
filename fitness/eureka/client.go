package eureka

import (
	"bytes"
	"encoding/xml"
	"fmt"
	"io"
	"net/http"
	"os"
	"strings"
	"time"
)

func Register() {

	eurekaURL := os.Getenv("EUREKA_URL")
	serviceName := os.Getenv("SERVICE_NAME")
	host := os.Getenv("SERVICE_HOST")
	port := os.Getenv("SERVICE_PORT")

	instanceID := fmt.Sprintf("%s:%s:%s", serviceName, host, port)
	serviceID := strings.ToLower(serviceName)
	homePageURL := fmt.Sprintf("http://%s:%s/", host, port)
	statusPageURL := homePageURL
	healthCheckURL := fmt.Sprintf("http://%s:%s/actuator/health", host, port)
	swaggerURL := swaggerPublicURL()

	payload := fmt.Sprintf(`
	<instance>
		<instanceId>%s</instanceId>
		<hostName>%s</hostName>
		<app>%s</app>
		<ipAddr>%s</ipAddr>
		<vipAddress>%s</vipAddress>
		<secureVipAddress>%s</secureVipAddress>
		<homePageUrl>%s</homePageUrl>
		<statusPageUrl>%s</statusPageUrl>
		<healthCheckUrl>%s</healthCheckUrl>

		<status>UP</status>

		<port enabled="true">%s</port>

		<metadata>
			<swagger-url>%s</swagger-url>
			<sidebar.links.0.label>Swagger</sidebar.links.0.label>
			<sidebar.links.0.url>%s</sidebar.links.0.url>
		</metadata>

		<dataCenterInfo class="com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo">
			<name>MyOwn</name>
		</dataCenterInfo>
	</instance>
	`,
		xmlEscape(instanceID),
		xmlEscape(host),
		xmlEscape(serviceName),
		xmlEscape(host),
		xmlEscape(serviceID),
		xmlEscape(serviceID),
		xmlEscape(homePageURL),
		xmlEscape(statusPageURL),
		xmlEscape(healthCheckURL),
		xmlEscape(port),
		xmlEscape(swaggerURL),
		xmlEscape(swaggerURL),
	)

	url := fmt.Sprintf(
		"%s/apps/%s",
		eurekaURL,
		serviceName,
	)

	req, err := http.NewRequest(
		http.MethodPost,
		url,
		bytes.NewBuffer([]byte(payload)),
	)

	if err != nil {
		panic(err)
	}

	req.Header.Set(
		"Content-Type",
		"application/xml",
	)

	client := &http.Client{
		Timeout: 5 * time.Second,
	}

	resp, err := client.Do(req)

	if err != nil {
		fmt.Println("Eureka registration failed:", err)
		return
	}

	defer func(Body io.ReadCloser) {
		err := Body.Close()
		if err != nil {

		}
	}(resp.Body)

	fmt.Println(
		"Eureka registration:",
		resp.StatusCode,
	)
}

func SendHeartbeat() {
	eurekaURL := os.Getenv("EUREKA_URL")
	serviceName := os.Getenv("SERVICE_NAME")
	host := os.Getenv("SERVICE_HOST")
	port := os.Getenv("SERVICE_PORT")

	instanceID := fmt.Sprintf(
		"%s:%s:%s",
		serviceName,
		host,
		port,
	)

	url := fmt.Sprintf(
		"%s/apps/%s/%s",
		eurekaURL,
		serviceName,
		instanceID,
	)

	req, err := http.NewRequest(
		http.MethodPut,
		url,
		nil,
	)

	if err != nil {
		fmt.Println("Eureka heartbeat error:", err)
		return
	}

	client := &http.Client{
		Timeout: 5 * time.Second,
	}

	resp, err := client.Do(req)

	if err != nil {
		fmt.Println("Eureka heartbeat failed:", err)
		return
	}

	defer resp.Body.Close()

	fmt.Println(
		"Eureka heartbeat:",
		resp.StatusCode,
	)

	if resp.StatusCode == http.StatusNotFound {
		Register()
	}
}

func StartHeartbeat() {

	go func() {

		ticker := time.NewTicker(
			30 * time.Second,
		)

		for range ticker.C {
			SendHeartbeat()
		}

	}()

}

func swaggerPublicURL() string {
	if url := strings.TrimSpace(os.Getenv("SWAGGER_PUBLIC_URL")); url != "" {
		return url
	}
	base := strings.TrimRight(strings.TrimSpace(os.Getenv("SERVER_URL")), "/")
	if base == "" {
		base = "http://localhost"
	}
	return base + "/fitness/swagger/index.html"
}

func xmlEscape(value string) string {
	var buffer bytes.Buffer
	_ = xml.EscapeText(&buffer, []byte(value))
	return buffer.String()
}
