"""Generates the armored harness assets and data (iron, diamond and netherite, one item per harness colour).

Each armored harness draws the vanilla harness of its colour and our metal plates on top of it, both in the world
(equipment asset layers) and in the inventory (item model layers), so only the plates are our own textures.

Writes the plate textures, the 48 equipment assets, item definitions and item models, the upgrade recipes (any harness
+ 4 iron -> iron, iron + 4 diamonds -> diamond, diamond -> netherite at the smithing table), one redye recipe per
colour and tier, the minecraft:harnesses tag entries, and the item names. Run from the repository root:
python happy-ghast/tools/gen_armored_harness.py

The entity plate rectangles follow the vanilla 128x128 happy_ghast_body harness texture: its top face is x 32..63,
the side strip starts at y 32 (west 0..31, front 32..63, east 64..95, back 96..127), and the brown leather the plates
sit on covers the top face and the upper rows of the side strip.
"""
import json
import os
import struct
import zlib

HERE = os.path.dirname(__file__)
RES = os.path.join(HERE, '..', 'src', 'main', 'resources')
ASSETS = os.path.join(RES, 'assets', 'happyghastplus')
DATA = os.path.join(RES, 'data')
LANG = os.path.join(ASSETS, 'lang')

COLORS = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan',
          'purple', 'blue', 'brown', 'green', 'red', 'black']
COLOR_EN = {c: c.replace('_', ' ').title() for c in COLORS}
COLOR_DE = {
    'white': 'Weißes', 'orange': 'Oranges', 'magenta': 'Magenta', 'light_blue': 'Hellblaues', 'yellow': 'Gelbes',
    'lime': 'Hellgrünes', 'pink': 'Rosa', 'gray': 'Graues', 'light_gray': 'Hellgraues', 'cyan': 'Türkises',
    'purple': 'Violettes', 'blue': 'Blaues', 'brown': 'Braunes', 'green': 'Grünes', 'red': 'Rotes',
    'black': 'Schwarzes',
}
TIERS = ['iron', 'diamond', 'netherite']
TIER_EN = {'iron': 'Iron', 'diamond': 'Diamond', 'netherite': 'Netherite'}
TIER_DE = {'iron': 'Eisengeschirr', 'diamond': 'Diamantgeschirr', 'netherite': 'Netheritgeschirr'}

# outline, shadow, mid, base, highlight, then the accent (rivets, trim, inlays) and its shadow
PALETTES = {
    'iron': [(70, 70, 76), (128, 128, 136), (168, 168, 174), (198, 198, 204), (238, 238, 242), (200, 152, 70), (128, 88, 36)],
    'diamond': [(10, 68, 64), (24, 148, 138), (56, 198, 186), (98, 228, 216), (196, 255, 248), (255, 255, 255), (40, 120, 200)],
    'netherite': [(22, 18, 20), (46, 40, 44), (62, 55, 59), (78, 70, 74), (116, 106, 110), (236, 186, 58), (150, 96, 20)],
}
OUTLINE, SHADOW, MID, BASE, HIGHLIGHT, ACCENT, ACCENT_SHADOW = range(7)

# The vanilla 128x128 happy_ghast_body harness texture is a 16x16x16 box at 2 pixels per unit: the top face is
# x 32..63, y 0..31 (y 31 is the front edge), and the side faces start at y 32: west x 0..31, front 32..63, east
# 64..95, back 96..127. The goggles are a 16x5x5 box whose front face is x 10..41, y 74..83, with the lenses at
# x 15..22 and 29..36 and its side faces at x 0..9 and 42..51. Everything here is mirror-symmetric where the vanilla
# straps are: the buckle straps hang at x 13..17 (west) and 78..82 (east), the tail straps at x 107..109 and 114..116.


def rect(x0, y0, x1, y1, round_corners=True):
    cells = {(x, y) for x in range(x0, x1 + 1) for y in range(y0, y1 + 1)}
    if round_corners:
        cells -= {(x0, y0), (x1, y0), (x0, y1), (x1, y1)}
    return cells


def lame(x0, y0, x1, y1, point):
    """A skirt plate whose lower edge comes to a blunt point `point` rows below y1."""
    cells = rect(x0, y0, x1, y1)
    cx, hw = (x0 + x1) / 2, (x1 - x0) / 2
    for k in range(1, point + 1):
        w = hw * (1 - k / (point + 1))
        cells |= {(x, y1 + k) for x in range(x0, x1 + 1) if abs(x - cx) <= w}
    return cells


def spine(x0, y0, x1, y1):
    """The plate down the middle of the saddle, pointed at the back end (y0)."""
    cells = rect(x0, y0 + 3, x1, y1)
    cx = (x0 + x1) / 2
    for k in range(3):
        cells |= {(x, y0 + k) for x in range(x0, x1 + 1) if abs(x - cx) <= 1.5 + k * 2}
    return cells


def shield(x0, y0, x1, y1):
    """The chest piece: a small shield that runs from the saddle down over the leather tongue."""
    cells = rect(x0, y0, x1, y1 - 3, round_corners=False)
    cx = (x0 + x1) / 2
    for k in range(3):
        cells |= {(x, y1 - 2 + k) for x in range(x0, x1 + 1) if abs(x - cx) <= (x1 - x0) / 2 - 1 - k * 1.5}
    return cells


def shade(px, pal, cells, wear=True):
    """Outline the edge, catch the light on the top-left inner edge and shade the bottom-right one."""
    edge = {c for c in cells if any((c[0] + dx, c[1] + dy) not in cells for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))}
    for x, y in cells:
        if (x, y) in edge:
            c = OUTLINE
        elif (x, y - 1) in edge or (x - 1, y) in edge:
            c = HIGHLIGHT
        elif (x, y + 1) in edge or (x + 1, y) in edge:
            c = SHADOW
        elif wear and (x * 7 + y * 13) % 11 == 0:
            c = MID
        else:
            c = BASE
        put(px, x, y, pal[c])


def rivet(px, pal, x, y):
    put(px, x, y, pal[ACCENT])
    put(px, x + 1, y + 1, pal[ACCENT_SHADOW])


def groove(px, pal, x0, x1, y):
    """An embossed line across a plate: a dark line with a lit line under it."""
    for x in range(x0, x1 + 1):
        put(px, x, y, pal[SHADOW])
        put(px, x, y + 1, pal[HIGHLIGHT])


def inlay(px, pal, cx, cy):
    """A small diamond-shaped accent stud, centred between pixels cx and cx + 1."""
    for x, y in ((cx, cy - 1), (cx + 1, cy - 1), (cx - 1, cy), (cx + 2, cy), (cx, cy + 1), (cx + 1, cy + 1)):
        put(px, x, y, pal[ACCENT_SHADOW])
    put(px, cx, cy, pal[ACCENT])
    put(px, cx + 1, cy, pal[ACCENT])


def flank(px, pal, x0, x1, strap0, strap1):
    """One side face: two pointed lames either side of the hanging strap, then the upper band over them."""
    shade(px, pal, lame(x0, 39, strap0 - 1, 44, 3))
    shade(px, pal, lame(strap1 + 1, 39, x1, 44, 3))
    shade(px, pal, rect(x0, 32, x1, 40))
    groove(px, pal, x0 + 2, x1 - 2, 37)
    for x in range(x0 + 3, x1 - 1, 5):
        rivet(px, pal, x, 34)


def entity_texture(pal):
    px = blank(128, 128)
    # top: three short ribs across the saddle under a pointed spine, and a shield down the front
    for y in (6, 12, 18):
        shade(px, pal, rect(36, y, 59, y + 3), wear=False)
    shade(px, pal, spine(42, 2, 53, 24))
    for y in range(6, 23):
        put(px, 47, y, pal[HIGHLIGHT])   # raised ridge along the spine
        put(px, 48, y, pal[SHADOW])
    for y in (9, 15, 21):
        rivet(px, pal, 44, y)
        rivet(px, pal, 50, y)
    shade(px, pal, shield(42, 24, 53, 34))
    inlay(px, pal, 47, 28)
    # sides
    flank(px, pal, 1, 29, 13, 17)        # west, around the buckle strap
    flank(px, pal, 66, 94, 78, 82)       # east
    # back: lames outside and between the two tail straps, band over them
    shade(px, pal, lame(97, 39, 106, 44, 3))
    shade(px, pal, lame(110, 39, 113, 44, 2))
    shade(px, pal, lame(117, 39, 126, 44, 3))
    shade(px, pal, rect(97, 32, 126, 40))
    groove(px, pal, 99, 124, 37)
    for x in (100, 105, 110, 116, 121):
        rivet(px, pal, x, 34)
    inlay(px, pal, 111, 34)
    visor(px, pal)
    return px


def visor(px, pal):
    """An armored frame around the goggles: brow guard, rims around open eye windows, nose bridge, cheek guards and
    side hinges. The windows are left fully open so the goggle lenses, and the eyes behind them, stay visible."""
    windows = [rect(15, 76, 22, 81, round_corners=False), rect(29, 76, 36, 81, round_corners=False)]
    open_cells = set().union(*windows)
    face = rect(11, 74, 40, 83) - open_cells
    for x in (11, 12, 39, 40):       # cut the lower corners into cheeks
        for y in range(82 - (x in (12, 39)), 84):
            face.discard((x, y))
    face -= {(x, 83) for x in range(23, 29)}  # a notch under the nose bridge
    shade(px, pal, face)
    # brow guard in the accent colour along the top edge, thicker over the nose
    for x in range(12, 40):
        put(px, x, 74, pal[ACCENT])
    for x in range(23, 29):
        put(px, x, 75, pal[ACCENT_SHADOW])
    # rims: a lit inner edge on top and left of each window, a dark one bottom and right
    for x0, x1 in ((15, 22), (29, 36)):
        for x in range(x0, x1 + 1):
            put(px, x, 75, pal[OUTLINE])
            put(px, x, 82, pal[HIGHLIGHT])
        for y in range(76, 82):
            put(px, x0 - 1, y, pal[OUTLINE])
            put(px, x1 + 1, y, pal[OUTLINE])
        rivet(px, pal, x0 - 3, 79)
    # nose bridge ridge
    for y in range(76, 83):
        put(px, 25, y, pal[HIGHLIGHT])
        put(px, 26, y, pal[SHADOW])
    # hinges where the frame meets the goggle straps, on the box's side faces
    for x0 in (4, 43):
        shade(px, pal, rect(x0, 77, x0 + 4, 80), wear=False)
        rivet(px, pal, x0 + 2, 78)


# Inventory icon: the leather saddle of the vanilla icon (rows 1..6), drawn as a plate.
ICON_PLATE = [
    (7, 1), (8, 1), (9, 1),
    *[(x, 2) for x in range(5, 12)],
    *[(x, 3) for x in range(3, 14)],
    *[(x, 4) for x in range(3, 16)],
    *[(x, 5) for x in range(3, 16)],
    *[(x, 6) for x in range(4, 16)],
]
ICON_RIVETS = [(5, 4), (13, 4), (9, 3)]


def blank(w, h):
    return [[(0, 0, 0, 0)] * w for _ in range(h)]


def put(px, x, y, rgb):
    px[y][x] = rgb + (255,)


def icon_texture(pal):
    px = blank(16, 16)
    cells = set(ICON_PLATE)
    for x, y in ICON_PLATE:
        if (x, y - 1) not in cells:
            c = HIGHLIGHT
        elif (x, y + 1) not in cells or (x + 1, y) not in cells:
            c = SHADOW
        else:
            c = BASE
        put(px, x, y, pal[c])
    for x, y in ICON_RIVETS:
        put(px, x, y, pal[HIGHLIGHT])
        put(px, x + 1, y, pal[SHADOW])
    return px


def write_png(path, px):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    raw = b''.join(b'\x00' + bytes(v for p in row for v in p) for row in px)

    def chunk(t, c):
        return struct.pack('>I', len(c)) + t + c + struct.pack('>I', zlib.crc32(t + c))

    with open(path, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', len(px[0]), len(px), 8, 6, 0, 0, 0))
                + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', newline='\n', encoding='utf-8') as f:
        json.dump(data, f, indent=1, ensure_ascii=False)
        f.write('\n')


def name(color, tier):
    return f'{color}_{tier}_harness'


def ref(color, tier):
    return 'minecraft:' + color + '_harness' if tier is None else 'happyghastplus:' + name(color, tier)


def main():
    for tier, pal in PALETTES.items():
        write_png(os.path.join(ASSETS, 'textures', 'entity', 'equipment', 'happy_ghast_body', f'{tier}_harness_plates.png'),
                  entity_texture(pal))
        write_png(os.path.join(ASSETS, 'textures', 'item', f'{tier}_harness_plates.png'), icon_texture(pal))

    recipes = os.path.join(DATA, 'happyghastplus', 'recipe')
    previous = {'iron': None, 'diamond': 'iron', 'netherite': 'diamond'}
    tag = []
    lang_en, lang_de = {}, {}
    for tier in TIERS:
        for color in COLORS:
            n = name(color, tier)
            tag.append(ref(color, tier))
            lang_en[f'item.happyghastplus.{n}'] = f'{COLOR_EN[color]} {TIER_EN[tier]} Harness'
            lang_de[f'item.happyghastplus.{n}'] = f'{COLOR_DE[color]} {TIER_DE[tier]}'
            write_json(os.path.join(ASSETS, 'equipment', n + '.json'), {'layers': {'happy_ghast_body': [
                {'texture': f'minecraft:{color}_harness'},
                {'texture': f'happyghastplus:{tier}_harness_plates'},
            ]}})
            write_json(os.path.join(ASSETS, 'items', n + '.json'),
                       {'model': {'type': 'minecraft:model', 'model': 'happyghastplus:item/' + n}})
            write_json(os.path.join(ASSETS, 'models', 'item', n + '.json'), {
                'parent': 'minecraft:item/generated',
                'textures': {'layer0': f'minecraft:item/{color}_harness', 'layer1': f'happyghastplus:item/{tier}_harness_plates'},
            })

            source = previous[tier]
            if tier == 'netherite':
                write_json(os.path.join(recipes, n + '_smithing.json'), {
                    'type': 'minecraft:smithing_transform',
                    'addition': 'minecraft:netherite_ingot',
                    'base': ref(color, source),
                    'result': {'id': ref(color, tier)},
                    'template': 'minecraft:netherite_upgrade_smithing_template',
                })
            else:
                write_json(os.path.join(recipes, n + '.json'), {
                    'type': 'minecraft:crafting_shaped',
                    'category': 'equipment',
                    'group': f'{tier}_harness',
                    'key': {'#': ref(color, source), 'M': 'minecraft:iron_ingot' if tier == 'iron' else 'minecraft:diamond'},
                    'pattern': [' M ', 'M#M', ' M '],
                    'result': {'id': ref(color, tier)},
                })
            write_json(os.path.join(recipes, f'dye_{n}.json'), {
                'type': 'minecraft:crafting_shapeless',
                'category': 'equipment',
                'group': f'{tier}_harness_dye',
                'ingredients': [f'minecraft:{color}_dye', [ref(c, tier) for c in COLORS if c != color]],
                'result': {'id': ref(color, tier)},
            })

    write_json(os.path.join(DATA, 'minecraft', 'tags', 'item', 'harnesses.json'), {'replace': False, 'values': tag})

    for file, entries in (('en_us.json', lang_en), ('de_de.json', lang_de)):
        path = os.path.join(LANG, file)
        with open(path, encoding='utf-8') as f:
            lang = json.load(f)
        lang = {k: v for k, v in lang.items() if not (k.startswith('item.happyghastplus.') and k.endswith(tuple(f'_{t}_harness' for t in TIERS)))}
        lang.update(entries)
        with open(path, 'w', newline='\n', encoding='utf-8') as f:
            json.dump(lang, f, indent=2, ensure_ascii=False)
            f.write('\n')


if __name__ == '__main__':
    main()
