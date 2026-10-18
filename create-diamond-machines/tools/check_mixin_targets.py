"""Mixins only fail when the game runs, so check every injection point against the Create jar's bytecode.

Usage: python3 check_mixin_targets.py <create classes jar> [minecraft jar]
For each mixin method (target class, method, instruction) it disassembles the method with javap and checks the
field access or call the mixin hooks into is there.
"""
import os
import subprocess
import sys

CHECKS = [
    # (class, method name, method descriptor fragment, instruction text)
    ("com.zurrtum.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity", "tick", "()",
     "Field crushingspeed:F"),
    ("com.zurrtum.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity", "applyRecipe", "()",
     "Method com/zurrtum/create/foundation/recipe/RecipeApplier.applyRecipeOn:(Lnet/minecraft/util/RandomSource;ILnet/minecraft/world/item/crafting/RecipeInput;Lcom/zurrtum/create/foundation/recipe/CreateRollableRecipe;)Ljava/util/List;"),
    ("com.zurrtum.create.client.content.kinetics.drill.DrillRenderer", "extractRenderState",
     "(com.zurrtum.create.content.kinetics.drill.DrillBlockEntity,", "AllPartialModels.DRILL_HEAD"),
    ("com.zurrtum.create.client.content.kinetics.drill.DrillActorVisual", "com.zurrtum.create.client.content.kinetics.drill.DrillActorVisual",
     "(", "AllPartialModels.DRILL_HEAD"),
    ("com.zurrtum.create.client.content.kinetics.drill.DrillMovementRenderBehaviour", "getRenderState", "(",
     "AllPartialModels.DRILL_HEAD"),
    ("com.zurrtum.create.client.content.kinetics.saw.SawVisual", "updateSaw", "(", "Supplier.get"),
    ("com.zurrtum.create.client.content.kinetics.saw.SawRenderer", "extractRenderState",
     "(com.zurrtum.create.content.kinetics.saw.SawBlockEntity,", "Field sprite:"),
]
for orientation in ("HORIZONTAL", "VERTICAL"):
    for state in ("ACTIVE", "INACTIVE", "REVERSED"):
        blade = f"AllPartialModels.SAW_BLADE_{orientation}_{state}:"
        CHECKS.append(("com.zurrtum.create.client.content.kinetics.saw.SawVisual", "updateSaw", "(", blade))
        CHECKS.append(("com.zurrtum.create.client.content.kinetics.saw.SawRenderer", "extractRenderState",
                       "(com.zurrtum.create.content.kinetics.saw.SawBlockEntity,", blade))
        if state != "REVERSED":
            CHECKS.append(("com.zurrtum.create.client.content.kinetics.saw.SawMovementRenderBehaviour",
                           "getRenderState", "(", blade))


def methods(classpath, cls):
    out = subprocess.run(["javap", "-c", "-p", "-cp", classpath, cls], capture_output=True, text=True).stdout
    found, name, body = {}, None, []
    for line in out.splitlines():
        if line.startswith("  ") and not line.startswith("    ") and "(" in line and line.rstrip().endswith(";"):
            if name:
                found.setdefault(name, []).append("\n".join(body))
            name, body = line.strip(), []
        elif name:
            body.append(line)
    if name:
        found.setdefault(name, []).append("\n".join(body))
    return found


def main():
    classpath = os.pathsep.join(sys.argv[1:])
    failed = 0
    cache = {}
    for cls, method, desc, needle in CHECKS:
        if cls not in cache:
            cache[cls] = methods(classpath, cls)
        bodies = [b for sig, bs in cache[cls].items() if f" {method}{desc}" in " " + sig.split(" ", 10)[-1] or f"{method}{desc}" in sig for b in bs]
        ok = any(needle in b for b in bodies)
        print(("ok   " if ok else "FAIL ") + f"{cls.split('.')[-1]}.{method}: {needle}")
        failed += not ok
    # the is() override must match the method Create calls
    out = subprocess.run(["javap", "-c", "-p", "-cp", classpath,
                          "com.zurrtum.create.content.kinetics.crusher.CrushingWheelBlock"], capture_output=True, text=True).stdout
    ok = "Method net/minecraft/world/level/block/state/BlockState.is:(Ljava/lang/Object;)Z" in out
    print(("ok   " if ok else "FAIL ") + "CrushingWheelBlock calls BlockState.is(Object)")
    failed += not ok
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
