package com.firemerald.additionalplacements.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Replacements for the collection helpers that were removed from {@link FriendlyByteBuf}.
 */
public class BufUtils {
	public static <T> List<T> readList(FriendlyByteBuf buf, Function<FriendlyByteBuf, T> reader) {
		int size = buf.readVarInt();
		List<T> list = new ArrayList<>(size);
		for (int i = 0; i < size; i++) list.add(reader.apply(buf));
		return list;
	}

	public static <T> void writeCollection(FriendlyByteBuf buf, Collection<T> collection, BiConsumer<FriendlyByteBuf, T> writer) {
		buf.writeVarInt(collection.size());
		for (T value : collection) writer.accept(buf, value);
	}

	public static <K, V> Map<K, V> readMap(FriendlyByteBuf buf, Function<FriendlyByteBuf, K> keyReader, Function<FriendlyByteBuf, V> valueReader) {
		int size = buf.readVarInt();
		Map<K, V> map = new LinkedHashMap<>(size);
		for (int i = 0; i < size; i++) {
			K key = keyReader.apply(buf);
			map.put(key, valueReader.apply(buf));
		}
		return map;
	}

	public static <K, V> void writeMap(FriendlyByteBuf buf, Map<K, V> map, BiConsumer<FriendlyByteBuf, K> keyWriter, BiConsumer<FriendlyByteBuf, V> valueWriter) {
		buf.writeVarInt(map.size());
		map.forEach((key, value) -> {
			keyWriter.accept(buf, key);
			valueWriter.accept(buf, value);
		});
	}
}
