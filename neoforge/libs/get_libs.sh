#!/bin/bash

# Check if a directory is provided as an argument
if [ $# -ne 1 ]; then
    echo "Usage: $0 path/to/your/directory"
    exit 1
fi

# Directory to search for JAR files
DIRECTORY="$1"

# Find the latest non-source JAR file
latest_jar=$(find "$DIRECTORY" -type f -name '*.jar' ! -name '*-sources.jar' | \
    sort -V | tail -n 1)

# Check if a JAR file was found
if [[ -z "$latest_jar" ]]; then
    echo "No non-source JAR files found in the directory."
else
    # Copy the latest JAR to the current directory
    cp "$latest_jar" ./
    rm "$latest_jar"
    echo "Moved latest non-source JAR file to the current directory: $(basename "$latest_jar")"
fi
