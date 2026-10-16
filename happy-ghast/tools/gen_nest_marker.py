#!/usr/bin/env python3
"""The ghast nest's forward marker and its blockstate file.

A powder snow bucket stands on the rim at the front of the nest, a little right of the middle, so you can see which
way a settled ghast will face. The blockstate is multipart: every cell gets its straw piece (the same for all four
facings), and the front cell for each facing also gets the bucket, turned with the nest.

gen_nest_assets.py calls write(cells) with the straw piece of every cell. Run on its own
(python3 happy-ghast/tools/gen_nest_marker.py) it rebuilds the blockstate from the existing one, for when only the
marker changed. Textures are vanilla ones, referenced, not copied.
"""
import json
import os

A = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "happyghastplus")

# Front cell (on the nest's rim) for each facing, a little right of the middle; the bucket model is drawn for north.
MARKER_CELL = {"north": (3, 0), "east": (5, 3), "south": (2, 5), "west": (0, 2)}
ROTATION = {"north": 0, "east": 90, "south": 180, "west": 270}


def box(f, t, tex, top=None):
    faces = {d: {"texture": tex} for d in ("north", "south", "east", "west", "down")}
    faces["up"] = {"texture": top or tex}
    return {"from": f, "to": t, "faces": faces}


def marker_model():
    # Bucket centred on (12, 5) in the north rim, its foot pressed into the straw at the rim top (13.5).
    turn = {"origin": [12, 15, 5], "axis": "y", "angle": 22.5}
    els = [
        box([9.5, 12, 2.5], [14.5, 15, 7.5], "#iron"),               # tapered lower body
        box([9, 15, 2], [15, 18.5, 8], "#iron"),                     # upper body
        box([8.8, 18.5, 1.8], [15.2, 19, 8.2], "#rim", top="#snow"),  # rim, filled to the brim
        box([10.2, 19, 3.2], [13.8, 19.6, 6.8], "#snow"),             # heaped powder snow
        box([8.6, 18, 4.75], [9.1, 21.5, 5.25], "#rim"),              # handle
        box([14.9, 18, 4.75], [15.4, 21.5, 5.25], "#rim"),
        box([8.6, 21.5, 4.75], [15.4, 22, 5.25], "#rim"),
    ]
    for e in els:
        e["rotation"] = turn
    return {
        "textures": {"iron": "minecraft:block/iron_block", "rim": "minecraft:block/anvil",
                     "snow": "minecraft:block/powder_snow", "particle": "minecraft:block/powder_snow"},
        "elements": els,
    }


def write(cells):
    """cells: {(x, z): {"model": ..., "y": optional rotation}} for all 36 cells."""
    parts = []
    for (x, z), piece in sorted(cells.items()):
        parts.append({"when": {"x": str(x), "z": str(z)}, "apply": piece})
    for facing, (x, z) in MARKER_CELL.items():
        apply = {"model": "happyghastplus:block/ghast_nest_marker"}
        if ROTATION[facing]:
            apply["y"] = ROTATION[facing]
        parts.append({"when": {"x": str(x), "z": str(z), "facing": facing}, "apply": apply})
    with open(os.path.join(A, "blockstates", "ghast_nest.json"), "w", newline="\n") as f:
        json.dump({"multipart": parts}, f, indent=1)
        f.write("\n")
    with open(os.path.join(A, "models", "block", "ghast_nest_marker.json"), "w", newline="\n") as f:
        json.dump(marker_model(), f, indent=1)
        f.write("\n")


def cells_from_existing():
    with open(os.path.join(A, "blockstates", "ghast_nest.json")) as f:
        data = json.load(f)
    cells = {}
    if "variants" in data:
        for key, piece in data["variants"].items():
            props = dict(kv.split("=") for kv in key.split(","))
            cells[(int(props["x"]), int(props["z"]))] = piece
    else:
        for part in data["multipart"]:
            when = part["when"]
            if "facing" not in when:
                cells[(int(when["x"]), int(when["z"]))] = part["apply"]
    return cells


if __name__ == "__main__":
    write(cells_from_existing())
    print("wrote ghast nest blockstate and marker")
