package com.firemerald.additionalplacements.util;


import java.nio.file.Path;

public class PlatformUtils {
        public static boolean isClient() {
		return com.firemerald.additionalplacements.util.fabric.PlatformUtilsImpl.isClient();
	}

        public static boolean isDedicatedServer() {
		return com.firemerald.additionalplacements.util.fabric.PlatformUtilsImpl.isDedicatedServer();
	}

        public static Path getConfigFolder() {
		return com.firemerald.additionalplacements.util.fabric.PlatformUtilsImpl.getConfigFolder();
	}

    public static void checkIsClient() {
        if (isDedicatedServer()) throw new IllegalStateException("Client-side access detected on dedicated server.");
    }

    public static void checkIsServer() {
        if (isClient()) throw new IllegalStateException("Dedicated server-side access detected on server.");
    }
}
