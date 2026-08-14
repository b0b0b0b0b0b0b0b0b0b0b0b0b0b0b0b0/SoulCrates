package bm.b0b0b0.soulCrates.api.event;

import bm.b0b0b0.soulCrates.animation.OpeningPhaseKind;
import bm.b0b0b0.soulCrates.model.OpeningContext;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class CrateOpenPhaseStartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final OpeningContext context;
    private final OpeningPhaseKind phase;

    public CrateOpenPhaseStartEvent(OpeningContext context, OpeningPhaseKind phase) {
        this.context = context;
        this.phase = phase;
    }

    public OpeningContext context() {
        return context;
    }

    public OpeningPhaseKind phase() {
        return phase;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
