# Makefile

# Application specifics
APP_NAMEAPP_NAME := kube-app-version
IMAGE_REGISTRY_USER := joostvdg # Your GitHub username or organization
IMAGE_BASE_NAME := kube-app-version
IMAGE_NAME := ghcr.io/$(IMAGE_REGISTRY_USER)/$(IMAGE_BASE_NAME)

# Attempt to automatically determine APP_VERSION from pom.xml
# Ensure mvnw is executable: chmod +x mvnw
APP_VERSION := $(shell ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null || echo "0.0.1-SNAPSHOT")

# Default tag for images, can be overridden on the command line: make TAG=latest docker-build
TAG ?= $(APP_VERSION)

# Docker Buildx Bake command
BAKE := docker buildx bake

.PHONY: all help clean build-jar build-native docker-build docker-build-jvm docker-build-native docker-push docker-push-jvm docker-push-native

all: docker-build

help:
	@echo "Makefile for $(APP_NAME) (version $(APP_VERSION))"
	@echo ""
	@echo "Usage: make [target]"
	@echo ""
	@echo "Primary Targets:"
	@echo "  build-jar             Build the Spring Boot JAR application."
	@echo "  build-native          Build the GraalVM native executable (requires GraalVM setup)."
	@echo "  docker-build          Build all Docker images (JVM and Native) for linux/amd64 and linux/arm64."
	@echo "  docker-push           Build and push all Docker images to $(IMAGE_NAME)."
	@echo ""
	@echo "Individual Build Targets:"
	@echo "  docker-build-jvm      Build only the JVM Docker image."
	@echo "  docker-build-native   Build only the Native Docker image."
	@echo "  docker-push-jvm       Build and push only the JVM Docker image."
	@echo "  docker-push-native    Build and push only the Native Docker image."
	@echo ""
	@echo "Utility Targets:"
	@echo "  clean                 Clean the Maven project."
	@echo "  help                  Show this help message."
	@echo ""
	@echo "Variables that can be overridden:"
	@echo "  TAG=$(TAG)            Docker image tag (defaults to application version: $(APP_VERSION))."
	@echo "  APP_VERSION=$(APP_VERSION) (auto-detected from pom.xml, fallback to '0.0.1-SNAPSHOT')."
	@echo "  IMAGE_NAME=$(IMAGE_NAME)"
	@echo "  IMAGE_REGISTRY_USER=$(IMAGE_REGISTRY_USER)"


# Maven build targets (these are also run inside Dockerfiles but useful for local dev)
build-jar:
	@echo ">>> Building Spring Boot JAR for $(APP_NAME) version $(APP_VERSION)..."
	./mvnw package -DskipTests

build-native:
	@echo ">>> Building GraalVM Native Image for $(APP_NAME) version $(APP_VERSION)..."
	./mvnw package -Pnative -DskipTests

# Docker build targets using Docker Bake
# We pass APP_VERSION and TAG to Docker Bake to override variables in docker-bake.hcl
docker-build:
	@echo ">>> Building all Docker images for $(IMAGE_NAME) with primary tag $(TAG) (version $(APP_VERSION))..."
	$(BAKE) --set "*.vars.APP_VERSION=$(APP_VERSION)" --set "*.vars.DEFAULT_TAG=$(TAG)" default

docker-build-jvm:
	@echo ">>> Building JVM Docker image for $(IMAGE_NAME) with tag $(TAG)-jvm (version $(APP_VERSION))..."
	$(BAKE) --set "*.vars.APP_VERSION=$(APP_VERSION)" --set "*.vars.DEFAULT_TAG=$(TAG)" app-jvm

docker-build-native:
	@echo ">>> Building Native Docker image for $(IMAGE_NAME) with primary tag $(TAG) (version $(APP_VERSION))..."
	$(BAKE) --set "*.vars.APP_VERSION=$(APP_VERSION)" --set "*.vars.DEFAULT_TAG=$(TAG)" app-native

# Docker push targets
docker-push:
	@echo ">>> Building and Pushing all Docker images for $(IMAGE_NAME) with primary tag $(TAG) (version $(APP_VERSION))..."
	$(BAKE) --set "*.vars.APP_VERSION=$(APP_VERSION)" --set "*.vars.DEFAULT_TAG=$(TAG)" --push default

docker-push-jvm:
	@echo ">>> Building and Pushing JVM Docker image for $(IMAGE_NAME) with tag $(TAG)-jvm (version $(APP_VERSION))..."
	$(BAKE) --set "*.vars.APP_VERSION=$(APP_VERSION)" --set "*.vars.DEFAULT_TAG=$(TAG)" --push app-jvm

docker-push-native:
	@echo ">>> Building and Pushing Native Docker image for $(IMAGE_NAME) with primary tag $(TAG) (version $(APP_VERSION))..."
	$(BAKE) --set "*.vars.APP_VERSION=$(APP_VERSION)" --set "*.vars.DEFAULT_TAG=$(TAG)" --push app-native

clean:
	@echo ">>> Cleaning Maven project..."
	./mvnw clean
	@echo ">>> Maven project cleaned."
	@echo ">>> To prune Docker build cache, run: docker builder prune -af"
