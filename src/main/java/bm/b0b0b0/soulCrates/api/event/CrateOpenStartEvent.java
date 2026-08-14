package bm.b0b0b0.soulCrates.api.event;

import bm.b0b0b0.soulCrates.model.OpeningContext;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class CrateOpenStartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final OpeningContext context;

    public CrateOpenStartEvent(OpeningContext context) {
        this.context = context;
    }

    public OpeningContext context() {
        return context;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
