"""Draws the Shrine Core textures (16x16, vanilla palette style). Run from ore-shrines/: python3 tools/make_textures.py"""
import json, os, random
from PIL import Image

OUT = "src/main/resources/assets/oreshrines/textures/block"
os.makedirs(OUT, exist_ok=True)

STONE = [(30, 30, 32), (40, 40, 42), (52, 51, 52), (63, 63, 65), (74, 75, 78), (88, 88, 90), (108, 108, 110)]
GOLD = [(150, 96, 22), (204, 142, 39), (226, 168, 40), (245, 204, 39), (255, 222, 70), (255, 240, 110), (255, 253, 170)]
IRON = [(120, 120, 124), (160, 160, 162), (185, 185, 185), (206, 206, 206), (222, 222, 222), (236, 236, 236), (248, 248, 248)]
CRYSTALS = {
    "dormant": [(46, 54, 58), (74, 84, 88), (108, 120, 124), (146, 158, 160), (188, 198, 198), (232, 240, 240)],
    "harvest": [(14, 70, 34), (24, 116, 54), (38, 166, 78), (78, 208, 116), (148, 238, 166), (224, 255, 230)],
    "ward": [(28, 36, 112), (46, 66, 170), (76, 104, 218), (116, 148, 246), (168, 194, 255), (230, 238, 255)],
    "mending": [(104, 22, 56), (164, 38, 86), (218, 70, 124), (246, 122, 164), (255, 182, 204), (255, 236, 242)],
    "hearth": [(114, 36, 8), (180, 70, 12), (230, 114, 24), (255, 160, 50), (255, 208, 118), (255, 246, 214)],
    "pasture": [(102, 76, 18), (160, 122, 32), (208, 172, 54), (238, 210, 88), (250, 236, 156), (255, 252, 226)],
    "wisdom": [(52, 104, 14), (96, 160, 22), (150, 214, 40), (198, 246, 82), (230, 255, 156), (250, 255, 228)],
}


def img(pixels):
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), pixels[y][x] + (255,))
    return im


def grid(fn):
    return [[fn(x, y) for x in range(16)] for y in range(16)]


def stone_noise(r):
    return STONE[r.choice([3, 3, 4, 4, 4, 5, 5, 2])]


def stone_side():
    r = random.Random(11)
    def px(x, y):
        if y == 0:
            return STONE[6]
        if y == 15:
            return STONE[0]
        if y == 1:
            return STONE[5]
        # engraved band with gold inlay, rows 6-8 (used by the plinth)
        if y == 6:
            return STONE[1]
        if y == 7:
            return GOLD[4] if x % 4 in (1, 2) else STONE[0]
        if y == 8:
            return GOLD[1] if x % 4 in (1, 2) else STONE[2]
        if y == 9:
            return STONE[5]
        # carved bricks below the band (used by the base slab, rows 13-15)
        if y == 12:
            return STONE[2]
        if y in (13, 14) and x in (0, 8):
            return STONE[1]
        return stone_noise(r)
    return img(grid(px))


def stone_top():
    r = random.Random(12)
    def px(x, y):
        edge = min(x, y, 15 - x, 15 - y)
        if edge == 0:
            return STONE[1] if (x == 15 or y == 15) else STONE[5]
        if edge == 3:
            return GOLD[3] if (x + y) % 2 else GOLD[5]
        if edge == 4:
            return STONE[1]
        if edge >= 5:
            return STONE[r.choice([1, 2, 2, 3])]
        return stone_noise(r)
    return img(grid(px))


def gilding():
    r = random.Random(13)
    def px(x, y):
        if y == 0 or x == 0:
            return GOLD[6]
        if y == 15 or x == 15:
            return GOLD[0]
        for cx, cy in ((3, 3), (12, 3), (3, 12), (12, 12)):
            if (x, y) == (cx, cy):
                return GOLD[6]
            if (x, y) in ((cx + 1, cy), (cx, cy + 1), (cx + 1, cy + 1)):
                return GOLD[1]
        band = (y // 2) % 2
        return GOLD[r.choice([3, 4] if band else [4, 5])]
    return img(grid(px))


def pillar():
    r = random.Random(14)
    flute = [1, 3, 5, 6, 4, 2, 4, 6, 5, 3, 1, 3, 5, 6, 4, 2]
    def px(x, y):
        if y == 0:
            return IRON[6]
        if y == 15:
            return IRON[0]
        v = flute[x] + (1 if r.random() < 0.08 else 0) - (1 if r.random() < 0.08 else 0)
        return IRON[max(0, min(6, v))]
    return img(grid(px))


def crystal_frame(ramp, shift):
    """Faceted gem: four facets lit from the top left, a dark rim, and a glint that travels with `shift`."""
    def px(x, y):
        cx, cy = x - 7.5, y - 7.5
        edge = min(x, y, 15 - x, 15 - y)
        if edge == 0:
            return ramp[0]
        if abs(cx) - abs(cy) in (-0.0, 0.0) or abs(abs(cx) - abs(cy)) < 0.6:
            v = 4
        elif cy < 0 and abs(cx) < abs(cy):
            v = 4  # top facet
        elif cx < 0 and abs(cy) < abs(cx):
            v = 3  # left facet
        elif cx > 0 and abs(cy) < abs(cx):
            v = 2  # right facet
        else:
            v = 1  # bottom facet
        if edge == 1:
            v = max(1, v - 1)
        d = (x + y - shift) % 32
        if d in (0, 1):
            v = min(5, v + 2)
        if (x, y) in ((4, 4), (5, 3), (3, 5)):
            v = 5
        return ramp[v]
    return img(grid(px))


def save(name, im):
    im.save(f"{OUT}/{name}.png")


save("shrine_stone_side", stone_side())
save("shrine_stone_top", stone_top())
save("shrine_gilding", gilding())
save("shrine_pillar", pillar())
for name, ramp in CRYSTALS.items():
    if name == "dormant":
        save("shrine_crystal_dormant", crystal_frame(ramp, 100))
        continue
    frames = [crystal_frame(ramp, s) for s in range(0, 32, 4)]
    sheet = Image.new("RGBA", (16, 16 * len(frames)))
    for i, f in enumerate(frames):
        sheet.paste(f, (0, 16 * i))
    save(f"shrine_crystal_{name}", sheet)
    with open(f"{OUT}/shrine_crystal_{name}.png.mcmeta", "w") as f:
        json.dump({"animation": {"frametime": 4, "interpolate": True}}, f, indent=2)
