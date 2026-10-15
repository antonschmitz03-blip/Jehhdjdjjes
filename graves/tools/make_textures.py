"""Paints the grave's custom 16x16 textures in a vanilla stone style.

Run with: python3 graves/tools/make_textures.py  (needs Pillow). Output goes to the mod's
assets; the PNGs are committed, so this only needs re-running after changing it.
"""
import random
import sys
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/simplegraves/textures/block"

STONE = [(0x7f, 0x7f, 0x7f), (0x77, 0x77, 0x77), (0x86, 0x86, 0x86), (0x70, 0x70, 0x70), (0x8c, 0x8c, 0x8c)]
MOSS = [(0x59, 0x6e, 0x2d), (0x4d, 0x5f, 0x26), (0x6b, 0x84, 0x34), (0x3f, 0x50, 0x1f)]
GROOVE = (0x58, 0x58, 0x5b)
GROOVE_DEEP = (0x4b, 0x4b, 0x4f)


def shade(c, d):
    return tuple(max(0, min(255, v + d)) for v in c)


def noise(rng, lighten=0):
    return [[shade(rng.choice(STONE), lighten + rng.randint(-3, 3)) for _ in range(16)] for _ in range(16)]


def moss(rng, px, rows, chance):
    """Sprinkles moss on the given rows, denser towards the bottom and the edges."""
    for v in rows:
        for u in range(16):
            edge = 1.6 if u < 3 or u > 12 else 1.0
            depth = (v - rows[0] + 1) / len(rows)
            if rng.random() < chance * depth * edge:
                px[v][u] = rng.choice(MOSS)


def headstone_mask():
    """Front silhouette in texture space (u = 16 - x, v = 16 - y), matching the model's elements."""
    rows = {1: (6, 10), 2: (5, 11), 3: (4, 12)}
    mask = [[False] * 16 for _ in range(16)]
    for v in range(1, 13):
        lo, hi = rows.get(v, (3, 13))
        for u in range(lo, hi):
            mask[v][u] = True
    return mask


def front(rng):
    # Smoother than raw stone, like a dressed face, so the engraving reads.
    px = [[tuple((c + 0x84) // 2 for c in p) for p in row] for row in noise(rng, 4)]
    mask = headstone_mask()
    inside = lambda u, v: 0 <= u < 16 and 0 <= v < 16 and mask[v][u]
    # Worn bevel: lit along the top/left of the silhouette, shadowed along the bottom/right.
    for v in range(16):
        for u in range(16):
            if not mask[v][u]:
                continue
            if not inside(u, v - 1) or not inside(u - 1, v):
                px[v][u] = shade(px[v][u], 14)
            elif not inside(u, v + 1) or not inside(u + 1, v):
                px[v][u] = shade(px[v][u], -16)
    # Engraved cross: dark groove, light lip below/right of it.
    groove = {(u, v) for v in range(4, 11) for u in (7, 8)} | {(u, 6) for u in range(5, 11)}
    for u, v in groove:
        px[v][u] = GROOVE_DEEP if (u - 1, v) not in groove or (u, v - 1) not in groove else GROOVE
    for u, v in groove:
        for du, dv in ((1, 0), (0, 1)):
            n = (u + du, v + dv)
            if n not in groove and inside(*n):
                px[n[1]][n[0]] = shade(px[n[1]][n[0]], 10)
    # Two short worn inscription lines under the cross.
    for u in range(5, 11):
        if rng.random() < 0.8:
            px[11][u] = GROOVE
    for u in range(6, 10):
        if rng.random() < 0.7:
            px[12][u] = shade(GROOVE, 8)
    # Hairline crack from the top right shoulder.
    for u, v in ((11, 3), (11, 4), (10, 5), (10, 6)):
        px[v][u] = shade(px[v][u], -22)
    moss(rng, px, list(range(10, 13)), 0.12)
    return px


def side(rng):
    px = noise(rng, 2)
    for u in range(16):
        px[15][u] = shade(px[15][u], -12)
    moss(rng, px, list(range(11, 16)), 0.25)
    return px


def top(rng):
    px = noise(rng, 8)
    for _ in range(5):
        u, v = rng.randrange(16), rng.randrange(16)
        px[v][u] = rng.choice(MOSS)
    return px


def save(name, px):
    img = Image.new("RGBA", (16, 16))
    for v in range(16):
        for u in range(16):
            img.putpixel((u, v), px[v][u] + (255,))
    OUT.mkdir(parents=True, exist_ok=True)
    img.save(OUT / f"{name}.png")


if __name__ == "__main__":
    seed = int(sys.argv[1]) if len(sys.argv) > 1 else 7
    save("headstone_front", front(random.Random(seed)))
    save("headstone_side", side(random.Random(seed + 1)))
    save("headstone_top", top(random.Random(seed + 2)))
