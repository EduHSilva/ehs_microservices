FROM --platform=linux/amd64 golang:1.26.5 AS builder

WORKDIR /app

COPY go.mod go.sum ./

RUN go mod download

COPY . .

RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 GOAMD64=v1 go build -o health .

FROM --platform=linux/amd64 alpine:3.20

WORKDIR /app

RUN apk add --no-cache wget

COPY --from=builder /app/health /app/health

RUN chmod +x /app/health

EXPOSE 9090

ENTRYPOINT ["/app/health"]