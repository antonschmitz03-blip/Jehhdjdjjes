"""Builds the Shrine Core block models and blockstates. Run from ore-shrines/: python3 tools/make_models.py

The block is a respawn-anchor-like shell of gilded blackstone with windows into a glowing heart, and a crystal
cluster (amethyst-cluster style) growing out of the top. Heart and cluster take the active effect's colour.
"""
import json, os

OUT = "src/main/resources/assets/oreshrines/models/block"
for old in os.listdir(OUT):
    os.remove(os.path.join(OUT, old))


SIDES = ("north", "south", "east", "west")


def box(f, t, tex_by_dir, uv=None, cull=(), **extra):
    """uv is for the side faces; tops and bottoms map straight onto their texture."""
    fs = {}
    for d, tex in tex_by_dir.items():
        if d in ("up", "down"):
            face_uv = [f[0], f[2], t[0], t[2]]
        else:
            face_uv = uv or [0, 0, 16, 16]
        fs[d] = {"uv": face_uv, "texture": tex, **({"cullface": d} if d in cull else {})}
    return {"from": f, "to": t, **extra, "faces": fs}


ALL = SIDES + ("up", "down")
elements = [
    # The glowing heart, seen through the windows and the opening in the capstone.
    box([3, 4, 3], [13, 13, 13], {d: "#heart" for d in SIDES + ("up",)}, uv=[3, 3, 13, 12], shade=False, light_emission=15),
    # Footing: full width, gilded lip at the bottom.
    box([0, 0, 0], [16, 2, 16], {**{d: "#trim" for d in SIDES}, "up": "#bottom", "down": "#bottom"}, uv=[0, 14, 16, 16], cull=ALL),
    # Step between footing and body.
    box([1, 2, 1], [15, 4, 15], {**{d: "#trim" for d in SIDES}, "up": "#bottom"}, uv=[1, 0, 15, 2]),
    # Body wall, set back two pixels, with the windows.
    box([2, 4, 2], [14, 13, 14], {d: "#side" for d in SIDES}, uv=[2, 3, 14, 12]),
    # Capstone: overhangs the body on all sides, gilded lip on top, opening in the middle.
    box([0, 13, 0], [16, 16, 16], {**{d: "#trim" for d in SIDES}, "up": "#top", "down": "#bottom"}, uv=[0, 0, 16, 3], cull=SIDES + ("up",)),
]
# Corner pillars holding up the capstone, standing proud of the body.
for x, z in ((1, 1), (12, 1), (1, 12), (12, 12)):
    elements.append(box([x, 4, z], [x + 3, 13, z + 3], {d: "#pillar" for d in SIDES}, uv=[5, 3, 8, 12]))
# Crystal cluster growing out of the top: two crossed planes, like vanilla clusters.
elements += [
    {"from": [0.8, 13, 8], "to": [15.2, 26, 8], "shade": False, "light_emission": 15,
     "rotation": {"angle": 45, "axis": "y", "origin": [8, 13, 8]},
     "faces": {d: {"uv": [0, 0, 16, 16], "texture": "#cluster"} for d in ("north", "south")}},
    {"from": [8, 13, 0.8], "to": [8, 26, 15.2], "shade": False, "light_emission": 15,
     "rotation": {"angle": 45, "axis": "y", "origin": [8, 13, 8]},
     "faces": {d: {"uv": [0, 0, 16, 16], "texture": "#cluster"} for d in ("east", "west")}},
]
template = {
    "parent": "minecraft:block/block",
    "ambientocclusion": False,
    "textures": {"particle": "#pillar"},
    "elements": elements,
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
            "trim": "oreshrines:block/shrine_core_trim",
            "pillar": "oreshrines:block/shrine_core_pillar",
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
