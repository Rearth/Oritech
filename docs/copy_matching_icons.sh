#!/bin/bash

# Check if the destination directory is provided
if [ -z "$1" ]; then
    echo "Usage: $0 /path/to/destination/directory"
    exit 1
fi

set -x

# Define the source directory as the current directory
SOURCE_DIR="."

# Define the destination directory from the script's first argument
DEST_DIR="$1"

# Ensure the destination directory exists
mkdir -p "$DEST_DIR"

# Read the list of filenames from the deduplicated list file
while IFS= read -r filename; do
    # Construct the full path to the image in the source directory
    full_path=$(find "$SOURCE_DIR" -type f -name "$filename.png" 2>/dev/null)

    # Check if the file exists and copy it to the destination directory
    if [ -n "$full_path" ]; then
        echo "Copying $full_path to $DEST_DIR"
        cp "$full_path" "$DEST_DIR"
    else
        echo "File $filename.png not found in $SOURCE_DIR"
    fi
done < ./icons.txt