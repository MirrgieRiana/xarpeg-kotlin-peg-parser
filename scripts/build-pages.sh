#!/bin/bash
set -e

# Build Pages site with Jekyll from bundleRelease directory
# This script expects build/bundleRelease to exist with all content prepared

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BUNDLE_DIR="$PROJECT_ROOT/build/bundleRelease"
SITE_OUTPUT="${1:-$PROJECT_ROOT/build/site}"

if [ ! -d "$BUNDLE_DIR" ]; then
    echo "Error: build/bundleRelease directory not found"
    echo "Please run './gradlew bundleRelease' first"
    exit 1
fi

echo "Building Jekyll site from: $BUNDLE_DIR"
echo "Output directory: $SITE_OUTPUT"

# Check if bundler is available
if ! command -v bundle &> /dev/null; then
    echo "Error: Bundler is not installed"
    echo "Install with: gem install bundler"
    exit 1
fi

# Build with Jekyll using bundler
cd "$BUNDLE_DIR"
bundle exec jekyll build --destination "$SITE_OUTPUT" --verbose

echo "Jekyll build complete!"
echo "Site generated at: $SITE_OUTPUT"
