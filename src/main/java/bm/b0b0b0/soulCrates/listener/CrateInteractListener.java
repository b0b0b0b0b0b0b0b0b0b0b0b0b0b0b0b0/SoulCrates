package bm.b0b0b0.soulCrates.listener;

import bm.b0b0b0.soulCrates.config.settings.IdleDisplaySettings;
import bm.b0b0b0.soulCrates.service.CrateService;
import bm.b0b0b0.soulCrates.service.location.CrateLocationService;
import java.util.Optional;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class CrateInteractListener implements Listener {

    private final CrateService crateService;
    private final CrateLocationService locationService;
    private IdleDisplaySettings idleSettings;

    public CrateInteractListener(
            CrateService crateService,
            CrateLocationService locationService,
            IdleDisplaySettings idleSettings
    ) {
        this.crateService = crateService;
        this.locationService = locationService;
        this.idleSettings = idleSettings;
    }

    public void applySettings(IdleDisplaySettings idleSettings) {
        this.idleSettings = idleSettings;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Optional<String> crateId = locationService.findCrateId(block.getLocation());
        if (crateId.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        if (event.getPlayer().isSneaking()) {
            crateService.openPreview(event.getPlayer(), crateId.get());
            return;
        }
        playInteractSound(block);
        crateService.beginOpen(event.getPlayer(), crateId.get(), block.getLocation());
    }

    private void playInteractSound(Block block) {
        if (idleSettings == null || !idleSettings.interactSound) {
            return;
        }
        Sound sound = parseSound(idleSettings.interactSoundName);
        if (sound == null) {
            return;
        }
        block.getWorld().playSound(
                block.getLocation().add(0.5, 0.5, 0.5),
                sound,
                idleSettings.interactSoundVolume,
                idleSettings.interactSoundPitch
        );
    }

    private static Sound parseSound(String raw) {
        if (raw == null || raw.isBlank()) {
            return Sound.BLOCK_ENDER_CHEST_OPEN;
        }
        try {
            return Sound.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return Sound.BLOCK_ENDER_CHEST_OPEN;
        }
    }
}
