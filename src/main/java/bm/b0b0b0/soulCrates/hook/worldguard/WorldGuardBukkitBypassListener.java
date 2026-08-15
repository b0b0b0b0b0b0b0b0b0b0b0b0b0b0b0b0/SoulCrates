package bm.b0b0b0.soulCrates.hook.worldguard;

import bm.b0b0b0.soulCrates.animation.MobCirclePickPhase;
import bm.b0b0b0.soulCrates.animation.ShulkerPickPhase;
import bm.b0b0b0.soulCrates.service.location.CrateLocationService;
import bm.b0b0b0.soulCrates.service.physical.PhysicalCrateService;
import bm.b0b0b0.soulCrates.session.SessionRegistry;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class WorldGuardBukkitBypassListener implements Listener {

    private final JavaPlugin plugin;
    private final PhysicalCrateService physicalCrateService;
    private final CrateLocationService locationService;
    private final SessionRegistry sessionRegistry;

    public WorldGuardBukkitBypassListener(
            JavaPlugin plugin,
            PhysicalCrateService physicalCrateService,
            CrateLocationService locationService,
            SessionRegistry sessionRegistry
    ) {
        this.plugin = plugin;
        this.physicalCrateService = physicalCrateService;
        this.locationService = locationService;
        this.sessionRegistry = sessionRegistry;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        markIfNeeded(event.getPlayer(), block == null ? null : block.getLocation());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        markIfNeeded(event.getPlayer(), event.getRightClicked().getLocation());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        markIfNeeded(event.getPlayer(), event.getRightClicked().getLocation());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        markIfNeeded(player, event.getEntity().getLocation());
    }

    private void markIfNeeded(Player player, Location targetLocation) {
        if (player == null || !shouldBypass(player, targetLocation)) {
            return;
        }
        WorldGuardBypassContext.mark(player.getUniqueId());
        PluginSchedulers.runLater(plugin, player, 5L, () -> WorldGuardBypassContext.unmark(player.getUniqueId()));
    }

    private boolean shouldBypass(Player player, Location targetLocation) {
        if (!physicalCrateService.worldGuardIntegrationEnabled()) {
            return false;
        }
        if (!isBypassLocation(player.getLocation()) && !isBypassLocation(targetLocation)) {
            return false;
        }
        return matchesSoulCratesAction(player, targetLocation);
    }

    private boolean isBypassLocation(Location location) {
        return location != null && physicalCrateService.isWorldGuardBypassRegion(location);
    }

    private boolean matchesSoulCratesAction(Player player, Location targetLocation) {
        if (targetLocation != null) {
            if (physicalCrateService.findAt(targetLocation).isPresent()) {
                return true;
            }
            if (locationService != null && locationService.findCrateId(targetLocation).isPresent()) {
                return true;
            }
        }
        if (MobCirclePickPhase.activePhase(player.getUniqueId()).isPresent()) {
            return true;
        }
        if (ShulkerPickPhase.activePhase(player.getUniqueId()).isPresent()) {
            return true;
        }
        if (sessionRegistry != null && sessionRegistry.hasActive(player.getUniqueId())) {
            return true;
        }
        return false;
    }
}
