package com.jci.github;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GitHubCli {

    private final Path workingDirectory;

    public GitHubCli(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public record CommandResult(int exitCode, String output, String error) {
        public boolean isSuccess() {
            return exitCode == 0;
        }
    }

    public CommandResult execute(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("gh");
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

    public boolean isInstalled() {
        try {
            CommandResult result = execute("--version");
            return result.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAuthenticated() {
        try {
            CommandResult result = execute("auth", "status");
            return result.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    public CommandResult setSecret(String name, String value) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("gh", "secret", "set", name);
        pb.directory(workingDirectory.toFile());

        Process process = pb.start();

        // Write the secret value to stdin
        process.getOutputStream().write(value.getBytes());
        process.getOutputStream().close();

        String output = readStream(process.getInputStream());
        String error = readStream(process.getErrorStream());

        int exitCode = process.waitFor();
        return new CommandResult(exitCode, output, error);
    }

    public CommandResult listSecrets() throws IOException, InterruptedException {
        return execute("secret", "list");
    }

    public boolean secretExists(String name) {
        try {
            CommandResult result = listSecrets();
            if (result.isSuccess()) {
                return result.output().contains(name);
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    public CommandResult applyBranchProtection(String branch, String owner, String repo,
                                                boolean requirePr, int requiredApprovals,
                                                boolean dismissStaleReviews, boolean requireStatusChecks,
                                                List<String> statusChecks, boolean enforceAdmins)
            throws IOException, InterruptedException {

        // Build the protection rules JSON
        StringBuilder json = new StringBuilder();
        json.append("{");

        if (requirePr) {
            json.append("\"required_pull_request_reviews\": {");
            json.append("\"dismiss_stale_reviews\": ").append(dismissStaleReviews).append(",");
            json.append("\"required_approving_review_count\": ").append(requiredApprovals);
            json.append("},");
        }

        if (requireStatusChecks && !statusChecks.isEmpty()) {
            json.append("\"required_status_checks\": {");
            json.append("\"strict\": true,");
            json.append("\"contexts\": [");
            for (int i = 0; i < statusChecks.size(); i++) {
                if (i > 0) json.append(",");
                json.append("\"").append(statusChecks.get(i)).append("\"");
            }
            json.append("]},");
        }

        json.append("\"enforce_admins\": ").append(enforceAdmins).append(",");
        json.append("\"restrictions\": null");
        json.append("}");

        return execute("api",
            "-X", "PUT",
            "/repos/" + owner + "/" + repo + "/branches/" + branch + "/protection",
            "-f", "json=" + json.toString()
        );
    }

    public CommandResult getBranchProtection(String branch, String owner, String repo)
            throws IOException, InterruptedException {
        return execute("api",
            "/repos/" + owner + "/" + repo + "/branches/" + branch + "/protection"
        );
    }
}
