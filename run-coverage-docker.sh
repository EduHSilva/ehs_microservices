#!/bin/bash
set -e

echo "Running tests + coverage with Maven + JDK 21 (Docker)..."
docker run --rm \
  -v "$(pwd)":/app \
  -w /app \
  -v "$HOME/.m2":/root/.m2 \
  maven:3.9.9-eclipse-temurin-21 \
  mvn clean verify

echo ""
echo "Coverage reports generated per module:"
find . -name "index.html" -path "*/target/site/jacoco/index.html" | sort

echo ""
echo "Done. Open the reports above in your browser."
