"""Quick isometric preview of the Metal Decor blocks, drawn straight from the generated textures.

Run from the metal-decor folder: python3 tools/render_preview.py out.png
"""
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/metaldecor/textures"
S = 7  # screen pixels per model pixel along a cube edge (16 model pixels per block)
C30, S30 = math.cos(math.pi / 6), math.sin(math.pi / 6)
SHADE = {"up": 1.0, "south": 0.82, "east": 0.64, "north": 0.64, "west": 0.82, "down": 0.5}


def tex(name, kind="block"):
    return Image.open(TEX / kind / f"{name}.png").convert("RGBA")


def project(x, y, z):
    return ((x - z) * C30 * S, (x + z) * S30 * S - y * S)


def shaded(img, f):
    r, g, b, a = img.split()
    r, g, b = (ch.point(lambda v: int(v * f)) for ch in (r, g, b))
    return Image.merge("RGBA", (r, g, b, a))


def face(canvas, origin, texture, uv, p0, pu, pv, shade):
    """Draws texture[uv] onto the parallelogram p0, p0 + pu, p0 + pv (model coordinates)."""
    u0, v0, u1, v1 = uv
    crop = texture.crop((min(u0, u1), min(v0, v1), max(u0, u1), max(v0, v1)))
    if u1 < u0:
        crop = crop.transpose(Image.FLIP_LEFT_RIGHT)
    if v1 < v0:
        crop = crop.transpose(Image.FLIP_TOP_BOTTOM)
    crop = shaded(crop.resize((crop.width * 8, crop.height * 8), Image.NEAREST), shade)
    o = project(*p0)
    a = project(*[p0[i] + pu[i] for i in range(3)])
    b = project(*[p0[i] + pv[i] for i in range(3)])
    ux, uy = a[0] - o[0], a[1] - o[1]
    vx, vy = b[0] - o[0], b[1] - o[1]
    det = ux * vy - uy * vx
    if abs(det) < 1e-6:
        return
    w, h = crop.size
    ox, oy = origin[0] + o[0], origin[1] + o[1]
    # Inverse map: screen (X, Y) -> crop pixel.
    ia, ib = vy / det * w, -vx / det * w
    ic, id_ = -uy / det * h, ux / det * h
    coeffs = (ia, ib, -(ia * ox + ib * oy), ic, id_, -(ic * ox + id_ * oy))
    layer = crop.transform(canvas.size, Image.AFFINE, coeffs, resample=Image.NEAREST)
    canvas.alpha_composite(layer)


def box(canvas, origin, frm, to, faces, back=False):
    """faces: name -> (texture, uv). Only front faces unless back is set (for see-through blocks)."""
    x0, y0, z0 = frm
    x1, y1, z1 = to
    defs = {
        "north": ((x1, y1, z0), (x0 - x1, 0, 0), (0, y0 - y1, 0)),
        "west": ((x0, y1, z0), (0, 0, z1 - z0), (0, y0 - y1, 0)),
        "down": ((x0, y0, z1), (x1 - x0, 0, 0), (0, 0, z0 - z1)),
        "up": ((x0, y1, z0), (x1 - x0, 0, 0), (0, 0, z1 - z0)),
        "south": ((x0, y1, z1), (x1 - x0, 0, 0), (0, y0 - y1, 0)),
        "east": ((x1, y1, z1), (0, 0, z0 - z1), (0, y0 - y1, 0)),
    }
    order = (["north", "west", "down"] if back else []) + ["up", "south", "east"]
    for name in order:
        if name in faces:
            texture, uv = faces[name]
            p0, pu, pv = defs[name]
            face(canvas, origin, texture, uv, p0, pu, pv, SHADE[name])


def auto(texture, frm, to, top=None):
    """Faces with uvs taken from the box position, like a block model without explicit uvs."""
    x0, y0, z0 = frm
    x1, y1, z1 = to
    t = top or texture
    return {
        "up": (t, (x0, z0, x1, z1)), "down": (texture, (x0, z0, x1, z1)),
        "south": (texture, (x0, 16 - y1, x1, 16 - y0)), "north": (texture, (16 - x1, 16 - y1, 16 - x0, 16 - y0)),
        "east": (texture, (16 - z1, 16 - y1, 16 - z0, 16 - y0)), "west": (texture, (z0, 16 - y1, z1, 16 - y0)),
    }


def cube(canvas, o, t, top=None, back=False):
    box(canvas, o, (0, 0, 0), (16, 16, 16), auto(t, (0, 0, 0), (16, 16, 16), top), back)


def slab(canvas, o, t):
    box(canvas, o, (0, 0, 0), (16, 8, 16), auto(t, (0, 0, 0), (16, 8, 16)))


def stairs(canvas, o, t):
    box(canvas, o, (0, 0, 0), (16, 8, 16), auto(t, (0, 0, 0), (16, 8, 16)))
    box(canvas, o, (0, 8, 0), (16, 16, 8), auto(t, (0, 8, 0), (16, 16, 8)))


def pedestal(canvas, o, metal, item=None):
    base, column, top = tex(f"{metal}_cut"), tex(f"{metal}_pillar"), tex(f"{metal}_pedestal_top")
    box(canvas, o, (1, 0, 1), (15, 3, 15), {**auto(base, (1, 0, 1), (15, 3, 15)),
                                              **{f: (base, (1, 13, 15, 16)) for f in ("south", "east")}})
    box(canvas, o, (4, 3, 4), (12, 11, 12), {f: (column, (4, 4, 12, 12)) for f in ("south", "east")})
    box(canvas, o, (2, 11, 2), (14, 14, 14), {"up": (top, (0, 0, 16, 16)),
                                               **{f: (base, (2, 0, 14, 3)) for f in ("south", "east")}})
    if item is not None:
        icon = item.resize((64, 64), Image.NEAREST)
        x, y = project(8, 19, 8)
        canvas.alpha_composite(icon, (int(o[0] + x - 32), int(o[1] + y - 40)))


def lantern(canvas, o, name, hanging=False):
    t = tex(name)
    lift = 1 if hanging else 0
    body = {"up": (t, (0, 7, 6, 13)), "south": (t, (0, 0, 6, 7)), "east": (t, (0, 0, 6, 7))}
    cap = {"up": (t, (7, 2, 11, 6)), "south": (t, (7, 0, 11, 2)), "east": (t, (7, 0, 11, 2))}
    box(canvas, o, (5, lift, 5), (11, 7 + lift, 11), body)
    box(canvas, o, (6, 7 + lift, 6), (10, 9 + lift, 10), cap)
    box(canvas, o, (6.5, 9 + lift, 8), (9.5, 11 + lift, 8), {"south": (t, (12, 0, 15, 2))})


def label(canvas, xy, text):
    ImageDraw.Draw(canvas).text(xy, text, fill=(225, 228, 235, 255))


def main(out):
    cell_w, cell_h = 2 * C30 * 16 * S + 40, 2 * 16 * S + 60
    cols = 8
    rows = 4
    canvas = Image.new("RGBA", (int(cols * cell_w), int(rows * cell_h)), (38, 42, 50, 255))

    def at(col, row):
        return (int(col * cell_w + cell_w / 2), int(row * cell_h + 16 * S * S30 * 2 + 50 - 16 * S * S30 + 10))

    for row, metal in enumerate(("iron", "gold", "diamond")):
        items = [
            ("plating", lambda o: cube(canvas, o, tex(f"{metal}_plating"))),
            ("tiles", lambda o: cube(canvas, o, tex(f"{metal}_tiles"))),
            ("cut", lambda o: cube(canvas, o, tex(f"{metal}_cut"))),
            ("pillar", lambda o: cube(canvas, o, tex(f"{metal}_pillar"), tex(f"{metal}_pillar_top"))),
            ("grate", lambda o: cube(canvas, o, tex(f"{metal}_grate"), back=True)),
            ("stairs", lambda o: stairs(canvas, o, tex(f"{metal}_plating"))),
            ("slab", lambda o: slab(canvas, o, tex(f"{metal}_tiles"))),
            ("pedestal", lambda o: pedestal(canvas, o, metal, tex("diamond_lantern", "item") if metal == "gold" else None)),
        ]
        for col, (name, draw) in enumerate(items):
            o = at(col, row)
            draw(o)
            label(canvas, (o[0] - 30, int((row + 1) * cell_h) - 22), f"{metal} {name}")
    o = at(0, 3)
    for i, name in enumerate(("gold_lantern", "gold_soul_lantern", "diamond_lantern")):
        lantern(canvas, at(i, 3), name)
        label(canvas, (at(i, 3)[0] - 40, int(4 * cell_h) - 22), name.replace("_", " "))
    for i, name in enumerate(("gold_chain", "gold_bars", "gold_lantern", "diamond_lantern")):
        icon = tex(name, "item").resize((96, 96), Image.NEAREST)
        x, y = at(3 + i, 3)
        canvas.alpha_composite(icon, (x - 48, y - 20))
        label(canvas, (x - 40, int(4 * cell_h) - 22), name.replace("_", " ") + " (item)")
    canvas.save(out)


if __name__ == "__main__":
    main(sys.argv[1])
