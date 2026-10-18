package io.github.antonschmitz.vplustweaks;

public final class LargeTrees {
    /** True on the current thread while a sapling is being grown into a tree. */
    public static final ThreadLocal<Boolean> GROWING_FROM_SAPLING = ThreadLocal.withInitial(() -> false);

    private LargeTrees() {}
}
