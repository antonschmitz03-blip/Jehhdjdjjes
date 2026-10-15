"""Generates the rocket engine drawn on a speed-upgraded happy ghast: a 3D take on the vanilla firework rocket item,
strapped to a leather saddle pad.

Writes models/item/ghast_rocket_engine.json (cold nozzle), models/item/ghast_rocket_engine_lit.json (glowing nozzle,
drawn while the ghast flies forward) and textures/item/ghast_rocket_engine.png (16x16 atlas, 1 texel per model pixel). Run from the repository root: python happy-ghast/tools/gen_rocket_engine.py

Model space: z = 0 is the nose (points forward on the ghast), z = 16 the back (the nozzle ends at z = 14); +x faces the ghast's body. The layer
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
    'cone': (8, 0, 6, 6),
    'collar': (14, 0, 2, 6),
    'nozzle': (8, 6, 3, 2),
    'soot': (11, 6, 2, 2),
    'glow': (13, 6, 2, 2),
    'leather': (0, 8, 8, 4),
    'leather_dark': (0, 12, 8, 2),
    'iron': (8, 8, 2, 2),
    'hook': (10, 8, 2, 2),
}

# (material, from, to)
ELEMENTS = [
    # red paper tube with white flecks, like the item sprite
    ('paper', [6, 6, 5.5], [10, 10, 12.5]),
    # pale collar between tube and nose
    ('collar', [5.9, 5.9, 4.5], [10.1, 10.1, 5.5]),
    # red nose cone, wider than the tube, in three steps
    ('cone', [5, 5, 3.5], [11, 11, 4.5]),
    ('cone', [6, 6, 2.5], [10, 10, 3.5]),
    ('cone', [7, 7, 1.5], [9, 9, 2.5]),
    # charred nozzle at the back: a square rim around a sooty opening
    ('nozzle', [6.5, 8.75, 12.5], [9.5, 9.5, 14]),
    ('nozzle', [6.5, 6.5, 12.5], [9.5, 7.25, 14]),
    ('nozzle', [6.5, 7.25, 12.5], [7.25, 8.75, 14]),
    ('nozzle', [8.75, 7.25, 12.5], [9.5, 8.75, 14]),
    ('soot', [7.25, 7.25, 12.5], [8.75, 8.75, 12.6]),
    # saddle pad between rocket and harness
    ('leather', [10, 6.5, 5.5], [12, 9.5, 12]),
    ('leather_dark', [11.5, 6, 5.5], [12, 10, 12]),
    # two straps around the rocket, buckled on the outer side
    ('leather', [5.8, 5.8, 6.5], [10.2, 10.2, 7.2]),
    ('leather', [5.8, 5.8, 10.5], [10.2, 10.2, 11.2]),
    ('iron', [5.5, 7.4, 6.4], [5.8, 8.6, 7.3]),
    ('iron', [5.5, 7.4, 10.4], [5.8, 8.6, 11.3]),
    # small iron hooks into the harness
    ('hook', [12, 7.5, 6.5], [12.6, 8.5, 7.1]),
    ('hook', [12, 7.5, 10.5], [12.6, 8.5, 11.1]),
]

FACE_SIZE = {
    'north': (0, 1), 'south': (0, 1), 'east': (2, 1), 'west': (2, 1), 'up': (0, 2), 'down': (0, 2),
}


def face_uv(material, size, face):
    u, v, w, h = REGIONS[material]
    a, b = FACE_SIZE[face]
    fw, fh = min(size[a], w), min(size[b], h)
    return [u, v, round(u + fw, 3), round(v + fh, 3)]


def model(lit):
    elements = []
    for material, a, b in ELEMENTS:
        size = [b[i] - a[i] for i in range(3)]
        element = {'from': a, 'to': b}
        if lit and material == 'soot':
            material = 'glow'
            element['light_emission'] = 15
        element['faces'] = {f: {'uv': face_uv(material, size, f), 'texture': '#engine'} for f in FACE_SIZE}
        elements.append(element)
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

    # colours taken from the vanilla firework rocket sprite
    red = [(185, 41, 41), (185, 41, 41), (153, 41, 41)]
    fill('paper', lambda x, y: (244, 244, 244) if (x * 3 + y * 5) % 11 == 0 else ((178, 204, 209) if (x * 5 + y) % 13 == 0 else rnd.choice(red)))
    fill('cone', lambda x, y: (214, 42, 42) if x + y < 3 else ((109, 23, 23) if x + y > 8 else rnd.choice([(185, 41, 41), (153, 41, 41), (132, 27, 27)])))
    fill('collar', lambda x, y: (244, 244, 244) if y % 3 == 0 else (178, 204, 209))
    fill('nozzle', lambda x, y: (96, 92, 88) if y == 0 else rnd.choice([(70, 66, 64), (60, 56, 54)]))
    fill('soot', lambda x, y: (28, 24, 22) if (x + y) % 2 else (36, 30, 28))
    fill('glow', lambda x, y: (255, 228, 120) if (x, y) == (0, 0) else (255, 158, 48))
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
    for name, lit in (('ghast_rocket_engine', False), ('ghast_rocket_engine_lit', True)):
        with open(os.path.join(ROOT, 'models', 'item', name + '.json'), 'w', newline='\n') as f:
            json.dump(model(lit), f, indent=1)
            f.write('\n')
    write_png(os.path.join(ROOT, 'textures', 'item', 'ghast_rocket_engine.png'), texture())
    print('wrote ghast_rocket_engine models and texture')
