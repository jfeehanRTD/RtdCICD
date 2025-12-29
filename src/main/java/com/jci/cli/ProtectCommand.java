package com.jci.cli;

import com.jci.config.JciConfig;
import com.jci.github.GitHubCli;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
    name = "protect",
    description = "GitHub branch protection management",
    subcommands = {
        ProtectCommand.ApplyCommand.class,
        ProtectCommand.ShowCommand.class
    }
)
public class ProtectCommand implements Callable<Integer> {

    @ParentCommand
    JciCommand parent;

    @Override
    public Integer call() {
        System.out.println("Use 'jci protect apply' or 'jci protect show'");
        return 0;
    }

    @Command(name = "apply", description = "Apply branch protection rules from configuration")
    public static class ApplyCommand implements Callable<Integer> {

        @ParentCommand
        ProtectCommand protectParent;

        @Option(names = {"-b", "--branch"}, description = "Branch to protect (default: main)")
        String branch;

        @Option(names = {"--dry-run"}, description = "Show what would be applied without making changes")
        boolean dryRun;

        @Override
        public Integer call() throws Exception {
            JciCommand parent = protectParent.parent;
            Path projectPath = Path.of(System.getProperty("user.dir"));
            Path configPath = projectPath.resolve(parent.getConfigPath());

            // Load config
            if (!Files.exists(configPath)) {
                System.err.println("Configuration not found. Run 'jci init' first.");
                return 1;
            }

            JciConfig config = JciConfig.load(configPath);

            String targetBranch = branch != null ? branch : config.getGit().getMainBranch();
            String owner = config.getGithub().getOwner();
            String repo = config.getGithub().getRepo();

            if (owner.isEmpty() || repo.isEmpty()) {
                System.err.println("GitHub owner and repo must be configured");
                return 1;
            }

            var rules = config.getBranchProtection().getMain();

            System.out.println("Branch protection rules for '" + targetBranch + "':");
            System.out.println("  Repository: " + owner + "/" + repo);
            System.out.println("  Require pull request: " + rules.isRequirePullRequest());
            System.out.println("  Required approvals: " + rules.getRequiredApprovals());
            System.out.println("  Dismiss stale reviews: " + rules.isDismissStaleReviews());
            System.out.println("  Require status checks: " + rules.isRequireStatusChecks());
            if (rules.isRequireStatusChecks()) {
                System.out.println("  Status checks: " + String.join(", ", rules.getStatusChecks()));
            }
            System.out.println("  Enforce for admins: " + rules.isEnforceAdmins());
            System.out.println();

            if (dryRun) {
                System.out.println("[Dry run - no changes made]");
                return 0;
            }

            GitHubCli gh = new GitHubCli(projectPath);

            if (!gh.isInstalled()) {
                System.err.println("GitHub CLI (gh) is not installed");
                return 1;
            }

            if (!gh.isAuthenticated()) {
                System.err.println("GitHub CLI is not authenticated. Run 'gh auth login' first.");
                return 1;
            }

            System.out.println("Applying branch protection rules...");

            var result = gh.applyBranchProtection(
                targetBranch,
                owner,
                repo,
                rules.isRequirePullRequest(),
                rules.getRequiredApprovals(),
                rules.isDismissStaleReviews(),
                rules.isRequireStatusChecks(),
                rules.getStatusChecks(),
                rules.isEnforceAdmins()
            );

            if (result.isSuccess()) {
                System.out.println("Branch protection applied successfully!");
                return 0;
            } else {
                System.err.println("Failed to apply protection: " + result.error());
                return 1;
            }
        }
    }

    @Command(name = "show", description = "Show current branch protection rules")
    public static class ShowCommand implements Callable<Integer> {

        @ParentCommand
        ProtectCommand protectParent;

        @Option(names = {"-b", "--branch"}, description = "Branch to check (default: main)")
        String branch;

        @Override
        public Integer call() throws Exception {
            JciCommand parent = protectParent.parent;
            Path projectPath = Path.of(System.getProperty("user.dir"));
            Path configPath = projectPath.resolve(parent.getConfigPath());

            // Load config
            if (!Files.exists(configPath)) {
                System.err.println("Configuration not found. Run 'jci init' first.");
                return 1;
            }

            JciConfig config = JciConfig.load(configPath);

            String targetBranch = branch != null ? branch : config.getGit().getMainBranch();
            String owner = config.getGithub().getOwner();
            String repo = config.getGithub().getRepo();

            if (owner.isEmpty() || repo.isEmpty()) {
                System.err.println("GitHub owner and repo must be configured");
                return 1;
            }

            GitHubCli gh = new GitHubCli(projectPath);

            if (!gh.isInstalled()) {
                System.err.println("GitHub CLI (gh) is not installed");
                return 1;
            }

            if (!gh.isAuthenticated()) {
                System.err.println("GitHub CLI is not authenticated. Run 'gh auth login' first.");
                return 1;
            }

            System.out.println("Fetching protection rules for '" + targetBranch + "' on " + owner + "/" + repo);
            System.out.println();

            var result = gh.getBranchProtection(targetBranch, owner, repo);

            if (result.isSuccess()) {
                System.out.println("Current protection rules:");
                System.out.println(result.output());
            } else if (result.error().contains("404") || result.error().contains("Branch not protected")) {
                System.out.println("No protection rules configured for this branch");
            } else {
                System.err.println("Failed to get protection rules: " + result.error());
                return 1;
            }

            return 0;
        }
    }
}
