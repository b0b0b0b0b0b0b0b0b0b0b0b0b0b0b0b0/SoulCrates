package bm.b0b0b0.soulCrates.animation;

import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.session.CrateOpeningSession;
import org.bukkit.entity.Player;

public final class SpinnerPhase implements PhaseRunner {

    private final String animationId;
    private int ticksRemaining;

    public SpinnerPhase(String animationId, int durationTicks) {
        this.animationId = animationId == null || animationId.isEmpty() ? "spinner" : animationId;
        this.ticksRemaining = Math.max(1, durationTicks);
    }

    @Override
    public OpeningPhaseKind kind() {
        return OpeningPhaseKind.SECOND;
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
