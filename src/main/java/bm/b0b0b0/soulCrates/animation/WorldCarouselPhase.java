package bm.b0b0b0.soulCrates.animation;

import bm.b0b0b0.soulCrates.config.settings.AnimationDisplaySettings;
import bm.b0b0b0.soulCrates.config.settings.AnimationPhaseProperties;
import bm.b0b0b0.soulCrates.config.settings.AnimationPhaseSettings;
import bm.b0b0b0.soulCrates.config.settings.RarityTierSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.service.reward.BroadcastService;
import bm.b0b0b0.soulCrates.service.reward.RewardDisplayService;
import bm.b0b0b0.soulCrates.session.CrateOpeningSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public final class WorldCarouselPhase implements PhaseRunner {

    private static final int RING_SLOTS = 8;
    private static final double RING_RADIUS = 1.55;
    private static final double LABEL_OFFSET_Y = 0.42;
    private static final float BLOCK_SCALE = 0.4f;
    private static final float POINTER_SCALE = 0.52f;
    private static final float WINNER_SCALE = 0.64f;
    private static final int LOCK_DURATION = 30;
    private static final int COLLAPSE_DURATION = 40;
    private static final double POINTER_ANGLE = Math.PI / 2.0;
    private static final double CENTER_TICK_EPS = 0.048;
    private static final double CENTER_HIGHLIGHT_EPS = 0.11;
    private static final double POINTER_RING_GAP = 0.55;
    private static final double POINTER_PLAYER_PULL = 0.28;
    private static final float DIAMOND_TWIST = (float) (Math.PI / 4.0);
    private static final float DIAMOND_LEAN = (float) (Math.PI / 10.0);

    private enum Stage {
        SPINNING,
        LOCKED,
        COLLAPSING,
        FINISHED
    }

    private enum SuspenseKind {
        SURGE,
        HOLD
    }

    private static final class SuspenseBeat {
        private final double triggerProgress;
        private final SuspenseKind kind;
        private final int durationTicks;
        private final double velocityFloor;
        private final double velocityCap;
        private boolean triggered;

        private SuspenseBeat(
                double triggerProgress,
                SuspenseKind kind,
                int durationTicks,
                double velocityFloor,
                double velocityCap
        ) {
            this.triggerProgress = triggerProgress;
            this.kind = kind;
            this.durationTicks = durationTicks;
            this.velocityFloor = velocityFloor;
            this.velocityCap = velocityCap;
        }
    }

    private final MessageService messageService;
    private final BroadcastService broadcastService;
    private final CrateDefinition crateDefinition;
    private final RewardDefinition rolledReward;
    private final AnimationPhaseSettings settings;
    private final int spinDuration;

    private Stage stage = Stage.SPINNING;
    private int stageTicks;
    private int spinTicksRemaining;
    private boolean locked;
    private boolean revealSent;
    private Location anchor;
    private Location winnerAnchor;
    private Vector right;
    private Vector up;
    private Vector towardPlayer;
    private double ringAngle;
    private double angularVelocity;
    private double targetRingAngle;
    private int lockedPointerSlot;
    private int lastPointerSlot = -1;
    private int lockTickCounter;
    private float collapseProgress;
    private final List<RingSlot> slots = new ArrayList<>();
    private TextDisplay pointerDisplay;
    private List<RewardDefinition> pool;
    private List<SuspenseBeat> suspenseBeats = List.of();
    private SuspenseBeat activeBeat;
    private int activeBeatTicks;
    private double velocityFloorOverride = -1.0;
    private double velocityCapOverride = -1.0;

    public WorldCarouselPhase(
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
        this.settings = settings;
        this.spinDuration = Math.max(120, settings.durationTicks);
    }

    @Override
    public OpeningPhaseKind kind() {
        return OpeningPhaseKind.SECOND;
    }

    @Override
    public void load(Player player, CrateOpeningSession session) {
        player.closeInventory();
        stage = Stage.SPINNING;
        stageTicks = 0;
        spinTicksRemaining = spinDuration;
        locked = false;
        revealSent = false;
        ringAngle = 0.0;
        angularVelocity = 0.055;
        collapseProgress = 0.0f;
        lockTickCounter = 0;
        lastPointerSlot = -1;
        pool = crateDefinition.rewards().stream().filter(RewardDefinition::enabled).toList();
        if (pool.isEmpty()) {
            pool = List.of(rolledReward);
        }
        anchor = resolveAnchor(player, session);
        resolveAxes(player);
        targetRingAngle = resolveTargetRingAngle();
        suspenseBeats = buildSuspenseSchedule(player, session);
        activeBeat = null;
        activeBeatTicks = 0;
        velocityFloorOverride = -1.0;
        velocityCapOverride = -1.0;
        spawnRing(player);
        spawnPointer(player);
        refreshRing(player);
        World world = anchor.getWorld();
        if (world != null) {
            world.playSound(anchor, Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.35f);
        }
    }

    @Override
    public void tick(Player player, CrateOpeningSession session) {
        if (anchor == null || anchor.getWorld() == null || pool.isEmpty()) {
            finishIfNeeded();
            return;
        }
        switch (stage) {
            case SPINNING -> tickSpinning(player);
            case LOCKED -> tickLocked(player);
            case COLLAPSING -> tickCollapsing(player);
            case FINISHED -> {
            }
        }
        updatePointer();
    }

    @Override
    public void unload(Player player, CrateOpeningSession session) {
        removeDisplays();
    }

    @Override
    public boolean finished() {
        return stage == Stage.FINISHED;
    }

    private void tickSpinning(Player player) {
        ringAngle += angularVelocity;
        int nearest = nearestSlot();
        double centerError = slotCenterError(nearest);
        if (centerError < CENTER_TICK_EPS && lastPointerSlot != nearest) {
            lastPointerSlot = nearest;
            playSpinTick();
        } else if (centerError > CENTER_HIGHLIGHT_EPS * 1.35) {
            lastPointerSlot = -1;
        }
        angularVelocity = resolveAngularVelocity();
        updateSuspense(player);
        if (velocityCapOverride >= 0.0) {
            angularVelocity = Math.min(angularVelocity, velocityCapOverride);
        }
        if (velocityFloorOverride >= 0.0) {
            angularVelocity = Math.max(angularVelocity, velocityFloorOverride);
        }
        double remaining = targetRingAngle - ringAngle;
        if (remaining > 0.0 && spinTicksRemaining <= (int) (spinDuration * 0.4)) {
            double ticksLeft = Math.max(1.0, spinTicksRemaining);
            double desired = remaining / ticksLeft;
            angularVelocity = Math.min(angularVelocity, Math.max(0.0035, desired * 0.9));
        }
        refreshRing(player);
        spawnPointerTrail(anchor.getWorld());
        spinTicksRemaining--;
        if (ringAngle >= targetRingAngle - 0.0005 && spinTicksRemaining <= 0) {
            ringAngle = targetRingAngle;
            lockWinner(player);
            stage = Stage.LOCKED;
            stageTicks = LOCK_DURATION;
        } else if (spinTicksRemaining <= 0 && ringAngle < targetRingAngle - 0.0005) {
            angularVelocity = Math.max(0.004, (targetRingAngle - ringAngle) / 24.0);
        }
    }

    private void tickLocked(Player player) {
        refreshRing(player);
        spawnPointerTrail(anchor.getWorld());
        lockTickCounter++;
        if (lockTickCounter % 7 == 0) {
            playLockTick();
        }
        stageTicks--;
        if (stageTicks <= 0) {
            stage = Stage.COLLAPSING;
            stageTicks = COLLAPSE_DURATION;
            collapseProgress = 0.0f;
            winnerAnchor = slotLocation(lockedPointerSlot, WINNER_SCALE);
            World world = anchor.getWorld();
            if (world != null) {
                world.playSound(winnerAnchor, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
            }
        }
    }

    private void tickCollapsing(Player player) {
        collapseProgress = Math.min(1.0f, collapseProgress + 1.0f / COLLAPSE_DURATION);
        Location target = winnerAnchor == null ? anchor : winnerAnchor;
        for (int index = 0; index < slots.size(); index++) {
            RingSlot slot = slots.get(index);
            if (slot.block == null || slot.block.isDead()) {
                continue;
            }
            Location start = slotLocation(index, index == lockedPointerSlot ? WINNER_SCALE : BLOCK_SCALE);
            float startScale = index == lockedPointerSlot ? WINNER_SCALE : BLOCK_SCALE;
            if (index == lockedPointerSlot) {
                float scale = lerp(startScale, 0.78f, collapseProgress);
                applyBlockTransform(slot.block, scale, index);
                slot.block.teleport(lerpLocation(start, target, collapseProgress));
                if (slot.label != null && !slot.label.isDead()) {
                    slot.label.teleport(lerpLocation(start.clone().add(0.0, LABEL_OFFSET_Y, 0.0), target.clone().add(0.0, LABEL_OFFSET_Y, 0.0), collapseProgress));
                }
            } else {
                float scale = lerp(startScale, 0.02f, collapseProgress);
                applyBlockTransform(slot.block, scale, index);
                slot.block.teleport(lerpLocation(start, target, collapseProgress));
                if (slot.label != null && !slot.label.isDead()) {
                    if (collapseProgress > 0.35f) {
                        slot.label.text(Component.space());
                    }
                    slot.label.teleport(lerpLocation(start.clone().add(0.0, LABEL_OFFSET_Y, 0.0), target, collapseProgress));
                }
            }
        }
        if (pointerDisplay != null && !pointerDisplay.isDead()) {
            pointerDisplay.teleport(pointerLocation().add(0.0, lerp(0.0, -0.35, collapseProgress), 0.0));
        }
        stageTicks--;
        if (collapseProgress >= 1.0f || stageTicks <= 0) {
            playConfettiBurst(target);
            removeDisplays();
            stage = Stage.FINISHED;
        }
    }

    private void finishIfNeeded() {
        if (stage != Stage.FINISHED) {
            stage = Stage.FINISHED;
        }
    }

    private double slotStepAngle() {
        return (Math.PI * 2.0) / RING_SLOTS;
    }

    private double resolveTargetRingAngle() {
        int winnerIndex = indexOfReward(rolledReward);
        if (winnerIndex < 0) {
            winnerIndex = 0;
        }
        double slotStep = slotStepAngle();
        int minStep = (int) Math.ceil(ringAngle / slotStep) + 28;
        for (int step = minStep; step < minStep + pool.size() * RING_SLOTS * 8; step++) {
            double candidate = step * slotStep;
            if (rewardIndexUnderPointerAt(candidate) == winnerIndex) {
                return candidate;
            }
        }
        return (minStep + winnerIndex) * slotStep;
    }

    private int rewardIndexUnderPointerAt(double angle) {
        double step = slotStepAngle();
        int slot = Math.floorMod(Math.round(-angle / step), RING_SLOTS);
        return Math.floorMod(slot, pool.size());
    }

    private int pointerSlotAt(double angle) {
        return Math.floorMod(Math.round(-angle / slotStepAngle()), RING_SLOTS);
    }

    private int pointerSlot() {
        if (locked) {
            return lockedPointerSlot;
        }
        int nearest = nearestSlot();
        if (slotCenterError(nearest) <= CENTER_HIGHLIGHT_EPS) {
            return nearest;
        }
        return -1;
    }

    private int nearestSlot() {
        int best = 0;
        double bestError = Double.MAX_VALUE;
        double step = slotStepAngle();
        for (int slot = 0; slot < RING_SLOTS; slot++) {
            double error = slotCenterError(slot, step);
            if (error < bestError) {
                bestError = error;
                best = slot;
            }
        }
        return best;
    }

    private double slotCenterError(int slot) {
        return slotCenterError(slot, slotStepAngle());
    }

    private double slotCenterError(int slot, double step) {
        return angularDistance(ringAngle + slot * step, 0.0);
    }

    private RewardDefinition rewardAtSlot(int slot) {
        return pool.get(Math.floorMod(slot, pool.size()));
    }

    private RewardDefinition rewardUnderPointer() {
        return rewardAtSlot(pointerSlot());
    }

    private static double angularDistance(double angle, double target) {
        return Math.abs(Math.atan2(Math.sin(angle - target), Math.cos(angle - target)));
    }

    private Location resolveAnchor(Player player, CrateOpeningSession session) {
        Location base = crateOpeningAnchor(player, session);
        AnimationDisplaySettings display = crateDefinition.animationDisplay();
        AnimationPhaseProperties properties = settings.properties == null ? new AnimationPhaseProperties() : settings.properties;
        return ParticleEffectUtil.offset(
                base,
                display.rewardItemOffsetX + properties.offsetX,
                properties.offsetY,
                display.rewardItemOffsetZ + properties.offsetZ
        );
    }

    private Location crateOpeningAnchor(Player player, CrateOpeningSession session) {
        AnimationDisplaySettings display = crateDefinition.animationDisplay();
        double configuredHeight = display == null ? 1.5 : display.rewardItemOffsetY;
        double ringHeight = Math.max(configuredHeight, RING_RADIUS + (double) BLOCK_SCALE + 0.2);
        Location raw = session.context().crateLocation();
        if (raw != null && raw.getWorld() != null && isBlockLocation(raw)) {
            return raw.getBlock().getLocation().clone().add(0.5, 1.0 + ringHeight, 0.5);
        }
        if (raw != null && raw.getWorld() != null) {
            return raw.clone().add(0.0, 1.2 + ringHeight, 0.0);
        }
        return player.getLocation().clone().add(0.0, 1.2 + ringHeight, 0.0);
    }

    private static boolean isBlockLocation(Location location) {
        return location.getX() == location.getBlockX()
                && location.getY() == location.getBlockY()
                && location.getZ() == location.getBlockZ();
    }

    private void resolveAxes(Player player) {
        Vector face = player.getEyeLocation().toVector().subtract(anchor.toVector());
        face.setY(0.0);
        if (face.lengthSquared() < 0.0001) {
            face = player.getLocation().getDirection();
            face.setY(0.0);
        }
        if (face.lengthSquared() < 0.0001) {
            face = new Vector(0.0, 0.0, 1.0);
        } else {
            face.normalize();
        }
        right = new Vector(-face.getZ(), 0.0, face.getX()).normalize();
        towardPlayer = new Vector(right.getZ(), 0.0, -right.getX()).normalize();
        up = new Vector(0.0, 1.0, 0.0);
        if (isHorizontalLine()) {
            up = new Vector(0.0, 0.12, 0.0);
        }
    }

    private boolean isHorizontalLine() {
        AnimationPhaseProperties properties = settings.properties == null ? new AnimationPhaseProperties() : settings.properties;
        String alignment = properties.alignment;
        return alignment != null && "HORIZONTAL".equalsIgnoreCase(alignment.trim());
    }

    private void spawnRing(Player player) {
        World world = anchor.getWorld();
        if (world == null) {
            return;
        }
        for (int index = 0; index < RING_SLOTS; index++) {
            int slotIndex = index;
            Location spawnLocation = slotLocation(slotIndex, BLOCK_SCALE);
            BlockDisplay block = world.spawn(spawnLocation, BlockDisplay.class, entity -> {
                entity.setBlock(Material.PURPLE_CONCRETE.createBlockData());
                entity.setBillboard(Display.Billboard.FIXED);
                entity.setPersistent(false);
                entity.setViewRange(80.0f);
                entity.setBrightness(new Display.Brightness(15, 15));
                applyBlockTransform(entity, BLOCK_SCALE, slotIndex);
            });
            Location labelLocation = spawnLocation.clone().add(0.0, LABEL_OFFSET_Y, 0.0);
            TextDisplay label = world.spawn(labelLocation, TextDisplay.class, entity -> {
                entity.text(Component.space());
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setSeeThrough(false);
                entity.setShadowed(true);
                entity.setDefaultBackground(false);
                entity.setPersistent(false);
                entity.setViewRange(80.0f);
                entity.setBrightness(new Display.Brightness(15, 15));
                entity.setLineWidth(220);
            });
            slots.add(new RingSlot(block, label));
        }
    }

    private void spawnPointer(Player player) {
        World world = anchor.getWorld();
        if (world == null) {
            return;
        }
        pointerDisplay = world.spawn(pointerLocation(), TextDisplay.class, entity -> {
            entity.text(messageService.parse(messageService.raw(player.getUniqueId(), "carousel-pointer")));
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setSeeThrough(false);
            entity.setShadowed(true);
            entity.setDefaultBackground(false);
            entity.setPersistent(false);
            entity.setViewRange(80.0f);
            entity.setBrightness(new Display.Brightness(15, 15));
        });
    }

    private Location pointerLocation() {
        Location location = anchor.clone().add(up.clone().multiply(RING_RADIUS + POINTER_RING_GAP));
        if (towardPlayer != null && !isHorizontalLine()) {
            location.add(towardPlayer.clone().multiply(POINTER_PLAYER_PULL));
        }
        return location;
    }

    private void updatePointer() {
        if (pointerDisplay == null || pointerDisplay.isDead()) {
            return;
        }
        pointerDisplay.teleport(pointerLocation());
    }

    private void refreshRing(Player player) {
        int pointer = locked ? lockedPointerSlot : pointerSlot();
        for (int index = 0; index < slots.size(); index++) {
            RingSlot slot = slots.get(index);
            if (slot.block == null || slot.block.isDead()) {
                continue;
            }
            RewardDefinition reward = rewardAtSlot(index);
            boolean underPointer = pointer >= 0 && index == pointer;
            float scale = underPointer ? (locked ? WINNER_SCALE : POINTER_SCALE) : BLOCK_SCALE;
            Location blockLocation = slotLocation(index, scale);
            slot.block.setBlock(rewardBlock(reward));
            slot.block.setGlowColorOverride(rewardColor(reward));
            slot.block.setGlowing(underPointer || isRare(reward));
            applyBlockTransform(slot.block, scale, index);
            slot.block.teleport(blockLocation);
            if (slot.label != null && !slot.label.isDead()) {
                slot.label.text(rewardLabel(player, reward, underPointer));
                slot.label.teleport(blockLocation.clone().add(0.0, LABEL_OFFSET_Y, 0.0));
            }
        }
    }

    private Location slotLocation(int slotIndex, float scaleHint) {
        if (isHorizontalLine()) {
            double offset = (slotIndex - (RING_SLOTS / 2.0)) * 0.72;
            return anchor.clone().add(right.clone().multiply(offset));
        }
        double angle = ringAngle + slotIndex * slotStepAngle() + POINTER_ANGLE;
        Vector offset = right.clone().multiply(Math.cos(angle) * RING_RADIUS)
                .add(up.clone().multiply(Math.sin(angle) * RING_RADIUS));
        return anchor.clone().add(offset);
    }

    private void lockWinner(Player player) {
        locked = true;
        angularVelocity = 0.0;
        lockedPointerSlot = nearestSlot();
        lastPointerSlot = lockedPointerSlot;
        refreshRing(player);
        World world = anchor.getWorld();
        if (world != null) {
            world.playSound(anchor, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            world.playSound(anchor, Sound.ENTITY_PLAYER_LEVELUP, 0.85f, 1.15f);
            world.playSound(anchor, Sound.BLOCK_BEACON_POWER_SELECT, 0.9f, 1.6f);
            org.bukkit.Color burst = rewardColor(rewardUnderPointer());
            ParticleEffectUtil.spawn(world, anchor, Particle.TOTEM_OF_UNDYING, burst, 24, 0.25, 0.25, 0.25, 0.02);
        }
        if (!revealSent && broadcastService != null) {
            broadcastService.dramaticWinBroadcast(player, crateDefinition, rolledReward);
            revealSent = true;
        }
        messageService.send(
                player.getUniqueId(),
                "carousel-reveal-self",
                Placeholder.component(
                        "reward",
                        RewardDisplayService.displayName(messageService, player.getUniqueId(), crateDefinition.id(), rolledReward)
                ),
                messageService.placeholder("crate", crateDefinition.displayName())
        );
    }

    private void playSpinTick() {
        World world = anchor.getWorld();
        if (world == null) {
            return;
        }
        float pitch = (float) Math.min(1.8, 0.65 + angularVelocity * 8.0);
        world.playSound(anchor, Sound.BLOCK_NOTE_BLOCK_HAT, 0.55f, pitch);
    }

    private void playLockTick() {
        World world = anchor.getWorld();
        if (world == null) {
            return;
        }
        float pitch = 0.85f + (LOCK_DURATION - stageTicks) * 0.015f;
        world.playSound(anchor, Sound.BLOCK_NOTE_BLOCK_PLING, 0.75f, pitch);
    }

    private void playConfettiBurst(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        org.bukkit.Color color = rewardColor(rolledReward);
        world.playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.1f);
        world.playSound(location, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.9f, 1.2f);
        ParticleEffectUtil.spawn(world, location, Particle.FIREWORK, color, 28, 0.35, 0.35, 0.35, 0.06);
        ParticleEffectUtil.spawn(world, location, Particle.TOTEM_OF_UNDYING, color, 32, 0.4, 0.4, 0.4, 0.03);
        ParticleEffectUtil.spawn(world, location, Particle.DUST, color, 36, 0.35, 0.35, 0.35, 0.0);
        ParticleEffectUtil.spawn(world, location, Particle.END_ROD, org.bukkit.Color.WHITE, 12, 0.1, 0.15, 0.1, 0.01);
    }

    private void removeDisplays() {
        for (RingSlot slot : slots) {
            slot.remove();
        }
        slots.clear();
        if (pointerDisplay != null && !pointerDisplay.isDead()) {
            pointerDisplay.remove();
        }
        pointerDisplay = null;
    }

    private int indexOfReward(RewardDefinition reward) {
        for (int index = 0; index < pool.size(); index++) {
            if (pool.get(index).id().equals(reward.id())) {
                return index;
            }
        }
        return -1;
    }

    private void updateSuspense(Player player) {
        if (suspenseBeats.isEmpty()) {
            return;
        }
        if (activeBeat != null) {
            activeBeatTicks--;
            if (activeBeatTicks <= 0) {
                releaseSuspense(activeBeat);
                activeBeat = null;
                velocityFloorOverride = -1.0;
                velocityCapOverride = -1.0;
            }
            return;
        }
        double progress = 1.0 - (double) spinTicksRemaining / spinDuration;
        for (SuspenseBeat beat : suspenseBeats) {
            if (!beat.triggered && progress >= beat.triggerProgress) {
                startSuspense(player, beat);
                break;
            }
        }
    }

    private void startSuspense(Player player, SuspenseBeat beat) {
        beat.triggered = true;
        activeBeat = beat;
        activeBeatTicks = beat.durationTicks;
        if (beat.velocityFloor >= 0.0) {
            velocityFloorOverride = beat.velocityFloor;
        }
        if (beat.velocityCap >= 0.0) {
            velocityCapOverride = beat.velocityCap;
        }
        if (beat.kind == SuspenseKind.HOLD) {
            spinTicksRemaining += 8;
        }
        World world = anchor.getWorld();
        if (world == null) {
            return;
        }
        if (beat.kind == SuspenseKind.SURGE) {
            world.playSound(anchor, Sound.BLOCK_BEACON_POWER_SELECT, 0.42f, 1.55f);
            ParticleEffectUtil.spawn(
                    world,
                    anchor,
                    Particle.END_ROD,
                    org.bukkit.Color.fromRGB(255, 255, 200),
                    3,
                    0.08,
                    0.1,
                    0.08,
                    0.0
            );
        } else {
            world.playSound(anchor, Sound.BLOCK_NOTE_BLOCK_BASS, 0.32f, 0.52f);
        }
    }

    private void releaseSuspense(SuspenseBeat beat) {
        World world = anchor.getWorld();
        if (world == null) {
            return;
        }
        if (beat.kind == SuspenseKind.HOLD) {
            world.playSound(anchor, Sound.BLOCK_NOTE_BLOCK_HAT, 0.38f, 1.05f);
        }
    }

    private List<SuspenseBeat> buildSuspenseSchedule(Player player, CrateOpeningSession session) {
        AnimationPhaseProperties properties = settings.properties == null ? new AnimationPhaseProperties() : settings.properties;
        if (!properties.suspenseEnabled) {
            return List.of();
        }
        int count = Math.max(1, Math.min(3, properties.suspenseMoments));
        long seed = player.getUniqueId().getMostSignificantBits()
                ^ player.getUniqueId().getLeastSignificantBits()
                ^ rolledReward.id().hashCode()
                ^ session.sessionId().getMostSignificantBits();
        Random random = new Random(seed);
        double[] bases = {0.5, 0.62, 0.74, 0.83};
        List<Double> triggers = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            double base = bases[Math.min(index, bases.length - 1)];
            double jitter = (random.nextDouble() - 0.5) * 0.05;
            triggers.add(Math.min(0.86, Math.max(0.44, base + jitter)));
        }
        Collections.sort(triggers);
        List<SuspenseBeat> beats = new ArrayList<>();
        for (int index = 0; index < triggers.size(); index++) {
            SuspenseKind kind = index % 2 == 0 ? SuspenseKind.SURGE : SuspenseKind.HOLD;
            int duration = kind == SuspenseKind.SURGE ? 7 + random.nextInt(3) : 9 + random.nextInt(4);
            double floor = kind == SuspenseKind.SURGE ? 0.017 + random.nextDouble() * 0.01 : -1.0;
            double cap = kind == SuspenseKind.HOLD ? 0.0012 + random.nextDouble() * 0.0008 : -1.0;
            beats.add(new SuspenseBeat(triggers.get(index), kind, duration, floor, cap));
        }
        return beats;
    }

    private double resolveAngularVelocity() {
        int elapsed = spinDuration - spinTicksRemaining;
        double progress = (double) elapsed / spinDuration;
        if (progress < 0.25) {
            return 0.055;
        }
        if (progress < 0.45) {
            return 0.042;
        }
        if (progress < 0.62) {
            return 0.028;
        }
        if (progress < 0.78) {
            return 0.016;
        }
        if (progress < 0.9) {
            return 0.008;
        }
        return 0.004;
    }

    private void spawnPointerTrail(World world) {
        if (world == null) {
            return;
        }
        AnimationPhaseProperties properties = settings.properties == null ? new AnimationPhaseProperties() : settings.properties;
        org.bukkit.Color trail = ParticleEffectUtil.parseBukkitColor(properties.color, org.bukkit.Color.fromRGB(85, 255, 85));
        int pointer = locked ? lockedPointerSlot : pointerSlot();
        if (pointer < 0 || pointer >= slots.size()) {
            return;
        }
        RingSlot slot = slots.get(pointer);
        if (slot.block == null || slot.block.isDead()) {
            return;
        }
        Location location = slot.block.getLocation().clone().add(0.0, 0.08, 0.0);
        ParticleEffectUtil.spawn(world, location, Particle.DUST, trail, 1, 0.015, 0.015, 0.015, 0.0);
        if (locked) {
            ParticleEffectUtil.spawn(world, location.clone().add(0.0, 0.08, 0.0), Particle.END_ROD, org.bukkit.Color.WHITE, 2, 0.04, 0.06, 0.04, 0.0);
        }
    }

    private BlockData rewardBlock(RewardDefinition reward) {
        Material material = Material.matchMaterial(reward.material());
        if (material != null && material.isBlock() && !material.isAir()) {
            return material.createBlockData();
        }
        return fallbackBlock(reward).createBlockData();
    }

    private Material fallbackBlock(RewardDefinition reward) {
        String rarity = reward.rarityId() == null ? "" : reward.rarityId().toLowerCase(Locale.ROOT);
        return switch (rarity) {
            case "legendary", "mythic", "epic" -> Material.YELLOW_CONCRETE;
            case "rare", "deluxe", "vip" -> Material.PURPLE_CONCRETE;
            case "uncommon" -> Material.LIGHT_BLUE_CONCRETE;
            default -> Material.RED_CONCRETE;
        };
    }

    private org.bukkit.Color rewardColor(RewardDefinition reward) {
        if (crateDefinition.rarities() != null && reward.rarityId() != null) {
            for (RarityTierSettings tier : crateDefinition.rarities()) {
                if (tier.id != null && tier.id.equalsIgnoreCase(reward.rarityId())) {
                    return rarityColor(tier);
                }
            }
        }
        return org.bukkit.Color.fromRGB(255, 85, 85);
    }

    private org.bukkit.Color rarityColor(RarityTierSettings tier) {
        if (tier.color != null && tier.color.contains("#")) {
            int hashIndex = tier.color.indexOf('#');
            String hex = tier.color.substring(hashIndex, Math.min(hashIndex + 7, tier.color.length()));
            return ParticleEffectUtil.parseBukkitColor(hex, org.bukkit.Color.WHITE);
        }
        String id = tier.id == null ? "" : tier.id.toLowerCase(Locale.ROOT);
        return switch (id) {
            case "legendary", "mythic", "epic" -> org.bukkit.Color.fromRGB(255, 215, 0);
            case "rare", "deluxe", "vip" -> org.bukkit.Color.fromRGB(170, 85, 255);
            case "uncommon" -> org.bukkit.Color.fromRGB(85, 170, 255);
            default -> org.bukkit.Color.fromRGB(255, 85, 85);
        };
    }

    private Component rewardLabel(Player player, RewardDefinition reward, boolean highlight) {
        Component name = RewardDisplayService.displayName(
                messageService,
                player.getUniqueId(),
                crateDefinition.id(),
                reward
        );
        if (highlight) {
            name = name.decoration(TextDecoration.BOLD, true);
        }
        return messageService.parse(rarityPrefix(reward)).append(name);
    }

    private String rarityPrefix(RewardDefinition reward) {
        if (crateDefinition.rarities() == null || reward.rarityId() == null) {
            return "<white>";
        }
        for (RarityTierSettings tier : crateDefinition.rarities()) {
            if (tier.id != null && tier.id.equalsIgnoreCase(reward.rarityId())) {
                return tier.color == null || tier.color.isBlank() ? "<white>" : tier.color;
            }
        }
        return "<white>";
    }

    private boolean isRare(RewardDefinition reward) {
        if (reward.rarityId() == null) {
            return false;
        }
        String rarity = reward.rarityId().toLowerCase(Locale.ROOT);
        return rarity.contains("legend")
                || rarity.contains("myth")
                || rarity.contains("epic")
                || rarity.contains("deluxe")
                || rarity.contains("vip");
    }

    private void applyBlockTransform(BlockDisplay display, float scale, int slotIndex) {
        AxisAngle4f leftRotation;
        AxisAngle4f rightRotation = new AxisAngle4f(0.0f, 0.0f, 1.0f, 0.0f);
        if (isHorizontalLine()) {
            leftRotation = new AxisAngle4f(DIAMOND_TWIST, 0.0f, 1.0f, 0.0f);
        } else {
            double slotAngle = ringAngle + slotIndex * slotStepAngle() + POINTER_ANGLE;
            float radialX = (float) (Math.cos(slotAngle) * right.getX() + Math.sin(slotAngle) * up.getX());
            float radialY = (float) (Math.cos(slotAngle) * right.getY() + Math.sin(slotAngle) * up.getY());
            float radialZ = (float) (Math.cos(slotAngle) * right.getZ() + Math.sin(slotAngle) * up.getZ());
            float tangentX = (float) (-Math.sin(slotAngle) * right.getX() + Math.cos(slotAngle) * up.getX());
            float tangentY = (float) (-Math.sin(slotAngle) * right.getY() + Math.cos(slotAngle) * up.getY());
            float tangentZ = (float) (-Math.sin(slotAngle) * right.getZ() + Math.cos(slotAngle) * up.getZ());
            leftRotation = new AxisAngle4f(DIAMOND_TWIST, tangentX, tangentY, tangentZ);
            rightRotation = new AxisAngle4f(DIAMOND_LEAN, radialX, radialY, radialZ);
        }
        display.setTransformation(new Transformation(
                new Vector3f(0.0f, 0.0f, 0.0f),
                leftRotation,
                new Vector3f(scale, scale, scale),
                rightRotation
        ));
    }

    private static float lerp(float start, float end, float progress) {
        return start + (end - start) * progress;
    }

    private static double lerp(double start, double end, float progress) {
        return start + (end - start) * progress;
    }

    private static Location lerpLocation(Location start, Location end, float progress) {
        return new Location(
                start.getWorld(),
                lerp(start.getX(), end.getX(), progress),
                lerp(start.getY(), end.getY(), progress),
                lerp(start.getZ(), end.getZ(), progress)
        );
    }

    private static final class RingSlot {
        private final BlockDisplay block;
        private final TextDisplay label;

        private RingSlot(BlockDisplay block, TextDisplay label) {
            this.block = block;
            this.label = label;
        }

        private void remove() {
            if (block != null && !block.isDead()) {
                block.remove();
            }
            if (label != null && !label.isDead()) {
                label.remove();
            }
        }
    }
}
