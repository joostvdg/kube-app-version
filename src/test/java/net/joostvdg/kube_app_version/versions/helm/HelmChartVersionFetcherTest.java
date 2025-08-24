/* (C)2025 */
package net.joostvdg.kube_app_version.versions.helm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.joostvdg.kube_app_version.api.model.AppArtifact;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.error.YAMLException;

@ExtendWith(MockitoExtension.class)
class HelmChartVersionFetcherTest {

  @Mock private HttpClient httpClient;

  @Mock private HttpResponse<String> httpResponse;

  @InjectMocks private HelmChartVersionFetcher fetcher;

  @BeforeEach
  void setUp() {}

  @AfterEach
  void tearDown() {
    // Clear interrupted state if a test sets it
    Thread.interrupted();
  }

  @Test
  void getAvailableVersions_successForSpecificChart() throws Exception {
    // Arrange
    AppArtifact appArtifact =
        new AppArtifact("https://charts.cloudbees.com/public/cloudbees", "helm", "cloudbees-sda");
    String indexContent =
        Files.readString(
            Path.of(new ClassPathResource("cloudbees-helm-chart-index.yaml").getURI()));
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(indexContent);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    // Act
    List<String> versions = fetcher.getAvailableVersions(appArtifact);

    // Assert
    assertNotNull(versions);
    assertFalse(versions.isEmpty());

    // The versions in the file are not sorted by date. The fetcher should sort them descending.
    // Let's check the latest few versions from the file for cloudbees-sda, which are normalized.
    assertEquals("1.659.0+0a71e6d8c986", versions.get(0));
    assertEquals("1.657.0+06ad9895de9e", versions.get(1));
    assertEquals("1.655.0+b1b7f39f681d", versions.get(2));

    // Check total count for cloudbees-sda in the test file
    assertEquals(119, versions.size());
  }

  @Test
  void getAvailableVersions_largeIndexFileShouldNotThrowException() throws Exception {
    // Arrange
    AppArtifact prometheusApp =
        new AppArtifact("https://prometheus-community.github.io/helm-charts", "helm", "prometheus");
    String largeIndexContent =
        Files.readString(
            Path.of(new ClassPathResource("prometheus-community-index.yaml").getURI()));

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(largeIndexContent);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    // Act & Assert
    assertDoesNotThrow(
        () -> {
          List<String> versions = fetcher.getAvailableVersions(prometheusApp);
          assertNotNull(versions);
          assertFalse(versions.isEmpty());
          // Check if we got a reasonable number of versions for prometheus
          assertTrue(versions.size() > 50);
        });
  }

  @Test
  void getAvailableVersions_httpError() throws Exception {
    // Arrange
    AppArtifact appArtifact =
        new AppArtifact("https://charts.cloudbees.com/public/cloudbees", "helm", "any-chart");
    when(httpResponse.statusCode()).thenReturn(500);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    // Act & Assert
    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> fetcher.getAvailableVersions(appArtifact));
    assertTrue(exception.getMessage().contains("Failed to fetch"));
    assertTrue(exception.getMessage().contains("HTTP status: 500"));
  }

  // java
  @Test
  void getAvailableVersions_invalidYaml() throws Exception {
    // Arrange
    AppArtifact appArtifact =
        new AppArtifact("https://charts.cloudbees.com/public/cloudbees", "helm", "any-chart");
    String invalidYaml = "this: is: not valid yaml";
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(invalidYaml);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    // Act & Assert
    Exception exception =
        assertThrows(Exception.class, () -> fetcher.getAvailableVersions(appArtifact));

    // The wrapper message should start with our context, but SnakeYAML details can differ by
    // version.
    assertTrue(
        exception
            .getMessage()
            .startsWith("Failed to parse YAML from " + appArtifact.getSource() + "/index.yaml:"),
        "Unexpected wrapper message: " + exception.getMessage());

    assertInstanceOf(YAMLException.class, exception.getCause());

    // Allow common SnakeYAML variants
    String causeMsg = exception.getCause().getMessage();
    assertTrue(
        causeMsg.contains("mapping values are not allowed here")
            || causeMsg.contains("while parsing a block mapping")
            || causeMsg.contains("expected <block end>")
            || causeMsg.contains("found '<scalar>'"),
        "Unexpected SnakeYAML message: " + causeMsg);
  }

  @Test
  void getAvailableVersions_chartNotFound() throws Exception {
    // Arrange
    AppArtifact nonExistentApp =
        new AppArtifact(
            "https://charts.cloudbees.com/public/cloudbees", "helm", "non-existent-chart");
    String indexContent =
        Files.readString(
            Path.of(new ClassPathResource("cloudbees-helm-chart-index.yaml").getURI()));
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(indexContent);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    // Act
    List<String> versions = fetcher.getAvailableVersions(nonExistentApp);

    // Assert
    assertNotNull(versions);
    assertTrue(versions.isEmpty());
  }

  @Test
  void getAvailableVersions_networkErrorIOException() throws IOException, InterruptedException {
    // Arrange
    AppArtifact appArtifact =
        new AppArtifact("https://charts.cloudbees.com/public/cloudbees", "helm", "any-chart");
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new IOException("Network is down"));

    // Act & Assert
    Exception exception =
        assertThrows(Exception.class, () -> fetcher.getAvailableVersions(appArtifact));
    assertEquals(
        "Failed to send request to " + appArtifact.getSource() + "/index.yaml: Network is down",
        exception.getMessage());
    assertInstanceOf(IOException.class, exception.getCause());
  }

  @Test
  void getAvailableVersions_networkErrorInterruptedException()
      throws IOException, InterruptedException {
    // Arrange
    AppArtifact appArtifact =
        new AppArtifact("https://charts.cloudbees.com/public/cloudbees", "helm", "any-chart");
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new InterruptedException("Request was interrupted"));

    // Act & Assert
    Exception exception =
        assertThrows(Exception.class, () -> fetcher.getAvailableVersions(appArtifact));
    assertEquals(
        "Failed to send request to "
            + appArtifact.getSource()
            + "/index.yaml: Request was interrupted",
        exception.getMessage());
    assertInstanceOf(InterruptedException.class, exception.getCause());
    // Note: The implementation does not re-set the interrupted flag, so we do not assert it here.
  }

  @Test
  void getAvailableVersions_noEntriesInIndex() throws Exception {
    // Arrange
    AppArtifact appArtifact =
        new AppArtifact("https://charts.cloudbees.com/public/cloudbees", "helm", "any-chart");
    String indexContent = "apiVersion: v1\nentries: {}";
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(indexContent);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    // Act
    List<String> versions = fetcher.getAvailableVersions(appArtifact);

    // Assert
    assertNotNull(versions);
    assertTrue(versions.isEmpty());
  }

  @Test
  void getAvailableVersions_worksWithChartNameFromArgoApp() throws Exception {
    // Arrange - simulate an ArgoCD app named "cloudbees" that uses "cloudbees-core" chart
    AppArtifact helmArtifact =
        new AppArtifact(
            "https://public-charts.artifacts.cloudbees.com/repository/public",
            "helm",
            "cloudbees-core"); // Chart name, not ArgoCD app name

    String indexContent =
        Files.readString(
            Path.of(new ClassPathResource("cloudbees-helm-chart-index.yaml").getURI()));
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(indexContent);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    // Act
    List<String> versions = fetcher.getAvailableVersions(helmArtifact);

    // Assert
    assertNotNull(versions);
    assertFalse(versions.isEmpty());
    // TODO: how do we keep this in Sync?
    assertEquals("3.28985.0+797a7003b371", versions.get(0));
  }
}
