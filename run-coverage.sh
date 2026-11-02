#!/bin/bash
set -e

export JAVA_HOME=/home/eduardo/.jdks/jdk-21.0.8+9
export PATH=$JAVA_HOME/bin:$PATH

echo "Running tests + coverage with $(java -version 2>&1 | head -n1)"
mvn clean verify

echo ""
echo "Coverage reports generated per module:"
find . -name "index.html" -path "*/target/site/jacoco/index.html" | sort

echo ""
echo "Done. Open the reports above in your browser."
