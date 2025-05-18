// docker-bake.hcl
// Docker Bake configuration for building multi-platform images.

variable "IMAGE_REGISTRY_USER" {
  default = "joostvdg" // Your GitHub username or organization
}

variable "IMAGE_BASE_NAME" {
  default = "kube-app-version"
}

variable "IMAGE_NAME" {
  default = "ghcr.io/${var.IMAGE_REGISTRY_USER}/${var.IMAGE_BASE_NAME}"
}

// APP_VERSION is expected to be passed from the Makefile or environment.
// This default is a fallback.
variable "APP_VERSION" {
  default = "0.0.1-SNAPSHOT"
}

// DEFAULT_TAG is also expected to be passed from Makefile (make TAG=...).
// It defaults to APP_VERSION if not overridden.
variable "DEFAULT_TAG" {
  default = var.APP_VERSION
}

// Defines the default targets to build when 'docker buildx bake' is run without specifying targets.
group "default" {
  targets = ["app-jvm", "app-native"]
}

// --- JVM Image Definition ---
target "app-jvm" {
  context    = "." // Build context is the current directory
  dockerfile = "Dockerfile.jvm"
  platforms  = ["linux/amd64", "linux/arm64"]
  tags = [
    "${var.IMAGE_NAME}:${var.DEFAULT_TAG}-jvm",    // e.g., ghcr.io/user/app:latest-jvm or ghcr.io/user/app:0.0.1-SNAPSHOT-jvm
    "${var.IMAGE_NAME}:${var.APP_VERSION}-jvm"     // e.g., ghcr.io/user/app:0.0.1-SNAPSHOT-jvm (explicit version)
  ]
  args = {
    // This JAR_FILE path is relative to the WORKDIR in the builder stage of Dockerfile.jvm
    // after 'mvnw package'.
    JAR_FILE = "target/kube-app-version-${var.APP_VERSION}.jar"
  }
  # Cache from previous builds for these tags, if available
  cache-from = [
    "type=registry,ref=${var.IMAGE_NAME}:${var.APP_VERSION}-jvm",
    "type=registry,ref=${var.IMAGE_NAME}:latest-jvm" // A common cache tag
  ]
  # Cache to these tags upon successful build
  cache-to = ["type=inline"] // Or "type=registry,ref=${var.IMAGE_NAME}:${var.APP_VERSION}-jvm-cache,mode=max" for persistent cache
}

// --- Native Image Definition ---
target "app-native" {
  context    = "."
  dockerfile = "Dockerfile.native"
  platforms  = ["linux/amd64", "linux/arm64"]
  tags = [
    // Native image gets the primary tags (without -native suffix by default)
    "${var.IMAGE_NAME}:${var.DEFAULT_TAG}",        // e.g., ghcr.io/user/app:latest or ghcr.io/user/app:0.0.1-SNAPSHOT
    "${var.IMAGE_NAME}:${var.APP_VERSION}",         // e.g., ghcr.io/user/app:0.0.1-SNAPSHOT (explicit version)
    // Also add specific -native suffixed tags for clarity
    "${var.IMAGE_NAME}:${var.DEFAULT_TAG}-native", // e.g., ghcr.io/user/app:latest-native
    "${var.IMAGE_NAME}:${var.APP_VERSION}-native"  // e.g., ghcr.io/user/app:0.0.1-SNAPSHOT-native
  ]
  args = {
    // APP_NAME is the name of the executable produced by 'mvnw package -Pnative'
    // It typically matches the <artifactId> from pom.xml.
    APP_NAME = "kube-app-version"
  }
  # Cache from previous builds for these tags, if available
  cache-from = [
    "type=registry,ref=${var.IMAGE_NAME}:${var.APP_VERSION}-native",
    "type=registry,ref=${var.IMAGE_NAME}:latest-native",
    "type=registry,ref=${var.IMAGE_NAME}:${var.APP_VERSION}",
    "type=registry,ref=${var.IMAGE_NAME}:latest"
  ]
  # Cache to these tags upon successful build
  cache-to = ["type=inline"]
}
