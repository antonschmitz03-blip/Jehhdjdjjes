"""Writes blockstates, models, item definitions, loot tables, recipes and tags.

Usage: python3 make_resources.py <unzipped Create Fly jar>
The blockstates reuse Create's rotation tables (read from the jar) with Create's own block models, so the static
parts of our machines look exactly like Create's. Only the moving parts get our own models, which name Create's
model as parent and swap the texture.
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources")
NS = "diamondmachines"

MACHINES = {
    # ours: Create's block
    "diamond_drill": "mechanical_drill",
    "diamond_saw": "mechanical_saw",
    "diamond_crushing_wheel": "crushing_wheel",
}


def write(path, data):
    path = os.path.join(RES, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def tex(name):
    return f"{NS}:block/{name}"


def main():
    create = sys.argv[1]
    for ours, theirs in MACHINES.items():
        with open(os.path.join(create, "assets", "create", "blockstates", theirs + ".json")) as f:
            write(f"assets/{NS}/blockstates/{ours}.json", json.load(f))

    # Drill: the cone is the anvil texture in Create's model.
    head = tex("diamond_drill_head")
    write(f"assets/{NS}/models/block/diamond_drill/head.json",
          {"parent": "create:block/mechanical_drill/head", "textures": {"2": head, "particle": head}})
    write(f"assets/{NS}/models/block/diamond_drill/item.json",
          {"parent": "create:block/mechanical_drill/item", "textures": {"2": head, "particle": head}})
    write(f"assets/{NS}/items/diamond_drill.json",
          {"model": {"type": "create:model/normal", "model": f"{NS}:block/diamond_drill/item"}})

    # Saw: running blades use the animated texture, still blades the first frame.
    for orientation in ("horizontal", "vertical"):
        for state in ("active", "reversed"):
            write(f"assets/{NS}/models/block/diamond_saw/blade_{orientation}_{state}.json",
                  {"parent": f"create:block/mechanical_saw/blade_{orientation}_{state}",
                   "textures": {"stonecutter_saw": tex("diamond_saw"), "particle": tex("diamond_saw")}})
        write(f"assets/{NS}/models/block/diamond_saw/blade_{orientation}_inactive.json",
              {"parent": f"create:block/mechanical_saw/blade_{orientation}_active",
               "textures": {"stonecutter_saw": tex("diamond_saw_static"),
                            "stonecutter_saw_reversed": tex("diamond_saw_static"),
                            "particle": tex("diamond_saw_static")}})
    write(f"assets/{NS}/models/block/diamond_saw/item.json",
          {"parent": "create:block/mechanical_saw/item", "textures": {"stonecutter_saw": tex("diamond_saw")}})
    write(f"assets/{NS}/items/diamond_saw.json",
          {"model": {"type": "minecraft:model", "model": f"{NS}:block/diamond_saw/item"}})

    # Crushing wheel: Create's .obj wheel with diamond plates.
    wheel_textures = {
        "axis": "create:block/axis",
        "axis_top": "create:block/axis_top",
        "plates": tex("diamond_crushing_wheel_plates"),
        "insert": "create:block/crushing_wheel_insert",
        "spruce_log_top": "minecraft:block/spruce_log_top",
        "particle": "minecraft:block/diamond_block",
    }
    wheel = {"parent": "block/block", "loader": "neoforge:obj", "flip_v": True,
             "model": "create:models/block/crushing_wheel/crushing_wheel.obj", "textures": wheel_textures}
    write(f"assets/{NS}/models/block/diamond_crushing_wheel/block.json", wheel)
    with open(os.path.join(create, "assets", "create", "models", "block", "crushing_wheel", "item.json")) as f:
        display = json.load(f)["display"]
    write(f"assets/{NS}/models/block/diamond_crushing_wheel/item.json", {**wheel, "display": display})
    write(f"assets/{NS}/items/diamond_crushing_wheel.json",
          {"model": {"type": "create:model/normal", "model": f"{NS}:block/diamond_crushing_wheel/item"}})

    for ours, theirs in MACHINES.items():
        write(f"data/{NS}/loot_table/blocks/{ours}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NS}:{ours}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": f"{NS}:blocks/{ours}",
        })
        write(f"data/{NS}/recipe/{ours}.json", {
            "type": "minecraft:crafting_shapeless",
            "category": "misc",
            "ingredients": [f"create:{theirs}", "minecraft:diamond_block"],
            "result": {"id": f"{NS}:{ours}"},
        })
    write("data/minecraft/tags/block/mineable/pickaxe.json",
          {"values": [f"{NS}:{ours}" for ours in MACHINES]})


if __name__ == "__main__":
    main()
