package io.github.antonschmitz.arsenal;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** CI smoke test: ammo tags, ammo lookup, a throw that spawns a stone, and the stone hurting a cow. */
final class SelfTest {
    private SelfTest() {
    }

    static void run(MinecraftServer server) {
        ServerLevel level = server.overworld();
        FakePlayer player = FakePlayer.get(level);
        try {
            check(SlingAmmo.of(new ItemStack(Items.COBBLESTONE)) == SlingAmmo.STONE_BLOCK, "cobblestone is not stone ammo");
            check(SlingAmmo.of(new ItemStack(Items.ANDESITE)) == SlingAmmo.STONE_BLOCK, "andesite is not stone ammo");
            check(SlingAmmo.of(new ItemStack(Items.DEEPSLATE_TILES)) == SlingAmmo.DEEPSLATE_BLOCK, "deepslate tiles are not deepslate ammo");
            check(SlingAmmo.of(new ItemStack(Items.DIRT)) == null, "dirt counts as ammo");
            check(SlingAmmo.DEEPSLATE_BLOCK.damage() > SlingAmmo.STONE_BLOCK.damage(), "deepslate should hit harder than stone");
            VanillaArsenal.LOGGER.info("vanillaarsenal self test: ammo ok");

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(VanillaArsenal.SLING));
            player.getInventory().setItem(10, new ItemStack(Items.DEEPSLATE, 4));
            ItemStack found = SlingItem.findAmmo(player);
            check(found != null && found.is(Items.DEEPSLATE), "ammo not found in the inventory");
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.COBBLESTONE, 4));
            check(SlingItem.findAmmo(player).is(Items.COBBLESTONE), "off hand ammo should come first");
            check(SlingItem.power(0) < 0.1F && SlingItem.power(20) == 1.0F, "draw curve wrong");

            int before = level.getEntitiesOfClass(SlingProjectile.class, player.getBoundingBox().inflate(8)).size();
            SlingItem.fire(level, player, new ItemStack(Items.GRAVEL), SlingAmmo.GRAVEL, 1.0F);
            int after = level.getEntitiesOfClass(SlingProjectile.class, player.getBoundingBox().inflate(8)).size();
            check(after - before == 3, "gravel should throw 3 pellets, got " + (after - before));
            level.getEntitiesOfClass(SlingProjectile.class, player.getBoundingBox().inflate(8)).forEach(e -> e.discard());
            VanillaArsenal.LOGGER.info("vanillaarsenal self test: throw ok");

            Cow cow = EntityType.COW.create(level, EntitySpawnReason.COMMAND);
            check(cow != null, "no cow");
            cow.setPos(player.getX() + 3, player.getY(), player.getZ());
            level.addFreshEntity(cow);
            float health = cow.getHealth();
            SlingProjectile stone = new SlingProjectile(level, player, new ItemStack(Items.COBBLESTONE), SlingAmmo.STONE_BLOCK, 1.0F);
            stone.hit(cow);
            check(cow.getHealth() < health || cow.isDeadOrDying(), "stone did no damage to a cow");
            cow.discard();
            VanillaArsenal.LOGGER.info("vanillaarsenal self test: hit ok");
        } catch (Exception | AssertionError e) {
            VanillaArsenal.LOGGER.error("vanillaarsenal self test FAILED", e);
            return;
        } finally {
            player.getInventory().clearContent();
        }
        VanillaArsenal.LOGGER.info("vanillaarsenal self test passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
