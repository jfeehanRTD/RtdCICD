package com.jci.detector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BuildToolDetectorTest {

    private BuildToolDetector detector;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        detector = new BuildToolDetector();
    }

    @Test
    void detectMavenProject() throws IOException {
        // Create a pom.xml
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1.0.0</version>
                <properties>
                    <maven.compiler.source>21</maven.compiler.source>
                    <maven.compiler.target>21</maven.compiler.target>
                </properties>
            </project>
            """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);

        Optional<BuildToolDetector.DetectionResult> result = detector.detect(tempDir);

        assertTrue(result.isPresent());
        assertEquals(BuildToolDetector.BuildTool.MAVEN, result.get().buildTool());
        assertEquals("21", result.get().javaVersion());
        assertEquals("my-app", result.get().projectName());
        assertFalse(result.get().useKotlinDsl());
    }

    @Test
    void detectGradleProject() throws IOException {
        // Create a build.gradle
        String gradleContent = """
            plugins {
                id 'java'
            }

            sourceCompatibility = '17'
            """;
        Files.writeString(tempDir.resolve("build.gradle"), gradleContent);

        // Create settings.gradle with project name
        String settingsContent = """
            rootProject.name = 'my-gradle-app'
            """;
        Files.writeString(tempDir.resolve("settings.gradle"), settingsContent);

        Optional<BuildToolDetector.DetectionResult> result = detector.detect(tempDir);

        assertTrue(result.isPresent());
        assertEquals(BuildToolDetector.BuildTool.GRADLE, result.get().buildTool());
        assertEquals("17", result.get().javaVersion());
        assertEquals("my-gradle-app", result.get().projectName());
        assertFalse(result.get().useKotlinDsl());
    }

    @Test
    void detectGradleKotlinDslProject() throws IOException {
        // Create a build.gradle.kts
        String gradleContent = """
            plugins {
                java
            }

            java {
                sourceCompatibility = JavaVersion.VERSION_21
            }
            """;
        Files.writeString(tempDir.resolve("build.gradle.kts"), gradleContent);

        Optional<BuildToolDetector.DetectionResult> result = detector.detect(tempDir);

        assertTrue(result.isPresent());
        assertEquals(BuildToolDetector.BuildTool.GRADLE, result.get().buildTool());
        assertTrue(result.get().useKotlinDsl());
    }

    @Test
    void detectNoBuildTool() {
        // Empty directory
        Optional<BuildToolDetector.DetectionResult> result = detector.detect(tempDir);

        assertFalse(result.isPresent());
    }

    @Test
    void mavenTakesPrecedenceOverGradle() throws IOException {
        // Create both pom.xml and build.gradle
        Files.writeString(tempDir.resolve("pom.xml"), "<project><artifactId>maven-app</artifactId></project>");
        Files.writeString(tempDir.resolve("build.gradle"), "plugins { id 'java' }");

        Optional<BuildToolDetector.DetectionResult> result = detector.detect(tempDir);

        assertTrue(result.isPresent());
        assertEquals(BuildToolDetector.BuildTool.MAVEN, result.get().buildTool());
    }

    @Test
    void detectJavaVersionFromMavenProperties() throws IOException {
        String pomContent = """
            <project>
                <artifactId>test</artifactId>
                <properties>
                    <java.version>17</java.version>
                </properties>
            </project>
            """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);

        Optional<BuildToolDetector.DetectionResult> result = detector.detect(tempDir);

        assertTrue(result.isPresent());
        assertEquals("17", result.get().javaVersion());
    }
}
