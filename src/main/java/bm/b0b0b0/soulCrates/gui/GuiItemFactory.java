package bm.b0b0b0.soulCrates.gui;

import bm.b0b0b0.soulCrates.config.settings.RarityTierSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class GuiItemFactory {

    private GuiItemFactory() {
    }

    public static ItemStack filler(String materialName) {
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.BLACK_STAINED_GLASS_PANE;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack rewardPreview(
            MessageService messages,
            Player player,
            CrateDefinition crate,
            RewardDefinition reward,
            double chancePercent
    ) {
        Material material = Material.matchMaterial(reward.material());
        if (material == null || material.isAir()) {
            material = Material.PAPER;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messages.component(player.getUniqueId(), "reward-preview-name", messages.placeholder("reward", reward.displayName())));
            List<Component> lore = new ArrayList<>();
            if (reward.rarityId() != null && !reward.rarityId().isBlank() && crate.rarities() != null) {
                for (RarityTierSettings tier : crate.rarities()) {
                    if (tier.id != null && tier.id.equalsIgnoreCase(reward.rarityId())) {
                        lore.add(messages.parse(tier.color + tier.displayName));
                        break;
                    }
                }
            }
            lore.add(messages.component(player.getUniqueId(), "reward-preview-chance", messages.placeholder("chance", formatChance(chancePercent))));
            lore.add(messages.component(player.getUniqueId(), "reward-preview-id", messages.placeholder("id", reward.id())));
            meta.lore(lore);
            if (reward.customModelData() >= 0) {
                meta.setCustomModelData(reward.customModelData());
            }
            if (reward.pityEligible()) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack rewardPreview(MessageService messages, Player player, RewardDefinition reward, double chancePercent) {
        Material material = Material.matchMaterial(reward.material());
        if (material == null || material.isAir()) {
            material = Material.PAPER;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messages.component(player.getUniqueId(), "reward-preview-name", messages.placeholder("reward", reward.displayName())));
            meta.lore(List.of(
                    messages.component(player.getUniqueId(), "reward-preview-chance", messages.placeholder("chance", formatChance(chancePercent))),
                    messages.component(player.getUniqueId(), "reward-preview-id", messages.placeholder("id", reward.id()))
            ));
            if (reward.customModelData() >= 0) {
                meta.setCustomModelData(reward.customModelData());
            }
            if (reward.pityEligible()) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack actionButton(MessageService messages, Player player, String titleKey, String loreKey) {
        ItemStack item = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messages.component(player.getUniqueId(), titleKey));
            if (loreKey != null) {
                meta.lore(List.of(messages.component(player.getUniqueId(), loreKey)));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack cancelButton(MessageService messages, Player player) {
        ItemStack item = new ItemStack(Material.RED_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messages.component(player.getUniqueId(), "confirm-cancel-title"));
            meta.lore(List.of(messages.component(player.getUniqueId(), "confirm-cancel-lore")));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack rerollButton(MessageService messages, Player player, int remaining, double cost) {
        ItemStack item = new ItemStack(Material.YELLOW_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messages.component(
                    player.getUniqueId(),
                    "reroll-action-title",
                    messages.placeholder("remaining", Integer.toString(remaining))
            ));
            if (cost > 0.0) {
                meta.lore(List.of(messages.component(
                        player.getUniqueId(),
                        "reroll-action-lore-paid",
                        messages.placeholder("cost", formatMoney(cost))
                )));
            } else {
                meta.lore(List.of(messages.component(player.getUniqueId(), "reroll-action-lore-free")));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void fillBorder(org.bukkit.inventory.Inventory inventory, List<Integer> slots, String fillerMaterial) {
        ItemStack filler = filler(fillerMaterial);
        for (int slot : slots) {
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private static String formatChance(double chancePercent) {
        if (chancePercent >= 10.0) {
            return String.format("%.1f", chancePercent);
        }
        return String.format("%.2f", chancePercent);
    }

    private static String formatMoney(double amount) {
        if (amount >= 10.0) {
            return String.format("%.2f", amount);
        }
        return String.format("%.2f", amount);
    }
}
