package com.jci.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(
    name = "jci",
    description = "Java CI/CD Automation CLI Tool",
    mixinStandardHelpOptions = true,
    version = "jci 1.0.0",
    subcommands = {
        InitCommand.class,
        CommitCommand.class,
        WorkflowCommand.class,
        DockerCommand.class,
        SonarCommand.class,
        ProtectCommand.class,
        CommandLine.HelpCommand.class
    }
)
public class JciCommand implements Callable<Integer> {

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    boolean verbose;

    @Option(names = {"-c", "--config"}, description = "Path to config file", defaultValue = ".jci.yaml")
    String configPath;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new JciCommand())
            .setExecutionStrategy(new CommandLine.RunAll())
            .execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        // Root command just shows help when called without subcommand
        CommandLine.usage(this, System.out);
        return 0;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public String getConfigPath() {
        return configPath;
    }
}
