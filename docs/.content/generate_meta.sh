#!/bin/bash

# Ensure jq is installed
if ! command -v jq &> /dev/null; then
    echo "Error: 'jq' is not installed."
    exit 1
fi

# Function to extract title from Frontmatter or fallback to Filename
get_title() {
    local file="$1"
    local filename=$(basename "$file")

    # 1. If it's a directory, just use filename converted to Title Case
    if [ -d "$file" ]; then
        echo "$filename" | sed -E 's/[-_]/ /g' | awk '{for(i=1;i<=NF;i++){ $i=toupper(substr($i,1,1)) substr($i,2) }}1'
        return
    fi

    # 2. Try to find "title:" in the first 10 lines (YAML Frontmatter)
    # We strip quotes and extra whitespace
    local fm_title
    fm_title=$(grep -m 1 "^title:" "$file" | head -n 1 | sed -E 's/^title:[[:space:]]*//; s/^["'\'']//; s/["'\'']$//')

    if [ -n "$fm_title" ]; then
        echo "$fm_title"
    else
        # 3. Fallback: Title Case the filename (minus extension)
        local clean="${filename%.*}"
        echo "$clean" | sed -E 's/[-_]/ /g' | awk '{for(i=1;i<=NF;i++){ $i=toupper(substr($i,1,1)) substr($i,2) }}1'
    fi
}

process_directory() {
    local dir="$1"

    # Push to directory
    pushd "$dir" > /dev/null || return

    # Check for empty directory
    shopt -s nullglob
    files=(*)
    if [ ${#files[@]} -eq 0 ]; then
        popd > /dev/null
        return
    fi

    # Create a temporary JSON object representing the CURRENT file state
    # We build this incrementally to avoid argument list too long errors
    echo "{}" > .curr_state.json.tmp

    for item in *; do
        if [[ "$item" == "_meta.json" ]] || [[ "$item" == .* ]] || [[ "$item" == *.tmp ]]; then
            continue
        fi

        # Get the title (Frontmatter > Filename)
        title=$(get_title "$item")

        # Add to current state json safely using jq
        jq --arg k "$item" --arg v "$title" '. + {($k): $v}' .curr_state.json.tmp > .curr_state.json.tmp.2
        mv .curr_state.json.tmp.2 .curr_state.json.tmp
    done

    # Prepare existing _meta.json (or empty object if none)
    if [ -f "_meta.json" ]; then
        cp "_meta.json" .old_meta.json.tmp
    else
        echo "{}" > .old_meta.json.tmp
    fi

    # --- MERGE LOGIC WITH JQ ---
    # 1. $old: The existing _meta.json (Source of truth for ORDER and MANUAL TITLES)
    # 2. $new: The scan of current files (Source of truth for EXISTENCE and NEW TITLES)

    jq -n --slurpfile old .old_meta.json.tmp --slurpfile new .curr_state.json.tmp '
        ($old[0] // {}) as $o |
        ($new[0] // {}) as $n |

        # A. Get keys that exist in BOTH (preserve order from OLD)
        ($o | keys_unsorted | map(select($n[.] != null))) as $kept_keys |

        # B. Get keys that are ONLY in NEW (append to end)
        ($n | keys_unsorted | map(select($o[.] == null))) as $new_keys |

        # Combine keys: Old Kept + New
        reduce ($kept_keys + $new_keys)[] as $k (
            {};
            . + {
                # Value logic: If defined in Old, keep Old. Else use New.
                ($k): ($o[$k] // $n[$k])
            }
        )
    ' > _meta.json

    # Clean up temp files
    rm .curr_state.json.tmp .old_meta.json.tmp

    # Output status
    echo "Updated: $(pwd)/_meta.json"

    # Recurse into subdirectories
    for item in *; do
        if [ -d "$item" ]; then
            process_directory "$item"
        fi
    done

    popd > /dev/null
}

echo "Starting recursive update..."
process_directory "."
echo "Done."