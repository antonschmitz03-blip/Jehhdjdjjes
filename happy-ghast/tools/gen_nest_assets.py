#!/usr/bin/env python3
"""Generates the Ghast Nest assets: block models, blockstate, textures, item definitions, recipe and the
closed-eyes overlay. Deterministic; run from anywhere:  python3 happy-ghast/tools/gen_nest_assets.py <vanilla happy_ghast.png>
(the vanilla skin is only read to sample the face colour for the eyelid overlay, nothing of it is copied.)

The nest is 32 blocks (a 6x6 footprint without its four corner cells). Block state x/z (0..5) is the cell inside the
footprint; a cell gets a rim on every side that has no neighbouring cell: none = centre piece, one = edge piece,
two = corner piece."""
import json, os, random, sys
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
A = os.path.join(ROOT, "assets", "happyghastplus")
D = os.path.join(ROOT, "data", "happyghastplus")
for p in ("models/block", "models/item", "textures/block", "textures/item", "items", "blockstates", "lang"):
    os.makedirs(os.path.join(A, p), exist_ok=True)
os.makedirs(os.path.join(D, "recipe"), exist_ok=True)

# ---------------------------------------------------------------- textures
rnd = random.Random(7)
tones = [(212, 172, 76), (200, 160, 62), (222, 184, 90), (188, 146, 52)]
img = Image.new("RGB", (16, 16), tones[1])
for y in range(16):
    x = rnd.randrange(0, 4) - 4
    while x < 16:
        ln = rnd.randrange(4, 9)
        c = rnd.choice(tones)
        for i in range(ln):
            if 0 <= x + i < 16:
                img.putpixel((x + i, y), c)
        x += ln
for _ in range(5):
    img.putpixel((rnd.randrange(16), rnd.randrange(16)), (120, 84, 32))
img.save(f"{A}/textures/block/ghast_nest_straw.png")
lin = Image.new("RGB", (16, 16))
for x in range(16):
    for y in range(16):
        lin.putpixel((x, y), tuple(int(v * 0.74) for v in img.getpixel((x, y))))
lin.save(f"{A}/textures/block/ghast_nest_lining.png")

STRAW, LINING = "happyghastplus:block/ghast_nest_straw", "happyghastplus:block/ghast_nest_lining"

# ---------------------------------------------------------------- piece models
r = random.Random(2026)
FLOOR = 5


def face_uv(a, b):
    a, b = int(min(16, max(1, round(a)))), int(min(16, max(1, round(b))))
    ox, oy = r.randint(0, 16 - a), r.randint(0, 16 - b)
    return [ox, oy, ox + a, oy + b]


def cube(f, t, down=True, tex="#s"):
    dx, dy, dz = abs(t[0] - f[0]), abs(t[1] - f[1]), abs(t[2] - f[2])
    faces = {
        "north": dict(uv=face_uv(dx, dy), texture=tex), "south": dict(uv=face_uv(dx, dy), texture=tex),
        "east": dict(uv=face_uv(dz, dy), texture=tex), "west": dict(uv=face_uv(dz, dy), texture=tex),
        "up": dict(uv=face_uv(dx, dz), texture=tex),
    }
    if down:
        faces["down"] = dict(uv=face_uv(dx, dz), texture=tex)
    return {"from": [round(v, 2) for v in f], "to": [round(v, 2) for v in t], "faces": faces}


def floor():
    return [cube([0, 0, 0], [16, FLOOR, 16], tex="#l")]


def loose_straws(n):
    els = []
    for k in range(n):
        ln = r.uniform(5, 10)
        a, b = r.uniform(1, 15 - ln), r.uniform(1, 14)
        y = FLOOR + (0.2 * (k + 1)) % 1.4
        if r.random() < 0.5:
            els.append(cube([a, y, b], [a + ln, y + 1, b + 1], down=False))
        else:
            els.append(cube([b, y, a], [b + 1, y + 1, a + ln], down=False))
    return els


def rolls(side, shift=0.0):
    els = []
    for lvl, (y0, y1) in enumerate([(FLOOR - 1, 8), (8, 11), (11, 13.5)]):
        inset = [0.0, 0.8, 1.8][lvl] + r.uniform(0, 0.6) + shift
        thick = [7, 6, 5][lvl]
        lo = r.choice([0, 0, 1]) if lvl else 0
        hi = 16 - (r.choice([0, 0, 1]) if lvl else 0)
        yy0, yy1 = y0 + shift * 0.5, y1 + shift * 0.5
        if side == "n":
            els.append(cube([lo, yy0, inset], [hi, yy1, inset + thick]))
        else:
            els.append(cube([inset, yy0, lo], [inset + thick, yy1, hi]))
    return els


def stalks(side, n):
    els = []
    for _ in range(n):
        along, y = r.uniform(0.5, 14.5), r.uniform(FLOOR, 12)
        ln, th = r.uniform(4, 8), r.choice([1, 1, 1.5])
        if side == "n":
            els.append(cube([along, y, -ln], [along + th, y + th, 2.0]))
        else:
            els.append(cube([-ln, y, along], [2.0, y + th, along + th]))
    for _ in range(max(2, n // 2)):
        along, top = r.uniform(0.5, 14.5), r.uniform(2, 4)
        d0, d1 = r.uniform(1.5, 4), 2 + r.uniform(1.5, 4)
        if side == "n":
            els.append(cube([along, 13, d0], [along + 1, 13 + top, d1], down=False))
        else:
            els.append(cube([d0, 13, along], [d1, 13 + top, along + 1], down=False))
    return els


def write_model(name, els):
    m = {"textures": {"s": STRAW, "l": LINING, "particle": STRAW}, "elements": els}
    json.dump(m, open(f"{A}/models/block/{name}.json", "w"), indent=1)


write_model("ghast_nest_center", floor() + loose_straws(13))
write_model("ghast_nest_edge", floor() + rolls("n") + stalks("n", 9) + loose_straws(3))
write_model("ghast_nest_corner", floor() + rolls("n") + rolls("w", 0.25) + stalks("n", 6) + stalks("w", 6))

# ---------------------------------------------------------------- blockstate
def valid(x, z):
    return 0 <= x <= 5 and 0 <= z <= 5 and (x, z) not in {(0, 0), (5, 0), (0, 5), (5, 5)}


variants = {}
for x in range(6):
    for z in range(6):
        n, e, s, w = (not valid(x, z - 1), not valid(x + 1, z), not valid(x, z + 1), not valid(x - 1, z))
        sides = [n, e, s, w]          # clockwise from north
        if not valid(x, z) or sum(sides) == 0:
            model, rot = "center", ((x * 3 + z * 7) % 4) * 90
        elif sum(sides) == 1:
            model, rot = "edge", sides.index(True) * 90
        else:
            model = "corner"        # the corner model has its rim on the north and west sides
            rot = {(0, 3): 0, (0, 1): 90, (1, 2): 180, (2, 3): 270}[tuple(i for i in range(4) if sides[i])]
        v = {"model": f"happyghastplus:block/ghast_nest_{model}"}
        if rot:
            v["y"] = rot
        variants[(x, z)] = v
# multipart, with the powder snow bucket marking the front: see gen_nest_marker.py
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_nest_marker
gen_nest_marker.write(variants)

# ---------------------------------------------------------------- item models
disp = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.5, 0.5, 0.5]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
}
ir = random.Random(11)


def icon_cube(f, t, tex="#s"):
    c = cube(f, t, tex=tex)
    return c


r = ir
icon = [icon_cube([0, 0, 0], [16, 4, 16], "#l")]
for f, t in (([0, 4, 0], [16, 9, 4]), ([0, 4, 12], [16, 9, 16]), ([0, 4, 4], [4, 9, 12]), ([12, 4, 4], [16, 9, 12])):
    icon.append(icon_cube(f, t))
icon += [icon_cube([2, 9, 0], [9, 11, 3]), icon_cube([8, 9, 13], [15, 11, 16]), icon_cube([0, 9, 7], [3, 11, 14]),
         icon_cube([13, 9, 3], [16, 11, 9]), icon_cube([-2, 5, 5], [2, 6, 6]), icon_cube([14, 5, 10], [18, 6, 11])]
json.dump({"textures": {"s": STRAW, "l": LINING, "particle": STRAW}, "elements": icon, "display": disp},
          open(f"{A}/models/item/ghast_nest.json", "w"), indent=1)
for name in ("ghast_nest", "ghast_eyelids"):
    json.dump({"model": {"type": "minecraft:model", "model": f"happyghastplus:item/{name}"}},
              open(f"{A}/items/{name}.json", "w"), indent=2)

# ---------------------------------------------------------------- closed-eyes overlay (32x32 = the face, 8 px per block)
EYES = [(5, 15), (19, 29)]          # x ranges of the two eyes on the 32x32 face
TOP, BOT = 9, 12                    # rows covered
ov = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
skin = (232, 230, 230)
if len(sys.argv) > 1 and os.path.exists(sys.argv[1]):
    face = Image.open(sys.argv[1]).convert("RGBA").crop((32, 32, 64, 64))
    pix = [face.getpixel((x, y))[:3] for y in (7, 8, 13) for x in list(range(6, 15)) + list(range(20, 29))]
    skin = tuple(sorted(p[i] for p in pix)[len(pix) // 2] for i in range(3))
for x0, x1 in EYES:
    for y in range(TOP, BOT + 1):
        for x in range(x0, x1 + 1):
            ov.putpixel((x, y), skin + (255,))
    line = (86, 80, 86, 255)
    mid = (x0 + x1) // 2
    for x in range(x0 + 1, x1):          # a downward arc, like a happy closed eye
        d = abs(x - mid)
        y = 10 if d >= 4 else (11 if d >= 2 else 12)
        ov.putpixel((x, y), line)
    # shadow row under the lid for a bit of depth
ov.save(f"{A}/textures/item/ghast_eyelids.png")
json.dump({"textures": {"t": "happyghastplus:item/ghast_eyelids", "particle": "happyghastplus:item/ghast_eyelids"},
           "elements": [{"from": [0, 0, 8], "to": [16, 16, 8],
                         "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#t"},
                                   "south": {"uv": [0, 0, 16, 16], "texture": "#t"}}}]},
          open(f"{A}/models/item/ghast_eyelids.json", "w"), indent=1)

# ---------------------------------------------------------------- recipe and names
json.dump({"type": "minecraft:crafting_shaped", "category": "misc",
           "key": {"H": "minecraft:hay_block", "S": "minecraft:stick"},
           "pattern": ["HSH", "SHS", "HSH"], "result": {"id": "happyghastplus:ghast_nest"}},
          open(f"{D}/recipe/ghast_nest.json", "w"), indent=2)


def merge_lang(code, extra):
    p = f"{A}/lang/{code}.json"
    data = json.load(open(p)) if os.path.exists(p) else {}
    data.update(extra)
    json.dump(data, open(p, "w"), indent=2, ensure_ascii=False)
    open(p, "a").write("\n")


merge_lang("en_us", {
    "item.happyghastplus.ghast_nest": "Ghast Nest",
    "block.happyghastplus.ghast_nest": "Ghast Nest",
    "item.happyghastplus.ghast_eyelids": "Ghast Eyelids",
    "message.happyghastplus.ghast_nest.no_room": "Not enough room: a nest needs a free 6x6 area",
})
merge_lang("de_de", {
    "item.happyghastplus.ghast_nest": "Ghast-Nest",
    "block.happyghastplus.ghast_nest": "Ghast-Nest",
    "item.happyghastplus.ghast_eyelids": "Ghast-Augenlider",
    "message.happyghastplus.ghast_nest.no_room": "Nicht genug Platz: ein Nest braucht eine freie 6x6-Fläche",
})
print("assets written")
