package bm.b0b0b0.soulCrates.hook.worldguard;

import bm.b0b0b0.soulCrates.animation.MobCirclePickPhase;
import bm.b0b0b0.soulCrates.animation.ShulkerPickPhase;
import bm.b0b0b0.soulCrates.service.location.CrateLocationService;
import bm.b0b0b0.soulCrates.service.physical.PhysicalCrateService;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class WorldGuardBypassListener {

    private final JavaPlugin plugin;
    private final PhysicalCrateService physicalCrateService;
    private final CrateLocationService locationService;
    private final WorldGuardHook worldGuardHook;

    public WorldGuardBypassListener(
            JavaPlugin plugin,
            PhysicalCrateService physicalCrateService,
            CrateLocationService locationService,
            WorldGuardHook worldGuardHook
    ) {
        this.plugin = plugin;
        this.physicalCrateService = physicalCrateService;
        this.locationService = locationService;
        this.worldGuardHook = worldGuardHook;
    }

    public void handleLow(Object delegateEvent) {
        handle(delegateEvent, false);
    }

    public void handleHigh(Object delegateEvent) {
        handle(delegateEvent, true);
    }

    private void handle(Object delegateEvent, boolean force) {
        if (delegateEvent == null || !physicalCrateService.worldGuardIntegrationEnabled()) {
            return;
        }
        if (!force && worldGuardHook.isDelegateAllowed(delegateEvent)) {
            return;
        }
        Player player = worldGuardHook.resolveDelegatePlayer(delegateEvent);
        if (player != null && WorldGuardBypassContext.marked(player.getUniqueId())) {
            worldGuardHook.allowDelegateEvent(delegateEvent);
            return;
        }
        Location location = worldGuardHook.resolveDelegateLocation(delegateEvent);
        if (!isBypassLocation(location) && (player == null || !isBypassLocation(player.getLocation()))) {
            return;
        }
        Entity entity = worldGuardHook.resolveDelegateEntity(delegateEvent);
        if (!matchesSoulCratesAction(location, player, entity)) {
            return;
        }
        worldGuardHook.allowDelegateEvent(delegateEvent);
    }

    private boolean isBypassLocation(Location location) {
        return location != null && physicalCrateService.isWorldGuardBypassRegion(location);
    }

    private boolean matchesSoulCratesAction(Location location, Player player, Entity entity) {
        if (location != null) {
            if (physicalCrateService.findAt(location).isPresent()) {
                return true;
            }
            if (locationService != null && locationService.findCrateId(location).isPresent()) {
                return true;
            }
        }
        if (entity != null && MobCirclePickPhase.isMobPickEntity(plugin, entity)) {
            return true;
        }
        if (player == null) {
            return false;
        }
        if (MobCirclePickPhase.activePhase(player.getUniqueId()).isPresent()) {
            return true;
        }
        return ShulkerPickPhase.activePhase(player.getUniqueId()).isPresent();
    }
}
