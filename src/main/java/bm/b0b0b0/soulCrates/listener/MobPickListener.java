package bm.b0b0b0.soulCrates.listener;

import bm.b0b0b0.soulCrates.animation.MobCirclePickPhase;
import bm.b0b0b0.soulCrates.util.SoulCratesKeys;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class MobPickListener implements Listener {

    private final JavaPlugin plugin;

    public MobPickListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        MobCirclePickPhase.activePhase(event.getPlayer().getUniqueId()).ifPresent(phase -> {
            if (!phase.shouldConfinePlayer()) {
                return;
            }
            Location to = event.getTo();
            if (to == null || !phase.isOutsideBoundary(to)) {
                return;
            }
            Location clamped = phase.clampLocation(to);
            clamped.setYaw(to.getYaw());
            clamped.setPitch(to.getPitch());
            event.setTo(clamped);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR
                && action != Action.LEFT_CLICK_BLOCK
                && action != Action.RIGHT_CLICK_AIR
                && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (tryPickLook(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (MobCirclePickPhase.isMobPickEntity(plugin, clicked)) {
            tryPickEntity(event.getPlayer(), clicked);
            event.setCancelled(true);
            return;
        }
        if (tryPickEntity(event.getPlayer(), clicked)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (MobCirclePickPhase.isMobPickEntity(plugin, clicked)) {
            tryPickEntity(event.getPlayer(), clicked);
            event.setCancelled(true);
            return;
        }
        if (tryPickEntity(event.getPlayer(), clicked)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobPickupItem(EntityPickupItemEvent event) {
        if (MobCirclePickPhase.isMobPickEntity(plugin, event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onMobHit(EntityDamageByEntityEvent event) {
        if (!MobCirclePickPhase.isMobPickEntity(plugin, event.getEntity())) {
            return;
        }
        if (!(event.getDamager() instanceof Player player)) {
            event.setCancelled(true);
            event.setDamage(0.0);
            return;
        }
        if (!tryPickEntity(player, event.getEntity())) {
            event.setCancelled(true);
            event.setDamage(0.0);
            return;
        }
        event.setCancelled(true);
        event.setDamage(0.0);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobDeath(EntityDeathEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(SoulCratesKeys.mobPickSession(plugin), PersistentDataType.STRING)) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    private static boolean tryPickLook(Player player) {
        return MobCirclePickPhase.activePhase(player.getUniqueId())
                .map(phase -> phase.tryPickLook(player))
                .orElse(false);
    }

    private static boolean tryPickEntity(Player player, Entity entity) {
        return MobCirclePickPhase.activePhase(player.getUniqueId())
                .map(phase -> phase.tryPick(player, entity))
                .orElse(false);
    }
}
