package io.github.antonschmitz.vplustweaks;

public final class EnchantingCosts {
    private EnchantingCosts() {
    }

    /** Half the level cost, rounded up, so a cost of 1 stays 1. Zero and negative values are kept. */
    public static int halve(int cost) {
        return cost <= 0 ? cost : (cost + 1) / 2;
    }

    /** A quarter of the level cost, rounded up, so a cost of 1 stays 1. Zero and negative values are kept. */
    public static int quarter(int cost) {
        return cost <= 0 ? cost : (cost + 3) / 4;
    }

    public static int[] halve(int[] costs) {
        int[] halved = new int[costs.length];
        for (int i = 0; i < costs.length; i++) {
            halved[i] = halve(costs[i]);
        }
        return halved;
    }
}
