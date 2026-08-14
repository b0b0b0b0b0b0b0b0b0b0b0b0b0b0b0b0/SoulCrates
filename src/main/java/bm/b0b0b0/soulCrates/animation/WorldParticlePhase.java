package bm.b0b0b0.soulCrates.animation;

import bm.b0b0b0.soulCrates.config.settings.AnimationPhaseProperties;
import bm.b0b0b0.soulCrates.config.settings.AnimationPhaseSettings;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.session.CrateOpeningSession;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public final class WorldParticlePhase implements PhaseRunner {

    public enum Style {
        DEFAULT,
        SWIRL,
        HELIX,
        BALL,
        FIRE,
        CRACK,
        KEY_OPENER,
        NONE
    }

    private final OpeningPhaseKind kind;
    private final Style style;
    private final AnimationPhaseSettings settings;
    private final RewardDefinition reward;
    private int ticksRemaining;
    private int tickCounter;
    private Location center;

    public WorldParticlePhase(
            OpeningPhaseKind kind,
            Style style,
            AnimationPhaseSettings settings,
            RewardDefinition reward
    ) {
        this.kind = kind;
        this.style = style;
        this.settings = settings;
        this.reward = reward;
    }

    @Override
    public OpeningPhaseKind kind() {
        return kind;
    }

    @Override
    public void load(Player player, CrateOpeningSession session) {
        if (style == Style.NONE) {
            ticksRemaining = 1;
            return;
        }
        ticksRemaining = Math.max(1, settings.durationTicks);
        tickCounter = 0;
        center = ParticleEffectUtil.crateCenter(player, session);
        AnimationPhaseProperties properties = settings.properties == null ? new AnimationPhaseProperties() : settings.properties;
        center = ParticleEffectUtil.offset(center, properties.offsetX, properties.offsetY, properties.offsetZ);
    }

    @Override
    public void tick(Player player, CrateOpeningSession session) {
        if (style == Style.NONE) {
            ticksRemaining = 0;
            return;
        }
        if (center == null || center.getWorld() == null) {
            ticksRemaining--;
            return;
        }
        AnimationPhaseProperties properties = settings.properties == null ? new AnimationPhaseProperties() : settings.properties;
        org.bukkit.Color color = ParticleEffectUtil.parseBukkitColor(properties.color, org.bukkit.Color.WHITE);
        Particle particle = switch (style) {
            case FIRE -> Particle.FLAME;
            case CRACK -> Particle.CRIT;
            case KEY_OPENER -> Particle.ENCHANT;
            case HELIX -> Particle.END_ROD;
            case BALL, SWIRL, DEFAULT -> Particle.DUST;
            default -> Particle.DUST;
        };
        switch (style) {
            case SWIRL -> spawnSwirl(particle, color);
            case HELIX -> spawnHelix(particle, color);
            case BALL -> spawnBall(particle, color);
            case FIRE -> spawnFire();
            case CRACK -> spawnCrack(particle);
            case KEY_OPENER -> spawnKeyOpener(particle, color);
            default -> spawnBurst(particle, color, 0.35);
        }
        tickCounter++;
        ticksRemaining--;
    }

    @Override
    public void unload(Player player, CrateOpeningSession session) {
    }

    @Override
    public boolean finished() {
        return ticksRemaining <= 0;
    }

    private void spawnBurst(Particle particle, org.bukkit.Color color, double spread) {
        ParticleEffectUtil.spawn(center.getWorld(), center, particle, color, 6, spread, spread, spread, 0.02);
    }

    private void spawnSwirl(Particle particle, org.bukkit.Color color) {
        double angle = tickCounter * 0.35;
        double radius = 0.8;
        double x = Math.cos(angle) * radius;
        double z = Math.sin(angle) * radius;
        Location point = center.clone().add(x, 0.1, z);
        ParticleEffectUtil.spawn(point.getWorld(), point, particle, color, 3, 0.05, 0.05, 0.05, 0.0);
    }

    private void spawnHelix(Particle particle, org.bukkit.Color color) {
        double angle = tickCounter * 0.4;
        double radius = 0.6;
        double y = (tickCounter % 20) * 0.08;
        double x = Math.cos(angle) * radius;
        double z = Math.sin(angle) * radius;
        Location point = center.clone().add(x, y, z);
        ParticleEffectUtil.spawn(point.getWorld(), point, particle, color, 2, 0.02, 0.02, 0.02, 0.0);
    }

    private void spawnBall(Particle particle, org.bukkit.Color color) {
        double angle = tickCounter * 0.5;
        double radius = 0.5 + Math.sin(tickCounter * 0.2) * 0.2;
        double x = Math.cos(angle) * radius;
        double z = Math.sin(angle) * radius;
        Location point = center.clone().add(x, 0.4, z);
        ParticleEffectUtil.spawn(point.getWorld(), point, particle, color, 4, 0.04, 0.04, 0.04, 0.0);
    }

    private void spawnFire() {
        ParticleEffectUtil.spawn(center.getWorld(), center, Particle.FLAME, null, 8, 0.25, 0.15, 0.25, 0.02);
        ParticleEffectUtil.spawn(center.getWorld(), center, Particle.LAVA, null, 1, 0.1, 0.1, 0.1, 0.0);
    }

    private void spawnCrack(Particle particle) {
        ParticleEffectUtil.spawn(center.getWorld(), center, particle, org.bukkit.Color.GRAY, 10, 0.4, 0.2, 0.4, 0.05);
        ParticleEffectUtil.spawn(center.getWorld(), center, Particle.SMOKE, null, 2, 0.1, 0.1, 0.1, 0.01);
    }

    private void spawnKeyOpener(Particle particle, org.bukkit.Color color) {
        spawnBurst(particle, color, 0.2);
        if (tickCounter % 4 == 0) {
            ParticleEffectUtil.spawn(center.getWorld(), center.clone().add(0.0, 0.5, 0.0), Particle.HAPPY_VILLAGER, null, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
