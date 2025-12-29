package com.jci.cli;

import com.jci.config.JciConfig;
import com.jci.git.GitOperations;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.Console;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "commit",
    description = "Smart git commit with conventional commits support"
)
public class CommitCommand implements Callable<Integer> {

    @ParentCommand
    private JciCommand parent;

    @Option(names = {"-m", "--message"}, description = "Commit message")
    String message;

    @Option(names = {"-t", "--type"}, description = "Commit type (feat, fix, docs, style, refactor, test, chore)")
    String type;

    @Option(names = {"-s", "--scope"}, description = "Commit scope")
    String scope;

    @Option(names = {"-p", "--push"}, description = "Push after commit")
    boolean push;

    @Option(names = {"-a", "--all"}, description = "Stage all changes before commit")
    boolean addAll;

    @Parameters(description = "Files to stage (if not using -a)")
    List<String> files;

    private static final List<String> COMMIT_TYPES = List.of(
        "feat", "fix", "docs", "style", "refactor", "perf", "test", "build", "ci", "chore", "revert"
    );

    @Override
    public Integer call() throws Exception {
        Path projectPath = Path.of(System.getProperty("user.dir"));
        Path configPath = projectPath.resolve(parent.getConfigPath());

        GitOperations git = new GitOperations(projectPath);

        if (!git.isGitRepository()) {
            System.err.println("Not a git repository");
            return 1;
        }

        // Load config for conventional commit settings
        JciConfig config = JciConfig.load(configPath);
        boolean useConventional = config.getGit().getCommit().isConventional();

        // Show status
        var status = git.status();
        if (status.output().isEmpty()) {
            System.err.println("Nothing to commit, working tree clean");
            return 1;
        }

        System.out.println("Changes to commit:");
        System.out.println(status.output());
        System.out.println();

        // Stage files
        if (addAll) {
            var addResult = git.add(".");
            if (!addResult.isSuccess()) {
                System.err.println("Failed to stage files: " + addResult.error());
                return 1;
            }
        } else if (files != null && !files.isEmpty()) {
            var addResult = git.add(files.toArray(new String[0]));
            if (!addResult.isSuccess()) {
                System.err.println("Failed to stage files: " + addResult.error());
                return 1;
            }
        }

        // Build commit message
        String commitMessage;
        if (message != null) {
            if (useConventional && type != null) {
                commitMessage = buildConventionalMessage(type, scope, message);
            } else {
                commitMessage = message;
            }
        } else {
            // Interactive mode
            commitMessage = interactiveCommitMessage(useConventional);
            if (commitMessage == null) {
                System.err.println("Commit cancelled");
                return 1;
            }
        }

        // Perform commit
        System.out.println("Committing with message: " + commitMessage);
        var commitResult = git.commit(commitMessage);

        if (!commitResult.isSuccess()) {
            System.err.println("Commit failed: " + commitResult.error());
            return 1;
        }

        System.out.println("Commit successful!");

        // Push if requested
        if (push) {
            System.out.println("Pushing to remote...");
            var pushResult = git.push();
            if (!pushResult.isSuccess()) {
                // Try with upstream
                String branch = git.getCurrentBranch();
                pushResult = git.pushUpstream(branch);
                if (!pushResult.isSuccess()) {
                    System.err.println("Push failed: " + pushResult.error());
                    return 1;
                }
            }
            System.out.println("Pushed successfully!");
        }

        return 0;
    }

    private String buildConventionalMessage(String type, String scope, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append(type);
        if (scope != null && !scope.isEmpty()) {
            sb.append("(").append(scope).append(")");
        }
        sb.append(": ").append(message);
        return sb.toString();
    }

    private String interactiveCommitMessage(boolean useConventional) {
        Console console = System.console();
        if (console == null) {
            System.err.println("No console available for interactive mode. Use -m flag.");
            return null;
        }

        if (useConventional) {
            System.out.println("Commit types: " + String.join(", ", COMMIT_TYPES));
            String inputType = console.readLine("Type: ").trim();
            if (!COMMIT_TYPES.contains(inputType)) {
                System.err.println("Invalid commit type");
                return null;
            }

            String inputScope = console.readLine("Scope (optional): ").trim();
            String inputMessage = console.readLine("Message: ").trim();

            if (inputMessage.isEmpty()) {
                return null;
            }

            return buildConventionalMessage(inputType, inputScope, inputMessage);
        } else {
            String inputMessage = console.readLine("Commit message: ").trim();
            return inputMessage.isEmpty() ? null : inputMessage;
        }
    }
}
