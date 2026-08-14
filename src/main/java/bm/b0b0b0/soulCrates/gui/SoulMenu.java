package bm.b0b0b0.soulCrates.gui;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public abstract class SoulMenu implements InventoryHolder {

    private final UUID viewerId;
    private final Inventory inventory;

    protected SoulMenu(UUID viewerId, int size, Component title) {
        this.viewerId = viewerId;
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public UUID viewerId() {
        return viewerId;
    }

    public abstract void refresh();

    public void handleClick(SoulMenuClick click) {
    }

    public boolean cancelDragToTop() {
        return true;
    }

    public boolean cancelClicksOnTop() {
        return true;
    }

    public void onClose(Player player) {
    }
}
