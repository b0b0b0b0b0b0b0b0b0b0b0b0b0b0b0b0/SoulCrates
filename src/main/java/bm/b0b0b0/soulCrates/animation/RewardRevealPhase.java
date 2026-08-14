package bm.b0b0b0.soulCrates.animation;

import bm.b0b0b0.soulCrates.session.CrateOpeningSession;
import org.bukkit.entity.Player;

public final class RewardRevealPhase implements PhaseRunner {

    private final String animationId;
    private int ticksRemaining;

    public RewardRevealPhase(String animationId, int durationTicks) {
        this.animationId = animationId == null || animationId.isEmpty() ? "reveal" : animationId;
        this.ticksRemaining = Math.max(1, durationTicks);
    }

    @Override
    public OpeningPhaseKind kind() {
        return OpeningPhaseKind.THIRD;
    }

    @Override
    public void load(Player player, CrateOpeningSession session) {
    }

    @Override
    public void tick(Player player, CrateOpeningSession session) {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
    }

    @Override
    public void unload(Player player, CrateOpeningSession session) {
    }

    @Override
    public boolean finished() {
        return ticksRemaining <= 0;
    }
}
