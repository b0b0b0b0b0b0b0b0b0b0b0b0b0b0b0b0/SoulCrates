package bm.b0b0b0.soulCrates.model;

public record RewardWinStats(int wins, long lastWinAt) {

    public static RewardWinStats empty() {
        return new RewardWinStats(0, 0L);
    }
}
