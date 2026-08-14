package bm.b0b0b0.soulCrates.animation;

import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.session.CrateOpeningSession;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class FireworkRevealPhase implements PhaseRunner {

    private final int durationTicks;
    private int ticksRemaining;
    private boolean launched;

    public FireworkRevealPhase(RewardDefinition reward, int durationTicks) {
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
            playRevealBurst(player, session);
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

    private void playRevealBurst(Player player, CrateOpeningSession session) {
        Location location = ParticleEffectUtil.crateCenter(player, session);
        if (location.getWorld() == null) {
            return;
        }
        location.getWorld().playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.1f);
        location.getWorld().playSound(location, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.9f, 1.2f);
        location.getWorld().playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 0.65f, 1.15f);
        ParticleEffectUtil.spawn(location.getWorld(), location, Particle.FIREWORK, null, 32, 0.4, 0.45, 0.4, 0.08);
        ParticleEffectUtil.spawn(location.getWorld(), location, Particle.TOTEM_OF_UNDYING, null, 12, 0.25, 0.35, 0.25, 0.02);
    }
}
