"""Generates the ghast searchlight assets.

Writes the lamp's base and head item models (the head twice: dark lens and lit lens), their 16x16 texture atlas, the
inventory icon, the item definition that picks a part by custom model data, and the invisible light block's
blockstate. Run from the repository root: python happy-ghast/tools/gen_searchlight.py

Model space: the base stands on y = 0 and turns around its centre (8, 8). The head is centred on its tilt axis
(8, 8, 8) and its lens faces north (-z), which is forward on the ghast. GhastSearchlightLayer puts the head's axis
6.5 model pixels above the base's foot, so keep the yoke arms reaching that height.
"""
import json
import os
import random
import struct
import zlib

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'happyghastplus')

REGIONS = {
    'dark': (0, 0, 6, 6),
    'iron': (6, 0, 6, 6),
    'brass': (12, 0, 4, 4),
    'lens': (0, 6, 6, 6),
    'lens_lit': (6, 6, 6, 6),
}

BASE = [
    ('dark', [3, 0, 3], [13, 1, 13]),       # foot plate on the harness
    ('iron', [5, 1, 5], [11, 2.5, 11]),     # turntable
    ('iron', [2.5, 2.5, 7], [3.5, 8, 9]),   # yoke arms
    ('iron', [12.5, 2.5, 7], [13.5, 8, 9]),
    ('iron', [2.5, 2.5, 7], [13.5, 3.5, 9]),  # yoke crossbar
]

HEAD = [
    ('dark', [4.5, 4.5, 5], [11.5, 11.5, 12]),   # drum
    ('iron', [4.3, 4.3, 8], [11.7, 11.7, 9]),    # band
    ('dark', [6, 6, 12], [10, 10, 13]),          # back cap
    ('brass', [4, 11, 4], [12, 12, 5]),          # rim around the lens
    ('brass', [4, 4, 4], [12, 5, 5]),
    ('brass', [4, 5, 4], [5, 11, 5]),
    ('brass', [11, 5, 4], [12, 11, 5]),
    ('lens', [5, 5, 4.4], [11, 11, 5]),          # glass, set back inside the rim
    ('iron', [7, 11.5, 7.5], [9, 12.3, 9.5]),    # handle
    ('iron', [3.5, 7.5, 7.5], [4.5, 8.5, 8.5]),  # pivot pins into the yoke
    ('iron', [11.5, 7.5, 7.5], [12.5, 8.5, 8.5]),
]

FACE_SIZE = {'north': (0, 1), 'south': (0, 1), 'east': (2, 1), 'west': (2, 1), 'up': (0, 2), 'down': (0, 2)}


def face_uv(material, size, face):
    u, v, w, h = REGIONS[material]
    a, b = FACE_SIZE[face]
    return [u, v, round(u + min(size[a], w), 3), round(v + min(size[b], h), 3)]


def model(elements, lit=False):
    out = []
    for material, a, b in elements:
        if lit and material == 'lens':
            material = 'lens_lit'
        size = [b[i] - a[i] for i in range(3)]
        element = {'from': a, 'to': b}
        if material == 'lens_lit':
            element['light_emission'] = 15
        element['faces'] = {f: {'uv': face_uv(material, size, f), 'texture': '#lamp'} for f in FACE_SIZE}
        out.append(element)
    return {
        'textures': {'lamp': 'happyghastplus:item/ghast_searchlight_parts', 'particle': 'happyghastplus:item/ghast_searchlight_parts'},
        'elements': out,
    }


def atlas():
    rnd = random.Random(5)
    px = [[(0, 0, 0, 0)] * 16 for _ in range(16)]

    def fill(material, fn):
        u, v, w, h = REGIONS[material]
        for y in range(h):
            for x in range(w):
                px[v + y][u + x] = fn(x, y) + (255,)

    fill('dark', lambda x, y: rnd.choice([(62, 60, 64), (54, 52, 56), (70, 68, 72)]))
    fill('iron', lambda x, y: (206, 206, 212) if (x, y) in ((1, 1), (4, 4)) else rnd.choice([(150, 150, 156), (138, 138, 144), (162, 162, 168)]))
    fill('brass', lambda x, y: (214, 176, 92) if (x + y) % 3 == 0 else rnd.choice([(180, 140, 64), (166, 126, 56)]))
    fill('lens', lambda x, y: (196, 214, 222) if (x, y) in ((1, 1), (2, 1), (1, 2)) else rnd.choice([(120, 140, 150), (110, 128, 138)]))
    fill('lens_lit', lambda x, y: (255, 255, 240) if (x, y) in ((1, 1), (2, 1), (1, 2), (2, 2)) else rnd.choice([(255, 236, 170), (255, 228, 150)]))
    return px


def icon():
    # a small side view of the lamp on its yoke
    k = (0, 0, 0, 0)
    D, d, I, i, B, L, W = (54, 52, 56, 255), (70, 68, 72, 255), (162, 162, 168, 255), (124, 124, 130, 255), (196, 156, 72, 255), (255, 232, 160, 255), (255, 255, 236, 255)
    rows = [
        "................",
        "................",
        "......II........",
        "...BBBBBBBB.....",
        "...BLLLLLLBdd...",
        "...BLWWLLLBDDd..",
        "...BLWLLLLBDDDd.",
        "...BLLLLLLBDDDd.",
        "...BLLLLLLBDDd..",
        "...BBBBBBBBdd...",
        "....i...i.......",
        "....I...I.......",
        "....IIIII.......",
        "...ddddddd......",
        "..DDDDDDDDD.....",
        "................",
    ]
    lut = {'.': k, 'D': D, 'd': d, 'I': I, 'i': i, 'B': B, 'L': L, 'W': W}
    return [[lut[c] for c in row] for row in rows]


def write_png(path, px):
    raw = b''.join(b'\x00' + bytes(v for p in row for v in p) for row in px)

    def chunk(t, c):
        return struct.pack('>I', len(c)) + t + c + struct.pack('>I', zlib.crc32(t + c))

    with open(path, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', len(px[0]), len(px), 8, 6, 0, 0, 0))
                + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def write_json(path, data):
    with open(path, 'w', newline='\n') as f:
        json.dump(data, f, indent=1)
        f.write('\n')


def part(name):
    return {'type': 'minecraft:model', 'model': 'happyghastplus:item/' + name}


if __name__ == '__main__':
    models = os.path.join(ROOT, 'models', 'item')
    write_json(os.path.join(models, 'ghast_searchlight_base.json'), model(BASE))
    write_json(os.path.join(models, 'ghast_searchlight_head.json'), model(HEAD))
    write_json(os.path.join(models, 'ghast_searchlight_head_lit.json'), model(HEAD, lit=True))
    write_json(os.path.join(models, 'ghast_searchlight.json'),
               {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'happyghastplus:item/ghast_searchlight'}})
    write_png(os.path.join(ROOT, 'textures', 'item', 'ghast_searchlight_parts.png'), atlas())
    write_png(os.path.join(ROOT, 'textures', 'item', 'ghast_searchlight.png'), icon())
    write_json(os.path.join(ROOT, 'items', 'ghast_searchlight.json'), {'model': {
        'type': 'minecraft:select', 'property': 'minecraft:display_context',
        'cases': [{'when': 'none', 'model': {
            'type': 'minecraft:select', 'property': 'minecraft:custom_model_data', 'index': 0,
            'cases': [{'when': 'head', 'model': part('ghast_searchlight_head')},
                      {'when': 'head_lit', 'model': part('ghast_searchlight_head_lit')}],
            'fallback': part('ghast_searchlight_base'),
        }}],
        'fallback': part('ghast_searchlight'),
    }})
    write_json(os.path.join(ROOT, 'blockstates', 'searchlight_light.json'),
               {'multipart': [{'apply': {'model': 'happyghastplus:block/searchlight_light'}}]})
    write_json(os.path.join(ROOT, 'models', 'block', 'searchlight_light.json'),
               {'textures': {'particle': 'minecraft:block/glass'}})
    print('wrote ghast searchlight assets')
