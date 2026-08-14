package bm.b0b0b0.soulCrates.service.lootbox;

import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.util.SoulCratesKeys;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class LootBoxService {

    private final Plugin plugin;
    private final MessageService messageService;

    public LootBoxService(Plugin plugin, MessageService messageService) {
        this.plugin = plugin;
        this.messageService = messageService;
    }

    public boolean isLootBox(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(SoulCratesKeys.lootBoxType(plugin), PersistentDataType.STRING);
    }

    public String readCrateId(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(SoulCratesKeys.lootBoxType(plugin), PersistentDataType.STRING);
    }

    public String readGuaranteedRarity(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(SoulCratesKeys.lootBoxRarity(plugin), PersistentDataType.STRING);
    }

    public ItemStack createLootBox(CrateDefinition crate, int amount) {
        Material material = Material.matchMaterial(crate.lootBox().material);
        if (material == null || material.isAir()) {
            material = Material.CHEST;
        }
        ItemStack item = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messageService.component(null, "lootbox-item-name", messageService.placeholder("crate", crate.displayName())));
            meta.lore(java.util.List.of(messageService.component(null, "lootbox-item-lore", messageService.placeholder("crate", crate.id()))));
            if (crate.lootBox().customModelData >= 0) {
                meta.setCustomModelData(crate.lootBox().customModelData);
            }
            meta.getPersistentDataContainer().set(SoulCratesKeys.lootBoxType(plugin), PersistentDataType.STRING, crate.id());
            if (crate.lootBox().guaranteedRarity != null && !crate.lootBox().guaranteedRarity.isBlank()) {
                meta.getPersistentDataContainer().set(
                        SoulCratesKeys.lootBoxRarity(plugin),
                        PersistentDataType.STRING,
                        crate.lootBox().guaranteedRarity.toLowerCase(Locale.ROOT)
                );
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public void giveLootBox(Player player, CrateDefinition crate, int amount) {
        ItemStack lootBox = createLootBox(crate, amount);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(lootBox);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    public boolean consumeOne(Player player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        stack.setAmount(stack.getAmount() - 1);
        return true;
    }
}
