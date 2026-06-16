#!/bin/bash

# ================= CONFIGURATION =================

# --- 1. THE SOURCE DEFINITIONS ---

# A. Source Primary (The Original Orange)
SOURCE_PRIMARY=(
    "#c36c2b" # 1. Main
    "#b25020" # 2. Dark
    "#9c3215" # 3. Darker
    "#921507" # 4. Darkest Red
    "#d18b33" # 5. Highlight
    "#ae6531" # 6. Mid
    "#9b4c28" # 7. Mid-Dark
    "#8f391a" # 8. Shadow
    "#b38040" # 9. Muted
    "#7c2e13" # 10. Deep Shadow
)

# B. Source Secondary (The Blue parts found on machines)
SOURCE_ACCENTS=(
    "#252f64" # 1. Mid-Dark Blue
    "#1b1b4b" # 2. Darkest Blue
    "#2b4178" # 3. Mid Blue
    "#2d4c81" # 4. Light Blue
)

# C. Source Tech Door (Off-hue warm gradient)
SOURCE_TECH_DOOR_PRIMARY=(
    "#842a3f" # 1. Deep Maroon
    "#c6544c" # 2. Burnt Coral
    "#e87f4a" # 3. Orange Core
    "#f5ad69" # 4. Warm Highlight
    "#ffdc89" # 5. Pale Edge Highlight
)

# --- 2. THE TARGET PALETTES (PRIMARY) ---

# Variant: Diamond (Sci-Fi / Tech Blue)
COLORS_DIAMOND=(
    "#2bc3db" "#209ab2" "#157c9c" "#075292" "#33eed1"
    "#318cae" "#28739b" "#1a5c8f" "#40a0b3" "#184d5c"
)

TECH_DOOR_DIAMOND=(
    "#1f6982" "#3092b0" "#49b8d0" "#7fd3dc" "#b7f1ef"
)

# Variant: Emerald (Forest / Nature)
COLORS_CAMO=(
    "#62b33d" "#5c953c" "#417030" "#355d35" "#7cd156"
    "#5e9e3e" "#4e8236" "#1a4c39" "#699652" "#0f2e22"
)

TECH_DOOR_CAMO=(
    "#4e5a2a" "#6f8440" "#8fa84d" "#b7c86d" "#dce29a"
)

# Variant: Purpur (End City / Berry)
COLORS_FLUXITE=(
    "#e16971" "#c3527e" "#9d4387" "#8d317e" "#f0868e"
    "#d15d77" "#b04a82" "#7a276d" "#cc7a85" "#551b4c"
)

TECH_DOOR_FLUXITE=(
    "#7f3e75" "#a8548b" "#ca6799" "#e291ba" "#f4c7dd"
)

# Variant: Iron (Clean / Laboratory)
COLORS_WHITE=(
    "#eeeeee" "#dcdcdc" "#bdbdbd" "#9e9e9e" "#ffffff"
    "#e0e0e0" "#cfcfcf" "#bfbfbf" "#f5f5f5" "#737373"
)

TECH_DOOR_WHITE=(
    "#7f838a" "#b0b0b4" "#d1c4b8" "#e8dccd" "#fff3df"
)

# Variant: Industrial (Yellow / Construction)
COLORS_INDUSTRIAL=(
    "#ffce2b" "#dcb225" "#b5921f" "#8c7118" "#ffe680"
    "#e6bc29" "#cfa925" "#5e4c10" "#9e8e50" "#362b09"
)

TECH_DOOR_INDUSTRIAL=(
    "#7b4820" "#b56b24" "#db9328" "#f0ba4d" "#ffe08b"
)

# Variant: Netherite (Dark Grey / Heavy Armor)
COLORS_NETHERITE=(
    "#554f4f" "#454040" "#383232" "#2b2525" "#706a6a"
    "#4d4848" "#403a3a" "#1f1b1b" "#595252" "#120f0f"
)

TECH_DOOR_NETHERITE=(
    "#43343b" "#655257" "#847074" "#ab9494" "#dbc6b5"
)

# Variant: Redstone (Vibrant Red / Power)
COLORS_REDSTONE=(
    "#cf2f2f" "#b01e1e" "#911212" "#700808" "#f05959"
    "#bf2626" "#a11919" "#4f0303" "#b86060" "#2e0101"
)

TECH_DOOR_REDSTONE=(
    "#7a1828" "#b32d36" "#df453f" "#f6784c" "#ffbe6a"
)

# Variant: Sculk (Deep Dark / Vibrant Cyan & Black)
COLORS_SCULK=(
    "#0b3842" "#082930" "#051a21" "#031014" "#00b0ba"
    "#124d59" "#09303b" "#04161c" "#2d6e75" "#020b0d"
)

TECH_DOOR_SCULK=(
    "#14233c" "#1f4d68" "#1f798d" "#45b2b6" "#9ee3d9"
)

# --- 3. THE TARGET ACCENTS (SECONDARY) ---

# Diamond Variant -> Uses ANTIQUE BRASS / DARK COPPER accents
ACCENT_DIAMOND=(
    "#966f33" # Mid-Dark Brass
    "#5e4215" # Darkest Bronze
    "#bfa060" # Mid Brass
    "#dbc591" # Light Brass
)

# Emerald Variant -> Uses SPRUCE WOOD accents
ACCENT_CAMO=(
    "#704d30" "#4a321e" "#8f633d" "#ad7b50"
)

# Purpur Variant -> Uses WARPED HYPHAE (Teal) accents
ACCENT_FLUXITE=(
    "#1e8585" "#0f4242" "#3aa3a3" "#5cdbdb"
)

# Iron Variant -> Uses LAPIS (Original Blue) accents
ACCENT_WHITE=(
    "#252f64" "#1b1b4b" "#2b4178" "#2d4c81"
)

# Industrial Variant -> Uses OXIDIZED RUST accents
ACCENT_INDUSTRIAL=(
    "#8c4b3a" # Mid-Dark Rust
    "#592d21" # Darkest Rust
    "#b06652" # Mid Rust
    "#cc7a63" # Light Rust
)

# Netherite Variant -> Uses GOLD accents
ACCENT_NETHERITE=(
    "#f2bc32" "#98610a" "#fec93b" "#fff38b"
)

# Redstone Variant -> Uses QUARTZ/BRASS accents
ACCENT_REDSTONE=(
    "#f2cf6f" "#b59640" "#ffe5a0" "#fff4d6"
)

# Sculk Variant -> Uses AMETHYST / ECHO SHARD accents
# (Using purple to contrast against the dark teal body)
ACCENT_SCULK=(
    "#564291" "#362561" "#7762b5" "#a393d6"
)

# --- 4. IGNORE LIST ---
# Add filenames here that you want the script to skip completely.
IGNORE_LIST=(
    "tech_door.png"
    "particle_collector_block.png"
    "augment_application_block.png"
    "big_solar_panel_block.png"
    "enchanter_block.png"
    "enchantment_catalyst_block.png"
    "pipe_booster_block.png"
)

# --- 5. SETTINGS ---

VARIANTS=("diamond" "camo" "fluxite" "white" "industrial" "netherite" "redstone" "sculk")

FUZZ_AMOUNT="2%"
COMPRESSION_ARGS=("-strip" "-quality" "95")
OUTPUT_DIR="colored"
EXCLUDE_SUFFIX="_glowmask"

# =================================================

if [ "$#" -eq 0 ]; then
    echo "Usage: $0 all | <file1.png> [file2.png ...]"
    exit 1
fi

PROCESS_ALL=false
TARGET_FILES=()

if [ "$1" = "all" ]; then
    if [ "$#" -ne 1 ]; then
        echo "Error: 'all' cannot be combined with explicit filenames."
        exit 1
    fi

    PROCESS_ALL=true
    TARGET_FILES=( *.png )
else
    TARGET_FILES=("$@")
fi

# Check ImageMagick
if command -v magick &> /dev/null; then
    IM_CMD="magick"
elif command -v convert &> /dev/null; then
    IM_CMD="convert"
else
    echo "Error: ImageMagick is not installed."
    exit 1
fi

# Create output directory
if [ ! -d "$OUTPUT_DIR" ]; then
    mkdir -p "$OUTPUT_DIR"
fi

echo "Starting multi-color processing..."

# LOOP 1: Iterate through PNG files
for img in "${TARGET_FILES[@]}"; do
    
    # 1. Basic Validation
    [ -e "$img" ] || continue

    if [[ "$img" != *.png ]]; then
        echo "Skipping (Not a PNG): $img"
        continue
    fi
    
    # 2. Skip files with glowmask suffix
    if [[ "$img" == *"${EXCLUDE_SUFFIX}.png" ]]; then
        continue
    fi
    
    # 3. Skip files in the IGNORE_LIST when processing all files
    if [ "$PROCESS_ALL" = true ]; then
        skip_file=false
        for ignored in "${IGNORE_LIST[@]}"; do
            if [[ "$img" == "$ignored" ]]; then
                echo "Skipping (Ignored): $img"
                skip_file=true
                break
            fi
        done
        if [ "$skip_file" = true ]; then
            continue
        fi
    fi
    
    # 4. Skip files that look like generated variants
    already_processed=false
    for v in "${VARIANTS[@]}"; do
        if [[ "$img" == *"_${v}.png" ]]; then
            already_processed=true
        fi
    done
    if [ "$already_processed" = true ]; then
        continue
    fi

    filename=$(basename "$img" .png)
    echo ">> Source: $img"

    # LOOP 2: Iterate through requested variants
    for variant in "${VARIANTS[@]}"; do
        
        # 1. Select Arrays
        case $variant in
            "diamond")
                TGT_PRIM=("${COLORS_DIAMOND[@]}")
                TGT_DOOR=("${TECH_DOOR_DIAMOND[@]}")
                TGT_ACNT=("${ACCENT_DIAMOND[@]}")
                ;;
            "camo")
                TGT_PRIM=("${COLORS_CAMO[@]}")
                TGT_DOOR=("${TECH_DOOR_CAMO[@]}")
                TGT_ACNT=("${ACCENT_CAMO[@]}")
                ;;
            "fluxite")
                TGT_PRIM=("${COLORS_FLUXITE[@]}")
                TGT_DOOR=("${TECH_DOOR_FLUXITE[@]}")
                TGT_ACNT=("${ACCENT_FLUXITE[@]}")
                ;;
            "white")
                TGT_PRIM=("${COLORS_WHITE[@]}")
                TGT_DOOR=("${TECH_DOOR_WHITE[@]}")
                TGT_ACNT=("${ACCENT_WHITE[@]}")
                ;;
            "industrial")
                TGT_PRIM=("${COLORS_INDUSTRIAL[@]}")
                TGT_DOOR=("${TECH_DOOR_INDUSTRIAL[@]}")
                TGT_ACNT=("${ACCENT_INDUSTRIAL[@]}")
                ;;
            "netherite")
                TGT_PRIM=("${COLORS_NETHERITE[@]}")
                TGT_DOOR=("${TECH_DOOR_NETHERITE[@]}")
                TGT_ACNT=("${ACCENT_NETHERITE[@]}")
                ;;
            "redstone")
                TGT_PRIM=("${COLORS_REDSTONE[@]}")
                TGT_DOOR=("${TECH_DOOR_REDSTONE[@]}")
                TGT_ACNT=("${ACCENT_REDSTONE[@]}")
                ;;
            "sculk")
                TGT_PRIM=("${COLORS_SCULK[@]}")
                TGT_DOOR=("${TECH_DOOR_SCULK[@]}")
                TGT_ACNT=("${ACCENT_SCULK[@]}")
                ;;
            *)
                echo "Unknown variant: $variant"
                continue
                ;;
        esac

        output_name="${OUTPUT_DIR}/${filename}_${variant}.png"
        
        # 2. Build ImageMagick Arguments
        args=()
        args+=("-fuzz" "$FUZZ_AMOUNT")

        # A. Swap Primary Colors (Orange -> Variant)
        count_prim=${#SOURCE_PRIMARY[@]}
        for (( i=0; i<count_prim; i++ )); do
            src="${SOURCE_PRIMARY[$i]}"
            tgt="${TGT_PRIM[$i]}"
            [ -z "$tgt" ] && continue
            args+=("-fill" "$tgt" "-opaque" "$src")
        done

        # B. Swap Tech Door Colors (Off-hue warm ramp -> Variant-specific off-hue ramp)
        count_door=${#SOURCE_TECH_DOOR_PRIMARY[@]}
        for (( i=0; i<count_door; i++ )); do
            src="${SOURCE_TECH_DOOR_PRIMARY[$i]}"
            tgt="${TGT_DOOR[$i]}"
            [ -z "$tgt" ] && continue
            args+=("-fill" "$tgt" "-opaque" "$src")
        done

        # C. Swap Accent Colors (Source Blue -> Fitting Contrast)
        count_acnt=${#SOURCE_ACCENTS[@]}
        for (( i=0; i<count_acnt; i++ )); do
            src="${SOURCE_ACCENTS[$i]}"
            tgt="${TGT_ACNT[$i]}"
            [ -z "$tgt" ] && continue
            args+=("-fill" "$tgt" "-opaque" "$src")
        done

        # Add compression
        args+=("${COMPRESSION_ARGS[@]}")

        # Run
        "$IM_CMD" "$img" "${args[@]}" "$output_name"
    done
done

echo "Done! Check the '$OUTPUT_DIR' folder."