package bm.b0b0b0.soulCrates.animation;

import bm.b0b0b0.soulCrates.config.settings.AnimationPhaseProperties;
import bm.b0b0b0.soulCrates.config.settings.AnimationPhaseSettings;
import bm.b0b0b0.soulCrates.config.settings.RarityTierSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.service.reward.BroadcastService;
import bm.b0b0b0.soulCrates.service.reward.RewardDisplayService;
import bm.b0b0b0.soulCrates.session.CrateOpeningSession;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public final class ShulkerPickPhase implements PhaseRunner {

    private static final int POD_COUNT = 6;
    private static final float POD_SCALE = 0.82f;
    private static final float LABEL_GAP = 0.42f;
    private static final double FLOAT_BASE = 0.14;
    private static final double FLOAT_AMPLITUDE = 0.09;
    private static final double SWAY_AMPLITUDE = 0.04;
    private static final double FLOAT_SPEED = 0.1;
    private static final double PICK_BOUNCE_HEIGHT = 0.78;
    private static final double WINNER_BOUNCE_HEIGHT = 0.62;
    private static final int PICK_TIMEOUT_TICKS = 600;
    private static final double PICK_RANGE_SQ = 7.0 * 7.0;
    private static final int WINNER_BOUNCE_TICKS = 28;
    private static final int REVEAL_WIN_TICKS = 35;
    private static final int REVEAL_OTHER_TICKS = 18;
    private static final int FINISH_HOLD_TICKS = 20;

    private static final Map<UUID, ShulkerPickPhase> ACTIVE = new ConcurrentHashMap<>();

    private enum Stage {
        PICKING,
        WINNER_BOUNCE,
        REVEAL_WIN,
        REVEAL_OTHERS,
        FINISHED
    }

    private static final class Pod {
        private final int index;
        private RewardDefinition reward;
        private final Location base;
        private BlockDisplay block;
        private TextDisplay label;
        private Interaction interaction;
        private int bounceTicks;
        private double bounceHeight;

        private Pod(int index, RewardDefinition reward, Location base) {
            this.index = index;
            this.reward = reward;
            this.base = base;
        }
    }

    private final MessageService messageService;
    private final BroadcastService broadcastService;
    private final CrateDefinition crateDefinition;
    private final RewardDefinition rolledReward;
    private final boolean confinePlayer;
    private final double confineRadius;

    private Stage stage = Stage.PICKING;
    private int stageTicks;
    private int pickTicksRemaining = PICK_TIMEOUT_TICKS;
    private int rewardPodIndex;
    private int winnerIndex;
    private int revealOtherIndex;
    private boolean revealOthersDone;
    private boolean revealSent;
    private boolean picked;
    private Location center;
    private CrateOpeningSession session;
    private BossBar bossBar;
    private boolean restoreCollidable = true;
    private final List<Pod> pods = new ArrayList<>();

    public ShulkerPickPhase(
            MessageService messageService,
            BroadcastService broadcastService,
            CrateDefinition crateDefinition,
            RewardDefinition rolledReward,
            AnimationPhaseSettings settings
    ) {
        this.messageService = messageService;
        this.broadcastService = broadcastService;
        this.crateDefinition = crateDefinition;
        this.rolledReward = rolledReward;
        AnimationPhaseProperties properties = settings.properties == null ? new AnimationPhaseProperties() : settings.properties;
        this.confinePlayer = properties.confinePlayer;
        this.confineRadius = Math.max(0.5, properties.confineRadius);
    }

    public static Optional<ShulkerPickPhase> activePhase(UUID playerId) {
        return Optional.ofNullable(ACTIVE.get(playerId));
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
        revealOtherIndex = 0;
        revealOthersDone = false;
        revealSent = false;
        picked = false;
        center = PickArenaLayout.resolvePlayerCenter(player);
        session.hideOpeningCrateBlock();
        List<RewardDefinition> enabled = crateDefinition.rewards().stream().filter(RewardDefinition::enabled).toList();
        if (enabled.isEmpty()) {
            enabled = List.of(rolledReward);
        }
        buildPods(player, enabled, session);
        startBossBar(player);
        restoreCollidable = player.isCollidable();
        if (confinePlayer) {
            player.setCollidable(false);
        }
        ACTIVE.put(player.getUniqueId(), this);
        World world = center.getWorld();
        if (world != null) {
            world.playSound(center, Sound.BLOCK_SHULKER_BOX_OPEN, 0.85f, 0.85f);
            world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.1f);
        }
        messageService.send(player.getUniqueId(), "shulker-pick-prompt");
    }

    @Override
    public void tick(Player player, CrateOpeningSession session) {
        if (center == null || center.getWorld() == null || pods.isEmpty()) {
            stage = Stage.FINISHED;
            return;
        }
        stageTicks++;
        if (confinePlayer) {
            confinePlayer(player);
        }
        updatePodMotion();
        switch (stage) {
            case PICKING -> tickPicking(player);
            case WINNER_BOUNCE -> tickWinnerBounce(player);
            case REVEAL_WIN -> tickRevealWin(player);
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
                candidate -> candidate instanceof Interaction || candidate instanceof BlockDisplay
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
        if (player.getLocation().distanceSquared(podCenter(pod.base)) > PICK_RANGE_SQ) {
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
            Location center = podCenter(pod.base);
            if (player.getLocation().distanceSquared(center) > PICK_RANGE_SQ) {
                continue;
            }
            Vector toPod = center.toVector().subtract(player.getEyeLocation().toVector());
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

    private static boolean matchesPodEntity(Pod pod, Entity entity) {
        if (pod.interaction != null && !pod.interaction.isDead() && pod.interaction.getUniqueId().equals(entity.getUniqueId())) {
            return true;
        }
        return pod.block != null && !pod.block.isDead() && pod.block.getUniqueId().equals(entity.getUniqueId());
    }

    private static Location podCenter(Location base) {
        return blockHitboxCenter(base);
    }

    private static Location blockHitboxCenter(Location blockAnchor) {
        return blockAnchor.clone().add(0.0, POD_SCALE * 0.5, 0.0);
    }

    public boolean shouldConfinePlayer() {
        return confinePlayer && (stage == Stage.PICKING || stage == Stage.WINNER_BOUNCE);
    }

    public boolean isOutsideBoundary(Location location) {
        return PickArenaLayout.isOutsideBoundary(center, location, confineRadius);
    }

    public Location clampLocation(Location location) {
        return PickArenaLayout.clampLocation(center, location, confineRadius);
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
                "shulker-pick-timeout",
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
        clearBossBar(player);
        if (pickedIndex != rewardPodIndex) {
            Pod pickedPod = pods.get(pickedIndex);
            Pod rewardPod = pods.get(rewardPodIndex);
            RewardDefinition decoy = pickedPod.reward;
            pickedPod.reward = rolledReward;
            rewardPod.reward = decoy;
        }
        winnerIndex = pickedIndex;
        stage = Stage.WINNER_BOUNCE;
        stageTicks = 0;
        triggerBounce(winnerIndex, PICK_BOUNCE_HEIGHT);
        applyPodMotionImmediate();
        Pod pickedPod = pods.get(pickedIndex);
        if (pickedPod.block != null && !pickedPod.block.isDead()) {
            pickedPod.block.setGlowing(true);
        }
        World world = center.getWorld();
        if (world != null) {
            world.playSound(pods.get(pickedIndex).base, Sound.BLOCK_SHULKER_BOX_OPEN, 0.95f, 1.05f);
        }
    }

    private void tickWinnerBounce(Player player) {
        if (stageTicks == 1 || stageTicks % 11 == 0) {
            triggerBounce(winnerIndex, WINNER_BOUNCE_HEIGHT);
        }
        if (stageTicks >= WINNER_BOUNCE_TICKS) {
            stage = Stage.REVEAL_WIN;
            stageTicks = 0;
            revealWinner(player);
        }
    }

    private void tickRevealWin(Player player) {
        if (stageTicks >= REVEAL_WIN_TICKS) {
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
                stageTicks = 0;
            }
            return;
        }
        if (stageTicks >= FINISH_HOLD_TICKS) {
            stage = Stage.FINISHED;
        }
    }

    private void revealWinner(Player player) {
        Pod pod = pods.get(winnerIndex);
        if (pod.block != null && !pod.block.isDead()) {
            pod.block.setBlock(rewardBlock(pod.reward));
            pod.block.setGlowing(true);
            pod.block.setGlowColorOverride(rewardGlow(pod.reward));
        }
        if (pod.label != null && !pod.label.isDead()) {
            pod.label.text(messageService.component(
                    player.getUniqueId(),
                    "shulker-pick-won",
                    Placeholder.component(
                            "reward",
                            RewardDisplayService.displayName(messageService, player.getUniqueId(), crateDefinition.id(), pod.reward)
                    )
            ));
        }
        World world = center.getWorld();
        if (world != null) {
            world.playSound(pod.base, Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.2f);
            world.playSound(pod.base, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.85f, 1.0f);
            ParticleEffectUtil.spawn(world, pod.base.clone().add(0.0, 0.4, 0.0), Particle.TOTEM_OF_UNDYING, null, 16, 0.2, 0.35, 0.2, 0.02);
        }
        if (!revealSent && broadcastService != null) {
            broadcastService.dramaticWinBroadcast(player, crateDefinition, rolledReward);
            revealSent = true;
        }
        messageService.send(
                player.getUniqueId(),
                "shulker-pick-win-chat",
                Placeholder.component(
                        "reward",
                        RewardDisplayService.displayName(messageService, player.getUniqueId(), crateDefinition.id(), rolledReward)
                ),
                messageService.placeholder("crate", crateDefinition.displayName())
        );
    }

    private void revealOther(Player player, Pod pod) {
        if (pod.block != null && !pod.block.isDead()) {
            pod.block.setBlock(rewardBlock(pod.reward));
            pod.block.setGlowing(false);
        }
        if (pod.label != null && !pod.label.isDead()) {
            pod.label.text(messageService.component(
                    player.getUniqueId(),
                    "shulker-pick-lost",
                    Placeholder.component(
                            "reward",
                            RewardDisplayService.displayName(messageService, player.getUniqueId(), crateDefinition.id(), pod.reward)
                    )
            ));
        }
        World world = center.getWorld();
        if (world != null) {
            world.playSound(pod.base, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.55f, 1.35f);
            ParticleEffectUtil.spawn(world, pod.base.clone().add(0.0, 0.35, 0.0), Particle.ENCHANT, null, 12, 0.15, 0.2, 0.15, 0.4);
            ParticleEffectUtil.spawn(world, pod.base.clone().add(0.0, 0.2, 0.0), Particle.SMOKE, null, 4, 0.08, 0.08, 0.08, 0.01);
        }
    }

    private void buildPods(Player player, List<RewardDefinition> enabled, CrateOpeningSession session) {
        int podCount = POD_COUNT;
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
        BlockData shulker = Material.SHULKER_BOX.createBlockData();
        for (int index = 0; index < podCount; index++) {
            Location base = podLocation(index, podCount);
            Pod pod = new Pod(index, assigned.get(index), base);
            pod.block = world.spawn(base, BlockDisplay.class, entity -> {
                entity.setBlock(shulker);
                entity.setBillboard(Display.Billboard.FIXED);
                entity.setPersistent(false);
                entity.setViewRange(64.0f);
                entity.setBrightness(new Display.Brightness(15, 15));
                applyScale(entity, POD_SCALE);
            });
            Location labelLocation = labelLocation(base);
            pod.label = world.spawn(labelLocation, TextDisplay.class, entity -> {
                entity.text(messageService.component(player.getUniqueId(), "shulker-pick-hidden"));
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setSeeThrough(false);
                entity.setShadowed(true);
                entity.setDefaultBackground(false);
                entity.setPersistent(false);
                entity.setViewRange(64.0f);
                entity.setBrightness(new Display.Brightness(15, 15));
                entity.setLineWidth(220);
            });
            pod.interaction = world.spawn(blockHitboxCenter(base), Interaction.class, entity -> {
                entity.setInteractionWidth(POD_SCALE * 1.2f);
                entity.setInteractionHeight(POD_SCALE * 1.15f);
                entity.setResponsive(true);
                entity.setPersistent(false);
            });
            pods.add(pod);
        }
    }
    private void startBossBar(Player player) {
        bossBar = BossBar.bossBar(
                bossBarTitle(player, pickTicksRemaining),
                1.0f,
                BossBar.Color.YELLOW,
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
                "shulker-pick-bossbar",
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

    private void triggerBounce(int index, double height) {
        if (index < 0 || index >= pods.size()) {
            return;
        }
        Pod pod = pods.get(index);
        pod.bounceTicks = 12;
        pod.bounceHeight = height;
    }

    private void applyPodMotionImmediate() {
        updatePodMotion();
    }

    private void updatePodMotion() {
        for (Pod pod : pods) {
            if (pod.block == null || pod.block.isDead()) {
                continue;
            }
            Motion motion = resolveMotion(pod);
            Location blockAnchor = pod.base.clone().add(motion.swayX(), motion.lift(), motion.swayZ());
            Location hitboxCenter = blockAnchor.clone().add(0.0, POD_SCALE * 0.5, 0.0);
            pod.block.teleport(blockAnchor);
            if (pod.label != null && !pod.label.isDead()) {
                pod.label.teleport(labelLocation(blockAnchor));
            }
            if (pod.interaction != null && !pod.interaction.isDead()) {
                pod.interaction.teleport(hitboxCenter);
            }
            boolean highlight = (stage == Stage.WINNER_BOUNCE || stage.ordinal() >= Stage.REVEAL_WIN.ordinal())
                    && pod.index == winnerIndex;
            pod.block.setGlowing(highlight);
        }
    }

    private Motion resolveMotion(Pod pod) {
        double bounceLift = 0.0;
        if (pod.bounceTicks > 0) {
            double progress = 1.0 - (pod.bounceTicks / 12.0);
            bounceLift = Math.sin(progress * Math.PI) * pod.bounceHeight;
            pod.bounceTicks--;
        }
        if (stage == Stage.PICKING || (stage == Stage.WINNER_BOUNCE && pod.index != winnerIndex)) {
            double phase = pod.index * 1.047 + stageTicks * FLOAT_SPEED;
            double floatLift = FLOAT_BASE + Math.sin(phase) * FLOAT_AMPLITUDE;
            double swayX = Math.sin(phase * 0.73 + pod.index * 0.6) * SWAY_AMPLITUDE;
            double swayZ = Math.cos(phase * 0.81 + pod.index * 0.45) * SWAY_AMPLITUDE;
            return new Motion(floatLift + bounceLift, swayX, swayZ);
        }
        if (stage == Stage.WINNER_BOUNCE && pod.index == winnerIndex) {
            return new Motion(bounceLift, 0.0, 0.0);
        }
        return new Motion(bounceLift, 0.0, 0.0);
    }

    private static Location labelLocation(Location blockAnchor) {
        return blockAnchor.clone().add(0.0, POD_SCALE + LABEL_GAP, 0.0);
    }

    private record Motion(double lift, double swayX, double swayZ) {
    }

    private Location podLocation(int index, int count) {
        return PickArenaLayout.podLocation(center, index, count);
    }

    private BlockData rewardBlock(RewardDefinition reward) {
        Material material = Material.matchMaterial(reward.material());
        if (material != null && material.isBlock() && !material.isAir()) {
            return material.createBlockData();
        }
        return Material.DIAMOND_BLOCK.createBlockData();
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
        return org.bukkit.Color.fromRGB(85, 255, 170);
    }

    private static Random random(CrateOpeningSession session) {
        long seed = session.sessionId().getMostSignificantBits()
                ^ session.sessionId().getLeastSignificantBits()
                ^ session.context().playerId().getMostSignificantBits();
        return new Random(seed);
    }

    private static void applyScale(BlockDisplay display, float scale) {
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
        if (pod.block != null && !pod.block.isDead()) {
            pod.block.remove();
        }
        pod.interaction = null;
        pod.label = null;
        pod.block = null;
    }
}
