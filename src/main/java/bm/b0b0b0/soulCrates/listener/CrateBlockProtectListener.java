package bm.b0b0b0.soulCrates.listener;

import bm.b0b0b0.soulCrates.config.settings.IdleDisplaySettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.service.idle.IdleCrateDisplayService;
import bm.b0b0b0.soulCrates.service.location.CrateLocationService;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import java.util.Iterator;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class CrateBlockProtectListener implements Listener {

    private final JavaPlugin plugin;
    private final MessageService messageService;
    private final CrateLocationService locationService;
    private final IdleCrateDisplayService idleCrateDisplayService;
    private IdleDisplaySettings idleSettings;

    public CrateBlockProtectListener(
            JavaPlugin plugin,
            MessageService messageService,
            CrateLocationService locationService,
            IdleCrateDisplayService idleCrateDisplayService,
            IdleDisplaySettings idleSettings
    ) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.locationService = locationService;
        this.idleCrateDisplayService = idleCrateDisplayService;
        this.idleSettings = idleSettings;
    }

    public void applySettings(IdleDisplaySettings idleSettings) {
        this.idleSettings = idleSettings;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isProtected()) {
            return;
        }
        Block block = event.getBlock();
        Optional<String> crateId = locationService.findCrateId(block.getLocation());
        if (crateId.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        if (canBypass(player)) {
            unbind(block.getLocation());
            return;
        }
        event.setCancelled(true);
        messageService.send(player.getUniqueId(), "crate-break-denied");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!isProtected()) {
            return;
        }
        removeProtectedBlocks(event.blockList().iterator());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!isProtected()) {
            return;
        }
        removeProtectedBlocks(event.blockList().iterator());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!isProtected()) {
            return;
        }
        for (Block block : event.getBlocks()) {
            if (isBound(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!isProtected()) {
            return;
        }
        for (Block block : event.getBlocks()) {
            if (isBound(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void removeProtectedBlocks(Iterator<Block> blocks) {
        while (blocks.hasNext()) {
            Block block = blocks.next();
            if (isBound(block.getLocation())) {
                blocks.remove();
            }
        }
    }

    private boolean isProtected() {
        return idleSettings != null && idleSettings.protectBoundBlocks;
    }

    private boolean isBound(Location location) {
        return locationService.findCrateId(location).isPresent();
    }

    private boolean canBypass(Player player) {
        String permission = idleSettings.breakBypassPermission;
        return permission != null && !permission.isBlank() && player.hasPermission(permission);
    }

    private void unbind(Location location) {
        locationService.unbind(location).thenRun(() -> PluginSchedulers.runAt(plugin, location, () -> {
            if (idleCrateDisplayService != null) {
                idleCrateDisplayService.onUnbind(location);
            }
        }));
    }
}
