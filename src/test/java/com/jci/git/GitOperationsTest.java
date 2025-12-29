package com.jci.git;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GitOperationsTest {

    @TempDir
    Path tempDir;

    private GitOperations git;

    @BeforeEach
    void setUp() throws IOException, InterruptedException {
        git = new GitOperations(tempDir);
        // Initialize a git repo
        git.execute("init");
        git.execute("config", "user.email", "test@test.com");
        git.execute("config", "user.name", "Test User");
    }

    @Test
    void isGitRepository() {
        assertTrue(git.isGitRepository());
    }

    @Test
    void isNotGitRepository() {
        GitOperations nonGit = new GitOperations(tempDir.resolve("nonexistent"));
        assertFalse(nonGit.isGitRepository());
    }

    @Test
    void statusOnCleanRepo() throws IOException, InterruptedException {
        var result = git.status();

        assertTrue(result.isSuccess());
        assertTrue(result.output().isEmpty());
    }

    @Test
    void statusWithUntrackedFiles() throws IOException, InterruptedException {
        Files.writeString(tempDir.resolve("test.txt"), "hello");

        var result = git.status();

        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("test.txt"));
    }

    @Test
    void addAndCommit() throws IOException, InterruptedException {
        // Create a file
        Files.writeString(tempDir.resolve("test.txt"), "hello");

        // Add
        var addResult = git.add("test.txt");
        assertTrue(addResult.isSuccess());

        // Commit
        var commitResult = git.commit("Test commit");
        assertTrue(commitResult.isSuccess());

        // Verify clean status
        var statusResult = git.status();
        assertTrue(statusResult.output().isEmpty());
    }

    @Test
    void addAllFiles() throws IOException, InterruptedException {
        Files.writeString(tempDir.resolve("file1.txt"), "content1");
        Files.writeString(tempDir.resolve("file2.txt"), "content2");

        var addResult = git.add(".");
        assertTrue(addResult.isSuccess());

        var commitResult = git.commit("Add all files");
        assertTrue(commitResult.isSuccess());

        var statusResult = git.status();
        assertTrue(statusResult.output().isEmpty());
    }

    @Test
    void getCurrentBranch() throws IOException, InterruptedException {
        // Make initial commit to have a branch
        Files.writeString(tempDir.resolve("init.txt"), "init");
        git.add(".");
        git.commit("Initial commit");

        String branch = git.getCurrentBranch();

        // Modern git uses "main" by default, but some systems use "master"
        assertTrue(branch.equals("main") || branch.equals("master"));
    }

    @Test
    void parseGitHubSshRemote() throws IOException, InterruptedException {
        git.execute("remote", "add", "origin", "git@github.com:owner/repo.git");

        Optional<GitOperations.GitHubInfo> info = git.parseGitHubRemote();

        assertTrue(info.isPresent());
        assertEquals("owner", info.get().owner());
        assertEquals("repo", info.get().repo());
    }

    @Test
    void parseGitHubHttpsRemote() throws IOException, InterruptedException {
        git.execute("remote", "add", "origin", "https://github.com/myowner/myrepo.git");

        Optional<GitOperations.GitHubInfo> info = git.parseGitHubRemote();

        assertTrue(info.isPresent());
        assertEquals("myowner", info.get().owner());
        assertEquals("myrepo", info.get().repo());
    }

    @Test
    void parseGitHubRemoteWithoutGitSuffix() throws IOException, InterruptedException {
        git.execute("remote", "add", "origin", "https://github.com/owner/repo");

        Optional<GitOperations.GitHubInfo> info = git.parseGitHubRemote();

        assertTrue(info.isPresent());
        assertEquals("owner", info.get().owner());
        assertEquals("repo", info.get().repo());
    }

    @Test
    void parseNonGitHubRemote() throws IOException, InterruptedException {
        git.execute("remote", "add", "origin", "https://gitlab.com/owner/repo.git");

        Optional<GitOperations.GitHubInfo> info = git.parseGitHubRemote();

        assertFalse(info.isPresent());
    }

    @Test
    void getRemoteUrlWhenNoRemote() {
        Optional<String> url = git.getRemoteUrl();
        assertFalse(url.isPresent());
    }

    @Test
    void getRemoteUrlWithOrigin() throws IOException, InterruptedException {
        git.execute("remote", "add", "origin", "https://github.com/test/test.git");

        Optional<String> url = git.getRemoteUrl();

        assertTrue(url.isPresent());
        assertEquals("https://github.com/test/test.git", url.get());
    }

    @Test
    void commandResultSuccess() throws IOException, InterruptedException {
        var result = git.execute("status");

        assertTrue(result.isSuccess());
        assertEquals(0, result.exitCode());
    }

    @Test
    void commandResultFailure() throws IOException, InterruptedException {
        var result = git.execute("invalid-command");

        assertFalse(result.isSuccess());
        assertNotEquals(0, result.exitCode());
    }
}
