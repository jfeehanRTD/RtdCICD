package com.jci.cli;

import com.jci.config.JciConfig;
import com.jci.detector.BuildToolDetector;
import com.jci.git.GitOperations;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
    name = "init",
    description = "Initialize jci for a Java project"
)
public class InitCommand implements Callable<Integer> {

    @ParentCommand
    private JciCommand parent;

    @Option(names = {"-f", "--force"}, description = "Overwrite existing configuration")
    boolean force;

    @Option(names = {"--no-detect"}, description = "Skip auto-detection, use defaults")
    boolean noDetect;

    @Override
    public Integer call() throws Exception {
        Path projectPath = Path.of(System.getProperty("user.dir"));
        Path configPath = projectPath.resolve(parent.getConfigPath());

        // Check if config already exists
        if (configPath.toFile().exists() && !force) {
            System.err.println("Configuration file already exists: " + configPath);
            System.err.println("Use --force to overwrite");
            return 1;
        }

        System.out.println("Initializing jci configuration...");

        JciConfig config = new JciConfig();

        // Detect build tool
        if (!noDetect) {
            BuildToolDetector detector = new BuildToolDetector();
            var result = detector.detect(projectPath);

            if (result.isPresent()) {
                var detection = result.get();
                System.out.println("Detected: " + detection.buildTool().getName() +
                    " project with Java " + detection.javaVersion());

                config.getBuild().setTool(detection.buildTool().getName());
                config.getBuild().setJavaVersion(detection.javaVersion());

                if (!detection.projectName().isEmpty()) {
                    config.getProject().setName(detection.projectName());
                }
            } else {
                System.out.println("No Maven or Gradle project detected, using defaults");
            }
        }

        // Detect GitHub remote
        GitOperations git = new GitOperations(projectPath);
        if (git.isGitRepository()) {
            var githubInfo = git.parseGitHubRemote();
            if (githubInfo.isPresent()) {
                System.out.println("Detected GitHub repository: " +
                    githubInfo.get().owner() + "/" + githubInfo.get().repo());

                config.getGithub().setOwner(githubInfo.get().owner());
                config.getGithub().setRepo(githubInfo.get().repo());

                // Set default SonarCloud values based on GitHub info
                config.getSonar().setOrganization(githubInfo.get().owner());
                config.getSonar().setProjectKey(
                    githubInfo.get().owner() + "_" + githubInfo.get().repo()
                );
            }

            // Detect main branch
            String branch = git.getCurrentBranch();
            config.getGit().setMainBranch(branch.isEmpty() ? "main" : branch);
        }

        // Save config
        config.save(configPath);
        System.out.println("Created configuration file: " + configPath);

        System.out.println();
        System.out.println("Next steps:");
        System.out.println("  1. Review and edit " + configPath);
        System.out.println("  2. Run 'jci workflow generate' to create GitHub Actions workflows");
        System.out.println("  3. Run 'jci docker generate' to create Dockerfile");
        System.out.println("  4. Run 'jci sonar setup' to configure SonarCloud");

        return 0;
    }
}
