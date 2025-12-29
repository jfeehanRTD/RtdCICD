package com.jci.cli;

import com.jci.config.JciConfig;
import com.jci.template.TemplateEngine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(
    name = "docker",
    description = "Docker management",
    subcommands = {
        DockerCommand.GenerateCommand.class
    }
)
public class DockerCommand implements Callable<Integer> {

    @ParentCommand
    JciCommand parent;

    @Override
    public Integer call() {
        System.out.println("Use 'jci docker generate'");
        return 0;
    }

    @Command(name = "generate", description = "Generate Dockerfile and .dockerignore")
    public static class GenerateCommand implements Callable<Integer> {

        @ParentCommand
        DockerCommand dockerParent;

        @Option(names = {"--jdk"}, description = "JDK version (default: from config or 21)")
        String jdkVersion;

        @Option(names = {"--base"}, description = "Base image (default: eclipse-temurin)")
        String baseImage = "eclipse-temurin";

        @Option(names = {"--port"}, description = "Application port (default: from config or 8080)")
        Integer port;

        @Option(names = {"-f", "--force"}, description = "Overwrite existing files")
        boolean force;

        @Override
        public Integer call() throws Exception {
            JciCommand parent = dockerParent.parent;
            Path projectPath = Path.of(System.getProperty("user.dir"));
            Path configPath = projectPath.resolve(parent.getConfigPath());

            // Load config
            JciConfig config;
            if (Files.exists(configPath)) {
                config = JciConfig.load(configPath);
            } else {
                config = new JciConfig();
                System.out.println("No config found, using defaults");
            }

            TemplateEngine engine = new TemplateEngine();

            // Build context
            Map<String, Object> context = new HashMap<>();
            context.put("javaVersion", jdkVersion != null ? jdkVersion : config.getBuild().getJavaVersion());
            context.put("baseImage", baseImage);
            context.put("port", port != null ? port : config.getDocker().getPort());
            context.put("buildTool", config.getBuild().getTool());
            context.put("isMaven", "maven".equals(config.getBuild().getTool()));
            context.put("isGradle", "gradle".equals(config.getBuild().getTool()));

            int generated = 0;

            // Generate Dockerfile
            Path dockerfilePath = projectPath.resolve("Dockerfile");
            if (Files.exists(dockerfilePath) && !force) {
                System.out.println("Skipping Dockerfile (exists, use --force to overwrite)");
            } else {
                String templateName = "docker/Dockerfile." + config.getBuild().getTool() + ".mustache";
                if (engine.templateExists(templateName)) {
                    engine.renderToFile(templateName, context, dockerfilePath);
                    System.out.println("Generated: Dockerfile");
                    generated++;
                } else {
                    System.err.println("Template not found: " + templateName);
                }
            }

            // Generate .dockerignore
            Path dockerignorePath = projectPath.resolve(".dockerignore");
            if (Files.exists(dockerignorePath) && !force) {
                System.out.println("Skipping .dockerignore (exists, use --force to overwrite)");
            } else {
                String templateName = "docker/dockerignore.mustache";
                if (engine.templateExists(templateName)) {
                    engine.renderToFile(templateName, context, dockerignorePath);
                    System.out.println("Generated: .dockerignore");
                    generated++;
                } else {
                    System.err.println("Template not found: " + templateName);
                }
            }

            System.out.println("Generated " + generated + " file(s)");
            return 0;
        }
    }
}
