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

# outline, shadow, mid, base, highlight
PALETTES = {
    'iron': [(70, 70, 76), (128, 128, 136), (168, 168, 174), (198, 198, 204), (238, 238, 242)],
    'diamond': [(10, 68, 64), (24, 148, 138), (56, 198, 186), (98, 228, 216), (196, 255, 248)],
    'netherite': [(22, 18, 20), (46, 40, 44), (62, 55, 59), (78, 70, 74), (116, 106, 110)],
}
OUTLINE, SHADOW, MID, BASE, HIGHLIGHT = range(5)

# Entity plates as (x0, y0, x1, y1) inclusive, on the leather of the vanilla harness, drawn in order (later plates
# overlap earlier ones). The top only gets a spine plate so the saddle shows; the sides get two rows of skirt plates
# that leave the bottom of the coloured flaps and the hanging buckle straps free.
PLATES = [
    (41, 3, 54, 22),    # spine plate on the saddle
    (42, 23, 53, 32),   # chest piece over the leather tongue at the front
    # west flank: lower row split around the buckle strap, then the upper row over it
    (1, 39, 12, 47), (18, 39, 30, 47), (1, 32, 30, 40),
    # east flank
    (65, 39, 77, 47), (83, 39, 94, 47), (65, 32, 94, 40),
    # back, split around the two tail straps
    (97, 39, 104, 47), (115, 39, 126, 47), (97, 32, 126, 40),
]
# Visor over the goggles: the goggles are a 16x5x5 box whose front face is x 10..41, y 74..83 on the texture, lenses
# at x 15..22 and 29..36. The plate covers the frame and leaves eye slits so the lenses show through.
VISOR = (11, 74, 40, 83)
VISOR_SLITS = [(14, 77, 23, 78), (28, 77, 37, 78)]

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


def draw_plate(px, pal, x0, y0, x1, y1):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if (x in (x0, x1)) and (y in (y0, y1)):
                continue  # rounded corners
            if x in (x0, x1) or y in (y0, y1):
                c = OUTLINE
            elif y == y0 + 1 or x == x0 + 1:
                c = HIGHLIGHT
            elif y == y1 - 1 or x == x1 - 1:
                c = SHADOW
            elif (x * 7 + y * 13) % 11 == 0:
                c = MID  # a little wear on the face
            else:
                c = BASE
            put(px, x, y, pal[c])
    if y1 - y0 >= 5 and x1 - x0 >= 7:
        for rx in (x0 + 2, x1 - 2):
            for ry in (y0 + 2, y1 - 2):
                put(px, rx, ry, pal[HIGHLIGHT])
                put(px, rx + 1 if rx + 1 < x1 else rx - 1, ry + 1 if ry + 1 < y1 else ry, pal[SHADOW])


def entity_texture(pal):
    px = blank(128, 128)
    for rect in PLATES:
        draw_plate(px, pal, *rect)
    draw_plate(px, pal, *VISOR)
    for x0, y0, x1, y1 in VISOR_SLITS:
        for y in range(y0 - 1, y1 + 2):
            for x in range(x0 - 1, x1 + 2):
                inside = x0 <= x <= x1 and y0 <= y <= y1
                px[y][x] = (0, 0, 0, 0) if inside else pal[SHADOW if y > y1 else OUTLINE] + (255,)
    for y in range(VISOR[1] + 1, VISOR[3]):
        put(px, 25, y, pal[HIGHLIGHT])  # ridge down the middle
        put(px, 26, y, pal[SHADOW])
    return px


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
