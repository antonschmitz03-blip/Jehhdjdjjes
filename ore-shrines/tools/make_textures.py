"""Draws the Shrine Core textures from vanilla samples, so they match the vanilla look.

Usage (from ore-shrines/): python3 tools/make_textures.py <folder with vanilla block textures>
The vanilla folder is assets/minecraft/textures/block from the game jar; it is only read, never shipped.
"""
import colorsys, json, os, random, sys
from PIL import Image

VANILLA = sys.argv[1]
OUT = "src/main/resources/assets/oreshrines/textures/block"
os.makedirs(OUT, exist_ok=True)

# Dark to light, six steps per effect; the grey one is the dormant crystal.
RAMPS = {
    "dormant": [(38, 40, 44), (62, 66, 70), (92, 96, 100), (128, 132, 134), (170, 172, 172), (214, 214, 210)],
    "harvest": [(10, 58, 30), (20, 98, 46), (36, 146, 68), (74, 194, 104), (140, 232, 152), (214, 255, 214)],
    "ward": [(24, 30, 96), (40, 56, 150), (66, 94, 204), (106, 138, 238), (160, 186, 255), (222, 232, 255)],
    "mending": [(90, 18, 50), (148, 32, 78), (204, 62, 114), (238, 112, 156), (252, 172, 198), (255, 228, 236)],
    "hearth": [(96, 30, 6), (160, 62, 10), (216, 104, 20), (250, 150, 44), (255, 200, 108), (255, 240, 200)],
    "pasture": [(90, 66, 14), (146, 110, 28), (196, 158, 48), (230, 200, 80), (246, 230, 146), (255, 250, 214)],
    "wisdom": [(40, 90, 10), (78, 142, 18), (130, 196, 34), (182, 236, 72), (220, 252, 146), (246, 255, 220)],
}
GOLD = [(118, 72, 14), (176, 112, 22), (222, 156, 30), (248, 196, 40), (255, 226, 80), (255, 248, 160)]


def load(name):
    return Image.open(f"{VANILLA}/{name}.png").convert("RGBA").crop((0, 0, 16, 16))


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def recolor(src, ramp, lift=0.0):
    """Maps each pixel's brightness rank onto `ramp`, keeping vanilla's shading pattern and alpha."""
    colors = sorted({px[:3] for px in src.getdata() if px[3] > 0}, key=lum)
    lo, hi = lum(colors[0]), lum(colors[-1])
    out = Image.new("RGBA", src.size)
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = src.getpixel((x, y))
            if a == 0:
                continue
            t = (lum((r, g, b)) - lo) / max(1, hi - lo)
            t = min(1.0, t + lift)
            f = t * (len(ramp) - 1)
            i = min(len(ramp) - 2, int(f))
            k = f - i
            c = tuple(int(ramp[i][j] * (1 - k) + ramp[i + 1][j] * k) for j in range(3))
            out.putpixel((x, y), c + (a,))
    return out


def gold_at(t):
    return GOLD[max(0, min(len(GOLD) - 1, t))] + (255,)


def shell_side():
    """Body wall between the corner pillars: polished blackstone with a gilded window into the glowing heart."""
    base = load("polished_blackstone").copy()
    gild = load("gilded_blackstone")
    r = random.Random(21)
    for y in range(16):
        for x in range(16):
            if r.random() < 0.06:
                base.putpixel((x, y), gild.getpixel((x, y)))
    for y in range(3, 13):
        for x in range(4, 12):
            ring = y in (3, 12) or x in (4, 11)
            if ring:
                # Worn gilding: lit on the top/left, shaded on the bottom/right, with the odd dark gap.
                lit = y == 3 or x == 4
                if r.random() < 0.1:
                    continue
                base.putpixel((x, y), gold_at(r.choice([3, 4, 4, 5]) if lit else r.choice([0, 1, 1, 2])))
            elif y in (4, 11) or x in (5, 10):
                base.putpixel((x, y), (24, 20, 26, 255))
            else:
                base.putpixel((x, y), (0, 0, 0, 0))
    return base


def trim():
    """Footing and capstone: blackstone with a gilded lip on the top and bottom rows."""
    base = load("polished_blackstone").copy()
    r = random.Random(24)
    for x in range(16):
        if r.random() < 0.9:
            base.putpixel((x, 0), gold_at(r.choice([3, 4, 4, 5])))
        if r.random() < 0.9:
            base.putpixel((x, 15), gold_at(r.choice([0, 1, 1, 2])))
        if r.random() < 0.25:
            base.putpixel((x, 1), gold_at(r.choice([1, 2])))
        if r.random() < 0.25:
            base.putpixel((x, 14), gold_at(r.choice([0, 1])))
    for x in range(16):
        base.putpixel((x, 2), tuple(int(v * 0.7) for v in base.getpixel((x, 2))[:3]) + (255,))
    return base


def pillar():
    """Corner pillars: gilded blackstone, so the gold veins run up the corners."""
    return load("gilded_blackstone").copy()


def shell_top():
    base = load("polished_blackstone").copy()
    r = random.Random(22)
    for y in range(16):
        for x in range(16):
            edge = min(x, y, 15 - x, 15 - y)
            if edge == 0 and r.random() < 0.85:
                base.putpixel((x, y), gold_at(r.choice([1, 2, 3])))
            elif edge == 3:
                lit = x == 3 or y == 3
                if r.random() < 0.1:
                    continue
                base.putpixel((x, y), gold_at(r.choice([3, 4, 4, 5]) if lit else r.choice([0, 1, 1, 2])))
            elif edge >= 4:
                base.putpixel((x, y), (0, 0, 0, 0))
    return base


def shell_bottom():
    base = load("polished_blackstone").copy()
    r = random.Random(23)
    for y in range(16):
        for x in range(16):
            if min(x, y, 15 - x, 15 - y) == 0 and r.random() < 0.6:
                base.putpixel((x, y), gold_at(r.choice([0, 1, 2])))
    return base


def pulse(frames_src, ramp):
    """Two frames, the second a little brighter; interpolated, so the crystal slowly breathes."""
    a = recolor(frames_src, ramp)
    b = recolor(frames_src, ramp, lift=0.12)
    sheet = Image.new("RGBA", (16, 32))
    sheet.paste(a, (0, 0))
    sheet.paste(b, (0, 16))
    return sheet


for old in os.listdir(OUT):
    os.remove(os.path.join(OUT, old))

shell_side().save(f"{OUT}/shrine_core_side.png")
trim().save(f"{OUT}/shrine_core_trim.png")
pillar().save(f"{OUT}/shrine_core_pillar.png")
shell_top().save(f"{OUT}/shrine_core_top.png")
shell_bottom().save(f"{OUT}/shrine_core_bottom.png")
heart = load("amethyst_block")
cluster = load("amethyst_cluster")
for name, ramp in RAMPS.items():
    if name == "dormant":
        recolor(heart, ramp).save(f"{OUT}/shrine_heart_dormant.png")
        recolor(cluster, ramp).save(f"{OUT}/shrine_cluster_dormant.png")
        continue
    for kind, src in (("heart", heart), ("cluster", cluster)):
        pulse(src, ramp).save(f"{OUT}/shrine_{kind}_{name}.png")
        with open(f"{OUT}/shrine_{kind}_{name}.png.mcmeta", "w") as f:
            json.dump({"animation": {"frametime": 30, "interpolate": True}}, f, indent=2)
