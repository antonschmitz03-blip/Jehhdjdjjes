package com.firemerald.additionalplacements.util;

import java.util.function.Consumer;

import com.google.gson.*;

import net.minecraft.nbt.*;

public class NBTUtils {
	public static JsonElement toJson(Tag nbt) {
		if (nbt instanceof CompoundTag) return toJson((CompoundTag) nbt);
		if (nbt instanceof CollectionTag) return toJson((CollectionTag) nbt);
		if (nbt instanceof NumericTag) return toJson((NumericTag) nbt);
		if (nbt instanceof StringTag) return toJson((StringTag) nbt);
		return JsonNull.INSTANCE; //TODO shouldn't happen but if it does throw error?
	}

	public static JsonObject toJson(CompoundTag compound) {
		JsonObject obj = new JsonObject();
		compound.keySet().forEach(key -> obj.add(key, toJson(compound.get(key))));
		return obj;
	}

	public static JsonArray toJson(CollectionTag list) {
		JsonArray array = new JsonArray();
		for (int i = 0; i < list.size(); ++i) array.add(toJson(list.get(i)));
		return array;
	}

	public static JsonPrimitive toJson(NumericTag number) {
		return new JsonPrimitive(number.box());
	}

	public static JsonPrimitive toJson(StringTag string) {
		return new JsonPrimitive(string.value());
	}

	public static void ifCompoundNotEmpty(CompoundTag tag, String key, Consumer<CompoundTag> action) {
		tag.getCompound(key).ifPresent(tag2 -> {
			if (!tag2.isEmpty()) action.accept(tag2);
		});
	}

	public static void ifListNotEmpty(CompoundTag tag, String key, int listTagType, Consumer<ListTag> action) {
		tag.getList(key).ifPresent(list -> {
			if (!list.isEmpty()) action.accept(list);
		});
	}
}
