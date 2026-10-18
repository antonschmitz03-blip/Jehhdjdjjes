"""Recolours vanilla textures into the diamond machine textures.

Usage: python3 make_textures.py <vanilla assets/minecraft/textures dir>
Writes into src/main/resources/assets/diamondmachines/textures/block.
"""
import json
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "diamondmachines", "textures", "block")


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def diamond_ramp(vanilla):
    """Colours of the diamond block sorted dark to light, so a grey's brightness picks a diamond shade."""
    im = Image.open(os.path.join(vanilla, "block", "diamond_block.png")).convert("RGBA")
    colours = sorted({im.getpixel((x, y))[:3] for x in range(16) for y in range(16)}, key=lum)
    return colours


def recolour(im, ramp, keep_below=None, lo_t=0.0, hi_t=1.0):
    """Swap every opaque pixel for the diamond shade at the same relative brightness."""
    im = im.convert("RGBA")
    pixels = [im.getpixel((x, y)) for y in range(im.height) for x in range(im.width)]
    lums = [lum(p) for p in pixels if p[3] > 0 and (keep_below is None or lum(p) >= keep_below)]
    lo, hi = min(lums), max(lums)
    out = Image.new("RGBA", im.size)
    for y in range(im.height):
        for x in range(im.width):
            p = im.getpixel((x, y))
            if p[3] == 0:
                continue
            l = lum(p)
            if keep_below is not None and l < keep_below:
                # dark steel parts (the saw hub) stay as they are
                out.putpixel((x, y), p)
                continue
            t = 0 if hi == lo else (l - lo) / (hi - lo)
            t = lo_t + t * (hi_t - lo_t)
            r = ramp[min(len(ramp) - 1, int(t * (len(ramp) - 1) + 0.5))]
            out.putpixel((x, y), (*r, p[3]))
    return out


def main():
    vanilla = sys.argv[1]
    os.makedirs(OUT, exist_ok=True)
    ramp = diamond_ramp(vanilla)

    # Drill cone: Create's head uses the anvil texture for the cone, so recolour the anvil.
    anvil = Image.open(os.path.join(vanilla, "block", "anvil.png"))
    head = recolour(anvil, ramp, lo_t=0.0, hi_t=0.9)
    head.save(os.path.join(OUT, "diamond_drill_head.png"))

    # Saw blade: animated, like the vanilla stonecutter blade it is made from.
    saw = Image.open(os.path.join(vanilla, "block", "stonecutter_saw.png"))
    blade = recolour(saw, ramp, keep_below=120, lo_t=0.1, hi_t=0.75)
    blade.save(os.path.join(OUT, "diamond_saw.png"))
    with open(os.path.join(vanilla, "block", "stonecutter_saw.png.mcmeta")) as f:
        meta = json.load(f)
    with open(os.path.join(OUT, "diamond_saw.png.mcmeta"), "w") as f:
        json.dump(meta, f, indent=2)
    # Still blade for a saw that isn't turning: the first frame.
    blade.crop((0, 0, 16, 16)).save(os.path.join(OUT, "diamond_saw_static.png"))

    # Crushing wheel plates: 7 textured columns on the left (the tread), dark on the right.
    plates = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    for y in range(16):
        for x in range(7):
            plates.putpixel((x, y), head.getpixel(((x + 4) % 16, y)))
        dark = ramp[1]
        plates.putpixel((15, y), (*dark, 255))
    plates.save(os.path.join(OUT, "diamond_crushing_wheel_plates.png"))


if __name__ == "__main__":
    main()
