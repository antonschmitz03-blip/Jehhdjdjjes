"""Builds the Shrine Core block models: one template plus a child per crystal colour. Run from ore-shrines/."""
import json, os

OUT = "src/main/resources/assets/oreshrines/models/block"
os.makedirs(OUT, exist_ok=True)
DIRS = ["north", "south", "east", "west", "up", "down"]


def auto_uv(f, t, d):
    if d in ("up", "down"):
        return [f[0], f[2], t[0], t[2]]
    if d in ("north", "south"):
        return [f[0], 16 - t[1], t[0], 16 - f[1]]
    return [f[2], 16 - t[1], t[2], 16 - f[1]]


def box(f, t, tex, top=None, uv=None, rot=None, glow=False, skip=(), top_uv=None):
    faces = {}
    for d in DIRS:
        if d in skip:
            continue
        texture = top if (top and d in ("up", "down")) else tex
        # The given uv is for the side faces; tops and bottoms always map straight onto their texture.
        if d in ("up", "down"):
            face_uv = top_uv or auto_uv(f, t, d)
        else:
            face_uv = uv or auto_uv(f, t, d)
        face = {"uv": face_uv, "texture": texture}
        faces[d] = face
    el = {"from": f, "to": t, "faces": faces}
    if rot:
        el["rotation"] = rot
    if glow:
        el["shade"] = False
        el["light_emission"] = 15
    return el


def y45(y):
    return {"angle": 45, "axis": "y", "origin": [8, y, 8]}


elements = [
    # Base slab: carved bricks (bottom rows of the side texture), tiled stone top.
    box([0, 0, 0], [16, 3, 16], "#side", top="#top", uv=[0, 13, 16, 16]),
    # Gold band.
    box([1, 3, 1], [15, 4, 15], "#gilding", uv=[1, 0, 15, 1]),
    # Plinth with the engraved gold-inlay band.
    box([3, 4, 3], [13, 7, 13], "#side", top="#top", uv=[3, 6, 13, 9]),
    # Gold dish the crystal floats over.
    box([5, 7, 5], [11, 8, 11], "#gilding", uv=[5, 7, 11, 8]),
]
# Four iron posts on the corners of the base, gold caps and a small shard on each.
for x, z in ((0.5, 0.5), (13, 0.5), (0.5, 13), (13, 13)):
    elements.append(box([x, 3, z], [x + 2.5, 10, z + 2.5], "#pillar", uv=[6, 3, 8.5, 10], skip=("down",)))
    elements.append(box([x - 0.25, 10, z - 0.25], [x + 2.75, 11, z + 2.75], "#gilding", uv=[1, 1, 4, 2], top_uv=[2, 2, 5, 5]))
    elements.append(box([x + 0.75, 11, z + 0.75], [x + 1.75, 13, z + 1.75], "#crystal", uv=[6, 4, 10, 12], glow=True))
# Floating crystal: a fat middle and two tips, turned 45 degrees so it reads as a gem.
elements += [
    box([7, 8.5, 7], [9, 10, 9], "#crystal", uv=[5, 11, 11, 15], rot=y45(9), glow=True, top_uv=[6, 6, 10, 10]),
    box([6, 10, 6], [10, 14, 10], "#crystal", uv=[0, 0, 16, 16], rot=y45(12), glow=True),
    box([7, 14, 7], [9, 16, 9], "#crystal", uv=[5, 1, 11, 7], rot=y45(15), glow=True, top_uv=[6, 6, 10, 10]),
]

template = {
    "parent": "minecraft:block/block",
    "ambientocclusion": False,
    "textures": {"particle": "oreshrines:block/shrine_gilding"},
    "display": {
        "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    },
    "elements": elements,
}
with open(f"{OUT}/shrine_core_template.json", "w") as f:
    json.dump(template, f, indent=2)

CRYSTALS = ["dormant", "harvest", "ward", "mending", "hearth", "pasture", "wisdom"]
for c in CRYSTALS:
    child = {
        "parent": "oreshrines:block/shrine_core_template",
        "textures": {
            "side": "oreshrines:block/shrine_stone_side",
            "top": "oreshrines:block/shrine_stone_top",
            "gilding": "oreshrines:block/shrine_gilding",
            "pillar": "oreshrines:block/shrine_pillar",
            "crystal": f"oreshrines:block/shrine_crystal_{c}",
        },
    }
    with open(f"{OUT}/shrine_core_{c}.json", "w") as f:
        json.dump(child, f, indent=2)

# Lit shrines show their effect's crystal; unlit ones (no effect or no pyramid) a dull grey one.
variants = {}
for effect in ["none"] + CRYSTALS[1:]:
    for lit in ("false", "true"):
        model = effect if (lit == "true" and effect != "none") else "dormant"
        variants[f"effect={effect},lit={lit}"] = {"model": f"oreshrines:block/shrine_core_{model}"}
with open("src/main/resources/assets/oreshrines/blockstates/shrine_core.json", "w") as f:
    json.dump({"variants": variants}, f, indent=2)
