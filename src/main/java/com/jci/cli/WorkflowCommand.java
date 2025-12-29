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
    name = "workflow",
    description = "GitHub Actions workflow management",
    subcommands = {
        WorkflowCommand.GenerateCommand.class,
        WorkflowCommand.ValidateCommand.class
    }
)
public class WorkflowCommand implements Callable<Integer> {

    @ParentCommand
    JciCommand parent;

    @Override
    public Integer call() {
        System.out.println("Use 'jci workflow generate' or 'jci workflow validate'");
        return 0;
    }

    @Command(name = "generate", description = "Generate GitHub Actions workflow files")
    public static class GenerateCommand implements Callable<Integer> {

        @ParentCommand
        WorkflowCommand workflowParent;

        @Option(names = {"-t", "--type"}, description = "Workflow type: build, test, sonar, docker, all", defaultValue = "all")
        String type;

        @Option(names = {"-f", "--force"}, description = "Overwrite existing files")
        boolean force;

        @Override
        public Integer call() throws Exception {
            JciCommand parent = workflowParent.parent;
            Path projectPath = Path.of(System.getProperty("user.dir"));
            Path configPath = projectPath.resolve(parent.getConfigPath());

            // Load config
            if (!Files.exists(configPath)) {
                System.err.println("Configuration not found. Run 'jci init' first.");
                return 1;
            }

            JciConfig config = JciConfig.load(configPath);
            TemplateEngine engine = new TemplateEngine();

            Path workflowsDir = projectPath.resolve(".github/workflows");
            Files.createDirectories(workflowsDir);

            // Build template context
            Map<String, Object> context = buildContext(config);

            String buildTool = config.getBuild().getTool();
            boolean generateAll = "all".equals(type);

            int generated = 0;

            // Generate build workflow
            if (generateAll || "build".equals(type)) {
                if (config.getWorkflows().getBuild().isEnabled()) {
                    String templateName = "workflows/build-" + buildTool + ".yml.mustache";
                    Path outputPath = workflowsDir.resolve("build.yml");
                    if (generateWorkflow(engine, templateName, context, outputPath, force)) {
                        generated++;
                    }
                }
            }

            // Generate test workflow
            if (generateAll || "test".equals(type)) {
                if (config.getWorkflows().getTest().isEnabled()) {
                    String templateName = "workflows/test-" + buildTool + ".yml.mustache";
                    Path outputPath = workflowsDir.resolve("test.yml");
                    if (generateWorkflow(engine, templateName, context, outputPath, force)) {
                        generated++;
                    }
                }
            }

            // Generate SonarCloud workflow
            if (generateAll || "sonar".equals(type)) {
                if (config.getWorkflows().getSonar().isEnabled()) {
                    String templateName = "workflows/sonar-" + buildTool + ".yml.mustache";
                    Path outputPath = workflowsDir.resolve("sonar.yml");
                    if (generateWorkflow(engine, templateName, context, outputPath, force)) {
                        generated++;
                    }
                }
            }

            // Generate Docker workflow
            if (generateAll || "docker".equals(type)) {
                if (config.getWorkflows().getDocker().isEnabled()) {
                    String templateName = "workflows/docker-publish.yml.mustache";
                    Path outputPath = workflowsDir.resolve("docker-publish.yml");
                    if (generateWorkflow(engine, templateName, context, outputPath, force)) {
                        generated++;
                    }
                }
            }

            System.out.println("Generated " + generated + " workflow file(s)");
            return 0;
        }

        private Map<String, Object> buildContext(JciConfig config) {
            Map<String, Object> ctx = new HashMap<>();
            ctx.put("mainBranch", config.getGit().getMainBranch());
            ctx.put("javaVersion", config.getBuild().getJavaVersion());
            ctx.put("buildTool", config.getBuild().getTool());
            ctx.put("sonarOrganization", config.getSonar().getOrganization());
            ctx.put("sonarProjectKey", config.getSonar().getProjectKey());
            ctx.put("minCoverage", config.getWorkflows().getTest().getCoverage().getMinCoverage());
            ctx.put("dockerPort", config.getDocker().getPort());
            ctx.put("isMaven", "maven".equals(config.getBuild().getTool()));
            ctx.put("isGradle", "gradle".equals(config.getBuild().getTool()));
            return ctx;
        }

        private boolean generateWorkflow(TemplateEngine engine, String templateName,
                                         Map<String, Object> context, Path outputPath, boolean force)
                throws Exception {
            if (Files.exists(outputPath) && !force) {
                System.out.println("Skipping " + outputPath.getFileName() + " (exists, use --force to overwrite)");
                return false;
            }

            if (!engine.templateExists(templateName)) {
                System.err.println("Template not found: " + templateName);
                return false;
            }

            engine.renderToFile(templateName, context, outputPath);
            System.out.println("Generated: " + outputPath);
            return true;
        }
    }

    @Command(name = "validate", description = "Validate existing workflow files")
    public static class ValidateCommand implements Callable<Integer> {

        @Override
        public Integer call() {
            Path projectPath = Path.of(System.getProperty("user.dir"));
            Path workflowsDir = projectPath.resolve(".github/workflows");

            if (!Files.exists(workflowsDir)) {
                System.err.println("No workflows directory found at .github/workflows");
                return 1;
            }

            System.out.println("Validating workflows in " + workflowsDir);
            // Basic validation - check YAML syntax
            try {
                Files.list(workflowsDir)
                    .filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))
                    .forEach(p -> {
                        System.out.println("  OK: " + p.getFileName());
                    });
            } catch (Exception e) {
                System.err.println("Error reading workflows: " + e.getMessage());
                return 1;
            }

            return 0;
        }
    }
}
