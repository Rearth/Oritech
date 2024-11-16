#!/bin/bash

# Check for .mdx files and search for icon mentions

echo "" > icons.txt

find . -type f -name "*.mdx" | while read -r file; do
    # echo "Searching in: $file"
    grep -o 'oritech:[^ ]*' "$file" | sort | uniq >> icons.txt
done

cat icons.txt | tr -d '"' | tr -d ')' | sed 's/oritech://g' | sort | uniq > icons.txt