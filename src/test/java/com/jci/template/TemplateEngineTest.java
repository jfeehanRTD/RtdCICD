package com.jci.template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TemplateEngineTest {

    private TemplateEngine engine;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        engine = new TemplateEngine();
    }

    @Test
    void templateExists() {
        assertTrue(engine.templateExists("workflows/build-maven.yml.mustache"));
        assertTrue(engine.templateExists("workflows/build-gradle.yml.mustache"));
        assertTrue(engine.templateExists("docker/Dockerfile.maven.mustache"));
        assertTrue(engine.templateExists("docker/Dockerfile.gradle.mustache"));
        assertFalse(engine.templateExists("nonexistent.mustache"));
    }

    @Test
    void renderBuildMavenTemplate() throws IOException {
        Map<String, Object> context = new HashMap<>();
        context.put("mainBranch", "main");
        context.put("javaVersion", "21");

        String result = engine.render("workflows/build-maven.yml.mustache", context);

        assertNotNull(result);
        assertTrue(result.contains("name: Build"));
        assertTrue(result.contains("branches: [main]"));
        assertTrue(result.contains("java-version: '21'"));
        assertTrue(result.contains("cache: maven"));
        assertTrue(result.contains("mvn -B package"));
    }

    @Test
    void renderBuildGradleTemplate() throws IOException {
        Map<String, Object> context = new HashMap<>();
        context.put("mainBranch", "develop");
        context.put("javaVersion", "17");

        String result = engine.render("workflows/build-gradle.yml.mustache", context);

        assertNotNull(result);
        assertTrue(result.contains("branches: [develop]"));
        assertTrue(result.contains("java-version: '17'"));
        assertTrue(result.contains("cache: gradle"));
        assertTrue(result.contains("./gradlew build"));
    }

    @Test
    void renderSonarTemplateWithGitHubActions() throws IOException {
        Map<String, Object> context = new HashMap<>();
        context.put("mainBranch", "main");
        context.put("javaVersion", "21");
        context.put("sonarOrganization", "myorg");
        context.put("sonarProjectKey", "myorg_myproject");

        String result = engine.render("workflows/sonar-maven.yml.mustache", context);

        // Verify GitHub Actions syntax is preserved
        assertTrue(result.contains("${{ secrets.GITHUB_TOKEN }}"));
        assertTrue(result.contains("${{ secrets.SONAR_TOKEN }}"));
        assertTrue(result.contains("${{ runner.os }}"));

        // Verify our variables are substituted
        assertTrue(result.contains("-Dsonar.organization=myorg"));
        assertTrue(result.contains("-Dsonar.projectKey=myorg_myproject"));
    }

    @Test
    void renderDockerPublishTemplate() throws IOException {
        Map<String, Object> context = new HashMap<>();
        context.put("mainBranch", "main");
        context.put("javaVersion", "21");
        context.put("buildTool", "gradle");
        context.put("isMaven", false);
        context.put("isGradle", true);

        String result = engine.render("workflows/docker-publish.yml.mustache", context);

        assertTrue(result.contains("${{ github.repository }}"));
        assertTrue(result.contains("${{ secrets.GITHUB_TOKEN }}"));
        assertTrue(result.contains("./gradlew build -x test"));
        assertFalse(result.contains("mvn -B package"));
    }

    @Test
    void renderDockerfileTemplate() throws IOException {
        Map<String, Object> context = new HashMap<>();
        context.put("javaVersion", "21");
        context.put("port", 8080);

        String result = engine.render("docker/Dockerfile.maven.mustache", context);

        assertTrue(result.contains("FROM eclipse-temurin:21-jdk"));
        assertTrue(result.contains("FROM eclipse-temurin:21-jre"));
        assertTrue(result.contains("EXPOSE 8080"));
        assertTrue(result.contains("mvnw"));
    }

    @Test
    void renderToFile() throws IOException {
        Path outputPath = tempDir.resolve("output.yml");

        Map<String, Object> context = new HashMap<>();
        context.put("mainBranch", "main");
        context.put("javaVersion", "21");

        engine.renderToFile("workflows/build-maven.yml.mustache", context, outputPath);

        assertTrue(Files.exists(outputPath));
        String content = Files.readString(outputPath);
        assertTrue(content.contains("name: Build"));
    }

    @Test
    void renderToFileCreatesParentDirectories() throws IOException {
        Path outputPath = tempDir.resolve("nested/dir/output.yml");

        Map<String, Object> context = new HashMap<>();
        context.put("mainBranch", "main");
        context.put("javaVersion", "21");

        engine.renderToFile("workflows/build-maven.yml.mustache", context, outputPath);

        assertTrue(Files.exists(outputPath));
        assertTrue(Files.exists(tempDir.resolve("nested/dir")));
    }

    @Test
    void renderNonExistentTemplateThrowsException() {
        Map<String, Object> context = new HashMap<>();

        assertThrows(IOException.class, () -> {
            engine.render("nonexistent.mustache", context);
        });
    }
}
