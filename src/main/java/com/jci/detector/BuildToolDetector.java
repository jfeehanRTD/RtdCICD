package com.jci.detector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuildToolDetector {

    public enum BuildTool {
        MAVEN("maven"),
        GRADLE("gradle");

        private final String name;

        BuildTool(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public record DetectionResult(
        BuildTool buildTool,
        String javaVersion,
        String projectName,
        boolean useKotlinDsl
    ) {}

    public Optional<DetectionResult> detect(Path projectPath) {
        // Check for Maven
        Path pomXml = projectPath.resolve("pom.xml");
        if (Files.exists(pomXml)) {
            return Optional.of(detectMaven(pomXml));
        }

        // Check for Gradle (Kotlin DSL first)
        Path buildGradleKts = projectPath.resolve("build.gradle.kts");
        if (Files.exists(buildGradleKts)) {
            return Optional.of(detectGradle(buildGradleKts, true));
        }

        // Check for Gradle (Groovy DSL)
        Path buildGradle = projectPath.resolve("build.gradle");
        if (Files.exists(buildGradle)) {
            return Optional.of(detectGradle(buildGradle, false));
        }

        return Optional.empty();
    }

    private DetectionResult detectMaven(Path pomXml) {
        String javaVersion = "21";
        String projectName = "";

        try {
            String content = Files.readString(pomXml);

            // Extract Java version from properties
            Pattern javaVersionPattern = Pattern.compile(
                "<(?:maven\\.compiler\\.(?:source|target)|java\\.version)>([^<]+)</",
                Pattern.MULTILINE
            );
            Matcher javaMatcher = javaVersionPattern.matcher(content);
            if (javaMatcher.find()) {
                javaVersion = javaMatcher.group(1).trim();
            }

            // Extract project name (artifactId)
            Pattern artifactIdPattern = Pattern.compile(
                "<artifactId>([^<]+)</artifactId>",
                Pattern.MULTILINE
            );
            Matcher artifactMatcher = artifactIdPattern.matcher(content);
            if (artifactMatcher.find()) {
                projectName = artifactMatcher.group(1).trim();
            }
        } catch (IOException e) {
            // Use defaults
        }

        return new DetectionResult(BuildTool.MAVEN, javaVersion, projectName, false);
    }

    private DetectionResult detectGradle(Path buildFile, boolean kotlinDsl) {
        String javaVersion = "21";
        String projectName = buildFile.getParent().getFileName().toString();

        try {
            String content = Files.readString(buildFile);

            // Extract Java version from sourceCompatibility or toolchain
            Pattern javaVersionPattern = Pattern.compile(
                "(?:sourceCompatibility|targetCompatibility|languageVersion)\\s*[=.]\\s*['\"]?(?:JavaVersion\\.VERSION_)?(\\d+)",
                Pattern.MULTILINE
            );
            Matcher javaMatcher = javaVersionPattern.matcher(content);
            if (javaMatcher.find()) {
                javaVersion = javaMatcher.group(1).trim();
            }

            // Check settings.gradle for project name
            Path settingsFile = buildFile.getParent().resolve(
                kotlinDsl ? "settings.gradle.kts" : "settings.gradle"
            );
            if (Files.exists(settingsFile)) {
                String settingsContent = Files.readString(settingsFile);
                Pattern rootProjectPattern = Pattern.compile(
                    "rootProject\\.name\\s*=\\s*['\"]([^'\"]+)['\"]"
                );
                Matcher rootMatcher = rootProjectPattern.matcher(settingsContent);
                if (rootMatcher.find()) {
                    projectName = rootMatcher.group(1).trim();
                }
            }
        } catch (IOException e) {
            // Use defaults
        }

        return new DetectionResult(BuildTool.GRADLE, javaVersion, projectName, kotlinDsl);
    }
}
