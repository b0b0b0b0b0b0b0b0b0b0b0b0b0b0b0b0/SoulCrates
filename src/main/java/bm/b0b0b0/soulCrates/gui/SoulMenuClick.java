package bm.b0b0b0.soulCrates.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public final class SoulMenuClick {

    private final Player player;
    private final SoulMenu menu;
    private final InventoryClickEvent event;
    private final int slot;
    private final ClickType clickType;

    public SoulMenuClick(Player player, SoulMenu menu, InventoryClickEvent event) {
        this.player = player;
        this.menu = menu;
        this.event = event;
        this.slot = event.getRawSlot();
        this.clickType = event.getClick();
    }

    public Player player() {
        return player;
    }

    public SoulMenu menu() {
        return menu;
    }

    public InventoryClickEvent event() {
        return event;
    }

    public int slot() {
        return slot;
    }

    public ClickType clickType() {
        return clickType;
    }

    public Inventory topInventory() {
        return event.getView().getTopInventory();
    }

    public boolean clickedTop() {
        Inventory clicked = event.getClickedInventory();
        return clicked != null && clicked.equals(topInventory());
    }
}
