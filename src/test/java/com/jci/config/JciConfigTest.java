package com.jci.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JciConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void defaultConfigValues() {
        JciConfig config = new JciConfig();

        assertEquals("1", config.getVersion());
        assertEquals("maven", config.getBuild().getTool());
        assertEquals("21", config.getBuild().getJavaVersion());
        assertEquals("main", config.getGit().getMainBranch());
        assertTrue(config.getGit().getCommit().isConventional());
        assertFalse(config.getGit().getCommit().isSign());
        assertEquals("ghcr.io", config.getDocker().getRegistry());
        assertEquals(8080, config.getDocker().getPort());
        assertTrue(config.getWorkflows().getBuild().isEnabled());
        assertTrue(config.getWorkflows().getTest().isEnabled());
        assertEquals(80, config.getWorkflows().getTest().getCoverage().getMinCoverage());
    }

    @Test
    void saveAndLoadConfig() throws IOException {
        Path configPath = tempDir.resolve(".jci.yaml");

        // Create and customize config
        JciConfig config = new JciConfig();
        config.getProject().setName("test-project");
        config.getBuild().setTool("gradle");
        config.getBuild().setJavaVersion("17");
        config.getGithub().setOwner("testowner");
        config.getGithub().setRepo("testrepo");
        config.getSonar().setOrganization("testorg");
        config.getSonar().setProjectKey("testorg_testrepo");

        // Save
        config.save(configPath);

        // Verify file exists
        assertTrue(Files.exists(configPath));

        // Load
        JciConfig loaded = JciConfig.load(configPath);

        // Verify values
        assertEquals("test-project", loaded.getProject().getName());
        assertEquals("gradle", loaded.getBuild().getTool());
        assertEquals("17", loaded.getBuild().getJavaVersion());
        assertEquals("testowner", loaded.getGithub().getOwner());
        assertEquals("testrepo", loaded.getGithub().getRepo());
        assertEquals("testorg", loaded.getSonar().getOrganization());
        assertEquals("testorg_testrepo", loaded.getSonar().getProjectKey());
    }

    @Test
    void loadNonExistentConfigReturnsDefault() throws IOException {
        Path configPath = tempDir.resolve("nonexistent.yaml");

        JciConfig config = JciConfig.load(configPath);

        assertNotNull(config);
        assertEquals("1", config.getVersion());
        assertEquals("maven", config.getBuild().getTool());
    }

    @Test
    void savedConfigHasNoClassTags() throws IOException {
        Path configPath = tempDir.resolve(".jci.yaml");

        JciConfig config = new JciConfig();
        config.save(configPath);

        String content = Files.readString(configPath);

        // Should not contain Java class tags
        assertFalse(content.contains("!!com.jci"));
        assertFalse(content.contains("!!java"));

        // Should contain expected YAML keys
        assertTrue(content.contains("build:"));
        assertTrue(content.contains("javaVersion:"));
        assertTrue(content.contains("mainBranch:"));
    }

    @Test
    void branchProtectionDefaults() {
        JciConfig config = new JciConfig();
        var rules = config.getBranchProtection().getMain();

        assertTrue(rules.isRequirePullRequest());
        assertEquals(1, rules.getRequiredApprovals());
        assertTrue(rules.isDismissStaleReviews());
        assertTrue(rules.isRequireStatusChecks());
        assertFalse(rules.isEnforceAdmins());
        assertEquals(3, rules.getStatusChecks().size());
        assertTrue(rules.getStatusChecks().contains("build"));
        assertTrue(rules.getStatusChecks().contains("test"));
    }

    @Test
    void loadConfigWithOldFormatClassTags() throws IOException {
        // Test backwards compatibility with old format that had class tags
        Path configPath = tempDir.resolve(".jci.yaml");
        String oldFormatContent = """
            !!com.jci.config.JciConfig
            build:
              javaVersion: '17'
              tool: gradle
            version: '1'
            """;
        Files.writeString(configPath, oldFormatContent);

        JciConfig config = JciConfig.load(configPath);

        assertEquals("gradle", config.getBuild().getTool());
        assertEquals("17", config.getBuild().getJavaVersion());
    }
}
