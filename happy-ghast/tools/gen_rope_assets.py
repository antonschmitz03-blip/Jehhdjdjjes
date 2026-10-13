#!/usr/bin/env python3
"""Generates the pieces of the ghast's rope ladder: item models ghast_rope_segment (one block of ladder) and
ghast_rope_top (the same with a flat strap over the ghast's back), item definitions, and the shared 16x16 texture.
The ladder plane is perpendicular to x, the strap on top runs towards -x (the ghast's body); rails run along y at z=2..4 and 12..14.
Texture: columns 0-3 twisted rope, columns 4-15 wood."""
import json, os, random
from PIL import Image

A = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "happyghastplus")
rng = random.Random(11)
T = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for y in range(16):
    for x in range(16):
        if x < 4:  # rope: diagonal twist
            base = (176, 140, 84) if (x + y) % 4 < 2 else (140, 106, 60)
        else:      # wood grain, vertical
            base = (140, 104, 58) if x % 3 else (116, 84, 46)
        d = rng.choice((-8, 0, 0, 8))
        c = tuple(max(0, min(255, v + d)) for v in base)
        T.putpixel((x, y), c + (255,))
T.save(f"{A}/textures/item/ghast_rope.png")


def face(uv):
    return {"uv": uv, "texture": "#t"}


def rail(z0, y0=0, y1=16):
    return {"from": [7, y0, z0], "to": [9, y1, z0 + 2],
            "faces": {"north": face([0, 0, 2, 16]), "south": face([0, 0, 2, 16]), "east": face([2, 0, 4, 16]),
                      "west": face([2, 0, 4, 16]), "up": face([0, 0, 2, 2]), "down": face([0, 0, 2, 2])}}


def rung(y0):
    return {"from": [6.5, y0, 4], "to": [8.5, y0 + 2, 12],
            "faces": {"north": face([4, 0, 6, 2]), "south": face([4, 0, 6, 2]), "east": face([4, 0, 12, 2]),
                      "west": face([4, 0, 12, 2]), "up": face([4, 2, 6, 10]), "down": face([4, 2, 6, 10])}}


segment = [rail(2), rail(12), rung(2), rung(10)]
# top piece: the rails stop at the top, where a flat strap runs over the ghast's back (towards -x)
strap = {"from": [-6, 13.5, 2], "to": [9, 16, 14],
         "faces": {d: face([4, 0, 12, 4]) for d in ("north", "south", "east", "west", "up", "down")}}
top = [rail(2, 0, 13.5), rail(12, 0, 13.5), rung(2), rung(8), strap]
tex = {"t": "happyghastplus:item/ghast_rope", "particle": "happyghastplus:item/ghast_rope"}
for name, els in (("ghast_rope_segment", segment), ("ghast_rope_top", top)):
    json.dump({"textures": tex, "elements": els}, open(f"{A}/models/item/{name}.json", "w"), indent=1)
    json.dump({"model": {"type": "minecraft:model", "model": f"happyghastplus:item/{name}"}}, open(f"{A}/items/{name}.json", "w"), indent=1)
print("rope assets written")
