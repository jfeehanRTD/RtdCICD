.PHONY: build clean test package native install help

# Variables
ARTIFACT_NAME = jci
VERSION = 1.0.0-SNAPSHOT
JAR_FILE = target/$(ARTIFACT_NAME)-$(VERSION).jar

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-15s\033[0m %s\n", $$1, $$2}'

build: ## Compile the project
	mvn compile

test: ## Run tests
	mvn test

package: ## Build the JAR file
	mvn package -DskipTests

clean: ## Clean build artifacts
	mvn clean

native: ## Build native image with GraalVM (requires GraalVM)
	mvn package -Pnative -DskipTests

install: package ## Install to local Maven repository
	mvn install -DskipTests

run: package ## Run the application
	java -jar $(JAR_FILE) $(ARGS)

# Development shortcuts
init: package ## Run jci init
	java -jar $(JAR_FILE) init

workflow: package ## Run jci workflow generate
	java -jar $(JAR_FILE) workflow generate

docker-gen: package ## Run jci docker generate
	java -jar $(JAR_FILE) docker generate

# Alias for running with arguments
# Usage: make jci ARGS="workflow generate --type build"
jci: package
	java -jar $(JAR_FILE) $(ARGS)
