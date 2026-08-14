package bm.b0b0b0.soulCrates.model;

public record WinLimitCheck(WinLimitVerdict verdict, int playerWins, int globalWins, long cooldownRemainingSeconds) {

    public static WinLimitCheck allowed(int playerWins, int globalWins) {
        return new WinLimitCheck(WinLimitVerdict.ALLOWED, playerWins, globalWins, 0L);
    }

    public boolean allowed() {
        return verdict == WinLimitVerdict.ALLOWED;
    }
}
