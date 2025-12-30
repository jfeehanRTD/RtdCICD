# SonarCloud Setup Guide for Java Projects

This guide walks you through setting up SonarCloud (SonarQube Cloud) for your Java project to analyze **code quality** and **test coverage**.

## What SonarCloud Analyzes

### Code Quality (Application Quality)
| Category | What It Finds |
|----------|---------------|
| **Bugs** | Code that will likely fail at runtime |
| **Vulnerabilities** | Security issues (SQL injection, XSS, etc.) |
| **Code Smells** | Maintainability issues (complex methods, duplications) |
| **Security Hotspots** | Code that needs security review |

### Test Quality
| Metric | What It Measures |
|--------|------------------|
| **Line Coverage** | % of code lines executed by tests |
| **Branch Coverage** | % of if/else branches tested |
| **Condition Coverage** | % of boolean conditions tested |
| **Uncovered Lines** | Specific lines with no test coverage |

---

## Step 1: Create SonarCloud Account

1. Go to [sonarcloud.io](https://sonarcloud.io)
2. Click **"Log in"** → **"GitHub"**
3. Authorize SonarCloud to access your GitHub account
4. Select your organization (or create one)

---

## Step 2: Import Your Project

1. Click **"+"** in the top right → **"Analyze new project"**
2. Select your GitHub repository
3. Click **"Set Up"**
4. Choose **"With GitHub Actions"**
5. Copy your **Project Key** (e.g., `myorg_myproject`)

---

## Step 3: Generate a Token

1. Go to **My Account** → **Security** ([direct link](https://sonarcloud.io/account/security))
2. Under "Generate Tokens", enter a name (e.g., `github-actions`)
3. Click **"Generate"**
4. **Copy the token immediately** (you won't see it again!)

---

## Step 4: Add Token to GitHub

1. Go to your GitHub repository
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **"New repository secret"**
4. Name: `SONAR_TOKEN`
5. Value: Paste your token
6. Click **"Add secret"**

Or use the GitHub CLI:
```bash
gh secret set SONAR_TOKEN
# Paste your token when prompted
```

---

## Step 5: Configure Your Project

### For Maven Projects

Add the SonarCloud properties to your `pom.xml`:

```xml
<properties>
    <!-- SonarCloud configuration -->
    <sonar.organization>your-organization</sonar.organization>
    <sonar.host.url>https://sonarcloud.io</sonar.host.url>
    <sonar.projectKey>your-organization_your-repo</sonar.projectKey>

    <!-- Test coverage with JaCoCo -->
    <sonar.coverage.jacoco.xmlReportPaths>
        ${project.build.directory}/site/jacoco/jacoco.xml
    </sonar.coverage.jacoco.xmlReportPaths>
</properties>
```

Add JaCoCo plugin for test coverage:

```xml
<build>
    <plugins>
        <!-- JaCoCo for code coverage -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.12</version>
            <executions>
                <execution>
                    <id>prepare-agent</id>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>verify</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### For Gradle Projects

Add to `build.gradle`:

```groovy
plugins {
    id 'java'
    id 'jacoco'
    id 'org.sonarqube' version '5.1.0.4882'
}

sonar {
    properties {
        property 'sonar.organization', 'your-organization'
        property 'sonar.host.url', 'https://sonarcloud.io'
        property 'sonar.projectKey', 'your-organization_your-repo'
    }
}

jacocoTestReport {
    reports {
        xml.required = true
    }
}

// Run JaCoCo report after tests
tasks.named('test') {
    finalizedBy jacocoTestReport
}

// Run Sonar after JaCoCo report
tasks.named('sonar') {
    dependsOn jacocoTestReport
}
```

For Kotlin DSL (`build.gradle.kts`):

```kotlin
plugins {
    java
    jacoco
    id("org.sonarqube") version "5.1.0.4882"
}

sonar {
    properties {
        property("sonar.organization", "your-organization")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.projectKey", "your-organization_your-repo")
    }
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.sonar {
    dependsOn(tasks.jacocoTestReport)
}
```

---

## Step 6: Create GitHub Actions Workflow

### Maven Workflow

Create `.github/workflows/sonar.yml`:

```yaml
name: SonarCloud Analysis

on:
  push:
    branches: [main, develop]
  pull_request:
    types: [opened, synchronize, reopened]

jobs:
  sonarcloud:
    name: SonarCloud Scan
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0  # Full history for better analysis

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Cache SonarCloud packages
        uses: actions/cache@v4
        with:
          path: ~/.sonar/cache
          key: ${{ runner.os }}-sonar
          restore-keys: ${{ runner.os }}-sonar

      - name: Build and analyze
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: |
          mvn -B verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
            -Dsonar.projectKey=your-organization_your-repo \
            -Dsonar.organization=your-organization
```

### Gradle Workflow

Create `.github/workflows/sonar.yml`:

```yaml
name: SonarCloud Analysis

on:
  push:
    branches: [main, develop]
  pull_request:
    types: [opened, synchronize, reopened]

jobs:
  sonarcloud:
    name: SonarCloud Scan
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: gradle

      - name: Cache SonarCloud packages
        uses: actions/cache@v4
        with:
          path: ~/.sonar/cache
          key: ${{ runner.os }}-sonar
          restore-keys: ${{ runner.os }}-sonar

      - name: Build and analyze
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: ./gradlew build sonar --info
```

---

## Step 7: Configure Quality Gates

Quality Gates define the conditions your code must meet to pass.

### Default Quality Gate (Sonar way)

| Metric | Condition |
|--------|-----------|
| Coverage on new code | ≥ 80% |
| Duplicated lines on new code | ≤ 3% |
| Maintainability rating | A |
| Reliability rating | A |
| Security rating | A |

### Creating a Custom Quality Gate

1. Go to SonarCloud → **Quality Gates**
2. Click **"Create"**
3. Add conditions:

**For Test Quality:**
```
Coverage on New Code >= 80%
Coverage on Overall Code >= 70%
```

**For Code Quality:**
```
Bugs = 0
Vulnerabilities = 0
Security Hotspots Reviewed = 100%
Code Smells on New Code <= 10
Duplicated Lines on New Code <= 3%
```

4. Assign to your project under **Administration** → **Quality Gate**

---

## Step 8: Understanding the Reports

### Dashboard Overview

After your first analysis, you'll see:

```
┌─────────────────────────────────────────────────────────┐
│  Quality Gate: PASSED ✓                                 │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Bugs          Vulnerabilities    Code Smells           │
│    0                 0                 12               │
│    A                 A                  A               │
│                                                         │
│  Coverage      Duplications                             │
│   78.5%           2.1%                                  │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Pull Request Decoration

SonarCloud automatically comments on PRs with:
- Quality Gate status (pass/fail)
- New issues introduced
- Coverage on new code
- Link to full analysis

Example PR comment:
```
SonarCloud Quality Gate passed ✓

Coverage: 82.3% (target: 80%)
0 Bugs, 0 Vulnerabilities, 2 Code Smells

See full analysis →
```

---

## Step 9: Fixing Common Issues

### Low Test Coverage

SonarCloud shows exactly which lines aren't covered:

1. Click on **"Coverage"** in the dashboard
2. Browse to a file
3. Lines marked in **red** = not covered
4. Lines marked in **green** = covered

**Fix:** Write tests that execute the red lines.

### Code Smells

Common issues and fixes:

| Code Smell | Fix |
|------------|-----|
| Method too complex | Break into smaller methods |
| Duplicated code | Extract to shared method |
| Magic numbers | Use named constants |
| Empty catch block | Log the exception or rethrow |
| Unused imports | Remove them |
| Long parameter list | Use a parameter object |

### Security Vulnerabilities

| Vulnerability | Fix |
|---------------|-----|
| SQL Injection | Use parameterized queries |
| XSS | Escape user input |
| Hardcoded password | Use environment variables |
| Insecure random | Use SecureRandom |

---

## Quick Setup with jci

Instead of manual setup, use `jci`:

```bash
# Navigate to your project
cd your-java-project

# Initialize (detects Maven/Gradle)
jci init

# Configure SonarCloud (prompts for token)
jci sonar setup

# Generate workflows
jci workflow generate

# Push to GitHub
git add .
git commit -m "Add SonarCloud integration"
git push
```

This automatically:
- Detects your build tool
- Creates the workflow file
- Stores your token as a GitHub secret
- Configures project settings

---

## Example: Complete Maven Setup

Here's a complete working example:

### pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-app</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>${java.version}</maven.compiler.source>
        <maven.compiler.target>${java.version}</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- SonarCloud -->
        <sonar.organization>mycompany</sonar.organization>
        <sonar.host.url>https://sonarcloud.io</sonar.host.url>
        <sonar.projectKey>mycompany_my-app</sonar.projectKey>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.12</version>
                <executions>
                    <execution>
                        <id>prepare-agent</id>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>verify</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### .github/workflows/sonar.yml

```yaml
name: SonarCloud

on:
  push:
    branches: [main]
  pull_request:
    types: [opened, synchronize, reopened]

jobs:
  analyze:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Cache SonarCloud
        uses: actions/cache@v4
        with:
          path: ~/.sonar/cache
          key: ${{ runner.os }}-sonar

      - name: Build and Analyze
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: mvn -B verify sonar:sonar
```

---

## Useful Links

- [SonarCloud Dashboard](https://sonarcloud.io)
- [SonarCloud Documentation](https://docs.sonarsource.com/sonarqube-cloud/)
- [Quality Gates Guide](https://docs.sonarsource.com/sonarqube-cloud/improving/quality-gates/)
- [Java Analysis Rules](https://rules.sonarsource.com/java/)
- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)

---

## Need Help?

Run `jci sonar status` to check your quality gate:

```bash
$ jci sonar status
Checking quality gate status for: mycompany_my-app

Quality Gate: PASSED ✓
  Coverage: 82.3%
  Bugs: 0
  Vulnerabilities: 0
  Code Smells: 5
```
