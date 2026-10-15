"""Renders a block model JSON to a PNG (orthographic, vanilla face shading) for previews in CI.

Usage: python3 render_preview.py <model.json> <mod textures dir> <vanilla textures dir> <out.png>
"""
import json
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "west": 0.6, "east": 0.6}
NORMAL = {"up": (0, 1, 0), "down": (0, -1, 0), "north": (0, 0, -1), "south": (0, 0, 1), "west": (-1, 0, 0), "east": (1, 0, 0)}


def corners(face, a, b):
    (x1, y1, z1), (x2, y2, z2) = a, b
    return {
        "north": [(x2, y2, z1), (x1, y2, z1), (x1, y1, z1), (x2, y1, z1)],
        "south": [(x1, y2, z2), (x2, y2, z2), (x2, y1, z2), (x1, y1, z2)],
        "west": [(x1, y2, z1), (x1, y2, z2), (x1, y1, z2), (x1, y1, z1)],
        "east": [(x2, y2, z2), (x2, y2, z1), (x2, y1, z1), (x2, y1, z2)],
        "up": [(x1, y2, z1), (x2, y2, z1), (x2, y2, z2), (x1, y2, z2)],
        "down": [(x1, y1, z2), (x2, y1, z2), (x2, y1, z1), (x1, y1, z1)],
    }[face]


def default_uv(face, a, b):
    (x1, y1, z1), (x2, y2, z2) = a, b
    return {
        "down": [x1, 16 - z2, x2, 16 - z1], "up": [x1, z1, x2, z2],
        "north": [16 - x2, 16 - y2, 16 - x1, 16 - y1], "south": [x1, 16 - y2, x2, 16 - y1],
        "west": [z1, 16 - y2, z2, 16 - y1], "east": [16 - z2, 16 - y2, 16 - z1, 16 - y1],
    }[face]


def resolve(textures, ref):
    while ref.startswith("#"):
        ref = textures[ref[1:]]
    return ref


def load_texture(ref, mod_dir, vanilla_dir):
    ns, path = ref.split(":") if ":" in ref else ("minecraft", ref)
    base = mod_dir if ns != "minecraft" else vanilla_dir
    img = Image.open(Path(base) / (path + ".png")).convert("RGBA")
    return img.crop((0, 0, img.width, img.width))  # first frame of animated textures


def render(model, mod_dir, vanilla_dir, yaw, pitch, scale):
    cy, sy, cp, sp = math.cos(yaw), math.sin(yaw), math.cos(pitch), math.sin(pitch)

    def project(p):
        x, y, z = p[0] - 8, p[1] - 8, p[2] - 8
        x, z = x * cy - z * sy, x * sy + z * cy
        y, z = y * cp - z * sp, y * sp + z * cp
        return (x * scale, -y * scale, z)

    quads = []
    for e in model["elements"]:
        a, b = e["from"], e["to"]
        for face, spec in e["faces"].items():
            n = NORMAL[face]
            np_ = project((8 + n[0], 8 + n[1], 8 + n[2]))[2] - project((8, 8, 8))[2]
            if np_ >= -1e-6:  # faces pointing away from the camera
                continue
            tex = load_texture(resolve(model["textures"], spec["texture"]), mod_dir, vanilla_dir)
            u1, v1, u2, v2 = spec.get("uv") or default_uv(face, a, b)
            c = corners(face, a, b)
            cols, rows = max(1, round(abs(u2 - u1))), max(1, round(abs(v2 - v1)))
            for j in range(rows):
                for i in range(cols):
                    s0, s1, t0, t1 = i / cols, (i + 1) / cols, j / rows, (j + 1) / rows
                    def at(s, t):
                        top = [c[0][k] + (c[1][k] - c[0][k]) * s for k in range(3)]
                        bot = [c[3][k] + (c[2][k] - c[3][k]) * s for k in range(3)]
                        return [top[k] + (bot[k] - top[k]) * t for k in range(3)]
                    pts = [project(at(s0, t0)), project(at(s1, t0)), project(at(s1, t1)), project(at(s0, t1))]
                    tu = u1 + (u2 - u1) * (i + 0.5) / cols
                    tv = v1 + (v2 - v1) * (j + 0.5) / rows
                    px = tex.getpixel((min(tex.width - 1, int(tu * tex.width / 16)), min(tex.height - 1, int(tv * tex.width / 16))))
                    if px[3] < 128:
                        continue
                    f = SHADE[face]
                    color = tuple(int(ch * f) for ch in px[:3])
                    depth = sum(p[2] for p in pts) / 4
                    quads.append((depth, [(p[0], p[1]) for p in pts], color))
    quads.sort(key=lambda q: -q[0])  # the camera looks towards +z, so draw the largest z first
    size = int(30 * scale)
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for _, pts, color in quads:
        d.polygon([(x + size / 2, y + size / 2 + scale * 2) for x, y in pts], fill=color + (255,), outline=color + (255,))
    return img.crop(img.getbbox())


def main():
    model_path, mod_dir, vanilla_dir, out = sys.argv[1:5]
    model = json.load(open(model_path))
    views = [render(model, mod_dir, vanilla_dir, math.radians(yaw), math.radians(pitch), 16)
             for yaw, pitch in ((-35, -28), (35, -28), (180 - 30, -25))]
    pad = 24
    w = sum(v.width for v in views) + pad * (len(views) + 1)
    h = max(v.height for v in views) + pad * 2
    sheet = Image.new("RGBA", (w, h), (0x9e, 0xc4, 0xe8, 255))
    x = pad
    for v in views:
        sheet.alpha_composite(v, (x, h - pad - v.height))
        x += v.width + pad
    sheet.save(out)


if __name__ == "__main__":
    main()
