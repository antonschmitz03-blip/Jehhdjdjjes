"""Draws every Metal Decor texture as 16x16 pixel art.

Iron and gold share vanilla-style patterns (bevelled plates, tiles, bricks), each painted with a shade ramp picked from
the vanilla metal blocks. The diamond set is drawn with elvish motifs instead: leaves, vines and pointed arches.

Run from the metal-decor folder: python3 tools/make_textures.py [preview.png]
"""
import math
import sys
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/metaldecor/textures"

# Darkest to brightest, sampled to sit next to the vanilla iron, gold and diamond blocks.
RAMPS = {
    "iron": ["#3f3f3f", "#5f5f5f", "#7f7f7f", "#a2a2a2", "#c3c3c3", "#dcdcdc", "#f4f4f4"],
    "gold": ["#83480e", "#ad6612", "#d4901b", "#eab427", "#f9d73e", "#fcee5a", "#fffcb5"],
    "diamond": ["#0d3b37", "#137469", "#1ca898", "#2ed1bf", "#62e8da", "#a6fbea", "#e6fffb"],
}
FLAMES = {
    "gold": ["#c8641a", "#f39a2c", "#ffc75a", "#fff1b0"],
    "soul_gold": ["#1f6f87", "#3fb3c9", "#86ecf2", "#e0ffff"],
    "diamond": ["#2a8f9a", "#7fe3e0", "#c9fff6", "#ffffff"],
}

T = None  # transparent pixel


def hex_rgba(color):
    color = color.lstrip("#")
    return tuple(int(color[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def grid(value=4):
    return [[value] * 16 for _ in range(16)]


def paint(shades, ramp):
    """Shade map (indices into the ramp, None for transparent) to an image."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    colors = [hex_rgba(c) for c in ramp]
    for y, row in enumerate(shades):
        for x, s in enumerate(row):
            if s is not None:
                img.putpixel((x, y), colors[max(0, min(len(colors) - 1, s))])
    return img


def bevel_box(g, x0, y0, x1, y1, base=4, light=5, shadow=2, corner=6):
    """A raised rectangle: lit top and left edges, shaded bottom and right edges (inclusive bounds)."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            g[y][x] = base
    for x in range(x0, x1 + 1):
        g[y0][x] = light
        g[y1][x] = shadow
    for y in range(y0, y1 + 1):
        g[y][x0] = light
        g[y][x1] = shadow
    g[y0][x0] = corner
    g[y1][x0] = 3
    g[y0][x1] = 3


# ---------------------------------------------------------------- vanilla-style iron and gold

def plating():
    g = grid()
    bevel_box(g, 0, 0, 15, 7)
    bevel_box(g, 0, 8, 15, 15)
    for y in (2, 10):
        for x in (2, 13):
            g[y][x] = 6
            g[y + 1][x] = 2
            g[y][x + 1] = 3
    # A soft sheen across each plate, like the vanilla iron block.
    for y, x in ((3, 6), (3, 7), (4, 5), (11, 9), (12, 8), (12, 10)):
        g[y][x] = 5
    return g


def tiles():
    g = grid()
    for ty in range(4):
        for tx in range(4):
            base = 4 if (tx + ty) % 2 == 0 else 3
            bevel_box(g, tx * 4, ty * 4, tx * 4 + 3, ty * 4 + 3, base=base)
    return g


def cut():
    g = grid()
    for row in range(4):
        offset = 4 if row % 2 else 0
        for start in range(-8, 16, 8):
            x0 = start + offset
            for x in range(max(0, x0), min(16, x0 + 8)):
                for y in range(row * 4, row * 4 + 4):
                    g[y][x] = 4
            # Bevel the brick, clipped to the texture.
            for x in range(max(0, x0), min(16, x0 + 8)):
                g[row * 4][x] = 5
                g[row * 4 + 3][x] = 2
            for y in range(row * 4, row * 4 + 4):
                if 0 <= x0 < 16:
                    g[y][x0] = 5
                if 0 <= x0 + 7 < 16:
                    g[y][x0 + 7] = 2
            if 0 <= x0 < 16:
                g[row * 4][x0] = 6
    return g


def pillar_side():
    profile = [2, 5, 4, 2, 5, 4, 4, 2, 5, 4, 4, 2, 5, 4, 3, 1]
    g = [[profile[x] for x in range(16)] for _ in range(16)]
    for x in range(16):
        g[0][x] = 6 if 0 < x < 15 else 3
        g[1][x] = 3
        g[14][x] = 5 if 0 < x < 15 else 3
        g[15][x] = 1
    return g


def pillar_end():
    rings = [5, 2, 4, 5, 3, 4, 3, 4]
    g = grid()
    for y in range(16):
        for x in range(16):
            d = min(x, y, 15 - x, 15 - y)
            g[y][x] = rings[d]
            # Light comes from the top left.
            if d in (0, 3) and (x == 15 - d or y == 15 - d):
                g[y][x] = 2
    return g


def grate():
    g = [[T] * 16 for _ in range(16)]
    bars = {0, 1, 7, 8, 14, 15}
    for y in range(16):
        for x in range(16):
            if x in bars or y in bars:
                g[y][x] = 4
    for y in range(16):
        for x in range(16):
            if g[y][x] is None:
                continue
            if (x in bars and x in (0, 7, 14)) or (y in bars and y in (0, 7, 14)):
                g[y][x] = 5
            if (y in (1, 8, 15) and x not in bars) or (x in (1, 8, 15) and y not in bars):
                g[y][x] = 2
    for y in (0, 7, 14):
        for x in (0, 7, 14):
            g[y][x] = 6
            g[y + 1][x + 1] = 3
    return g


def bars():
    """Vertical bars at x 2-3, 7-8 and 12-13; the bars model shows 0-7 on its arms and 7-9 on the post."""
    g = [[T] * 16 for _ in range(16)]
    for y in range(16):
        for x0 in (2, 7, 12):
            g[y][x0] = 5
            g[y][x0 + 1] = 2 if x0 != 7 else 3
        g[y][8] = 2
    for x0 in (2, 7, 12):
        g[0][x0] = 6
        g[15][x0 + 1] = 1
    return g


def chain():
    """Links for the two crossed planes: columns 0-2 for one plane, 3-5 for the other, offset by half a link."""
    g = [[T] * 16 for _ in range(16)]
    for plane, offset in ((0, 0), (3, 4)):
        for start in range(-8, 16, 8):
            top = start + offset
            for dy in range(6):
                y = top + dy
                if not 0 <= y < 16:
                    continue
                if dy in (0, 5):
                    g[y][plane + 1] = 5 if dy == 0 else 2
                else:
                    g[y][plane] = 4 if dy < 3 else 3
                    g[y][plane + 2] = 2
            # Edge-on view of the next link through the gap.
            for dy in (6, 7):
                y = top + dy
                if 0 <= y < 16:
                    g[y][plane + 1] = 3
    return g


def chain_item():
    g = [[T] * 16 for _ in range(16)]
    for i, top in enumerate((0, 5, 10)):
        x = 6 + (i % 2)
        for dy in range(6):
            y = top + dy
            if y > 15:
                break
            if dy in (0, 5):
                g[y][x + 1] = 5 if dy == 0 else 2
            else:
                g[y][x] = 4
                g[y][x + 2] = 2
    return g


def bars_item():
    g = bars()
    for x in range(1, 15):
        for y in (1, 14):
            g[y][x] = 4
        g[2][x] = 2
        g[15][x] = 2
    return g


# ---------------------------------------------------------------- elvish diamond

def engrave(g, mask, groove=1, rim=5):
    """Cuts the mask into the metal: dark groove with a lit lower-right rim, as if light came from the top left."""
    for y in range(16):
        for x in range(16):
            if mask[y][x]:
                g[y][x] = groove
    for y in range(16):
        for x in range(16):
            if mask[y][x]:
                nx, ny = (x + 1) % 16, (y + 1) % 16
                if not mask[ny][nx]:
                    g[ny][nx] = rim


def leaf(mask, cx, cy, angle, length=3.2, width=1.25):
    """A pointed (vesica) leaf centred on cx, cy."""
    ca, sa = math.cos(angle), math.sin(angle)
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            u = dx * ca + dy * sa
            v = -dx * sa + dy * ca
            if abs(u) <= length:
                half = width * (1 - (u / length) ** 2)
                if abs(v) <= half + 0.15:
                    mask[y][x] = True


def vine(mask, amplitude, phase, centre=8.0, thickness=0.55, vertical=False):
    for y in range(16):
        for x in range(16):
            a, b = (y, x) if vertical else (x, y)
            curve = centre + amplitude * math.sin(2 * math.pi * (a + 0.5) / 16 + phase)
            if abs(b + 0.5 - curve) < thickness + 0.5 * abs(math.cos(2 * math.pi * (a + 0.5) / 16 + phase)) * amplitude / 8:
                mask[y][x] = True


def empty_mask():
    return [[False] * 16 for _ in range(16)]


def filigree():
    g = grid()
    bevel_box(g, 0, 0, 15, 15)
    mask = empty_mask()
    vine(mask, 3.2, 0.0)
    leaf(mask, 4.3, 4.0, -0.9, 2.6, 1.1)
    leaf(mask, 11.7, 12.0, -0.9, 2.6, 1.1)
    leaf(mask, 12.4, 5.2, 0.7, 2.0, 0.9)
    leaf(mask, 3.6, 10.8, 0.7, 2.0, 0.9)
    for y in (0, 15):
        for x in range(16):
            mask[y][x] = False
    for x in (0, 15):
        for y in range(16):
            mask[y][x] = False
    engrave(g, mask, groove=2, rim=6)
    for y in range(16):
        g[y][15] = 2
    for x in range(16):
        g[15][x] = 2
    return g


def leaf_tiles():
    """Four bevelled tiles, each with a raised leaf lying corner to corner, alternating direction."""
    g = grid()
    for ty in (0, 8):
        for tx in (0, 8):
            bevel_box(g, tx, ty, tx + 7, ty + 7, base=3)
            mask = empty_mask()
            flip = (tx + ty) % 16 == 8
            leaf(mask, tx + 4, ty + 4, math.pi * (0.75 if flip else 0.25), 3.4, 1.5)
            for y in range(ty + 1, ty + 7):
                for x in range(tx + 1, tx + 7):
                    if mask[y][x]:
                        lx, ly = x - tx, y - ty
                        on_rib = (lx == ly) if not flip else (lx + ly == 7)
                        g[y][x] = 6 if on_rib else 4
                        if not mask[y + 1][x] or not mask[y][x + 1]:
                            g[y][x] = 2 if not on_rib else 5
    return g


def arches():
    """Carved blocks, each with a pointed elvish arch in relief."""
    g = grid()
    for by in (0, 8):
        for bx in (0, 8):
            bevel_box(g, bx, by, bx + 7, by + 7)
            for y in range(by + 2, by + 7):
                for x in range(bx + 2, bx + 6):
                    dx = abs(x + 0.5 - (bx + 4))
                    dy = y - (by + 2)
                    width = [0.6, 1.3, 1.9, 2.0, 2.0][dy]
                    if dx < width:
                        g[y][x] = 1 if dy > 0 else 2
                        if dx > width - 1.0:
                            g[y][x] = 2
            g[by + 6][bx + 3] = 3
            g[by + 6][bx + 4] = 3
    return g


def vine_pillar_side():
    g = pillar_side()
    mask = empty_mask()
    for y in range(16):
        x = 8 + 5.5 * math.sin(2 * math.pi * (y + 0.5) / 16)
        mask[y][int(x)] = True
        if 0 < int(x) < 15:
            mask[y][int(x) + (1 if math.cos(2 * math.pi * (y + 0.5) / 16) > 0 else -1)] = True
    leaf(mask, 13.0, 3.0, 1.1, 2.3, 0.9)
    leaf(mask, 3.0, 11.0, 1.1, 2.3, 0.9)
    for y in range(16):
        for x in range(16):
            if mask[y][x]:
                g[y][x] = 6 if (x + y) % 3 else 5
                if y + 1 < 16 and not mask[y + 1][x]:
                    g[y + 1][x] = 1
    return g


def rosette():
    """Pillar top: an eight-petal flower inside a ring."""
    g = pillar_end()
    mask = empty_mask()
    for i in range(8):
        a = i * math.pi / 4
        leaf(mask, 8 + 3.2 * math.cos(a), 8 + 3.2 * math.sin(a), a, 2.4, 1.0)
    for y in range(16):
        for x in range(16):
            if 2 <= min(x, y, 15 - x, 15 - y):
                g[y][x] = 3
                if mask[y][x]:
                    g[y][x] = 5 if x + y < 16 else 4
    g[7][7] = g[8][8] = 6
    g[7][8] = g[8][7] = 2
    return g


def lattice():
    """See-through elvish tracery: a frame, a pointed arch and leafy branches."""
    g = [[T] * 16 for _ in range(16)]
    mask = empty_mask()
    for i in range(16):
        for j in (0, 15):
            mask[i][j] = mask[j][i] = True
    for y in range(16):
        for x in range(16):
            # Two arcs meeting in a point at the top centre.
            for cx in (2.5, 13.5):
                r = math.hypot(x + 0.5 - cx, y + 0.5 - 15.5)
                if abs(r - 11.0) < 0.6 and y < 15:
                    mask[y][x] = True
    for x in range(16):
        mask[8][x] = mask[8][x] or abs(x + 0.5 - 8) > 5
    leaf(mask, 4.5, 4.5, -0.8, 2.2, 0.9)
    leaf(mask, 11.5, 4.5, 0.8, 2.2, 0.9)
    leaf(mask, 8.0, 11.5, math.pi / 2, 2.6, 1.0)
    for y in range(16):
        for x in range(16):
            if mask[y][x]:
                g[y][x] = 4
    for y in range(16):
        for x in range(16):
            if g[y][x] is None:
                continue
            above = g[y - 1][x] if y > 0 else None
            below = g[y + 1][x] if y < 15 else None
            if above is None:
                g[y][x] = 6 if y == 0 else 5
            elif below is None:
                g[y][x] = 2
    return g


# ---------------------------------------------------------------- lanterns and pedestal

def lantern_sheet(ramp_name, flame, elvish=False):
    """Layout used by template_lantern: body side 0,0-6,7; body top 0,7-6,13; cap side 7,0-11,2; cap top 7,2-11,6;
    handle 12,0-15,2; hanging chain 12,2-15,8."""
    ramp = [hex_rgba(c) for c in RAMPS[ramp_name]]
    fl = [hex_rgba(c) for c in FLAMES[flame]]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.putpixel
    for x in range(6):
        px((x, 0), ramp[5])
        px((x, 6), ramp[2])
    for y in range(1, 6):
        px((0, y), ramp[4])
        px((5, y), ramp[2])
        for x in range(1, 5):
            glow = [1, 2, 2, 1][x - 1]
            if y in (2, 3):
                glow += 1
            px((x, y), fl[min(3, glow)])
    if elvish:
        # Leaf-shaped window: the corners of the glass are metal.
        for x, y in ((1, 1), (4, 1), (1, 5), (4, 5)):
            px((x, y), ramp[3])
    for y in range(7, 13):
        for x in range(6):
            d = min(x, y - 7, 5 - x, 12 - y)
            px((x, y), ramp[[5, 3, 1][min(d, 2)]])
    for x in range(7, 11):
        px((x, 0), ramp[5])
        px((x, 1), ramp[3])
    for y in range(2, 6):
        for x in range(7, 11):
            d = min(x - 7, y - 2, 10 - x, 5 - y)
            px((x, y), ramp[4 if d == 0 else 2])
    for x in range(12, 15):
        px((x, 0), ramp[5])
    px((12, 1), ramp[3])
    px((14, 1), ramp[2])
    for y in range(2, 8):
        px((13, y), ramp[4 if y % 3 else 2])
        if y % 3 == 0:
            px((12, y), ramp[3])
            px((14, y), ramp[2])
    return img


def lantern_item(ramp_name, flame, elvish=False):
    ramp = [hex_rgba(c) for c in RAMPS[ramp_name]]
    fl = [hex_rgba(c) for c in FLAMES[flame]]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.putpixel
    for x in (6, 9):
        px((x, 1), ramp[4])
        px((x, 2), ramp[2])
    for x in (7, 8):
        px((x, 0), ramp[5])
    for x in range(5, 11):
        px((x, 3), ramp[5])
        px((x, 4), ramp[3])
    for y in range(5, 14):
        for x in range(4, 12):
            if x in (4, 11) or y in (5, 13):
                px((x, y), ramp[5 if x == 4 or y == 5 else 2])
            else:
                glow = 1 if x in (5, 10) else 2
                if 7 <= y <= 10 and x in (7, 8):
                    glow = 3
                px((x, y), fl[glow])
    if elvish:
        for x, y in ((5, 6), (10, 6), (5, 12), (10, 12)):
            px((x, y), ramp[3])
        for y in range(6, 13):
            px((7 if y % 2 else 8, y), ramp[6] if y in (6, 12) else fl[3])
    for x in range(4, 12):
        px((x, 14), ramp[1])
    return img


def pedestal_top(ramp_name):
    """A velvet-free display plate: bevelled metal with an inset ring where the trophy floats."""
    g = grid()
    bevel_box(g, 0, 0, 15, 15)
    for y in range(3, 13):
        for x in range(3, 13):
            d = min(x - 3, y - 3, 12 - x, 12 - y)
            g[y][x] = [2, 3, 4, 4, 5][min(d, 4)]
    return paint(g, RAMPS[ramp_name])


# ---------------------------------------------------------------- output

STYLES_VANILLA = {
    "plating": plating, "tiles": tiles, "cut": cut, "pillar": pillar_side, "pillar_top": pillar_end, "grate": grate,
}
STYLES_ELVISH = {
    "plating": filigree, "tiles": leaf_tiles, "cut": arches, "pillar": vine_pillar_side, "pillar_top": rosette,
    "grate": lattice,
}


def main():
    block = OUT / "block"
    item = OUT / "item"
    block.mkdir(parents=True, exist_ok=True)
    item.mkdir(parents=True, exist_ok=True)
    made = []
    for metal in ("iron", "gold", "diamond"):
        styles = STYLES_ELVISH if metal == "diamond" else STYLES_VANILLA
        for style, fn in styles.items():
            img = paint(fn(), RAMPS[metal])
            img.save(block / f"{metal}_{style}.png")
            made.append((f"{metal}_{style}", img))
        img = pedestal_top(metal)
        img.save(block / f"{metal}_pedestal_top.png")
        made.append((f"{metal}_pedestal_top", img))
    for name, fn in (("gold_chain", chain), ("gold_bars", bars)):
        img = paint(fn(), RAMPS["gold"])
        img.save(block / f"{name}.png")
        made.append((name, img))
    paint(chain_item(), RAMPS["gold"]).save(item / "gold_chain.png")
    paint(bars_item(), RAMPS["gold"]).save(item / "gold_bars.png")
    for name, ramp, flame, elvish in (("gold_lantern", "gold", "gold", False),
                                      ("gold_soul_lantern", "gold", "soul_gold", False),
                                      ("diamond_lantern", "diamond", "diamond", True)):
        sheet = lantern_sheet(ramp, flame, elvish)
        sheet.save(block / f"{name}.png")
        icon = lantern_item(ramp, flame, elvish)
        icon.save(item / f"{name}.png")
        made.append((name + " (item)", icon))

    if len(sys.argv) > 1:
        preview(made, sys.argv[1])


def preview(made, path):
    """Each texture tiled 2x2 so seams show, scaled up 6x."""
    scale, cols = 6, 6
    cell = 32 * scale + 12
    rows = (len(made) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell, rows * cell), (40, 44, 52, 255))
    for i, (_, img) in enumerate(made):
        tile = Image.new("RGBA", (32, 32))
        for ox in (0, 16):
            for oy in (0, 16):
                tile.paste(img, (ox, oy))
        tile = tile.resize((32 * scale, 32 * scale), Image.NEAREST)
        sheet.alpha_composite(tile, ((i % cols) * cell + 6, (i // cols) * cell + 6))
    sheet.save(path)


if __name__ == "__main__":
    main()
