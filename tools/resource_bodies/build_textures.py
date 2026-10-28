"""Restore the original generated surface tiles and build tinted haze shells.

Run from the repository root: python tools/resource_bodies/build_textures.py
Requires Pillow. Original surface tiles and generation prompts live beside this script.
"""

import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SOURCES = Path(__file__).resolve().parent / "sources"
ASSETS = ROOT / "space-age/src/main/resources/assets/oritech_space_age"
FACES = ("north", "east", "south", "west", "up", "down")
TINTS = {
    "luna": (155, 185, 220),
    "phobos": (235, 150, 90),
    "ceres": (120, 200, 245),
    "vesta": (170, 130, 225),
    "nyx": (140, 110, 240),
}


def shell(tint):
    result = Image.new("RGBA", (64, 64))
    for y in range(64):
        for x in range(64):
            distance = min(x, y, 63 - x, 63 - y)
            rim = max(0, 1 - distance / 16) ** 2
            result.putpixel((x, y), (*tint, round(8 + 24 * rim)))
    return result


def update_model(name):
    path = ASSETS / "models/block" / (name + ".json")
    model = json.loads(path.read_text(encoding="utf-8"))
    surface = model["elements"][0]
    surface["name"] = "surface"
    for face in FACES:
        surface["faces"][face] = {"uv": [0, 0, 16, 16], "texture": "#surface"}
    model["textures"]["haze"] = {"sprite": "oritech_space_age:block/" + name + "_haze",
                                     "force_translucent": True}
    model["elements"] = [surface, {
        "name": "haze",
        "from": [-8.8, -8.8, -8.8],
        "to": [24.8, 24.8, 24.8],
        "shade": False,
        "light_emission": 15,
        "faces": {face: {"uv": [0, 0, 16, 16], "texture": "#haze"} for face in FACES},
    }]
    path.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")


def main():
    for name, tint in TINTS.items():
        source = Image.open(SOURCES / (name + ".png")).convert("RGB")
        image = source
        image.save(ASSETS / "textures/block" / (name + "_surface.png"))
        shell(tint).save(ASSETS / "textures/block" / (name + "_haze.png"))
        update_model(name)
        print(name + ": original surface tile restored, translucent haze retained")


if __name__ == "__main__":
    main()
