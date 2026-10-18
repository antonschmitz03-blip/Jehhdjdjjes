"""Draws every Metal Decor texture as 16x16 pixel art, based on the vanilla textures so they sit right next to them.

- Iron and gold plating, cut blocks, grates and pedestal tops are the vanilla copper block, cut copper, copper grate and
  chiseled copper, recoloured with the shades of the vanilla iron and gold blocks.
- Gold lanterns, the gold chain and gold bars are the vanilla lantern, soul lantern, iron chain and iron bars with a
  gold frame. The diamond lantern gets a diamond frame and a pale elvish flame.
- Tiles, pillars and the whole elvish diamond set are drawn here, in the same shades, with the vanilla block's own
  pixel noise laid over the flat areas.

The vanilla samples come from the CI step "Vanilla texture samples" (decode its base64 lines into
tools/vanilla-samples/<name>.png); they are not committed. Run from the metal-decor folder:
python3 tools/make_textures.py [preview.png]
"""
import math
import sys
from pathlib import Path

from PIL import Image

TOOLS = Path(__file__).resolve().parent
OUT = TOOLS.parent / "src/main/resources/assets/metaldecor/textures"
VANILLA = TOOLS / "vanilla-samples"

# Eight shades, darkest to brightest, matched rank for rank to the eight vanilla copper shades. Picked from the vanilla
# iron, gold and diamond blocks, a step darker so the decor doesn't glare next to them.
RAMPS = {
    "iron": ["#5f5f5f", "#727272", "#868686", "#999999", "#acacac", "#bdbdbd", "#cfcfcf", "#e0e0e0"],
    "gold": ["#8f5212", "#ae6d26", "#c98c1c", "#e0a21f", "#f2bd2a", "#f8cf36", "#fde34a", "#fff28a"],
    "diamond": ["#0a7f86", "#0ea3a8", "#14bcbf", "#2ad3d6", "#45e3df", "#64f0e4", "#8ff8ea", "#c6fff4"],
}
COPPER = ["#904931", "#9a5038", "#a75a40", "#b26247", "#c26b4c", "#c87456", "#d67b5b", "#e3826c"]

T = None  # transparent pixel


def hex_rgba(color):
    color = color.lstrip("#")
    return tuple(int(color[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def vanilla(name):
    return Image.open(VANILLA / f"{name}.png").convert("RGBA")


def grid(value=4):
    return [[value] * 16 for _ in range(16)]


def lum(c):
    return 0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2]


def nearest(color, ramp):
    l = lum(color)
    return min(range(len(ramp)), key=lambda i: abs(lum(hex_rgba(ramp[i])) - l))


def field(turn=0):
    """The uneven diagonal shine of the vanilla copper block, squeezed into shades 3-6. Flat areas take their shade
    from it, so nothing is perfectly even. turn rotates it so neighbouring textures don't shine the same way."""
    src = vanilla("copper_block").rotate(90 * turn)
    table = {hex_rgba(c)[:3]: i for i, c in enumerate(COPPER)}
    return [[3 + round(table[src.getpixel((x, y))[:3]] * 3 / 7) for x in range(16)] for y in range(16)]


def paint(shades, metal, turn=0):
    """Shade map to an image. 4 is the flat surface (follows the shine field), 3 a little below it, 5 a little above,
    6 a highlight, 0-2 fixed dark shades, None transparent."""
    ramp = [hex_rgba(c) for c in RAMPS[metal]]
    f = field(turn)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(shades):
        for x, s in enumerate(row):
            if s is None:
                continue
            v = {3: f[y][x] - 1, 4: f[y][x], 5: min(7, f[y][x] + 1), 6: 7}.get(s, s)
            img.putpixel((x, y), ramp[max(0, min(7, v))])
    return img


def ascii_map(rows):
    """Hand-drawn map: . flat, d below, l above, H highlight, M/D/X dark (2/1/0), space transparent."""
    key = {".": 4, "d": 3, "l": 5, "H": 6, "M": 2, "D": 1, "X": 0, " ": None}
    return [[key[c] for c in row] for row in rows]


def recolor(name, mapping):
    """Swaps colours of a vanilla texture: mapping is source hex -> target hex."""
    src = vanilla(name)
    out = src.copy()
    table = {hex_rgba(k)[:3]: hex_rgba(v) for k, v in mapping.items()}
    for y in range(src.height):
        for x in range(src.width):
            p = src.getpixel((x, y))
            if p[3] and p[:3] in table:
                out.putpixel((x, y), table[p[:3]][:3] + (p[3],))
    return out


def from_copper(name, metal):
    return recolor(name, dict(zip(COPPER, RAMPS[metal])))


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

def tiles():
    return ascii_map([
        "llllHlll.lllHllM",
        "l..dl..Dl..dl.dD",
        "l...l.dDl...l..D",
        "dDMDdMDXdDDMdMDX",
        "lllMl.lll.llHllD",
        "l..Dl..dl..Dl..d",
        "l.ddl...l.ddl..D",
        "dMDXdDMDdMDDdDMX",
        "lHllll.DlllllHlM",
        "l..Dl..dl.dDl..D",
        "l...l..Dl..Dl.dd",
        "dDMDdDDXdDMDdMDX",
        "lllDlHllllHMlllD",
        "l..dl..Dl..dl..D",
        "l.dDl..dl...l.dD",
        "MDDXDMDXMDDXDDMX",
    ])


def pillar_side():
    return ascii_map([
        "lHllllHlllllHllM",
        "DMDDDMDDDDMDDDDX",
        "l.dDl..dDl.dDl.D",
        "l.dDl..dDl..Ml.D",
        "l..Dl.ddDl.dDl.D",
        "l.dMl..dDl.dDldD",
        "l.dDl..dDl.dDl.D",
        "l.dDl..dMl..Dl.D",
        "l.dDl.d.Dl.dDl.D",
        "l..Dl..dDl.dDl.M",
        "l.dDl..dDlddDl.D",
        "l.dDl..dDl.dMl.D",
        "l.dMl..dDl.dDl.D",
        "l.dDl..dDl..Dl.D",
        "llllHlllllllHllD",
        "MDDXDDMDDXDDDMDX",
    ])


def pillar_end():
    return ascii_map([
        "llllHllllllHlllM",
        "l..............D",
        "l.DDDMDDDDDDMD.D",
        "l.Dllllllllll..D",
        "l.Dl........dl.D",
        "l.Ml.HlllllD.l.D",
        "l.Dl.l.....D.l.D",
        "l.Dl.l.dd..D.l.D",
        "l.Dl.l.dd..M.l.D",
        "l.Dl.l.....D.l.D",
        "l.Dl.DDMDDDD.l.D",
        "l.Dl.........l.D",
        "l.Dllllllllll..D",
        "l..............D",
        "l..d......d....D",
        "MDDXDDMDDDDXDDMX",
    ])


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
    engrave(g, mask, groove=1, rim=5)
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

# ---------------------------------------------------------------- output

# Frame colours of the vanilla lanterns, chain and bars, darkest first.
LANTERN_FRAME = ["#252c3d", "#3e4453", "#424a5e", "#495065", "#814023", "#8b5230"]
LANTERN_FLAME = ["#c36322", "#f09149", "#f9c966", "#fdfd8b", "#ffffd5"]
GOLD_FRAME = ["#6e3f0f", "#ae6d26", "#b8761f", "#c98c1c", "#a8641c", "#d99a20"]
DIAMOND_FRAME = ["#0a5f63", "#0ebabd", "#15c2c6", "#3de0e5", "#1a9aa0", "#4bede6"]
ELVISH_FLAME = ["#5fd8e0", "#9ff3f0", "#d8fffa", "#f0fffd", "#ffffff"]
CHAIN = ["#252c3d", "#3e4453", "#495065"]
BARS = ["#686868", "#72796e", "#9c9c9c", "#abacab", "#c5c5c5"]


def save(img, path, made, label):
    img.save(path)
    made.append((label, img))


def main():
    block = OUT / "block"
    item = OUT / "item"
    block.mkdir(parents=True, exist_ok=True)
    item.mkdir(parents=True, exist_ok=True)
    made = []
    for metal in ("iron", "gold"):
        save(from_copper("copper_block", metal), block / f"{metal}_plating.png", made, f"{metal}_plating")
        save(paint(tiles(), metal, 1), block / f"{metal}_tiles.png", made, f"{metal}_tiles")
        save(from_copper("cut_copper", metal), block / f"{metal}_cut.png", made, f"{metal}_cut")
        save(paint(pillar_side(), metal, 0), block / f"{metal}_pillar.png", made, f"{metal}_pillar")
        save(paint(pillar_end(), metal, 2), block / f"{metal}_pillar_top.png", made, f"{metal}_pillar_top")
        save(from_copper("copper_grate", metal), block / f"{metal}_grate.png", made, f"{metal}_grate")
        save(from_copper("chiseled_copper", metal), block / f"{metal}_pedestal_top.png", made, f"{metal}_pedestal_top")
    for turn, (style, fn) in enumerate((("plating", filigree), ("tiles", leaf_tiles), ("cut", arches),
                                        ("pillar", vine_pillar_side), ("pillar_top", rosette), ("grate", lattice),
                                        ("pedestal_top", rosette))):
        save(paint(fn(), "diamond", turn % 4), block / f"diamond_{style}.png", made, f"diamond_{style}")

    gold_frame = dict(zip(LANTERN_FRAME, GOLD_FRAME))
    diamond_lantern = {**dict(zip(LANTERN_FRAME, DIAMOND_FRAME)), **dict(zip(LANTERN_FLAME, ELVISH_FLAME))}
    for name, source, mapping in (("gold_lantern", "lantern", gold_frame),
                                  ("gold_soul_lantern", "soul_lantern", gold_frame),
                                  ("diamond_lantern", "lantern", diamond_lantern)):
        save(recolor(source, mapping), block / f"{name}.png", made, name)
        # Three flicker frames, like the vanilla lantern.
        (block / f"{name}.png.mcmeta").write_text('{"animation": {"interpolate": true, "frametime": 8}}\n')
        save(recolor("item_" + source, mapping), item / f"{name}.png", made, name + " (item)")
    gold_chain = dict(zip(CHAIN, ["#8a5314", "#d39632", "#f9bd23"]))
    save(recolor("iron_chain", gold_chain), block / "gold_chain.png", made, "gold_chain")
    save(recolor("item_iron_chain", gold_chain), item / "gold_chain.png", made, "gold_chain (item)")
    gold_bars = dict(zip(BARS, ["#a8641c", "#c98c1c", "#eea41a", "#f7c431", "#ffec4f"]))
    save(recolor("iron_bars", gold_bars), block / "gold_bars.png", made, "gold_bars")
    save(recolor("iron_bars", gold_bars), item / "gold_bars.png", made, "gold_bars (item)")

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
                tile.paste(img.crop((0, 0, 16, 16)), (ox, oy))
        tile = tile.resize((32 * scale, 32 * scale), Image.NEAREST)
        sheet.alpha_composite(tile, ((i % cols) * cell + 6, (i // cols) * cell + 6))
    sheet.save(path)


if __name__ == "__main__":
    main()
