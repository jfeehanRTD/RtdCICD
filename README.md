# jci - Java CI/CD Automation Tool

A command-line tool that automatically sets up professional CI/CD pipelines for Java projects. No DevOps experience required!

## What Does This Tool Do?

When you push code to GitHub, wouldn't it be nice if someone automatically:
- **Built** your project to check for compile errors?
- **Ran all your tests** to make sure nothing broke?
- **Checked your code quality** and found potential bugs?
- **Created a Docker container** ready to deploy?

That's exactly what `jci` sets up for you!

---

## Quick Start (5 Minutes)

### Step 1: Install jci

```bash
# Clone this repo
git clone https://github.com/jfeehanRTD/RtdCICD.git
cd RtdCICD

# Install (adds 'jci' command to your system)
./install.sh
```

### Step 2: Set Up Your Java Project

```bash
# Go to your Java project
cd /path/to/your-java-project

# Initialize jci (auto-detects Maven or Gradle)
jci init

# Generate GitHub Actions workflows
jci workflow generate

# Commit and push
git add .
git commit -m "Add CI/CD workflows"
git push
```

**That's it!** GitHub will now automatically build and test your code on every push.

---

## What is CI/CD? (The Basics)

### CI = Continuous Integration
Every time you push code, automated checks run to catch problems early:
- Does the code compile?
- Do all tests pass?
- Are there code quality issues?

### CD = Continuous Delivery/Deployment
After checks pass, your code can be automatically:
- Packaged into a Docker container
- Deployed to a server
- Released to users

### Why Does This Matter?

| Without CI/CD | With CI/CD |
|---------------|------------|
| "Works on my machine" bugs | Catches issues immediately |
| Manual testing before merge | Automated test runs |
| Forgot to run tests | Tests always run |
| Hours debugging production | Problems caught early |
| Manual deployments | Automated, consistent deploys |

---

## What is SonarCloud?

SonarCloud is a free service that automatically reviews your code and finds:

- **Bugs** - Code that will probably fail
- **Security Issues** - Vulnerabilities hackers could exploit
- **Code Smells** - Code that works but is hard to maintain
- **Duplications** - Copy-pasted code that should be refactored
- **Test Coverage** - How much of your code is tested

### Example SonarCloud Report

```
Quality Gate: PASSED ✓

Bugs: 0
Vulnerabilities: 0
Code Smells: 3 (minor)
Coverage: 78%
Duplications: 2.1%
```

It's like having a senior developer review every pull request!

---

## Commands Reference

### `jci init`
Scans your project and creates a configuration file.

```bash
cd my-java-project
jci init
```

**What it does:**
- Detects if you use Maven (`pom.xml`) or Gradle (`build.gradle`)
- Finds your Java version
- Detects your GitHub repository
- Creates `.jci.yaml` configuration file

### `jci workflow generate`
Creates GitHub Actions workflow files.

```bash
jci workflow generate
```

**Files created in `.github/workflows/`:**

| File | What It Does |
|------|--------------|
| `build.yml` | Compiles your code on every push |
| `test.yml` | Runs all tests, reports coverage |
| `sonar.yml` | Analyzes code quality with SonarCloud |
| `docker-publish.yml` | Builds Docker image, pushes to GitHub registry |

### `jci docker generate`
Creates an optimized Dockerfile for your Java app.

```bash
jci docker generate
```

**What you get:**
- Multi-stage build (small final image)
- Proper caching for fast rebuilds
- Non-root user (security best practice)
- Health check endpoint

### `jci sonar setup`
Configures SonarCloud integration.

```bash
jci sonar setup
```

**What it does:**
1. Prompts for your SonarCloud token
2. Validates the token works
3. Stores it securely in GitHub Secrets
4. Generates the analysis workflow

### `jci commit`
Smart commit with conventional commit format.

```bash
jci commit -m "add login feature" -t feat -p
```

**Options:**
- `-m` - Commit message
- `-t` - Type: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`
- `-p` - Push after commit

### `jci protect apply`
Sets up branch protection rules on GitHub.

```bash
jci protect apply
```

**What it configures:**
- Require pull request before merging
- Require 1 approval
- Require CI checks to pass
- Dismiss stale reviews when new commits pushed

---

## Configuration File (.jci.yaml)

After running `jci init`, you'll have a `.jci.yaml` file:

```yaml
# Build settings
build:
  tool: maven          # or "gradle"
  javaVersion: "21"    # Java version to use

# Git settings
git:
  mainBranch: main     # Your main branch name

# GitHub repository
github:
  owner: your-username
  repo: your-repo-name

# Which workflows to generate
workflows:
  build:
    enabled: true      # Compile check
  test:
    enabled: true      # Run tests
    coverage:
      minCoverage: 80  # Fail if coverage below 80%
  sonar:
    enabled: true      # Code quality analysis
  docker:
    enabled: true      # Docker image build

# SonarCloud settings
sonar:
  organization: your-org
  projectKey: your-org_your-repo

# Docker settings
docker:
  port: 8080           # Port your app runs on
```

---

## Setting Up SonarCloud (First Time)

### 1. Create a SonarCloud Account

1. Go to [sonarcloud.io](https://sonarcloud.io)
2. Click "Log in" → "GitHub"
3. Authorize SonarCloud to access your GitHub

### 2. Create a Project

1. Click "+" → "Analyze new project"
2. Select your repository
3. Choose "GitHub Actions" as the analysis method
4. Copy your project key (looks like `your-org_your-repo`)

### 3. Generate a Token

1. Go to [sonarcloud.io/account/security](https://sonarcloud.io/account/security)
2. Generate a new token
3. Copy it (you won't see it again!)

### 4. Run jci sonar setup

```bash
cd your-project
jci sonar setup
```

Paste your token when prompted. Done!

---

## Understanding GitHub Actions

When you push code, GitHub runs your workflows automatically. Here's what happens:

```
You push code to GitHub
        ↓
GitHub sees .github/workflows/*.yml files
        ↓
GitHub spins up a fresh Linux server
        ↓
Runs the steps in your workflow:
   1. Checkout your code
   2. Install Java
   3. Build with Maven/Gradle
   4. Run tests
   5. Upload results
        ↓
Shows ✓ or ✗ on your commit/PR
```

### Viewing Results

1. Go to your repo on GitHub
2. Click "Actions" tab
3. See all workflow runs
4. Click a run to see details

### Status Badges

Add this to your README to show build status:

```markdown
![Build](https://github.com/YOUR-USERNAME/YOUR-REPO/actions/workflows/build.yml/badge.svg)
![Tests](https://github.com/YOUR-USERNAME/YOUR-REPO/actions/workflows/test.yml/badge.svg)
```

---

## Docker Basics

### What is Docker?

Docker packages your app and everything it needs to run into a "container". This container runs the same way everywhere - your laptop, a server, the cloud.

### The Generated Dockerfile

`jci docker generate` creates a Dockerfile that:

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk AS builder
# ... builds your JAR file

# Stage 2: Run
FROM eclipse-temurin:21-jre
# ... runs your app (smaller image, no build tools)
```

### Building and Running Locally

```bash
# Build the Docker image
docker build -t my-app .

# Run it
docker run -p 8080:8080 my-app

# Visit http://localhost:8080
```

### GitHub Container Registry

The `docker-publish.yml` workflow automatically:
1. Builds your Docker image
2. Pushes it to `ghcr.io/your-username/your-repo`
3. Tags it with the git SHA and branch name

Pull your image anywhere:
```bash
docker pull ghcr.io/your-username/your-repo:main
```

---

## Troubleshooting

### "Configuration not found"
Run `jci init` first to create `.jci.yaml`

### "GitHub CLI not authenticated"
```bash
gh auth login
```

### "SONAR_TOKEN secret not found"
Run `jci sonar setup` to configure SonarCloud

### Build fails on GitHub but works locally
- Check Java version matches (see `.jci.yaml`)
- Make sure all dependencies are in `pom.xml` or `build.gradle`
- Check the Actions log for specific errors

### Tests pass locally but fail on GitHub
- Tests might depend on local files or environment
- Use `@TempDir` for file-based tests
- Don't hardcode paths

---

## Glossary

| Term | Meaning |
|------|---------|
| **CI/CD** | Continuous Integration / Continuous Delivery |
| **Workflow** | A set of automated steps that run on GitHub |
| **Pipeline** | Another word for workflow |
| **Action** | A single step in a workflow |
| **Artifact** | A file produced by a build (JAR, Docker image) |
| **Secret** | A hidden value (like passwords or tokens) |
| **Quality Gate** | Rules that code must pass (coverage, no bugs) |
| **Code Coverage** | Percentage of code executed by tests |
| **PR** | Pull Request - a request to merge code |
| **GHCR** | GitHub Container Registry (Docker images) |

---

## Getting Help

- **jci help**: `jci --help` or `jci <command> --help`
- **GitHub Actions docs**: [docs.github.com/actions](https://docs.github.com/actions)
- **SonarCloud docs**: [docs.sonarcloud.io](https://docs.sonarcloud.io)

---

## Contributing

Found a bug or want a feature? Open an issue at:
https://github.com/jfeehanRTD/RtdCICD/issues

---

Built with Java 21, Picocli, and Mustache templates.
