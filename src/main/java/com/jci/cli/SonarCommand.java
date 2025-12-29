package com.jci.cli;

import com.jci.config.JciConfig;
import com.jci.github.GitHubCli;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.io.Console;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
    name = "sonar",
    description = "SonarCloud management",
    subcommands = {
        SonarCommand.SetupCommand.class,
        SonarCommand.StatusCommand.class
    }
)
public class SonarCommand implements Callable<Integer> {

    @ParentCommand
    JciCommand parent;

    @Override
    public Integer call() {
        System.out.println("Use 'jci sonar setup' or 'jci sonar status'");
        return 0;
    }

    @Command(name = "setup", description = "Configure SonarCloud integration")
    public static class SetupCommand implements Callable<Integer> {

        @ParentCommand
        SonarCommand sonarParent;

        @Option(names = {"--token"}, description = "SonarCloud token (will prompt if not provided)")
        String token;

        @Option(names = {"--skip-secret"}, description = "Skip setting GitHub secret")
        boolean skipSecret;

        @Override
        public Integer call() throws Exception {
            JciCommand parent = sonarParent.parent;
            Path projectPath = Path.of(System.getProperty("user.dir"));
            Path configPath = projectPath.resolve(parent.getConfigPath());

            // Load config
            if (!Files.exists(configPath)) {
                System.err.println("Configuration not found. Run 'jci init' first.");
                return 1;
            }

            JciConfig config = JciConfig.load(configPath);

            System.out.println("Setting up SonarCloud integration...");
            System.out.println();
            System.out.println("Configuration:");
            System.out.println("  Organization: " + config.getSonar().getOrganization());
            System.out.println("  Project Key:  " + config.getSonar().getProjectKey());
            System.out.println();

            if (config.getSonar().getOrganization().isEmpty() || config.getSonar().getProjectKey().isEmpty()) {
                System.err.println("SonarCloud organization and project key must be configured in " + configPath);
                return 1;
            }

            // Get token
            String sonarToken = token;
            if (sonarToken == null) {
                System.out.println("To generate a SonarCloud token:");
                System.out.println("  1. Go to https://sonarcloud.io/account/security");
                System.out.println("  2. Generate a new token");
                System.out.println();

                Console console = System.console();
                if (console == null) {
                    System.err.println("No console available. Use --token flag.");
                    return 1;
                }

                char[] tokenChars = console.readPassword("Enter SonarCloud token: ");
                if (tokenChars == null || tokenChars.length == 0) {
                    System.err.println("Token is required");
                    return 1;
                }
                sonarToken = new String(tokenChars);
            }

            // Validate token
            System.out.println("Validating token...");
            if (!validateSonarToken(sonarToken)) {
                System.err.println("Invalid SonarCloud token");
                return 1;
            }
            System.out.println("Token validated successfully");

            // Set GitHub secret
            if (!skipSecret) {
                GitHubCli gh = new GitHubCli(projectPath);

                if (!gh.isInstalled()) {
                    System.err.println("GitHub CLI (gh) is not installed. Install it or use --skip-secret");
                    return 1;
                }

                if (!gh.isAuthenticated()) {
                    System.err.println("GitHub CLI is not authenticated. Run 'gh auth login' first.");
                    return 1;
                }

                System.out.println("Setting GitHub secret SONAR_TOKEN...");
                var result = gh.setSecret("SONAR_TOKEN", sonarToken);
                if (!result.isSuccess()) {
                    System.err.println("Failed to set secret: " + result.error());
                    return 1;
                }
                System.out.println("GitHub secret SONAR_TOKEN set successfully");
            }

            System.out.println();
            System.out.println("SonarCloud setup complete!");
            System.out.println();
            System.out.println("Next steps:");
            System.out.println("  1. Run 'jci workflow generate --type sonar' to create the SonarCloud workflow");
            System.out.println("  2. Push changes and create a PR to trigger analysis");

            return 0;
        }

        private boolean validateSonarToken(String token) {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://sonarcloud.io/api/authentication/validate"))
                    .header("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString((token + ":").getBytes()))
                    .GET()
                    .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() == 200 && response.body().contains("\"valid\":true");
            } catch (Exception e) {
                return false;
            }
        }
    }

    @Command(name = "status", description = "Check SonarCloud quality gate status")
    public static class StatusCommand implements Callable<Integer> {

        @ParentCommand
        SonarCommand sonarParent;

        @Override
        public Integer call() throws Exception {
            JciCommand parent = sonarParent.parent;
            Path projectPath = Path.of(System.getProperty("user.dir"));
            Path configPath = projectPath.resolve(parent.getConfigPath());

            // Load config
            if (!Files.exists(configPath)) {
                System.err.println("Configuration not found. Run 'jci init' first.");
                return 1;
            }

            JciConfig config = JciConfig.load(configPath);

            String projectKey = config.getSonar().getProjectKey();
            if (projectKey.isEmpty()) {
                System.err.println("SonarCloud project key not configured");
                return 1;
            }

            System.out.println("Checking quality gate status for: " + projectKey);

            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://sonarcloud.io/api/qualitygates/project_status?projectKey=" + projectKey))
                    .GET()
                    .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String body = response.body();
                    if (body.contains("\"status\":\"OK\"")) {
                        System.out.println("Quality Gate: PASSED");
                        return 0;
                    } else if (body.contains("\"status\":\"ERROR\"")) {
                        System.out.println("Quality Gate: FAILED");
                        System.out.println();
                        System.out.println("View details at: https://sonarcloud.io/project/overview?id=" + projectKey);
                        return 1;
                    } else {
                        System.out.println("Quality Gate: " + body);
                    }
                } else if (response.statusCode() == 404) {
                    System.err.println("Project not found on SonarCloud");
                    System.err.println("Make sure the project has been analyzed at least once.");
                } else {
                    System.err.println("Failed to get status: HTTP " + response.statusCode());
                }
            } catch (Exception e) {
                System.err.println("Error checking status: " + e.getMessage());
            }

            return 1;
        }
    }
}
