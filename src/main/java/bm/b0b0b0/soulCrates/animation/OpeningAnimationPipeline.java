package bm.b0b0b0.soulCrates.animation;

import bm.b0b0b0.soulCrates.api.event.CrateOpenPhaseEndEvent;
import bm.b0b0b0.soulCrates.api.event.CrateOpenPhaseStartEvent;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.session.CrateOpeningSession;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class OpeningAnimationPipeline {

    private final JavaPlugin plugin;
    private final PhaseFactory phaseFactory;
    private final CrateDefinition crateDefinition;
    private RewardDefinition rolledReward;
    private PhaseRunner currentPhase;
    private OpeningPhaseKind currentKind;
    private ScheduledTask tickTask;
    private int currentIndex = -1;
    private boolean done;
    private Runnable completionCallback;

    public OpeningAnimationPipeline(
            JavaPlugin plugin,
            PhaseFactory phaseFactory,
            CrateDefinition crateDefinition,
            RewardDefinition rolledReward
    ) {
        this.plugin = plugin;
        this.phaseFactory = phaseFactory;
        this.crateDefinition = crateDefinition;
        this.rolledReward = rolledReward;
    }

    public void setCompletionCallback(Runnable completionCallback) {
        this.completionCallback = completionCallback;
    }

    public void start(Player player, CrateOpeningSession session) {
        nextPhase(player, session);
        tickTask = PluginSchedulers.runTimer(plugin, player, 1L, 1L, () -> tick(player, session));
    }

    public void restartFromSecondPhase(Player player, CrateOpeningSession session, RewardDefinition newReward) {
        this.rolledReward = newReward;
        this.done = false;
        if (currentPhase != null) {
            if (currentKind != null) {
                Bukkit.getPluginManager().callEvent(new CrateOpenPhaseEndEvent(session.context(), currentKind));
            }
            currentPhase.unload(player, session);
            currentPhase = null;
            currentKind = null;
        }
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        currentIndex = 0;
        startPhase(player, session, OpeningPhaseKind.SECOND);
        tickTask = PluginSchedulers.runTimer(plugin, player, 1L, 1L, () -> tick(player, session));
    }

    private void tick(Player player, CrateOpeningSession session) {
        if (done) {
            return;
        }
        if (!player.isOnline() || !session.isActive()) {
            unload(player, session);
            return;
        }
        if (currentPhase == null) {
            return;
        }
        currentPhase.tick(player, session);
        if (done || !session.isActive()) {
            unload(player, session);
            return;
        }
        if (currentPhase == null) {
            return;
        }
        if (currentPhase.finished()) {
            nextPhase(player, session);
        }
    }

    private void nextPhase(Player player, CrateOpeningSession session) {
        if (!session.isActive()) {
            unload(player, session);
            return;
        }
        if (currentPhase != null) {
            if (currentKind != null) {
                Bukkit.getPluginManager().callEvent(new CrateOpenPhaseEndEvent(session.context(), currentKind));
            }
            currentPhase.unload(player, session);
        }
        currentIndex++;
        OpeningPhaseKind kind = switch (currentIndex) {
            case 0 -> OpeningPhaseKind.FIRST;
            case 1 -> OpeningPhaseKind.SECOND;
            case 2 -> OpeningPhaseKind.THIRD;
            default -> null;
        };
        if (kind == null) {
            complete();
            return;
        }
        startPhase(player, session, kind);
    }

    private void startPhase(Player player, CrateOpeningSession session, OpeningPhaseKind kind) {
        currentKind = kind;
        currentPhase = phaseFactory.create(crateDefinition, kind, rolledReward);
        Bukkit.getPluginManager().callEvent(new CrateOpenPhaseStartEvent(session.context(), kind));
        currentPhase.load(player, session);
        if (session.displayComponent() != null) {
            session.displayComponent().playAnimation(phaseFactory.animationId(crateDefinition, kind));
        }
        if (kind == OpeningPhaseKind.THIRD) {
            session.markRevealing();
        }
    }

    private void complete() {
        done = true;
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (completionCallback != null) {
            completionCallback.run();
        }
    }

    public void unload(Player player, CrateOpeningSession session) {
        done = true;
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (currentPhase != null) {
            if (currentKind != null && session != null) {
                Bukkit.getPluginManager().callEvent(new CrateOpenPhaseEndEvent(session.context(), currentKind));
            }
            if (player != null && session != null) {
                currentPhase.unload(player, session);
            }
            currentPhase = null;
            currentKind = null;
        }
    }

    public void unload() {
        unload(null, null);
    }
}
