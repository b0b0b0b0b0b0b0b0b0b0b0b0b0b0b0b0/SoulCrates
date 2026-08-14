package bm.b0b0b0.soulCrates.listener;

import bm.b0b0b0.soulCrates.service.CrateService;
import bm.b0b0b0.soulCrates.service.lootbox.LootBoxService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class LootBoxListener implements Listener {

    private final CrateService crateService;
    private final LootBoxService lootBoxService;

    public LootBoxListener(CrateService crateService, LootBoxService lootBoxService) {
        this.crateService = crateService;
        this.lootBoxService = lootBoxService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (!lootBoxService.isLootBox(item)) {
            return;
        }
        event.setCancelled(true);
        crateService.openLootBox(event.getPlayer(), item);
    }
}
