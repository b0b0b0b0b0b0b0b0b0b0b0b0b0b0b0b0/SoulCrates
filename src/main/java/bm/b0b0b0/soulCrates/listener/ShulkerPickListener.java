package bm.b0b0b0.soulCrates.listener;

import bm.b0b0b0.soulCrates.animation.ShulkerPickPhase;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class ShulkerPickListener implements Listener {

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
        if (tryPickEntity(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (tryPickEntity(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (tryPickEntity(player, event.getEntity())) {
            event.setCancelled(true);
        }
    }

    private static boolean tryPickLook(Player player) {
        return ShulkerPickPhase.activePhase(player.getUniqueId())
                .map(phase -> phase.tryPickLook(player))
                .orElse(false);
    }

    private static boolean tryPickEntity(Player player, Entity entity) {
        return ShulkerPickPhase.activePhase(player.getUniqueId())
                .map(phase -> phase.tryPick(player, entity))
                .orElse(false);
    }
}
