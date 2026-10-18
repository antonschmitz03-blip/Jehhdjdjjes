"""Writes every Metal Decor blockstate, model, item definition, loot table, recipe, tag and language entry.

The block list here must match MetalDecor.java. Run from the metal-decor folder: python3 tools/make_assets.py
"""
import json
from pathlib import Path

NS = "metaldecor"
RES = Path(__file__).resolve().parent.parent / "src/main/resources"
ASSETS = RES / "assets" / NS
DATA = RES / "data" / NS

METALS = {
    # metal: (vanilla block, ingot or gem, nugget, mining tier tag)
    "iron": ("minecraft:iron_block", "minecraft:iron_ingot", "minecraft:iron_nugget", "needs_stone_tool"),
    "gold": ("minecraft:gold_block", "minecraft:gold_ingot", "minecraft:gold_nugget", "needs_iron_tool"),
    "diamond": ("minecraft:diamond_block", "minecraft:diamond", None, "needs_iron_tool"),
}

EN_METAL = {"iron": "Iron", "gold": "Gold", "diamond": "Diamond"}
DE_METAL = {"iron": "Eisen", "gold": "Gold", "diamond": "Diamant"}

# Per metal: (block id, stairs id, slab id, texture) for the three styles that get stairs and slabs.
def styles(m):
    return [
        (f"{m}_plating", f"{m}_plating_stairs", f"{m}_plating_slab", f"{m}_plating"),
        (f"{m}_tiles", f"{m}_tile_stairs", f"{m}_tile_slab", f"{m}_tiles"),
        (f"cut_{m}", f"cut_{m}_stairs", f"cut_{m}_slab", f"{m}_cut"),
    ]


def names(m):
    """English and German names of every decor block of one metal. The diamond set has elvish names."""
    en, de = EN_METAL[m], DE_METAL[m]
    if m == "diamond":
        en_style = {"plating": "Diamond Filigree", "tiles": "Diamond Leaf Tile", "cut": "Carved Diamond"}
        de_style = {"plating": "Diamantfiligran", "tiles": "Diamantblattfliesen", "cut": "Geschnitzte Diamant"}
        out_en = {
            "diamond_plating": "Diamond Filigree", "diamond_plating_stairs": "Diamond Filigree Stairs",
            "diamond_plating_slab": "Diamond Filigree Slab",
            "diamond_tiles": "Diamond Leaf Tiles", "diamond_tile_stairs": "Diamond Leaf Tile Stairs",
            "diamond_tile_slab": "Diamond Leaf Tile Slab",
            "cut_diamond": "Carved Diamond", "cut_diamond_stairs": "Carved Diamond Stairs",
            "cut_diamond_slab": "Carved Diamond Slab",
            "diamond_pillar": "Diamond Vine Pillar", "diamond_grate": "Diamond Lattice",
            "diamond_pedestal": "Diamond Pedestal",
        }
        out_de = {
            "diamond_plating": "Diamantfiligran", "diamond_plating_stairs": "Diamantfiligrantreppe",
            "diamond_plating_slab": "Diamantfiligranstufe",
            "diamond_tiles": "Diamantblattfliesen", "diamond_tile_stairs": "Diamantblattfliesentreppe",
            "diamond_tile_slab": "Diamantblattfliesenstufe",
            "cut_diamond": "Geschnitzter Diamantblock", "cut_diamond_stairs": "Geschnitzte Diamanttreppe",
            "cut_diamond_slab": "Geschnitzte Diamantstufe",
            "diamond_pillar": "Diamantrankensäule", "diamond_grate": "Diamantmaßwerk",
            "diamond_pedestal": "Diamantsockel",
        }
        return out_en, out_de
    out_en = {
        f"{m}_plating": f"{en} Plating", f"{m}_plating_stairs": f"{en} Plating Stairs",
        f"{m}_plating_slab": f"{en} Plating Slab",
        f"{m}_tiles": f"{en} Tiles", f"{m}_tile_stairs": f"{en} Tile Stairs", f"{m}_tile_slab": f"{en} Tile Slab",
        f"cut_{m}": f"Cut {en}", f"cut_{m}_stairs": f"Cut {en} Stairs", f"cut_{m}_slab": f"Cut {en} Slab",
        f"{m}_pillar": f"{en} Pillar", f"{m}_grate": f"{en} Grate", f"{m}_pedestal": f"{en} Pedestal",
    }
    out_de = {
        f"{m}_plating": f"{de}platten", f"{m}_plating_stairs": f"{de}plattentreppe",
        f"{m}_plating_slab": f"{de}plattenstufe",
        f"{m}_tiles": f"{de}fliesen", f"{m}_tile_stairs": f"{de}fliesentreppe", f"{m}_tile_slab": f"{de}fliesenstufe",
        f"cut_{m}": f"Geschnittener {de}block", f"cut_{m}_stairs": f"Geschnittene {de}treppe",
        f"cut_{m}_slab": f"Geschnittene {de}stufe",
        f"{m}_pillar": f"{de}säule", f"{m}_grate": f"{de}gitterrost", f"{m}_pedestal": f"{de}sockel",
    }
    return out_en, out_de


LIGHTS_EN = {"gold_lantern": "Gold Lantern", "gold_soul_lantern": "Gold Soul Lantern",
             "diamond_lantern": "Diamond Lantern", "gold_chain": "Gold Chain", "gold_bars": "Gold Bars"}
LIGHTS_DE = {"gold_lantern": "Goldlaterne", "gold_soul_lantern": "Goldene Seelenlaterne",
             "diamond_lantern": "Diamantlaterne", "gold_chain": "Goldkette", "gold_bars": "Goldgitter"}


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def ref(kind, name):
    return f"{NS}:{kind}/{name}"


def model(name, obj):
    write(ASSETS / "models" / "block" / f"{name}.json", obj)


def blockstate(name, obj):
    write(ASSETS / "blockstates" / f"{name}.json", obj)


def item_def(name, model_ref):
    write(ASSETS / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": model_ref}})


def loot(name, slab=False):
    entry = {"type": "minecraft:item", "name": f"{NS}:{name}"}
    if slab:
        entry["functions"] = [
            {"function": "minecraft:set_count", "count": 2.0, "add": False, "conditions": [
                {"condition": "minecraft:block_state_property", "block": f"{NS}:{name}",
                 "properties": {"type": "double"}}]},
            {"function": "minecraft:explosion_decay"},
        ]
    pool = {"rolls": 1.0, "bonus_rolls": 0.0, "entries": [entry]}
    if not slab:
        pool["conditions"] = [{"condition": "minecraft:survives_explosion"}]
    write(DATA / "loot_table" / "blocks" / f"{name}.json",
          {"type": "minecraft:block", "pools": [pool], "random_sequence": f"{NS}:blocks/{name}"})


def stonecut(name, ingredient, result, count=1):
    write(DATA / "recipe" / f"{name}.json", {
        "type": "minecraft:stonecutting", "ingredient": ingredient,
        "result": {"id": f"{NS}:{result}", "count": count}})


def shaped(name, pattern, key, result, count=1, category="building"):
    write(DATA / "recipe" / f"{name}.json", {
        "type": "minecraft:crafting_shaped", "category": category, "pattern": pattern, "key": key,
        "result": {"id": f"{NS}:{result}", "count": count}})


# ---------------------------------------------------------------- shapes

def cube(name, texture):
    model(name, {"parent": "minecraft:block/cube_all", "textures": {"all": ref("block", texture)}})
    blockstate(name, {"variants": {"": {"model": ref("block", name)}}})
    item_def(name, ref("block", name))
    loot(name)


def stairs(name, texture):
    t = {"bottom": ref("block", texture), "top": ref("block", texture), "side": ref("block", texture)}
    model(name, {"parent": "minecraft:block/stairs", "textures": t})
    model(name + "_inner", {"parent": "minecraft:block/inner_stairs", "textures": t})
    model(name + "_outer", {"parent": "minecraft:block/outer_stairs", "textures": t})
    variants = {}
    y_of = {"east": 0, "south": 90, "west": 180, "north": 270}
    for facing, base_y in y_of.items():
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                suffix = "" if shape == "straight" else ("_inner" if shape.startswith("inner") else "_outer")
                y = base_y
                if shape in ("inner_left", "outer_left"):
                    y -= 90
                if half == "top":
                    if shape in ("inner_right", "outer_right", "inner_left", "outer_left"):
                        # Upside-down corners turn the other way round.
                        y += 90
                v = {"model": ref("block", name + suffix)}
                if half == "top":
                    v["x"] = 180
                y %= 360
                if y:
                    v["y"] = y
                if half == "top" or y:
                    v["uvlock"] = True
                variants[f"facing={facing},half={half},shape={shape}"] = v
    blockstate(name, {"variants": variants})
    item_def(name, ref("block", name))
    loot(name)


def slab(name, texture, full_block):
    t = {"bottom": ref("block", texture), "top": ref("block", texture), "side": ref("block", texture)}
    model(name, {"parent": "minecraft:block/slab", "textures": t})
    model(name + "_top", {"parent": "minecraft:block/slab_top", "textures": t})
    blockstate(name, {"variants": {
        "type=bottom": {"model": ref("block", name)},
        "type=top": {"model": ref("block", name + "_top")},
        "type=double": {"model": ref("block", full_block)},
    }})
    item_def(name, ref("block", name))
    loot(name, slab=True)


def pillar(name, side, end):
    t = {"side": ref("block", side), "end": ref("block", end)}
    model(name, {"parent": "minecraft:block/cube_column", "textures": t})
    model(name + "_horizontal", {"parent": "minecraft:block/cube_column_horizontal", "textures": t})
    blockstate(name, {"variants": {
        "axis=y": {"model": ref("block", name)},
        "axis=z": {"model": ref("block", name + "_horizontal"), "x": 90},
        "axis=x": {"model": ref("block", name + "_horizontal"), "x": 90, "y": 90},
    }})
    item_def(name, ref("block", name))
    loot(name)


def pedestal(name, metal):
    model(name, {"parent": ref("block", "template_pedestal"), "textures": {
        "base": ref("block", f"{metal}_cut"), "column": ref("block", f"{metal}_pillar"),
        "top": ref("block", f"{metal}_pedestal_top"), "particle": ref("block", f"{metal}_cut")}})
    blockstate(name, {"variants": {"": {"model": ref("block", name)}}})
    item_def(name, ref("block", name))
    loot(name)


def faces(texture, uv_side, uv_top=None, cull_down=False, skip=()):
    out = {}
    for f in ("north", "south", "east", "west"):
        out[f] = {"uv": uv_side, "texture": texture}
    if uv_top:
        out["up"] = {"uv": uv_top, "texture": texture}
        out["down"] = {"uv": uv_top, "texture": texture}
        if cull_down:
            out["down"]["cullface"] = "down"
    for f in skip:
        out.pop(f, None)
    return out


def templates():
    model("template_pedestal", {
        "parent": "minecraft:block/block",
        "textures": {"particle": "#base"},
        "elements": [
            {"from": [1, 0, 1], "to": [15, 3, 15], "faces": {
                **faces("#base", [1, 13, 15, 16]),
                "up": {"uv": [1, 1, 15, 15], "texture": "#base"},
                "down": {"uv": [1, 1, 15, 15], "texture": "#base", "cullface": "down"}}},
            {"from": [4, 3, 4], "to": [12, 11, 12], "faces": faces("#column", [4, 4, 12, 12])},
            {"from": [2, 11, 2], "to": [14, 14, 14], "faces": {
                **faces("#base", [2, 0, 14, 3]),
                "up": {"uv": [0, 0, 16, 16], "texture": "#top"},
                "down": {"uv": [2, 2, 14, 14], "texture": "#base"}}},
        ]})

    def lantern_elements(lift):
        handle_top = 9 + lift
        out = [
            {"from": [5, lift, 5], "to": [11, 7 + lift, 11], "faces": {
                **faces("#lantern", [0, 0, 6, 7]),
                "up": {"uv": [0, 7, 6, 13], "texture": "#lantern"},
                "down": {"uv": [0, 7, 6, 13], "texture": "#lantern"}}},
            {"from": [6, 7 + lift, 6], "to": [10, 9 + lift, 10], "faces": {
                **faces("#lantern", [7, 0, 11, 2]),
                "up": {"uv": [7, 2, 11, 6], "texture": "#lantern"}}},
            {"from": [6.5, handle_top, 8], "to": [9.5, handle_top + 2, 8],
             "rotation": {"angle": 45, "axis": "y", "origin": [8, 8, 8]}, "shade": False, "faces": {
                "north": {"uv": [12, 0, 15, 2], "texture": "#lantern"},
                "south": {"uv": [12, 0, 15, 2], "texture": "#lantern"}}},
            {"from": [8, handle_top, 6.5], "to": [8, handle_top + 2, 9.5],
             "rotation": {"angle": 45, "axis": "y", "origin": [8, 8, 8]}, "shade": False, "faces": {
                "west": {"uv": [12, 0, 15, 2], "texture": "#lantern"},
                "east": {"uv": [12, 0, 15, 2], "texture": "#lantern"}}},
        ]
        if lift:
            chain_bottom = handle_top + 2
            out += [
                {"from": [6.5, chain_bottom, 8], "to": [9.5, 16, 8],
                 "rotation": {"angle": 45, "axis": "y", "origin": [8, 8, 8]}, "shade": False, "faces": {
                    "north": {"uv": [12, 2, 15, 2 + 16 - chain_bottom], "texture": "#lantern"},
                    "south": {"uv": [12, 2, 15, 2 + 16 - chain_bottom], "texture": "#lantern"}}},
                {"from": [8, chain_bottom, 6.5], "to": [8, 16, 9.5],
                 "rotation": {"angle": 45, "axis": "y", "origin": [8, 8, 8]}, "shade": False, "faces": {
                    "west": {"uv": [12, 2, 15, 2 + 16 - chain_bottom], "texture": "#lantern"},
                    "east": {"uv": [12, 2, 15, 2 + 16 - chain_bottom], "texture": "#lantern"}}},
            ]
        return out

    model("template_lantern", {"parent": "minecraft:block/block", "textures": {"particle": "#lantern"},
                               "elements": lantern_elements(0)})
    model("template_hanging_lantern", {"parent": "minecraft:block/block", "textures": {"particle": "#lantern"},
                                       "elements": lantern_elements(1)})


def lantern(name):
    model(name, {"parent": ref("block", "template_lantern"), "textures": {"lantern": ref("block", name)}})
    model(name + "_hanging", {"parent": ref("block", "template_hanging_lantern"),
                              "textures": {"lantern": ref("block", name)}})
    blockstate(name, {"variants": {
        "hanging=false": {"model": ref("block", name)},
        "hanging=true": {"model": ref("block", name + "_hanging")},
    }})
    flat_item(name)
    loot(name)


def flat_item(name):
    write(ASSETS / "models" / "item" / f"{name}.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": ref("item", name)}})
    item_def(name, ref("item", name))


def chain(name):
    tex = ref("block", name)
    model(name, {"textures": {"particle": tex, "all": tex}, "elements": [
        {"from": [6.5, 0, 8], "to": [9.5, 16, 8], "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45},
         "shade": False, "faces": {"north": {"uv": [0, 0, 3, 16], "texture": "#all"},
                                   "south": {"uv": [0, 0, 3, 16], "texture": "#all"}}},
        {"from": [8, 0, 6.5], "to": [8, 16, 9.5], "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45},
         "shade": False, "faces": {"west": {"uv": [3, 0, 6, 16], "texture": "#all"},
                                   "east": {"uv": [3, 0, 6, 16], "texture": "#all"}}},
    ]})
    blockstate(name, {"variants": {
        "axis=y": {"model": ref("block", name)},
        "axis=z": {"model": ref("block", name), "x": 90},
        "axis=x": {"model": ref("block", name), "x": 90, "y": 90},
    }})
    flat_item(name)
    loot(name)


def bars(name):
    tex = ref("block", name)
    model(name + "_post", {"textures": {"particle": tex, "bars": tex}, "elements": [
        {"from": [7, 0, 7], "to": [9, 16, 9], "faces": {
            **faces("#bars", [7, 0, 9, 16]),
            "up": {"uv": [7, 0, 9, 2], "texture": "#bars", "cullface": "up"},
            "down": {"uv": [7, 0, 9, 2], "texture": "#bars", "cullface": "down"}}}]})
    model(name + "_side", {"textures": {"particle": tex, "bars": tex}, "elements": [
        {"from": [7, 0, 0], "to": [9, 16, 7], "faces": {
            "west": {"uv": [0, 0, 7, 16], "texture": "#bars"},
            "east": {"uv": [0, 0, 7, 16], "texture": "#bars"},
            "north": {"uv": [7, 0, 9, 16], "texture": "#bars", "cullface": "north"},
            "up": {"uv": [2, 0, 4, 7], "texture": "#bars", "cullface": "up"},
            "down": {"uv": [2, 0, 4, 7], "texture": "#bars", "cullface": "down"}}}]})
    parts = [{"apply": {"model": ref("block", name + "_post")}}]
    for side, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        apply = {"model": ref("block", name + "_side")}
        if y:
            apply["y"] = y
        parts.append({"when": {side: "true"}, "apply": apply})
    blockstate(name, {"multipart": parts})
    flat_item(name)
    loot(name)


# ---------------------------------------------------------------- everything

def main():
    lang_en, lang_de = {}, {}
    tags = {"pickaxe": [], "needs_stone_tool": [], "needs_iron_tool": [], "stairs": [], "slabs": []}
    templates()
    for m, (block, ingot, nugget, tier) in METALS.items():
        en, de = names(m)
        lang_en.update({f"block.{NS}.{k}": v for k, v in en.items()})
        lang_de.update({f"block.{NS}.{k}": v for k, v in de.items()})
        made = []
        for full, st, sl, tex in styles(m):
            cube(full, tex)
            stairs(st, tex)
            slab(sl, tex, full)
            made += [full, st, sl]
            tags["stairs"].append(st)
            tags["slabs"].append(sl)
            stonecut(f"{full}_from_{m}_block", block, full)
            for source in (block, f"{NS}:{full}"):
                src = "block" if source == block else "base"
                stonecut(f"{st}_from_{src}_stonecutting", source, st)
                stonecut(f"{sl}_from_{src}_stonecutting", source, sl, 2)
            shaped(st, ["#  ", "## ", "###"], {"#": f"{NS}:{full}"}, st, 4)
            shaped(sl, ["###"], {"#": f"{NS}:{full}"}, sl, 6)
        pillar(f"{m}_pillar", f"{m}_pillar", f"{m}_pillar_top")
        stonecut(f"{m}_pillar_from_{m}_block", block, f"{m}_pillar")
        cube(f"{m}_grate", f"{m}_grate")
        stonecut(f"{m}_grate_from_{m}_block", block, f"{m}_grate")
        shaped(f"{m}_grate", [" # ", "# #", " # "], {"#": block}, f"{m}_grate", 4)
        pedestal(f"{m}_pedestal", m)
        shaped(f"{m}_pedestal", ["III", " B ", "III"], {"I": ingot, "B": block}, f"{m}_pedestal",
               category="misc")
        made += [f"{m}_pillar", f"{m}_grate", f"{m}_pedestal"]
        tags["pickaxe"] += made
        tags[tier] += made

    for name in ("gold_lantern", "gold_soul_lantern", "diamond_lantern"):
        lantern(name)
    chain("gold_chain")
    bars("gold_bars")
    shaped("gold_lantern", ["NNN", "NTN", "NNN"],
           {"N": "minecraft:gold_nugget", "T": "minecraft:torch"}, "gold_lantern", category="misc")
    shaped("gold_soul_lantern", ["NNN", "NTN", "NNN"],
           {"N": "minecraft:gold_nugget", "T": "minecraft:soul_torch"}, "gold_soul_lantern", category="misc")
    shaped("diamond_lantern", ["NDN", "DGD", "NDN"],
           {"N": "minecraft:gold_nugget", "D": "minecraft:diamond", "G": "minecraft:glowstone_dust"},
           "diamond_lantern", category="misc")
    shaped("gold_chain", ["N", "I", "N"],
           {"N": "minecraft:gold_nugget", "I": "minecraft:gold_ingot"}, "gold_chain", category="misc")
    shaped("gold_bars", ["III", "III"], {"I": "minecraft:gold_ingot"}, "gold_bars", 16, category="misc")
    lights = list(LIGHTS_EN)
    tags["pickaxe"] += lights
    tags["needs_iron_tool"] += lights
    lang_en.update({f"block.{NS}.{k}": v for k, v in LIGHTS_EN.items()})
    lang_de.update({f"block.{NS}.{k}": v for k, v in LIGHTS_DE.items()})

    lang_en[f"{NS}.message.pedestal_empty"] = "Use an item on the pedestal to put it on display"
    lang_de[f"{NS}.message.pedestal_empty"] = "Benutze einen Gegenstand am Sockel, um ihn auszustellen"
    write(ASSETS / "lang" / "en_us.json", lang_en)
    write(ASSETS / "lang" / "de_de.json", lang_de)

    mc_tags = RES / "data" / "minecraft" / "tags"
    vals = lambda ids: {"replace": False, "values": [f"{NS}:{i}" for i in ids]}
    write(mc_tags / "block" / "mineable" / "pickaxe.json", vals(tags["pickaxe"]))
    write(mc_tags / "block" / "needs_stone_tool.json", vals(tags["needs_stone_tool"]))
    write(mc_tags / "block" / "needs_iron_tool.json", vals(tags["needs_iron_tool"]))
    for kind in ("stairs", "slabs"):
        write(mc_tags / "block" / f"{kind}.json", vals(tags[kind]))
        write(mc_tags / "item" / f"{kind}.json", vals(tags[kind]))
    print(len(tags["pickaxe"]), "blocks")


if __name__ == "__main__":
    main()
