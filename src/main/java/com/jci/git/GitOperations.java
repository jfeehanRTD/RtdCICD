package com.jci.git;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GitOperations {

    private final Path workingDirectory;

    public GitOperations(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public record CommandResult(int exitCode, String output, String error) {
        public boolean isSuccess() {
            return exitCode == 0;
        }
    }

    public CommandResult execute(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDirectory.toFile());

        Process process = pb.start();

        String output = readStream(process.getInputStream());
        String error = readStream(process.getErrorStream());

        int exitCode = process.waitFor();
        return new CommandResult(exitCode, output, error);
    }

    private String readStream(java.io.InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(line);
            }
        }
        return sb.toString();
    }

    public boolean isGitRepository() {
        try {
            CommandResult result = execute("rev-parse", "--is-inside-work-tree");
            return result.isSuccess() && result.output().trim().equals("true");
        } catch (Exception e) {
            return false;
        }
    }

    public Optional<String> getRemoteUrl() {
        try {
            CommandResult result = execute("remote", "get-url", "origin");
            if (result.isSuccess()) {
                return Optional.of(result.output().trim());
            }
        } catch (Exception e) {
            // Ignore
        }
        return Optional.empty();
    }

    public record GitHubInfo(String owner, String repo) {}

    public Optional<GitHubInfo> parseGitHubRemote() {
        return getRemoteUrl().flatMap(url -> {
            // Handle SSH URLs: git@github.com:owner/repo.git
            if (url.startsWith("git@github.com:")) {
                String path = url.substring("git@github.com:".length());
                return parseOwnerRepo(path);
            }
            // Handle HTTPS URLs: https://github.com/owner/repo.git
            if (url.contains("github.com/")) {
                int idx = url.indexOf("github.com/") + "github.com/".length();
                String path = url.substring(idx);
                return parseOwnerRepo(path);
            }
            return Optional.empty();
        });
    }

    private Optional<GitHubInfo> parseOwnerRepo(String path) {
        // Remove .git suffix if present
        if (path.endsWith(".git")) {
            path = path.substring(0, path.length() - 4);
        }
        String[] parts = path.split("/");
        if (parts.length >= 2) {
            return Optional.of(new GitHubInfo(parts[0], parts[1]));
        }
        return Optional.empty();
    }

    public String getCurrentBranch() throws IOException, InterruptedException {
        CommandResult result = execute("branch", "--show-current");
        if (result.isSuccess()) {
            return result.output().trim();
        }
        return "main";
    }

    public CommandResult status() throws IOException, InterruptedException {
        return execute("status", "--porcelain");
    }

    public CommandResult add(String... files) throws IOException, InterruptedException {
        String[] args = new String[files.length + 1];
        args[0] = "add";
        System.arraycopy(files, 0, args, 1, files.length);
        return execute(args);
    }

    public CommandResult commit(String message) throws IOException, InterruptedException {
        return execute("commit", "-m", message);
    }

    public CommandResult push() throws IOException, InterruptedException {
        return execute("push");
    }

    public CommandResult pushUpstream(String branch) throws IOException, InterruptedException {
        return execute("push", "-u", "origin", branch);
    }
}
