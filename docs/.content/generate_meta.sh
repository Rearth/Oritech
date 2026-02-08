#!/bin/bash

# Function to convert string to Title Case
to_title_case() {
    echo "$1" | awk '{for(i=1;i<=NF;i++){ $i=toupper(substr($i,1,1)) substr($i,2) }}1'
}

generate_meta_recursive() {
    local current_dir="$1"

    # Push to directory to make file handling easier
    pushd "$current_dir" > /dev/null || return

    # Enable nullglob to handle empty directories gracefully
    shopt -s nullglob

    # Array to hold json entries
    json_entries=()

    # Iterate over all files and folders
    for item in *; do
        # Skip _meta.json itself and any hidden files
        if [[ "$item" == "_meta.json" ]] || [[ "$item" == .* ]]; then
            continue
        fi

        # LOGIC FOR DISPLAY NAME (The Value)
        # 1. Remove extension (only for the display name logic)
        clean_name="${item%.*}"
        # 2. Replace underscores and dashes with spaces
        clean_name="${clean_name//[-_]/ }"
        # 3. Convert to Title Case
        pretty_name=$(to_title_case "$clean_name")

        # Add to entries array: "filename": "Pretty Name"
        json_entries+=("\"$item\": \"$pretty_name\"")

        # RECURSION: If it is a directory, run the function inside it
        if [[ -d "$item" ]]; then
            generate_meta_recursive "$item"
        fi
    done

    # If we found items, write the _meta.json
    if [ ${#json_entries[@]} -gt 0 ]; then
        echo "{" > _meta.json

        # Loop through array to print with commas, except the last one
        last_idx=$(( ${#json_entries[@]} - 1 ))
        for i in "${!json_entries[@]}"; do
            if [[ $i -eq $last_idx ]]; then
                echo "  ${json_entries[$i]}" >> _meta.json
            else
                echo "  ${json_entries[$i]}," >> _meta.json
            fi
        done

        echo "}" >> _meta.json
        echo "Generated _meta.json in: $(pwd)"
    fi

    # Pop back to previous directory
    popd > /dev/null
}

# Start script from current directory
echo "Starting generation..."
generate_meta_recursive "."
echo "Done."