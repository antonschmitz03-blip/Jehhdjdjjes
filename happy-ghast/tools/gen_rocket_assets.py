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


IRON, IRON_HI, IRON_LO = (186, 189, 195), (214, 216, 220), (132, 135, 144)
cell(0, 0, lambda x, y: jitter(IRON_LO if y in (0, 7) else IRON_HI if y == 1 else IRON))                  # body plate
cell(1, 0, lambda x, y: jitter(IRON_LO if x in (0, 7) or y == 7 else (IRON_HI if (x, y) in ((2, 2), (5, 2), (2, 5), (5, 5)) else IRON)))  # riveted
cell(2, 0, lambda x, y: jitter((236, 236, 236) if y in (3, 4) else (196, 44, 44)))                         # nose: red, white band
cell(3, 0, lambda x, y: jitter((150, 30, 34) if y == 7 else (210, 58, 54)))                                # nose: plain red
cell(0, 1, lambda x, y: jitter((58, 58, 66) if (x + y) % 5 else (74, 74, 82)))                             # nozzle metal
cell(1, 1, lambda x, y: jitter((40, 40, 46)))                                                              # nozzle dark
cell(2, 1, lambda x, y: (255, 150, 30) if max(abs(x - 3.5), abs(y - 3.5)) > 1.8 else (255, 226, 120))     # ember glow
cell(3, 1, lambda x, y: (255, 226, 120))                                                                   # ember core
cell(0, 2, lambda x, y: jitter((222, 178, 62) if y not in (0, 7) else (170, 126, 40), 4))                  # gold trim
cell(1, 2, lambda x, y: jitter((124, 82, 46) if (x + y) % 4 else (104, 66, 36), 4))                        # leather strap
cell(2, 2, lambda x, y: jitter((92, 60, 34), 4))                                                           # leather dark
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


def octagon(z0, z1, s, mats):
    """regular octagonal prism along z with flat-to-flat width s: four bars through the centre (0, 90, +45, -45 degrees)."""
    a = s / 2
    side = 2 * a * 0.41421356
    h = side / 2
    zm = (z0 + z1) / 2
    # each bar's end caps sit 0.01 further in than the previous one, so no two caps are coplanar (no z-fighting)
    def bar(i, rot=None, wide=True):
        f = [CX - a, CY - h, z0 + 0.01 * i] if wide else [CX - h, CY - a, z0 + 0.01 * i]
        t = [CX + a, CY + h, z1 - 0.01 * i] if wide else [CX + h, CY + a, z1 - 0.01 * i]
        return box(f, t, mats, rot)
    bars = [bar(0), bar(1, wide=False)]
    for i, ang in enumerate((45, -45)):
        bars.append(bar(i + 2, {"angle": ang, "axis": "z", "origin": [CX, CY, zm]}))
    return bars


els = []
els += octagon(3.5, 12.5, 6.5, {"all": "body", "up": "rivet", "down": "rivet"})              # pod
els += octagon(1.5, 3.5, 5.2, {"all": "nose", "north": "nose"})                              # nose, lower step
els += octagon(0.0, 1.5, 3.0, {"all": "nose_band"})                                          # nose tip
els += octagon(4.6, 5.4, 7.0, "gold")                                                        # trim ring
els += octagon(10.4, 11.2, 7.0, "gold")
els += octagon(12.5, 14.5, 7.4, {"all": "metal", "south": "metal"})                          # nozzle bell
els += octagon(14.5, 15.5, 5.4, {"all": "dark", "south": "ember"})                           # nozzle lip with glowing mouth
els.append(box([CX - 1.5, CY - 1.5, 15.5], [CX + 1.5, CY + 1.5, 15.7], {"all": "core"}))      # hot core
# fins: up, down and away from the body (the strap side is +x)
els.append(box([CX - 0.5, CY + 3.2, 8.5], [CX + 0.5, CY + 6.6, 13.5], "rivet"))
els.append(box([CX - 0.5, CY - 6.6, 8.5], [CX + 0.5, CY - 3.2, 13.5], "rivet"))
els.append(box([CX - 6.6, CY - 0.5, 8.5], [CX - 3.2, CY + 0.5, 13.5], "rivet"))
# strap: two leather bands towards the ghast with a gold buckle
for z0 in (4.6, 10.4):
    els.append(box([CX + 3.1, CY - 1.4, z0], [16.0, CY + 1.4, z0 + 1.0], "leather"))
    els.append(box([CX + 3.1, CY - 1.4, z0], [CX + 4.2, CY + 1.4, z0 + 1.0], "gold"))
model = {"textures": {"engine": "happyghastplus:item/ghast_rocket_engine", "particle": "happyghastplus:item/ghast_rocket_engine"},
         "elements": els}
json.dump(model, open(f"{A}/models/item/ghast_rocket_engine.json", "w"), indent=1)

# ---------------------------------------------------------------- inventory icon (16x16): a rocket pod, nose up-right
I = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
P = {"o": (46, 46, 54), "b": (190, 193, 199), "h": (226, 228, 232), "s": (136, 139, 148), "r": (204, 52, 50), "R": (150, 32, 36),
     "g": (226, 182, 64), "f": (255, 150, 30), "y": (255, 226, 120), "m": (120, 126, 140)}
art = [
    "............oo..",
    "...........orro.",
    "..........orrRRo",
    ".........orhrRRo",
    "........obhbRro.",
    ".......obhbbso..",
    "......obhbbsmo..",
    ".....ogggbsso...",
    "....obhbbsso....",
    "...obhbbsso.....",
    "..obhbbsso......",
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
