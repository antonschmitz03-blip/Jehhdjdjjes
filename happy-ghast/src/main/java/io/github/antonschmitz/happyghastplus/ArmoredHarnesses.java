package io.github.antonschmitz.happyghastplus;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Iron, diamond and netherite harnesses for the happy ghast: the vanilla harness of each colour with metal plates
 * over it. They are body armor for the ghast, keep the strap colour, and are in the minecraft:harnesses tag, so
 * everything that takes a harness takes them. Assets, recipes and names come from tools/gen_armored_harness.py.
 */
public final class ArmoredHarnesses {
    public enum Tier {
        IRON("iron", 5.0, 0.0, 0.0, false),
        DIAMOND("diamond", 11.0, 2.0, 0.0, false),
        NETHERITE("netherite", 15.0, 3.0, 0.3, true);

        final String name;
        final double armor;
        final double toughness;
        final double knockbackResistance;
        final boolean fireResistant;

        public String id() {
            return name;
        }

        Tier(String name, double armor, double toughness, double knockbackResistance, boolean fireResistant) {
            this.name = name;
            this.armor = armor;
            this.toughness = toughness;
            this.knockbackResistance = knockbackResistance;
            this.fireResistant = fireResistant;
        }
    }

    private static final Map<Tier, Map<DyeColor, Item>> ITEMS = new EnumMap<>(Tier.class);

    private ArmoredHarnesses() {
    }

    static void register(BiFunction<String, Function<Item.Properties, Item>, Item> registrar) {
        HolderGetter<EntityType<?>> entities = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ENTITY_TYPE);
        for (Tier tier : Tier.values()) {
            ItemAttributeModifiers attributes = attributes(tier);
            Map<DyeColor, Item> byColor = new EnumMap<>(DyeColor.class);
            for (DyeColor color : DyeColor.values()) {
                String name = color.getSerializedName() + "_" + tier.name + "_harness";
                Equippable equippable = Equippable.builder(EquipmentSlot.BODY)
                        .setEquipSound(SoundEvents.HARNESS_EQUIP)
                        .setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, HappyGhastPlus.id(name)))
                        .setAllowedEntities(entities.getOrThrow(EntityTypeTags.CAN_EQUIP_HARNESS))
                        .setEquipOnInteract(true)
                        .setCanBeSheared(true)
                        .setShearingSound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.HARNESS_UNEQUIP))
                        .build();
                byColor.put(color, registrar.apply(name, properties -> {
                    properties.stacksTo(1).component(DataComponents.EQUIPPABLE, equippable).attributes(attributes);
                    if (tier.fireResistant) {
                        properties.fireResistant();
                    }
                    return new Item(properties);
                }));
            }
            ITEMS.put(tier, byColor);
        }
    }

    private static ItemAttributeModifiers attributes(Tier tier) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        String prefix = tier.name + "_harness_";
        builder.add(Attributes.ARMOR, new AttributeModifier(HappyGhastPlus.id(prefix + "armor"), tier.armor,
                AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.BODY);
        if (tier.toughness > 0) {
            builder.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(HappyGhastPlus.id(prefix + "toughness"), tier.toughness,
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.BODY);
        }
        if (tier.knockbackResistance > 0) {
            builder.add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(HappyGhastPlus.id(prefix + "knockback_resistance"),
                    tier.knockbackResistance, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.BODY);
        }
        return builder.build();
    }

    /** The armored tier of a harness item, or null for anything else (a plain harness included). */
    public static Tier tierOf(Item item) {
        for (Map.Entry<Tier, Map<DyeColor, Item>> entry : ITEMS.entrySet()) {
            if (entry.getValue().containsValue(item)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static Item get(Tier tier, DyeColor color) {
        return ITEMS.get(tier).get(color);
    }

    /** All armored harnesses, tier by tier in dye colour order. */
    public static List<Item> all() {
        List<Item> all = new ArrayList<>();
        for (Tier tier : Tier.values()) {
            all.addAll(ITEMS.get(tier).values());
        }
        return all;
    }
}
