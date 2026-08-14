package bm.b0b0b0.soulCrates.listener;

import bm.b0b0b0.soulCrates.service.idle.IdleCrateDisplayService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public final class CrateChunkListener implements Listener {

    private final IdleCrateDisplayService idleCrateDisplayService;

    public CrateChunkListener(IdleCrateDisplayService idleCrateDisplayService) {
        this.idleCrateDisplayService = idleCrateDisplayService;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        idleCrateDisplayService.onChunkLoaded(event.getChunk());
    }
}
