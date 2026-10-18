"""Builds the Shrine Core block models and blockstates. Run from ore-shrines/: python3 tools/make_models.py

The block is a respawn-anchor-like shell of gilded blackstone with windows into a glowing heart, and a crystal
cluster (amethyst-cluster style) growing out of the top. Heart and cluster take the active effect's colour.
"""
import json, os

OUT = "src/main/resources/assets/oreshrines/models/block"
for old in os.listdir(OUT):
    os.remove(os.path.join(OUT, old))


def faces(tex_by_dir, cull=False):
    return {d: {"uv": [0, 0, 16, 16], "texture": t, **({"cullface": d} if cull else {})} for d, t in tex_by_dir.items()}


SIDES = ("north", "south", "east", "west")
template = {
    "parent": "minecraft:block/block",
    "ambientocclusion": False,
    "textures": {"particle": "#side"},
    "elements": [
        # The glowing heart, seen through the windows and the opening on top.
        {"from": [1, 1, 1], "to": [15, 15, 15], "shade": False, "light_emission": 15,
         "faces": faces({d: "#heart" for d in SIDES + ("up",)})},
        # Shell.
        {"from": [0, 0, 0], "to": [16, 16, 16],
         "faces": faces({**{d: "#side" for d in SIDES}, "up": "#top", "down": "#bottom"}, cull=True)},
        # Crystal cluster growing out of the top: two crossed planes, like vanilla clusters.
        {"from": [0.8, 15, 8], "to": [15.2, 28, 8], "shade": False, "light_emission": 15,
         "rotation": {"angle": 45, "axis": "y", "origin": [8, 15, 8]},
         "faces": faces({"north": "#cluster", "south": "#cluster"})},
        {"from": [8, 15, 0.8], "to": [8, 28, 15.2], "shade": False, "light_emission": 15,
         "rotation": {"angle": 45, "axis": "y", "origin": [8, 15, 8]},
         "faces": faces({"east": "#cluster", "west": "#cluster"})},
    ],
}
with open(f"{OUT}/shrine_core_template.json", "w") as f:
    json.dump(template, f, indent=2)

CRYSTALS = ["dormant", "harvest", "ward", "mending", "hearth", "pasture", "wisdom"]
for c in CRYSTALS:
    child = {
        "parent": "oreshrines:block/shrine_core_template",
        "textures": {
            "side": "oreshrines:block/shrine_core_side",
            "top": "oreshrines:block/shrine_core_top",
            "bottom": "oreshrines:block/shrine_core_bottom",
            "heart": f"oreshrines:block/shrine_heart_{c}",
            "cluster": f"oreshrines:block/shrine_cluster_{c}",
        },
    }
    with open(f"{OUT}/shrine_core_{c}.json", "w") as f:
        json.dump(child, f, indent=2)

# Lit shrines show their effect's colour; unlit ones (no effect or no pyramid) are grey.
variants = {}
for effect in ["none"] + CRYSTALS[1:]:
    for lit in ("false", "true"):
        model = effect if (lit == "true" and effect != "none") else "dormant"
        variants[f"effect={effect},lit={lit}"] = {"model": f"oreshrines:block/shrine_core_{model}"}
with open("src/main/resources/assets/oreshrines/blockstates/shrine_core.json", "w") as f:
    json.dump({"variants": variants}, f, indent=2)
