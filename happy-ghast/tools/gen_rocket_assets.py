#!/usr/bin/env python3
"""Generates the Ghast Rocket Engine assets: the 3D model worn on the ghast (models/item/ghast_rocket_engine.json,
texture ghast_rocket_engine.png 32x32) and the 2D inventory icon of the speed upgrade (ghast_speed_upgrade.png).
Deterministic. Model axes: engine along z (nose at z=0 is the FRONT, nozzle at z=16 is the BACK), strap towards +x.
Texture layout: 8x8 px cells in a 4x4 grid (uv unit = 2 px, a cell is 4x4 uv units)."""
import json, os, random
from PIL import Image

A = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "happyghastplus")
rng = random.Random(5)

# ---------------------------------------------------------------- texture (32x32)
T = Image.new("RGBA", (32, 32), (0, 0, 0, 0))


def cell(cx, cy, fn):
    for y in range(8):
        for x in range(8):
            T.putpixel((cx * 8 + x, cy * 8 + y), fn(x, y) + (255,))


def jitter(c, n=6):
    return tuple(max(0, min(255, v + rng.randint(-n, n))) for v in c)


# vanilla-style palette: iron block greys, firework red, copper, leather, coal
def vp(base, spread):
    """vanilla-like per-pixel noise: pick one of a few close shades"""
    d = rng.choice(spread)
    return tuple(max(0, min(255, v + d)) for v in base)


IRON = (214, 214, 214)
cell(0, 0, lambda x, y: vp(IRON, (-18, -8, 0, 0, 8, 14)))                                                    # body plate
cell(1, 0, lambda x, y: vp((176, 176, 176), (-16, -6, 0, 6)) if x in (0, 7) or y in (0, 7) else vp(IRON, (-10, 0, 8)))  # plate edge
cell(2, 0, lambda x, y: vp((236, 232, 220), (-8, 0, 6)) if y in (3, 4) else vp((176, 46, 40), (-14, 0, 10)))  # tip: red, paper band
cell(3, 0, lambda x, y: vp((176, 46, 40), (-18, -6, 0, 8, 14)))                                              # tip: plain red
cell(0, 1, lambda x, y: vp((70, 70, 74), (-12, 0, 8, 14)))                                                   # nozzle iron
cell(1, 1, lambda x, y: vp((34, 34, 38), (-6, 0, 6)))                                                        # nozzle dark
cell(2, 1, lambda x, y: (255, 140, 30) if max(abs(x - 3.5), abs(y - 3.5)) > 1.8 else (255, 214, 90))         # ember
cell(3, 1, lambda x, y: (255, 214, 90))                                                                      # ember core
cell(0, 2, lambda x, y: vp((198, 112, 82), (-20, -8, 0, 10, 20)))                                           # copper band
cell(1, 2, lambda x, y: vp((130, 84, 48), (-14, -6, 0, 8)))                                                  # leather strap
cell(2, 2, lambda x, y: vp((98, 62, 36), (-10, 0, 8)))                                                       # leather dark
T.save(f"{A}/textures/item/ghast_rocket_engine.png")

CELL = {"body": (0, 0), "rivet": (1, 0), "nose_band": (2, 0), "nose": (3, 0), "metal": (0, 1), "dark": (1, 1),
        "ember": (2, 1), "core": (3, 1), "gold": (0, 2), "leather": (1, 2), "leather2": (2, 2)}


def uv(name):
    cx, cy = CELL[name]
    return [cx * 4, cy * 4, cx * 4 + 4, cy * 4 + 4]


def box(f, t, mats, rot=None):
    """mats: one cell name for all faces, or a dict face->name (default 'body')."""
    faces = {}
    for d in ("north", "south", "east", "west", "up", "down"):
        n = mats if isinstance(mats, str) else mats.get(d, mats.get("all", "body"))
        faces[d] = {"uv": uv(n), "texture": "#engine"}
    e = {"from": f, "to": t, "faces": faces}
    if rot:
        e["rotation"] = rot
    return e


CX, CY = 7.0, 8.0


els = []
els.append(box([CX - 3, CY - 3, 4.0], [CX + 3, CY + 3, 13.0], {"all": "body", "up": "rivet", "down": "rivet"}))     # body
els.append(box([CX - 2, CY - 2, 2.0], [CX + 2, CY + 2, 4.0], {"all": "nose"}))                                      # tip, wide step
els.append(box([CX - 1, CY - 1, 0.0], [CX + 1, CY + 1, 2.0], {"all": "nose_band"}))                                 # tip, narrow step
els.append(box([CX - 3.2, CY - 3.2, 6.0], [CX + 3.2, CY + 3.2, 7.0], "gold"))                                       # copper bands
els.append(box([CX - 3.2, CY - 3.2, 10.0], [CX + 3.2, CY + 3.2, 11.0], "gold"))
els.append(box([CX - 2, CY - 2, 13.0], [CX + 2, CY + 2, 15.0], {"all": "metal"}))                                    # nozzle
els.append(box([CX - 1.5, CY - 1.5, 15.0], [CX + 1.5, CY + 1.5, 15.5], {"all": "dark", "south": "ember"}))           # glowing mouth
# fins: flat planks up, down and away from the body (the strap side is +x)
els.append(box([CX - 0.5, CY + 3.0, 9.0], [CX + 0.5, CY + 5.5, 13.0], "metal"))
els.append(box([CX - 0.5, CY - 5.5, 9.0], [CX + 0.5, CY - 3.0, 13.0], "metal"))
els.append(box([CX - 5.5, CY - 0.5, 9.0], [CX - 3.0, CY + 0.5, 13.0], "metal"))
# strap: two plain leather bands towards the ghast
for z0 in (5.0, 11.5):
    els.append(box([CX + 3.0, CY - 1.0, z0], [16.0, CY + 1.0, z0 + 1.0], "leather"))
model = {"textures": {"engine": "happyghastplus:item/ghast_rocket_engine", "particle": "happyghastplus:item/ghast_rocket_engine"},
         "elements": els}
json.dump(model, open(f"{A}/models/item/ghast_rocket_engine.json", "w"), indent=1)

# ---------------------------------------------------------------- inventory icon (16x16): a rocket pod, nose up-right
I = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
P = {"o": (40, 40, 44), "b": (214, 214, 214), "h": (240, 240, 240), "s": (160, 160, 160), "r": (176, 46, 40), "R": (128, 30, 30),
     "g": (198, 112, 82), "f": (255, 140, 30), "y": (255, 214, 90), "m": (70, 70, 74)}
art = [
    "............oo..",
    "...........orro.",
    "..........orrRRo",
    ".........orrrRRo",
    "........obrrRro.",
    ".......obhbbso..",
    "......obhbbsso..",
    ".....ogggbsso...",
    "....obhbbsso....",
    "...obhbbsso.....",
    "..ogggbsso......",
    ".omsbsso........",
    "ofymbso.........",
    "fyfoo...........",
    ".f..............",
    "................",
]
for y, row in enumerate(art):
    for x, ch in enumerate(row):
        if ch != ".":
            I.putpixel((x, y), P[ch] + (255,))
I.save(f"{A}/textures/item/ghast_speed_upgrade.png")
print("rocket assets written")
