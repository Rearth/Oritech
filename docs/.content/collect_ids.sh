#!/bin/bash

# Check if arguments are provided
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <source_images_dir> <destination_images_dir>"
    echo "Example: $0 ./resources/assets/oritech/textures/block ./public/images"
    exit 1
fi

SOURCE_DIR="$1"
DEST_DIR="$2"

# Create destination directory if it doesn't exist
mkdir -p "$DEST_DIR"

echo "Scanning for IDs in markdown files..."
echo "-------------------------------------"

# Find all .md and .mdx files recursively
find . -type f \( -name "*.md" -o -name "*.mdx" \) | while read -r file; do

    # Extract the line starting with "id:" from the Frontmatter
    # We use grep to find the line, and sed to clean up "id:", quotes, and whitespace.
    id_line=$(grep "^id:" "$file" | head -n 1)

    if [ -n "$id_line" ]; then
        # Clean the ID: remove "id:", remove quotes (single or double), trim whitespace
        raw_id=$(echo "$id_line" | sed -E 's/^id:[[:space:]]*//; s/^["'\'']//; s/["'\'']$//')

        # Output the ID to console (as requested)
        echo "- $raw_id"

        # --- IMAGE COPYING LOGIC ---

        # 1. content after the colon (e.g., 'machine_plating_block' from 'oritech:machine_plating_block')
        image_name="${raw_id##*:}.png"

        # Check if the image exists in source folder
        if [ -f "$SOURCE_DIR/$image_name" ]; then
            cp "$SOURCE_DIR/$image_name" "$DEST_DIR/"
            # Optional: Uncomment below to see success logs
            # echo "  -> Copied image: $image_name"
        else
            # Try fallback: Check if the file exists with the full ID (e.g. oritech_thing.png)
            flat_name="${raw_id//:/_}.png"
            if [ -f "$SOURCE_DIR/$flat_name" ]; then
                 cp "$SOURCE_DIR/$flat_name" "$DEST_DIR/"
                 echo "  -> Found as flattened name: $flat_name"
            else
                 echo "  [!] Image not found: $image_name (checked in $SOURCE_DIR)" >&2
            fi
        fi
    fi
done

echo "-------------------------------------"
echo "Processing complete."