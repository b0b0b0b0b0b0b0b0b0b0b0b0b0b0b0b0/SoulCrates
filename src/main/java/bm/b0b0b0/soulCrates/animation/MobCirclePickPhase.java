package bm.b0b0b0.soulCrates.animation;

import bm.b0b0b0.soulCrates.config.settings.AnimationPhaseProperties;
import bm.b0b0b0.soulCrates.config.settings.AnimationPhaseSettings;
import bm.b0b0b0.soulCrates.config.settings.RarityTierSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.service.reward.BroadcastService;
import bm.b0b0b0.soulCrates.session.CrateOpeningSession;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import bm.b0b0b0.soulCrates.util.SoulCratesKeys;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public final class MobCirclePickPhase implements PhaseRunner {

    private static final double RADIUS = 3.0;
    private static final double PLAYER_BOUND_RADIUS = 2.45;
    private static final double PLAYER_BOUND_RADIUS_SQ = PLAYER_BOUND_RADIUS * PLAYER_BOUND_RADIUS;
    private static final float LABEL_GAP = 0.55f;
    private static final float HITBOX_WIDTH = 1.15f;
    private static final float HITBOX_HEIGHT = 1.85f;
    private static final float PRIZE_ITEM_SCALE = 0.65f;
    private static final float DECOY_ITEM_SCALE = 0.58f;
    private static final float BYTE_ITEM_SCALE = 0.38f;
    private static final int BYTE_COUNT = 18;
    private static final double PRIZE_FALL_HEIGHT = 2.75;
    private static final double PRIZE_LAND_Y = 0.35;
    private static final double FLOAT_AMPLITUDE = 0.07;
    private static final double SWAY_AMPLITUDE = 0.035;
    private static final double FLOAT_SPEED = 0.09;
    private static final int PICK_TIMEOUT_TICKS = 600;
    private static final double PICK_RANGE_SQ = 8.0 * 8.0;
    private static final int PRIZE_DROP_TICKS = 42;
    private static final int REVEAL_OTHER_TICKS = 16;
    private static final int OTHER_DESPAWN_DELAY = 50;
    private static final int FINISH_HOLD_TICKS = 18;

    private static final double FLOAT_BASE = 0.08;

    private static final Map<UUID, MobCirclePickPhase> ACTIVE = new ConcurrentHashMap<>();

    private record FallingByte(ItemDisplay display, double velocityY, int spinTicks) {
    }

    private enum Stage {
        PICKING,
        PRIZE_DROP,
        REVEAL_OTHERS,
        FINISHED
    }

    private static final class Pod {
        private final int index;
        private RewardDefinition reward;
        private Location base;
        private LivingEntity mob;
        private Interaction interaction;
        private TextDisplay label;
        private ItemDisplay rewardItem;
        private int bounceTicks;
        private double bounceHeight;

        private Pod(int index, RewardDefinition reward, Location base) {
            this.index = index;
            this.reward = reward;
            this.base = base;
        }
    }

    private final Plugin plugin;
    private final MessageService messageService;
    private final BroadcastService broadcastService;
    private final CrateDefinition crateDefinition;
    private final RewardDefinition rolledReward;
    private final EntityType entityType;
    private final int podCount;

    private Stage stage = Stage.PICKING;
    private int stageTicks;
    private int pickTicksRemaining = PICK_TIMEOUT_TICKS;
    private int rewardPodIndex;
    private int winnerIndex = -1;
    private int revealOtherIndex;
    private int otherDespawnTicks;
    private boolean revealOthersDone;
    private boolean revealSent;
    private boolean picked;
    private Location center;
    private Location prizeAnchor;
    private CrateOpeningSession session;
    private BossBar bossBar;
    private ItemDisplay prizeItem;
    private TextDisplay prizeLabel;
    private final List<FallingByte> fallingBytes = new ArrayList<>();
    private final List<ItemStack> bytePool = new ArrayList<>();
    private boolean restoreCollidable = true;
    private final List<Pod> pods = new ArrayList<>();

    public MobCirclePickPhase(
            Plugin plugin,
            MessageService messageService,
            BroadcastService broadcastService,
            CrateDefinition crateDefinition,
            RewardDefinition rolledReward,
            AnimationPhaseSettings settings
    ) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.broadcastService = broadcastService;
        this.crateDefinition = crateDefinition;
        this.rolledReward = rolledReward;
        AnimationPhaseProperties properties = settings.properties == null ? new AnimationPhaseProperties() : settings.properties;
        this.entityType = resolveEntityType(properties.mobEntity);
        this.podCount = Math.max(3, Math.min(12, properties.mobCount <= 0 ? 7 : properties.mobCount));
    }

    public static Optional<MobCirclePickPhase> activePhase(UUID playerId) {
        return Optional.ofNullable(ACTIVE.get(playerId));
    }

    public static boolean ownsEntity(Plugin plugin, Entity entity, UUID sessionId) {
        if (entity == null || sessionId == null) {
            return false;
        }
        String raw = entity.getPersistentDataContainer().get(SoulCratesKeys.mobPickSession(plugin), PersistentDataType.STRING);
        if (raw == null) {
            return false;
        }
        try {
            return sessionId.equals(UUID.fromString(raw));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @Override
    public OpeningPhaseKind kind() {
        return OpeningPhaseKind.SECOND;
    }

    @Override
    public void load(Player player, CrateOpeningSession session) {
        this.session = session;
        player.closeInventory();
        stage = Stage.PICKING;
        stageTicks = 0;
        pickTicksRemaining = PICK_TIMEOUT_TICKS;
        rewardPodIndex = 0;
        revealOtherIndex = 0;
        revealOthersDone = false;
        otherDespawnTicks = 0;
        revealSent = false;
        picked = false;
        winnerIndex = -1;
        prizeAnchor = null;
        center = resolveGroundCenter(player, session);
        hidePhysicalCrateBlock(session);
        List<RewardDefinition> enabled = crateDefinition.rewards().stream().filter(RewardDefinition::enabled).toList();
        if (enabled.isEmpty()) {
            enabled = List.of(rolledReward);
        }
        buildPods(player, enabled, session);
        buildBytePool(enabled);
        startBossBar(player);
        restoreCollidable = player.isCollidable();
        player.setCollidable(false);
        ACTIVE.put(player.getUniqueId(), this);
        World world = center.getWorld();
        if (world != null) {
            world.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.55f, 1.25f);
            world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.45f, 1.45f);
        }
        messageService.send(player.getUniqueId(), "mob-pick-prompt");
    }

    @Override
    public void tick(Player player, CrateOpeningSession session) {
        if (center == null || center.getWorld() == null || pods.isEmpty()) {
            stage = Stage.FINISHED;
            return;
        }
        stageTicks++;
        confinePlayer(player);
        updatePodMotion(player);
        switch (stage) {
            case PICKING -> tickPicking(player);
            case PRIZE_DROP -> tickPrizeDrop(player);
            case REVEAL_OTHERS -> tickRevealOthers(player);
            case FINISHED -> {
            }
        }
    }

    @Override
    public void unload(Player player, CrateOpeningSession session) {
        ACTIVE.remove(player.getUniqueId());
        clearBossBar(player);
        if (player != null && player.isOnline()) {
            player.setCollidable(restoreCollidable);
        }
        removePrizeDisplay();
        fallingBytes.clear();
        bytePool.clear();
        for (Pod pod : pods) {
            removePod(pod);
        }
        pods.clear();
        this.session = null;
    }

    @Override
    public boolean finished() {
        return stage == Stage.FINISHED;
    }

    public boolean shouldConfinePlayer() {
        return stage == Stage.PICKING || stage == Stage.PRIZE_DROP;
    }

    public boolean isOutsideBoundary(Location location) {
        if (center == null || location == null || location.getWorld() == null) {
            return false;
        }
        if (center.getWorld() != location.getWorld()) {
            return true;
        }
        double dx = location.getX() - center.getX();
        double dz = location.getZ() - center.getZ();
        return dx * dx + dz * dz > PLAYER_BOUND_RADIUS_SQ;
    }

    public Location clampLocation(Location location) {
        if (center == null || location == null) {
            return location;
        }
        double dx = location.getX() - center.getX();
        double dz = location.getZ() - center.getZ();
        double distSq = dx * dx + dz * dz;
        if (distSq <= PLAYER_BOUND_RADIUS_SQ) {
            return location;
        }
        double dist = Math.sqrt(distSq);
        Location clamped = location.clone();
        clamped.setX(center.getX() + dx / dist * PLAYER_BOUND_RADIUS);
        clamped.setZ(center.getZ() + dz / dist * PLAYER_BOUND_RADIUS);
        return clamped;
    }

    public boolean tryPick(Player player, Entity entity) {
        if (stage != Stage.PICKING || picked || session == null || entity == null) {
            return false;
        }
        if (!player.getUniqueId().equals(session.context().playerId())) {
            return false;
        }
        for (Pod pod : pods) {
            if (matchesPodEntity(pod, entity)) {
                return pickPod(player, pod.index);
            }
        }
        return false;
    }

    public boolean tryPickLook(Player player) {
        if (stage != Stage.PICKING || picked || session == null) {
            return false;
        }
        if (!player.getUniqueId().equals(session.context().playerId())) {
            return false;
        }
        RayTraceResult trace = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                7.5,
                0.45,
                candidate -> candidate instanceof Interaction
                        || (candidate instanceof LivingEntity living && ownsEntity(plugin, living, session.sessionId()))
        );
        if (trace != null && trace.getHitEntity() != null) {
            for (Pod pod : pods) {
                if (matchesPodEntity(pod, trace.getHitEntity())) {
                    return pickPod(player, pod.index);
                }
            }
        }
        Pod best = findLookTargetPod(player);
        if (best == null) {
            return false;
        }
        return pickPod(player, best.index);
    }

    private boolean pickPod(Player player, int index) {
        if (index < 0 || index >= pods.size()) {
            return false;
        }
        Pod pod = pods.get(index);
        Location mobCenter = podCenter(pod);
        if (player.getLocation().distanceSquared(mobCenter) > PICK_RANGE_SQ) {
            return false;
        }
        onPodPicked(player, index);
        return true;
    }

    private Pod findLookTargetPod(Player player) {
        Vector look = player.getEyeLocation().getDirection().normalize();
        Pod best = null;
        double bestScore = 0.62;
        for (Pod pod : pods) {
            Location centerLocation = podCenter(pod);
            if (player.getLocation().distanceSquared(centerLocation) > PICK_RANGE_SQ) {
                continue;
            }
            Vector toPod = centerLocation.toVector().subtract(player.getEyeLocation().toVector());
            double length = toPod.length();
            if (length < 0.001) {
                continue;
            }
            toPod.normalize();
            double dot = look.dot(toPod);
            if (dot > bestScore) {
                bestScore = dot;
                best = pod;
            }
        }
        return best;
    }

    private void tickPicking(Player player) {
        pickTicksRemaining--;
        updateBossBar(player);
        if (pickTicksRemaining <= 0) {
            timeoutPick(player);
        }
    }

    private void timeoutPick(Player player) {
        if (stage != Stage.PICKING || session == null) {
            return;
        }
        stage = Stage.FINISHED;
        clearBossBar(player);
        messageService.send(
                player.getUniqueId(),
                "mob-pick-timeout",
                messageService.placeholder("crate", crateDefinition.displayName())
        );
        session.setSuppressCancelMessage(true);
        session.cancel();
    }

    private void onPodPicked(Player player, int pickedIndex) {
        if (stage != Stage.PICKING || picked) {
            return;
        }
        picked = true;
        winnerIndex = pickedIndex;
        clearBossBar(player);
        if (pickedIndex != rewardPodIndex) {
            Pod pickedPod = pods.get(pickedIndex);
            Pod rewardPod = pods.get(rewardPodIndex);
            RewardDefinition decoy = pickedPod.reward;
            pickedPod.reward = rolledReward;
            rewardPod.reward = decoy;
        }
        stage = Stage.PRIZE_DROP;
        stageTicks = 0;
        removePickInteractions();
        Pod winner = pods.get(winnerIndex);
        Location deathLocation = killMob(winner);
        prizeAnchor = deathLocation == null ? podCenter(winner) : deathLocation;
        spawnPrizeDrop(player, prizeAnchor);
        World world = center.getWorld();
        if (world != null) {
            world.playSound(prizeAnchor, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.95f, 1.05f);
            world.playSound(prizeAnchor, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.35f, 1.6f);
        }
        if (!revealSent && broadcastService != null) {
            broadcastService.dramaticWinBroadcast(player, crateDefinition, rolledReward);
            revealSent = true;
        }
        messageService.send(
                player.getUniqueId(),
                "mob-pick-win-chat",
                messageService.placeholder("reward", rolledReward.displayName()),
                messageService.placeholder("crate", crateDefinition.displayName())
        );
    }

    private void tickPrizeDrop(Player player) {
        animatePrizeDrop();
        if (stageTicks >= PRIZE_DROP_TICKS) {
            stage = Stage.REVEAL_OTHERS;
            stageTicks = 0;
            revealOtherIndex = 0;
            revealOthersDone = false;
        }
    }

    private void tickRevealOthers(Player player) {
        if (!revealOthersDone) {
            if (stageTicks >= REVEAL_OTHER_TICKS) {
                while (revealOtherIndex < pods.size() && revealOtherIndex == winnerIndex) {
                    revealOtherIndex++;
                }
                if (revealOtherIndex < pods.size()) {
                    revealOther(player, pods.get(revealOtherIndex));
                    revealOtherIndex++;
                    stageTicks = 0;
                    return;
                }
                revealOthersDone = true;
                otherDespawnTicks = OTHER_DESPAWN_DELAY;
                stageTicks = 0;
            }
            return;
        }
        if (otherDespawnTicks > 0) {
            otherDespawnTicks--;
            if (otherDespawnTicks == 0) {
                despawnOtherMobs();
            }
        }
        if (stageTicks >= FINISH_HOLD_TICKS) {
            stage = Stage.FINISHED;
        }
    }

    private void revealOther(Player player, Pod pod) {
        Location anchor = podAnchor(pod);
        World world = center.getWorld();
        if (pod.mob != null && !pod.mob.isDead()) {
            if (world != null) {
                world.spawnParticle(Particle.POOF, anchor.clone().add(0.0, 0.55, 0.0), 12, 0.15, 0.22, 0.15, 0.01);
                world.playSound(anchor, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 0.35f, 0.85f);
            }
            pod.mob.setInvulnerable(false);
            pod.mob.setHealth(0.0);
            pod.mob = null;
        }
        pod.base = anchor.clone();
        if (world != null) {
            pod.rewardItem = world.spawn(anchor.clone().add(0.0, 0.12, 0.0), ItemDisplay.class, entity -> {
                entity.setItemStack(rewardItemStack(pod.reward));
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setPersistent(false);
                entity.setViewRange(64.0f);
                entity.setBrightness(new Display.Brightness(15, 15));
                entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
                applyItemScale(entity, DECOY_ITEM_SCALE);
            });
            world.playSound(anchor, Sound.ENTITY_VILLAGER_NO, 0.55f, 1.15f);
            ParticleEffectUtil.spawn(world, anchor.clone().add(0.0, 0.45, 0.0), Particle.SMOKE, null, 8, 0.12, 0.15, 0.12, 0.01);
        }
        if (pod.label != null && !pod.label.isDead()) {
            pod.label.text(messageService.component(
                    player.getUniqueId(),
                    "mob-pick-lost",
                    messageService.placeholder("reward", pod.reward.displayName())
            ));
            pod.label.teleport(labelLocation(anchor));
        }
        triggerBounce(pod.index, 0.35);
    }

    private Location killMob(Pod pod) {
        Location location = podCenter(pod);
        World world = location.getWorld();
        if (pod.interaction != null && !pod.interaction.isDead()) {
            pod.interaction.remove();
            pod.interaction = null;
        }
        if (pod.label != null && !pod.label.isDead()) {
            pod.label.remove();
            pod.label = null;
        }
        if (pod.mob == null || pod.mob.isDead()) {
            pod.mob = null;
            return location;
        }
        location = pod.mob.getLocation().clone();
        if (world != null) {
            world.playSound(location, Sound.ENTITY_GENERIC_DEATH, 0.85f, 1.05f);
            world.spawnParticle(Particle.CLOUD, location.clone().add(0.0, 0.6, 0.0), 14, 0.18, 0.25, 0.18, 0.02);
            world.spawnParticle(Particle.DAMAGE_INDICATOR, location.clone().add(0.0, 0.8, 0.0), 6, 0.12, 0.18, 0.12, 0.0);
        }
        pod.mob.setInvulnerable(false);
        pod.mob.setHealth(0.0);
        pod.mob = null;
        return location;
    }

    private void despawnOtherMobs() {
        for (Pod pod : pods) {
            if (pod.index == winnerIndex) {
                continue;
            }
            Location location = podAnchor(pod);
            World world = location.getWorld();
            if (world != null) {
                world.spawnParticle(Particle.POOF, location.clone().add(0.0, 0.45, 0.0), 10, 0.15, 0.2, 0.15, 0.01);
                world.playSound(location, Sound.ENTITY_ITEM_PICKUP, 0.35f, 1.35f);
            }
            if (pod.rewardItem != null && !pod.rewardItem.isDead()) {
                pod.rewardItem.remove();
                pod.rewardItem = null;
            }
            if (pod.label != null && !pod.label.isDead()) {
                pod.label.remove();
                pod.label = null;
            }
            if (pod.mob != null && !pod.mob.isDead()) {
                pod.mob.setInvulnerable(false);
                pod.mob.setHealth(0.0);
                pod.mob = null;
            }
        }
    }

    private void removePickInteractions() {
        for (Pod pod : pods) {
            if (pod.interaction != null && !pod.interaction.isDead()) {
                pod.interaction.remove();
                pod.interaction = null;
            }
        }
    }

    private void spawnPrizeDrop(Player player, Location origin) {
        World world = origin.getWorld();
        if (world == null) {
            return;
        }
        removePrizeDisplay();
        ItemStack stack = rewardItemStack(rolledReward);
        prizeItem = world.spawn(origin.clone().add(0.0, PRIZE_FALL_HEIGHT, 0.0), ItemDisplay.class, entity -> {
            entity.setItemStack(stack);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setPersistent(false);
            entity.setViewRange(64.0f);
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
            applyItemScale(entity, PRIZE_ITEM_SCALE);
        });
        prizeLabel = world.spawn(origin.clone().add(0.0, PRIZE_FALL_HEIGHT + 0.7, 0.0), TextDisplay.class, entity -> {
            entity.text(messageService.component(
                    player.getUniqueId(),
                    "mob-pick-won",
                    messageService.placeholder("reward", rolledReward.displayName())
            ));
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setSeeThrough(false);
            entity.setShadowed(true);
            entity.setDefaultBackground(false);
            entity.setPersistent(false);
            entity.setViewRange(64.0f);
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setLineWidth(220);
        });
        spawnFallingBytes(world, origin);
        world.playSound(origin, Sound.ENTITY_ITEM_PICKUP, 0.85f, 0.95f);
        world.playSound(origin, Sound.ENTITY_PLAYER_LEVELUP, 0.75f, 1.25f);
        ParticleEffectUtil.spawn(
                world,
                origin.clone().add(0.0, PRIZE_FALL_HEIGHT, 0.0),
                Particle.TOTEM_OF_UNDYING,
                rewardGlow(rolledReward),
                24,
                0.25,
                0.35,
                0.25,
                0.02
        );
    }

    private void spawnFallingBytes(World world, Location origin) {
        if (bytePool.isEmpty()) {
            return;
        }
        Random random = new Random(origin.hashCode() ^ rolledReward.id().hashCode());
        for (int index = 0; index < BYTE_COUNT; index++) {
            double offsetX = (random.nextDouble() - 0.5) * 2.2;
            double offsetZ = (random.nextDouble() - 0.5) * 2.2;
            double startY = 1.6 + random.nextDouble() * 2.4;
            Location spawn = origin.clone().add(offsetX, startY, offsetZ);
            ItemStack stack = bytePool.get(random.nextInt(bytePool.size())).clone();
            ItemDisplay display = world.spawn(spawn, ItemDisplay.class, entity -> {
                entity.setItemStack(stack);
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setPersistent(false);
                entity.setViewRange(48.0f);
                entity.setBrightness(new Display.Brightness(12, 12));
                entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
                applyItemScaleRotated(entity, BYTE_ITEM_SCALE + random.nextFloat() * 0.12f, random.nextFloat() * 6.28f);
            });
            fallingBytes.add(new FallingByte(display, 0.055 + random.nextDouble() * 0.07, random.nextInt(360)));
        }
    }

    private void animatePrizeDrop() {
        if (prizeAnchor == null) {
            return;
        }
        double progress = Math.min(1.0, stageTicks / (double) PRIZE_DROP_TICKS);
        double eased = 1.0 - Math.pow(1.0 - progress, 3.0);
        double currentY = PRIZE_FALL_HEIGHT - eased * (PRIZE_FALL_HEIGHT - PRIZE_LAND_Y);
        double bounce = Math.sin(progress * Math.PI * 2.5) * 0.06 * (1.0 - progress);
        Location itemLocation = prizeAnchor.clone().add(0.0, currentY + bounce, 0.0);
        if (prizeItem != null && !prizeItem.isDead()) {
            prizeItem.teleport(itemLocation);
            applyItemScale(prizeItem, PRIZE_ITEM_SCALE + (float) (Math.sin(stageTicks * 0.22) * 0.035));
        }
        if (prizeLabel != null && !prizeLabel.isDead()) {
            prizeLabel.teleport(prizeAnchor.clone().add(0.0, currentY + bounce + 0.62, 0.0));
        }
        tickFallingBytes();
        World world = prizeAnchor.getWorld();
        if (world != null && stageTicks % 3 == 0) {
            ParticleEffectUtil.spawn(
                    world,
                    itemLocation.clone().add(0.0, 0.08, 0.0),
                    Particle.END_ROD,
                    org.bukkit.Color.WHITE,
                    2,
                    0.06,
                    0.1,
                    0.06,
                    0.0
            );
        }
    }

    private void tickFallingBytes() {
        if (prizeAnchor == null) {
            return;
        }
        double groundY = prizeAnchor.getY() + 0.12;
        World world = prizeAnchor.getWorld();
        fallingBytes.removeIf(byteDrop -> {
            if (byteDrop.display().isDead()) {
                return true;
            }
            Location location = byteDrop.display().getLocation();
            location.subtract(0.0, byteDrop.velocityY(), 0.0);
            byteDrop.display().teleport(location);
            applyItemScaleRotated(
                    byteDrop.display(),
                    BYTE_ITEM_SCALE,
                    (byteDrop.spinTicks() + stageTicks) * 0.14f
            );
            if (location.getY() <= groundY) {
                if (world != null) {
                    world.spawnParticle(Particle.ITEM, location, 3, 0.08, 0.04, 0.08, 0.02, byteDrop.display().getItemStack());
                }
                byteDrop.display().remove();
                return true;
            }
            return false;
        });
    }

    private void buildBytePool(List<RewardDefinition> enabled) {
        bytePool.clear();
        bytePool.add(rewardItemStack(rolledReward));
        for (RewardDefinition reward : enabled) {
            bytePool.add(rewardItemStack(reward));
        }
        bytePool.add(new ItemStack(Material.PAPER));
        bytePool.add(new ItemStack(Material.BOOK));
        bytePool.add(new ItemStack(Material.MAP));
        bytePool.add(new ItemStack(Material.GLOWSTONE_DUST));
        bytePool.add(new ItemStack(Material.QUARTZ));
        bytePool.add(new ItemStack(Material.EMERALD));
        bytePool.add(new ItemStack(Material.DIAMOND));
        bytePool.add(new ItemStack(Material.GOLD_NUGGET));
        bytePool.add(new ItemStack(Material.IRON_NUGGET));
    }

    private void buildPods(Player player, List<RewardDefinition> enabled, CrateOpeningSession session) {
        List<RewardDefinition> others = new ArrayList<>();
        for (RewardDefinition reward : enabled) {
            if (!reward.id().equals(rolledReward.id())) {
                others.add(reward);
            }
        }
        Collections.shuffle(others, random(session));
        rewardPodIndex = podCount / 2;
        List<RewardDefinition> assigned = new ArrayList<>(podCount);
        for (int index = 0; index < podCount; index++) {
            assigned.add(null);
        }
        assigned.set(rewardPodIndex, rolledReward);
        int fill = 0;
        for (int index = 0; index < podCount; index++) {
            if (assigned.get(index) != null) {
                continue;
            }
            if (fill < others.size()) {
                assigned.set(index, others.get(fill++));
            } else if (!others.isEmpty()) {
                assigned.set(index, others.get(index % others.size()));
            } else {
                assigned.set(index, rolledReward);
            }
        }
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        for (int index = 0; index < podCount; index++) {
            Location base = podLocation(index, podCount);
            Pod pod = new Pod(index, assigned.get(index), base);
            pod.mob = spawnMob(world, base, session.sessionId());
            configureMob(pod.mob);
            pod.label = world.spawn(labelLocation(base), TextDisplay.class, entity -> {
                entity.text(messageService.component(player.getUniqueId(), "mob-pick-hidden"));
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setSeeThrough(false);
                entity.setShadowed(true);
                entity.setDefaultBackground(false);
                entity.setPersistent(false);
                entity.setViewRange(64.0f);
                entity.setBrightness(new Display.Brightness(15, 15));
                entity.setLineWidth(220);
            });
            pod.interaction = world.spawn(hitboxCenter(base), Interaction.class, entity -> {
                entity.setInteractionWidth(HITBOX_WIDTH);
                entity.setInteractionHeight(HITBOX_HEIGHT);
                entity.setResponsive(true);
                entity.setPersistent(false);
                entity.getPersistentDataContainer().set(
                        SoulCratesKeys.mobPickSession(plugin),
                        PersistentDataType.STRING,
                        session.sessionId().toString()
                );
            });
            pods.add(pod);
        }
    }

    private LivingEntity spawnMob(World world, Location base, UUID sessionId) {
        Entity spawned = world.spawnEntity(base, entityType);
        LivingEntity living;
        if (spawned instanceof LivingEntity entity) {
            living = entity;
        } else {
            spawned.remove();
            living = (LivingEntity) world.spawnEntity(base, EntityType.ALLAY);
        }
        living.getPersistentDataContainer().set(
                SoulCratesKeys.mobPickSession(plugin),
                PersistentDataType.STRING,
                sessionId.toString()
        );
        return living;
    }

    private void configureMob(LivingEntity mob) {
        mob.setAI(false);
        mob.setCollidable(false);
        mob.setGravity(false);
        mob.setSilent(true);
        mob.setInvulnerable(true);
        mob.setCanPickupItems(false);
        mob.setRemoveWhenFarAway(false);
        mob.setPersistent(false);
        mob.setMaximumNoDamageTicks(0);
        mob.setNoDamageTicks(0);
        if (mob instanceof Mob creature) {
            creature.setAware(false);
        }
    }

    private void updatePodMotion(Player player) {
        if (stage != Stage.PICKING && stage != Stage.REVEAL_OTHERS) {
            return;
        }
        Location eye = player == null ? null : player.getEyeLocation();
        for (Pod pod : pods) {
            if (pod.index == winnerIndex) {
                continue;
            }
            Motion motion = resolveMotion(pod);
            Location anchor = pod.base.clone().add(motion.swayX(), motion.lift(), motion.swayZ());
            if (pod.mob != null && !pod.mob.isDead()) {
                if (eye != null) {
                    Vector direction = eye.toVector().subtract(anchor.toVector());
                    if (direction.lengthSquared() > 0.0001) {
                        anchor.setDirection(direction);
                    }
                }
                pod.mob.teleport(anchor);
                if (pod.interaction != null && !pod.interaction.isDead()) {
                    pod.interaction.teleport(hitboxCenter(anchor));
                }
            } else if (pod.rewardItem != null && !pod.rewardItem.isDead()) {
                pod.rewardItem.teleport(anchor.clone().add(0.0, 0.12, 0.0));
            }
            if (pod.label != null && !pod.label.isDead()) {
                pod.label.teleport(labelLocation(anchor));
            }
        }
    }

    private Motion resolveMotion(Pod pod) {
        double bounceLift = 0.0;
        if (pod.bounceTicks > 0) {
            double progress = 1.0 - (pod.bounceTicks / 12.0);
            bounceLift = Math.sin(progress * Math.PI) * pod.bounceHeight;
            pod.bounceTicks--;
        }
        double phase = pod.index * 1.11 + stageTicks * FLOAT_SPEED;
        double floatLift = FLOAT_BASE + Math.sin(phase) * FLOAT_AMPLITUDE;
        double swayX = Math.sin(phase * 0.71 + pod.index * 0.55) * SWAY_AMPLITUDE;
        double swayZ = Math.cos(phase * 0.83 + pod.index * 0.47) * SWAY_AMPLITUDE;
        return new Motion(floatLift + bounceLift, swayX, swayZ);
    }

    private void hidePhysicalCrateBlock(CrateOpeningSession session) {
        if (session.context().instanceId() == null) {
            return;
        }
        Location crateLocation = session.context().crateLocation();
        if (crateLocation == null || crateLocation.getWorld() == null) {
            return;
        }
        PluginSchedulers.runAt(plugin, crateLocation, () -> {
            Block block = crateLocation.getBlock();
            if (!block.getType().isAir()) {
                block.setType(Material.AIR, false);
            }
        });
    }

    private void startBossBar(Player player) {
        bossBar = BossBar.bossBar(
                bossBarTitle(player, pickTicksRemaining),
                1.0f,
                BossBar.Color.RED,
                BossBar.Overlay.PROGRESS
        );
        player.showBossBar(bossBar);
    }

    private void updateBossBar(Player player) {
        if (bossBar == null) {
            return;
        }
        float progress = Math.max(0.0f, (float) pickTicksRemaining / PICK_TIMEOUT_TICKS);
        bossBar.progress(progress);
        bossBar.name(bossBarTitle(player, pickTicksRemaining));
    }

    private net.kyori.adventure.text.Component bossBarTitle(Player player, int ticksRemaining) {
        int seconds = Math.max(0, (int) Math.ceil(ticksRemaining / 20.0));
        return messageService.component(
                player.getUniqueId(),
                "mob-pick-bossbar",
                Placeholder.parsed("seconds", Integer.toString(seconds))
        );
    }

    private void clearBossBar(Player player) {
        if (bossBar == null) {
            return;
        }
        player.hideBossBar(bossBar);
        bossBar = null;
    }

    private void confinePlayer(Player player) {
        if (!shouldConfinePlayer() || center == null) {
            return;
        }
        Location current = player.getLocation();
        if (!isOutsideBoundary(current)) {
            return;
        }
        Location clamped = clampLocation(current);
        clamped.setYaw(current.getYaw());
        clamped.setPitch(current.getPitch());
        player.teleport(clamped);
    }

    private static Location hitboxCenter(Location anchor) {
        return anchor.clone().add(0.0, HITBOX_HEIGHT * 0.45, 0.0);
    }

    private static Location labelLocation(Location anchor) {
        return anchor.clone().add(0.0, LABEL_GAP + 0.35, 0.0);
    }

    private void triggerBounce(int index, double height) {
        if (index < 0 || index >= pods.size()) {
            return;
        }
        Pod pod = pods.get(index);
        pod.bounceTicks = 12;
        pod.bounceHeight = height;
    }

    private static Random random(CrateOpeningSession session) {
        long seed = session.sessionId().getMostSignificantBits()
                ^ session.sessionId().getLeastSignificantBits()
                ^ session.context().playerId().getMostSignificantBits();
        return new Random(seed);
    }

    private static Location podCenter(Pod pod) {
        return podAnchor(pod);
    }

    private static Location podAnchor(Pod pod) {
        if (pod.mob != null && !pod.mob.isDead()) {
            return pod.mob.getLocation();
        }
        if (pod.rewardItem != null && !pod.rewardItem.isDead()) {
            return pod.rewardItem.getLocation();
        }
        return pod.base;
    }

    private Location podLocation(int index, int count) {
        double angle = (Math.PI * 2.0 * index / count) - (Math.PI / 2.0);
        double x = Math.cos(angle) * RADIUS;
        double z = Math.sin(angle) * RADIUS;
        return center.clone().add(x, 0.0, z);
    }

    private Location resolveGroundCenter(Player player, CrateOpeningSession session) {
        Location anchor = session.context().crateLocation();
        if (anchor != null && anchor.getWorld() != null) {
            Block ground = anchor.getBlock();
            if (ground.isPassable()) {
                ground = ground.getRelative(0, -1, 0);
            }
            return ground.getLocation().add(0.5, 1.0, 0.5);
        }
        Location feet = player.getLocation();
        World world = feet.getWorld();
        if (world == null) {
            return feet.clone();
        }
        Block ground = feet.getBlock();
        if (ground.isPassable()) {
            ground = ground.getRelative(0, -1, 0);
        }
        return ground.getLocation().add(0.5, 1.0, 0.5);
    }

    private ItemStack rewardItemStack(RewardDefinition reward) {
        Material material = Material.matchMaterial(reward.material());
        if (material == null || material.isAir()) {
            material = Material.CHEST;
        }
        ItemStack stack = new ItemStack(material);
        if (reward.customModelData() > 0) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(reward.customModelData());
                stack.setItemMeta(meta);
            }
        }
        return stack;
    }

    private org.bukkit.Color rewardGlow(RewardDefinition reward) {
        if (crateDefinition.rarities() != null && reward.rarityId() != null) {
            for (RarityTierSettings tier : crateDefinition.rarities()) {
                if (tier.id != null && tier.id.equalsIgnoreCase(reward.rarityId())) {
                    if (tier.color != null && tier.color.contains("#")) {
                        int hashIndex = tier.color.indexOf('#');
                        String hex = tier.color.substring(hashIndex, Math.min(hashIndex + 7, tier.color.length()));
                        return ParticleEffectUtil.parseBukkitColor(hex, org.bukkit.Color.WHITE);
                    }
                }
            }
        }
        return org.bukkit.Color.fromRGB(255, 120, 85);
    }

    private static EntityType resolveEntityType(String raw) {
        if (raw == null || raw.isBlank()) {
            return EntityType.ALLAY;
        }
        EntityType type = EntityType.fromName(raw.trim().toUpperCase(Locale.ROOT));
        if (type == null || !type.isAlive()) {
            return EntityType.ALLAY;
        }
        return type;
    }

    private static boolean matchesPodEntity(Pod pod, Entity entity) {
        if (entity == null) {
            return false;
        }
        return (pod.mob != null && !pod.mob.isDead() && entity.getUniqueId().equals(pod.mob.getUniqueId()))
                || (pod.interaction != null && !pod.interaction.isDead() && entity.getUniqueId().equals(pod.interaction.getUniqueId()));
    }

    private void removePrizeDisplay() {
        for (FallingByte byteDrop : fallingBytes) {
            if (byteDrop.display() != null && !byteDrop.display().isDead()) {
                byteDrop.display().remove();
            }
        }
        fallingBytes.clear();
        if (prizeLabel != null && !prizeLabel.isDead()) {
            prizeLabel.remove();
        }
        if (prizeItem != null && !prizeItem.isDead()) {
            prizeItem.remove();
        }
        prizeLabel = null;
        prizeItem = null;
    }

    private static void applyItemScaleRotated(ItemDisplay display, float scale, float angleRadians) {
        float half = scale * 0.5f;
        display.setTransformation(new Transformation(
                new Vector3f(-half, 0.0f, -half),
                new AxisAngle4f(angleRadians, 0.0f, 1.0f, 0.0f),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(angleRadians * 0.65f, 1.0f, 0.0f, 0.0f)
        ));
    }

    private static void applyItemScale(ItemDisplay display, float scale) {
        float half = scale * 0.5f;
        display.setTransformation(new Transformation(
                new Vector3f(-half, 0.0f, -half),
                new AxisAngle4f(0.0f, 0.0f, 1.0f, 0.0f),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(0.0f, 0.0f, 1.0f, 0.0f)
        ));
    }

    private static void removePod(Pod pod) {
        if (pod.interaction != null && !pod.interaction.isDead()) {
            pod.interaction.remove();
        }
        if (pod.label != null && !pod.label.isDead()) {
            pod.label.remove();
        }
        if (pod.rewardItem != null && !pod.rewardItem.isDead()) {
            pod.rewardItem.remove();
        }
        if (pod.mob != null && !pod.mob.isDead()) {
            pod.mob.remove();
        }
        pod.interaction = null;
        pod.label = null;
        pod.rewardItem = null;
        pod.mob = null;
    }

    private record Motion(double lift, double swayX, double swayZ) {
    }
}
