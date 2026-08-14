package bm.b0b0b0.soulCrates.gui;

import bm.b0b0b0.soulCrates.config.settings.GuiGeneralSettings;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
public final class SoulGuiListener implements Listener {

    private final ClickGuard clickGuard;

    public SoulGuiListener(GuiGeneralSettings guiGeneralSettings) {
        this.clickGuard = new ClickGuard(guiGeneralSettings.clickDebounceMillis);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!ClickGuard.isSoulMenu(top)) {
            return;
        }
        SoulMenu menu = (SoulMenu) top.getHolder(false);
        if (!player.getUniqueId().equals(menu.viewerId())) {
            event.setCancelled(true);
            return;
        }
        if (menu.cancelClicksOnTop() && event.getClickedInventory() != null && event.getClickedInventory().equals(top)) {
            if (!clickGuard.allow(player.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
            event.setCancelled(true);
            menu.handleClick(new SoulMenuClick(player, menu, event));
        } else if (menu.cancelClicksOnTop()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!ClickGuard.isSoulMenu(top)) {
            return;
        }
        SoulMenu menu = (SoulMenu) top.getHolder(false);
        if (!player.getUniqueId().equals(menu.viewerId())) {
            event.setCancelled(true);
            return;
        }
        if (!menu.cancelDragToTop()) {
            return;
        }
        int topSize = top.getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!ClickGuard.isSoulMenu(top)) {
            return;
        }
        SoulMenu menu = (SoulMenu) top.getHolder(false);
        if (!player.getUniqueId().equals(menu.viewerId())) {
            return;
        }
        menu.onClose(player);
    }
}
