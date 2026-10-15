"""Generates the rocket engine drawn on a speed-upgraded happy ghast: a stubby firework rocket on a leather saddle pad.

Writes models/item/ghast_rocket_engine.json and textures/item/ghast_rocket_engine.png (16x16 atlas, 1 texel per model
pixel). Run from the repository root: python happy-ghast/tools/gen_rocket_engine.py

Model space: z = 0 is the nose (points forward on the ghast), z = 16 the back; +x faces the ghast's body. The layer
turns the left engine upside down, so keep the design symmetric top to bottom.
"""
import json
import os
import random
import struct
import zlib

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'happyghastplus')

# Atlas regions: material -> (u, v, width, height) in texels.
REGIONS = {
    'paper': (0, 0, 8, 8),
    'band': (8, 0, 2, 8),
    'crimp': (10, 0, 1, 8),
    'cone': (11, 0, 3, 3),
    'cap': (11, 3, 3, 3),
    'fuse': (14, 0, 2, 4),
    'leather': (0, 8, 8, 4),
    'leather_dark': (0, 12, 8, 2),
    'iron': (8, 8, 2, 2),
    'hook': (10, 8, 2, 2),
}

# (material, from, to)
ELEMENTS = [
    # rocket: red paper tube with a white band and a crimped ring near the back
    ('paper', [6, 6, 4], [10, 10, 12]),
    ('band', [5.95, 5.95, 6], [10.05, 10.05, 7.5]),
    ('crimp', [5.95, 5.95, 10.5], [10.05, 10.05, 11]),
    # stepped grey nose
    ('cone', [6.5, 6.5, 2.5], [9.5, 9.5, 4]),
    ('cone', [7, 7, 1.5], [9, 9, 2.5]),
    ('cone', [7.5, 7.5, 1], [8.5, 8.5, 1.5]),
    # paper end cap and fuse at the back
    ('cap', [6.5, 6.5, 12], [9.5, 9.5, 12.5]),
    ('fuse', [7.5, 7.5, 12.5], [8.5, 8.5, 14]),
    # saddle pad between rocket and harness
    ('leather', [10, 6.5, 4.5], [12, 9.5, 11.5]),
    ('leather_dark', [11.5, 6, 4.5], [12, 10, 11.5]),
    # two straps around the rocket, buckled on the outer side
    ('leather', [5.8, 5.8, 5], [10.2, 10.2, 5.8]),
    ('leather', [5.8, 5.8, 9.2], [10.2, 10.2, 10]),
    ('iron', [5.5, 7.4, 4.9], [5.8, 8.6, 5.9]),
    ('iron', [5.5, 7.4, 9.1], [5.8, 8.6, 10.1]),
    # small iron hooks into the harness
    ('hook', [12, 7.5, 5.1], [12.6, 8.5, 5.7]),
    ('hook', [12, 7.5, 9.3], [12.6, 8.5, 9.9]),
]

FACE_SIZE = {
    'north': (0, 1), 'south': (0, 1), 'east': (2, 1), 'west': (2, 1), 'up': (0, 2), 'down': (0, 2),
}


def face_uv(material, size, face):
    u, v, w, h = REGIONS[material]
    a, b = FACE_SIZE[face]
    fw, fh = min(size[a], w), min(size[b], h)
    return [u, v, round(u + fw, 3), round(v + fh, 3)]


def model():
    elements = []
    for material, a, b in ELEMENTS:
        size = [b[i] - a[i] for i in range(3)]
        elements.append({
            'from': a, 'to': b,
            'faces': {f: {'uv': face_uv(material, size, f), 'texture': '#engine'} for f in FACE_SIZE},
        })
    return {
        'textures': {'engine': 'happyghastplus:item/ghast_rocket_engine', 'particle': 'happyghastplus:item/ghast_rocket_engine'},
        'elements': elements,
    }


def texture():
    rnd = random.Random(11)
    px = [[(0, 0, 0, 0)] * 16 for _ in range(16)]

    def fill(material, fn):
        u, v, w, h = REGIONS[material]
        for y in range(h):
            for x in range(w):
                px[v + y][u + x] = fn(x, y) + (255,)

    red = [(176, 46, 38), (190, 54, 44), (184, 50, 40)]
    fill('paper', lambda x, y: (150, 36, 30) if (x + y) % 4 == 0 else ((206, 70, 58) if (x + 2 * y) % 7 == 0 else rnd.choice(red)))
    fill('band', lambda x, y: (222, 218, 208) if y % 3 == 1 else (240, 238, 232))
    fill('crimp', lambda x, y: (128, 30, 26) if y % 2 else (142, 34, 28))
    fill('cone', lambda x, y: (150, 150, 150) if (x, y) == (1, 1) else rnd.choice([(126, 126, 126), (114, 114, 114)]))
    fill('cap', lambda x, y: (176, 144, 98) if (x, y) == (1, 1) else rnd.choice([(204, 172, 120), (192, 160, 110)]))
    fill('fuse', lambda x, y: (86, 80, 72) if y == 0 else rnd.choice([(52, 46, 42), (44, 40, 36)]))
    fill('leather', lambda x, y: (176, 128, 84) if y == 1 and x % 2 == 0 else rnd.choice([(122, 76, 44), (112, 68, 40), (132, 84, 50)]))
    fill('leather_dark', lambda x, y: rnd.choice([(92, 56, 32), (84, 50, 30)]))
    fill('iron', lambda x, y: (214, 214, 220) if (x, y) == (0, 0) else (168, 168, 176))
    fill('hook', lambda x, y: (110, 110, 118) if (x + y) % 2 else (88, 88, 96))
    return px


def write_png(path, px):
    raw = b''.join(b'\x00' + bytes(v for p in row for v in p) for row in px)

    def chunk(t, c):
        return struct.pack('>I', len(c)) + t + c + struct.pack('>I', zlib.crc32(t + c))

    with open(path, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0))
                + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


if __name__ == '__main__':
    with open(os.path.join(ROOT, 'models', 'item', 'ghast_rocket_engine.json'), 'w', newline='\n') as f:
        json.dump(model(), f, indent=1)
        f.write('\n')
    write_png(os.path.join(ROOT, 'textures', 'item', 'ghast_rocket_engine.png'), texture())
    print('wrote ghast_rocket_engine model and texture')
