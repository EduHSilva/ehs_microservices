package notifications

import (
	"context"
	"encoding/json"
	"fmt"
	"os"
	"strings"
	"time"

	amqp "github.com/rabbitmq/amqp091-go"
)

// EmailMessage mirrors com.edu.silva.common.dtos.EmailDTO, consumed by the
// notifications service.
type EmailMessage struct {
	UserID    string         `json:"userID,omitempty"`
	Username  string         `json:"username"`
	EmailTo   string         `json:"emailTo"`
	LocaleTag string         `json:"localeTag"`
	EmailType string         `json:"emailType"`
	BtnURL    string         `json:"btnURL"`
	Variables map[string]any `json:"variables,omitempty"`
}

const (
	defaultQueue = "default.notifications"
	dialTimeout  = 5 * time.Second
)

// Send publishes the e-mail to the notifications queue. It is intentionally
// best effort: when the broker is unreachable the caller logs the failure and
// keeps the invitation, which can be resent later.
func Send(message EmailMessage) error {
	url := strings.TrimSpace(os.Getenv("RABBITMQ_URL"))
	if url == "" {
		url = "amqp://guest:guest@localhost:5672/"
	}

	connection, err := amqp.DialConfig(url, amqp.Config{
		Dial: amqp.DefaultDial(dialTimeout),
	})
	if err != nil {
		return fmt.Errorf("connect to broker: %w", err)
	}
	defer connection.Close()

	channel, err := connection.Channel()
	if err != nil {
		return fmt.Errorf("open channel: %w", err)
	}
	defer channel.Close()

	queue := os.Getenv("NOTIFICATIONS_QUEUE")
	if queue == "" {
		queue = defaultQueue
	}

	// Declaring the queue keeps the message safe while the notifications
	// service is restarting; the consumer declares the same durable queue.
	if _, err := channel.QueueDeclare(queue, true, false, false, false, nil); err != nil {
		return fmt.Errorf("declare notifications queue: %w", err)
	}

	body, err := json.Marshal(message)
	if err != nil {
		return fmt.Errorf("marshal email: %w", err)
	}

	ctx, cancel := context.WithTimeout(context.Background(), dialTimeout)
	defer cancel()

	return channel.PublishWithContext(ctx, "", queue, false, false, amqp.Publishing{
		ContentType:  "application/json",
		DeliveryMode: amqp.Persistent,
		Body:         body,
	})
}
