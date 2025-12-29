#!/usr/bin/env bash
#
# Install script for jci - Java CI/CD Automation CLI Tool
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INSTALL_DIR="${INSTALL_DIR:-/usr/local/bin}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Installing jci - Java CI/CD Automation CLI Tool${NC}"
echo ""

# Check prerequisites
echo "Checking prerequisites..."

if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java is not installed${NC}"
    echo "Please install Java 21+: brew install openjdk@21"
    exit 1
fi
echo -e "  ${GREEN}✓${NC} Java found"

if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Error: Maven is not installed${NC}"
    echo "Please install Maven: brew install maven"
    exit 1
fi
echo -e "  ${GREEN}✓${NC} Maven found"

# Build the project
echo ""
echo "Building project..."
cd "$SCRIPT_DIR"
mvn package -DskipTests -q

if [[ ! -f "$SCRIPT_DIR/target/jci-1.0.0-SNAPSHOT.jar" ]]; then
    echo -e "${RED}Error: Build failed${NC}"
    exit 1
fi
echo -e "  ${GREEN}✓${NC} Build successful"

# Make wrapper script executable
chmod +x "$SCRIPT_DIR/jci"

# Install to PATH
echo ""
echo "Installing to $INSTALL_DIR..."

if [[ -w "$INSTALL_DIR" ]]; then
    ln -sf "$SCRIPT_DIR/jci" "$INSTALL_DIR/jci"
    echo -e "  ${GREEN}✓${NC} Installed to $INSTALL_DIR/jci"
else
    echo -e "${YELLOW}Need sudo to install to $INSTALL_DIR${NC}"
    sudo ln -sf "$SCRIPT_DIR/jci" "$INSTALL_DIR/jci"
    echo -e "  ${GREEN}✓${NC} Installed to $INSTALL_DIR/jci"
fi

echo ""
echo -e "${GREEN}Installation complete!${NC}"
echo ""
echo "Usage:"
echo "  jci --help              Show help"
echo "  jci init                Initialize a project"
echo "  jci workflow generate   Generate GitHub Actions workflows"
echo "  jci docker generate     Generate Dockerfile"
echo "  jci sonar setup         Configure SonarCloud"
echo "  jci protect apply       Apply branch protection rules"
echo ""
echo "Run 'jci --help' to see all available commands."
