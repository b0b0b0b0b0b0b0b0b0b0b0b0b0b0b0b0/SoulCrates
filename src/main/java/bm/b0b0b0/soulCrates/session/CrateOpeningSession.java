package bm.b0b0b0.soulCrates.session;

import bm.b0b0b0.soulCrates.animation.OpeningAnimationPipeline;
import bm.b0b0b0.soulCrates.config.settings.RerollSettings;
import bm.b0b0b0.soulCrates.engine.DisplayComponent;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.OpeningContext;
import bm.b0b0b0.soulCrates.model.OpeningSessionState;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.model.RewardRollResult;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class CrateOpeningSession implements OpeningSession {

    private final UUID sessionId;
    private final OpeningContext context;
    private final CrateDefinition crateDefinition;
    private final JavaPlugin plugin;
    private RewardRollResult rollResult;
    private OpeningSessionState state;
    private DisplayComponent displayComponent;
    private OpeningAnimationPipeline animationPipeline;
    private Runnable onFinish;
    private Runnable onCancel;
    private int rerollsUsed;
    private boolean suppressCancelMessage;

    public CrateOpeningSession(
            UUID sessionId,
            OpeningContext context,
            CrateDefinition crateDefinition,
            RewardRollResult rollResult,
            JavaPlugin plugin
    ) {
        this.sessionId = sessionId;
        this.context = context;
        this.crateDefinition = crateDefinition;
        this.rollResult = rollResult;
        this.plugin = plugin;
        this.state = OpeningSessionState.STARTING;
    }

    @Override
    public UUID sessionId() {
        return sessionId;
    }

    @Override
    public OpeningContext context() {
        return context;
    }

    @Override
    public OpeningSessionState state() {
        return state;
    }

    @Override
    public boolean isActive() {
        return state == OpeningSessionState.STARTING
                || state == OpeningSessionState.ANIMATING
                || state == OpeningSessionState.REVEALING
                || state == OpeningSessionState.AWAITING_REROLL;
    }

    public CrateDefinition crateDefinition() {
        return crateDefinition;
    }

    public DisplayComponent displayComponent() {
        return displayComponent;
    }

    public void setDisplayComponent(DisplayComponent displayComponent) {
        this.displayComponent = displayComponent;
    }

    public OpeningAnimationPipeline animationPipeline() {
        return animationPipeline;
    }

    public void setAnimationPipeline(OpeningAnimationPipeline animationPipeline) {
        this.animationPipeline = animationPipeline;
    }

    public void setOnFinish(Runnable onFinish) {
        this.onFinish = onFinish;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    public void setSuppressCancelMessage(boolean suppressCancelMessage) {
        this.suppressCancelMessage = suppressCancelMessage;
    }

    public boolean suppressCancelMessage() {
        return suppressCancelMessage;
    }

    public void markAnimating() {
        state = OpeningSessionState.ANIMATING;
    }

    public void markRevealing() {
        state = OpeningSessionState.REVEALING;
    }

    public void markAwaitingReroll() {
        state = OpeningSessionState.AWAITING_REROLL;
    }

    public boolean tryBeginClaim() {
        if (state == OpeningSessionState.FINISHED || state == OpeningSessionState.CANCELLED) {
            return false;
        }
        state = OpeningSessionState.FINISHED;
        return true;
    }

    public void start(Player player) {
        if (animationPipeline != null) {
            markAnimating();
            animationPipeline.start(player, this);
        }
    }

    public void unload() {
        if (animationPipeline != null) {
            Player player = Bukkit.getPlayer(context.playerId());
            animationPipeline.unload(player, this);
            animationPipeline = null;
        }
        if (displayComponent != null) {
            displayComponent.destroy();
            displayComponent = null;
        }
    }

    @Override
    public void cancel() {
        if (!isActive()) {
            return;
        }
        state = OpeningSessionState.CANCELLED;
        unload();
        if (onCancel != null) {
            onCancel.run();
        }
    }

    @Override
    public void finish() {
        if (state == OpeningSessionState.FINISHED || state == OpeningSessionState.CANCELLED) {
            return;
        }
        state = OpeningSessionState.FINISHED;
        unload();
        if (onFinish != null) {
            onFinish.run();
        }
    }

    public RewardRollResult rollResult() {
        return rollResult;
    }

    public void updateRoll(RewardRollResult rollResult) {
        this.rollResult = rollResult;
    }

    public RewardDefinition rolledReward() {
        return rollResult.reward();
    }

    public int rerollsUsed() {
        return rerollsUsed;
    }

    public void incrementRerollsUsed() {
        rerollsUsed++;
    }

    public int rerollsRemaining() {
        RerollSettings settings = crateDefinition.reroll();
        return Math.max(0, settings.maxRolls - rerollsUsed);
    }

    public JavaPlugin plugin() {
        return plugin;
    }
}
