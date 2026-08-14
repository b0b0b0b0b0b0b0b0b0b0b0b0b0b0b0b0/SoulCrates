package bm.b0b0b0.soulCrates.animation;

import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.session.CrateOpeningSession;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

public final class FireworkRevealPhase implements PhaseRunner {

    private final RewardDefinition reward;
    private final int durationTicks;
    private int ticksRemaining;
    private boolean launched;

    public FireworkRevealPhase(RewardDefinition reward, int durationTicks) {
        this.reward = reward;
        this.durationTicks = Math.max(10, durationTicks);
    }

    @Override
    public OpeningPhaseKind kind() {
        return OpeningPhaseKind.THIRD;
    }

    @Override
    public void load(Player player, CrateOpeningSession session) {
        ticksRemaining = durationTicks;
        launched = false;
    }

    @Override
    public void tick(Player player, CrateOpeningSession session) {
        if (!launched) {
            launchFirework(player.getLocation().clone().add(0.0, 1.0, 0.0));
            launched = true;
        }
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

    private void launchFirework(Location location) {
        if (location.getWorld() == null) {
            return;
        }
        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .withColor(Color.ORANGE, Color.YELLOW, Color.RED)
                .withFade(Color.WHITE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .trail(true)
                .flicker(true)
                .build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);
        firework.detonate();
    }
}
