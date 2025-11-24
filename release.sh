#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Get current version
GRADLE_PROPS="app/gradle.properties"
CURRENT_VERSION_NAME=$(grep "^VERSION_NAME" "$GRADLE_PROPS" | cut -d'=' -f2 | tr -d ' ')
CURRENT_VERSION_CODE=$(grep "^VERSION_CODE" "$GRADLE_PROPS" | cut -d'=' -f2 | tr -d ' ')

echo -e "${BLUE}╔════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║        Cable Meter Release Script             ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}Current Version:${NC}"
echo -e "  Version Name: ${GREEN}$CURRENT_VERSION_NAME${NC}"
echo -e "  Version Code: ${GREEN}$CURRENT_VERSION_CODE${NC}"
echo ""

# Check if working directory is clean
if [[ -n $(git status -s) ]]; then
    echo -e "${RED}⚠️  Warning: You have uncommitted changes!${NC}"
    echo ""
    git status -s
    echo ""
    read -p "Do you want to continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${RED}Release cancelled.${NC}"
        exit 1
    fi
fi

# Get current branch
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo -e "${BLUE}Current branch: ${GREEN}$CURRENT_BRANCH${NC}"
echo ""

# Prompt for new version name
echo -e "${YELLOW}Enter new version name (e.g., 8.2.5):${NC}"
read -p "Version Name [$CURRENT_VERSION_NAME]: " NEW_VERSION_NAME
NEW_VERSION_NAME=${NEW_VERSION_NAME:-$CURRENT_VERSION_NAME}

# Prompt for new version code
SUGGESTED_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))
echo -e "${YELLOW}Enter new version code:${NC}"
read -p "Version Code [$SUGGESTED_VERSION_CODE]: " NEW_VERSION_CODE
NEW_VERSION_CODE=${NEW_VERSION_CODE:-$SUGGESTED_VERSION_CODE}

# Validate version code is a number
if ! [[ "$NEW_VERSION_CODE" =~ ^[0-9]+$ ]]; then
    echo -e "${RED}❌ Error: Version code must be a number!${NC}"
    exit 1
fi

# Confirm
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════${NC}"
echo -e "${YELLOW}Summary:${NC}"
echo -e "  Version Name: ${GREEN}$CURRENT_VERSION_NAME${NC} → ${GREEN}$NEW_VERSION_NAME${NC}"
echo -e "  Version Code: ${GREEN}$CURRENT_VERSION_CODE${NC} → ${GREEN}$NEW_VERSION_CODE${NC}"
echo -e "  Branch: ${GREEN}$CURRENT_BRANCH${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════${NC}"
echo ""
read -p "Proceed with release? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${RED}Release cancelled.${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}🚀 Starting release process...${NC}"
echo ""

# Update gradle.properties
echo -e "${YELLOW}[1/5]${NC} Updating $GRADLE_PROPS..."
cat > "$GRADLE_PROPS" << EOF
VERSION_NAME = $NEW_VERSION_NAME
VERSION_CODE = $NEW_VERSION_CODE
EOF
echo -e "${GREEN}✓${NC} Version updated"

# Stage the file
echo -e "${YELLOW}[2/5]${NC} Staging changes..."
git add "$GRADLE_PROPS"
echo -e "${GREEN}✓${NC} Changes staged"

# Commit
COMMIT_MSG="release: v$NEW_VERSION_NAME ($NEW_VERSION_CODE)"
echo -e "${YELLOW}[3/5]${NC} Committing: ${GREEN}$COMMIT_MSG${NC}"
git commit -m "$COMMIT_MSG"
echo -e "${GREEN}✓${NC} Committed"

# Create tag
TAG_NAME="v$NEW_VERSION_NAME"
echo -e "${YELLOW}[4/5]${NC} Creating tag: ${GREEN}$TAG_NAME${NC}"
git tag -a "$TAG_NAME" -m "Release $NEW_VERSION_NAME (build $NEW_VERSION_CODE)"
echo -e "${GREEN}✓${NC} Tag created"

# Ask if user wants to push
echo ""
echo -e "${YELLOW}[5/5]${NC} Push to remote?"
echo -e "  This will push the commit and tag to origin/$CURRENT_BRANCH"
read -p "Push now? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${BLUE}Pushing to remote...${NC}"
    git push origin "$CURRENT_BRANCH"
    git push origin "$TAG_NAME"
    echo -e "${GREEN}✓${NC} Pushed to remote"
    echo ""
    echo -e "${GREEN}╔════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║  🎉 Release $NEW_VERSION_NAME created successfully!  ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${BLUE}GitHub Actions will now build the release!${NC}"
    echo -e "${BLUE}Check: https://github.com/$(git remote get-url origin | sed 's/.*://;s/.git$//')/actions${NC}"
else
    echo ""
    echo -e "${YELLOW}⚠️  Changes committed locally but NOT pushed${NC}"
    echo -e "To push later, run:"
    echo -e "  ${GREEN}git push origin $CURRENT_BRANCH${NC}"
    echo -e "  ${GREEN}git push origin $TAG_NAME${NC}"
fi

echo ""
echo -e "${BLUE}Release Details:${NC}"
echo -e "  Tag: ${GREEN}$TAG_NAME${NC}"
echo -e "  Version: ${GREEN}$NEW_VERSION_NAME${NC}"
echo -e "  Build: ${GREEN}$NEW_VERSION_CODE${NC}"
echo -e "  Branch: ${GREEN}$CURRENT_BRANCH${NC}"
echo ""
