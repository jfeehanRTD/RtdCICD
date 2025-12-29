package com.jci.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class JciConfig {
    private String version = "1";
    private ProjectConfig project = new ProjectConfig();
    private BuildConfig build = new BuildConfig();
    private GitConfig git = new GitConfig();
    private GithubConfig github = new GithubConfig();
    private WorkflowsConfig workflows = new WorkflowsConfig();
    private SonarConfig sonar = new SonarConfig();
    private DockerConfig docker = new DockerConfig();
    private BranchProtectionConfig branchProtection = new BranchProtectionConfig();

    // Static factory methods
    public static JciConfig load(Path path) throws IOException {
        if (!Files.exists(path)) {
            return new JciConfig();
        }

        org.yaml.snakeyaml.LoaderOptions loaderOptions = new org.yaml.snakeyaml.LoaderOptions();
        loaderOptions.setTagInspector(tag -> true); // Allow all tags for backwards compatibility

        Constructor constructor = new Constructor(JciConfig.class, loaderOptions);

        DumperOptions dumperOptions = new DumperOptions();
        Representer representer = new Representer(dumperOptions);
        representer.getPropertyUtils().setSkipMissingProperties(true);

        Yaml yaml = new Yaml(constructor, representer);
        try (FileReader reader = new FileReader(path.toFile())) {
            JciConfig config = yaml.load(reader);
            return config != null ? config : new JciConfig();
        }
    }

    public void save(Path path) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);

        // Use representer that skips class tags for cleaner YAML output
        Representer representer = new Representer(options);
        representer.getPropertyUtils().setSkipMissingProperties(true);
        representer.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        // Add class tags for all config classes so they serialize without !! prefix
        representer.addClassTag(JciConfig.class, org.yaml.snakeyaml.nodes.Tag.MAP);
        representer.addClassTag(ProjectConfig.class, org.yaml.snakeyaml.nodes.Tag.MAP);
        representer.addClassTag(BuildConfig.class, org.yaml.snakeyaml.nodes.Tag.MAP);
        representer.addClassTag(GitConfig.class, org.yaml.snakeyaml.nodes.Tag.MAP);
        representer.addClassTag(GitConfig.CommitConfig.class, org.yaml.snakeyaml.nodes.Tag.MAP);
        representer.addClassTag(GithubConfig.class, org.yaml.snakeyaml.nodes.Tag.MAP);
        representer.addClassTag(WorkflowsConfig.class, org.yaml.snakeyaml.nodes.Tag.MAP);
        representer.addClassTag(WorkflowsConfig.WorkflowToggle.class, org.yaml.snakeyaml.nodes.Tag.MAP);
        representer.addClassTag(WorkflowsConfig.TestWorkflowConfig.class, org.yaml.snakeyaml.nodes.Tag.MAP);
        representer.addClassTag(WorkflowsConfig.TestWorkflowConfig.CoverageConfig.class, org.yaml.snakeyaml.nodes.Tag.MAP);
        representer.addClassTag(SonarConfig.class, org.yaml.snakeyaml.nodes.Tag.MAP);
        representer.addClassTag(SonarConfig.QualityGateConfig.class, org.yaml.snakeyaml.nodes.Tag.MAP);
        representer.addClassTag(DockerConfig.class, org.yaml.snakeyaml.nodes.Tag.MAP);
        representer.addClassTag(BranchProtectionConfig.class, org.yaml.snakeyaml.nodes.Tag.MAP);
        representer.addClassTag(BranchProtectionConfig.BranchRules.class, org.yaml.snakeyaml.nodes.Tag.MAP);

        Yaml yaml = new Yaml(representer, options);
        try (FileWriter writer = new FileWriter(path.toFile())) {
            yaml.dump(this, writer);
        }
    }

    // Getters and setters
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public ProjectConfig getProject() { return project; }
    public void setProject(ProjectConfig project) { this.project = project; }

    public BuildConfig getBuild() { return build; }
    public void setBuild(BuildConfig build) { this.build = build; }

    public GitConfig getGit() { return git; }
    public void setGit(GitConfig git) { this.git = git; }

    public GithubConfig getGithub() { return github; }
    public void setGithub(GithubConfig github) { this.github = github; }

    public WorkflowsConfig getWorkflows() { return workflows; }
    public void setWorkflows(WorkflowsConfig workflows) { this.workflows = workflows; }

    public SonarConfig getSonar() { return sonar; }
    public void setSonar(SonarConfig sonar) { this.sonar = sonar; }

    public DockerConfig getDocker() { return docker; }
    public void setDocker(DockerConfig docker) { this.docker = docker; }

    public BranchProtectionConfig getBranchProtection() { return branchProtection; }
    public void setBranchProtection(BranchProtectionConfig branchProtection) { this.branchProtection = branchProtection; }

    // Nested config classes
    public static class ProjectConfig {
        private String name = "";
        private String description = "";

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class BuildConfig {
        private String tool = "maven"; // maven or gradle
        private String javaVersion = "21";

        public String getTool() { return tool; }
        public void setTool(String tool) { this.tool = tool; }
        public String getJavaVersion() { return javaVersion; }
        public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }
    }

    public static class GitConfig {
        private String mainBranch = "main";
        private CommitConfig commit = new CommitConfig();

        public String getMainBranch() { return mainBranch; }
        public void setMainBranch(String mainBranch) { this.mainBranch = mainBranch; }
        public CommitConfig getCommit() { return commit; }
        public void setCommit(CommitConfig commit) { this.commit = commit; }

        public static class CommitConfig {
            private boolean conventional = true;
            private boolean sign = false;

            public boolean isConventional() { return conventional; }
            public void setConventional(boolean conventional) { this.conventional = conventional; }
            public boolean isSign() { return sign; }
            public void setSign(boolean sign) { this.sign = sign; }
        }
    }

    public static class GithubConfig {
        private String owner = "";
        private String repo = "";

        public String getOwner() { return owner; }
        public void setOwner(String owner) { this.owner = owner; }
        public String getRepo() { return repo; }
        public void setRepo(String repo) { this.repo = repo; }
    }

    public static class WorkflowsConfig {
        private WorkflowToggle build = new WorkflowToggle(true);
        private TestWorkflowConfig test = new TestWorkflowConfig();
        private WorkflowToggle sonar = new WorkflowToggle(true);
        private WorkflowToggle docker = new WorkflowToggle(true);

        public WorkflowToggle getBuild() { return build; }
        public void setBuild(WorkflowToggle build) { this.build = build; }
        public TestWorkflowConfig getTest() { return test; }
        public void setTest(TestWorkflowConfig test) { this.test = test; }
        public WorkflowToggle getSonar() { return sonar; }
        public void setSonar(WorkflowToggle sonar) { this.sonar = sonar; }
        public WorkflowToggle getDocker() { return docker; }
        public void setDocker(WorkflowToggle docker) { this.docker = docker; }

        public static class WorkflowToggle {
            private boolean enabled = true;

            public WorkflowToggle() {}
            public WorkflowToggle(boolean enabled) { this.enabled = enabled; }

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
        }

        public static class TestWorkflowConfig {
            private boolean enabled = true;
            private CoverageConfig coverage = new CoverageConfig();

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public CoverageConfig getCoverage() { return coverage; }
            public void setCoverage(CoverageConfig coverage) { this.coverage = coverage; }

            public static class CoverageConfig {
                private boolean enabled = true;
                private int minCoverage = 80;

                public boolean isEnabled() { return enabled; }
                public void setEnabled(boolean enabled) { this.enabled = enabled; }
                public int getMinCoverage() { return minCoverage; }
                public void setMinCoverage(int minCoverage) { this.minCoverage = minCoverage; }
            }
        }
    }

    public static class SonarConfig {
        private String organization = "";
        private String projectKey = "";
        private QualityGateConfig qualityGate = new QualityGateConfig();

        public String getOrganization() { return organization; }
        public void setOrganization(String organization) { this.organization = organization; }
        public String getProjectKey() { return projectKey; }
        public void setProjectKey(String projectKey) { this.projectKey = projectKey; }
        public QualityGateConfig getQualityGate() { return qualityGate; }
        public void setQualityGate(QualityGateConfig qualityGate) { this.qualityGate = qualityGate; }

        public static class QualityGateConfig {
            private boolean wait = true;
            private int timeout = 300;

            public boolean isWait() { return wait; }
            public void setWait(boolean wait) { this.wait = wait; }
            public int getTimeout() { return timeout; }
            public void setTimeout(int timeout) { this.timeout = timeout; }
        }
    }

    public static class DockerConfig {
        private String registry = "ghcr.io";
        private String imageName = "";
        private int port = 8080;

        public String getRegistry() { return registry; }
        public void setRegistry(String registry) { this.registry = registry; }
        public String getImageName() { return imageName; }
        public void setImageName(String imageName) { this.imageName = imageName; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
    }

    public static class BranchProtectionConfig {
        private BranchRules main = new BranchRules();

        public BranchRules getMain() { return main; }
        public void setMain(BranchRules main) { this.main = main; }

        public static class BranchRules {
            private boolean requirePullRequest = true;
            private int requiredApprovals = 1;
            private boolean dismissStaleReviews = true;
            private boolean requireStatusChecks = true;
            private List<String> statusChecks = List.of("build", "test", "SonarCloud Code Analysis");
            private boolean enforceAdmins = false;

            public boolean isRequirePullRequest() { return requirePullRequest; }
            public void setRequirePullRequest(boolean requirePullRequest) { this.requirePullRequest = requirePullRequest; }
            public int getRequiredApprovals() { return requiredApprovals; }
            public void setRequiredApprovals(int requiredApprovals) { this.requiredApprovals = requiredApprovals; }
            public boolean isDismissStaleReviews() { return dismissStaleReviews; }
            public void setDismissStaleReviews(boolean dismissStaleReviews) { this.dismissStaleReviews = dismissStaleReviews; }
            public boolean isRequireStatusChecks() { return requireStatusChecks; }
            public void setRequireStatusChecks(boolean requireStatusChecks) { this.requireStatusChecks = requireStatusChecks; }
            public List<String> getStatusChecks() { return statusChecks; }
            public void setStatusChecks(List<String> statusChecks) { this.statusChecks = statusChecks; }
            public boolean isEnforceAdmins() { return enforceAdmins; }
            public void setEnforceAdmins(boolean enforceAdmins) { this.enforceAdmins = enforceAdmins; }
        }
    }
}
