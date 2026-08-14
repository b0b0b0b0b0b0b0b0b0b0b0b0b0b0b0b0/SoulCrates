package bm.b0b0b0.soulCrates.gui;

import bm.b0b0b0.soulCrates.config.settings.RarityTierSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.util.ItemDisplayNames;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
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
            meta.displayName(rewardTitle(messages, player, reward, material));
            List<Component> lore = new ArrayList<>();
            if (hasCustomDisplayName(reward, material)) {
                lore.add(ItemDisplayNames.materialName(item));
            }
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
            meta.displayName(rewardTitle(messages, player, reward, material));
            List<Component> lore = new ArrayList<>();
            if (hasCustomDisplayName(reward, material)) {
                lore.add(ItemDisplayNames.materialName(item));
            }
            lore.add(messages.component(player.getUniqueId(), "reward-preview-chance", messages.placeholder("chance", formatChance(chancePercent))));
            lore.add(messages.component(player.getUniqueId(), "reward-preview-id", messages.placeholder("id", reward.id())));
            if (reward.customModelData() >= 0) {
                meta.setCustomModelData(reward.customModelData());
            }
            if (reward.pityEligible()) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static Component rewardTitle(
            MessageService messages,
            Player player,
            RewardDefinition reward,
            Material material
    ) {
        if (hasCustomDisplayName(reward, material)) {
            return messages.component(
                    player.getUniqueId(),
                    "reward-preview-name",
                    messages.placeholder("reward", reward.displayName())
            );
        }
        return Component.translatable(material.translationKey()).decoration(TextDecoration.ITALIC, false);
    }

    private static boolean hasCustomDisplayName(RewardDefinition reward, Material material) {
        if (reward.displayName() == null || reward.displayName().isBlank()) {
            return false;
        }
        String display = reward.displayName().trim();
        String materialKey = material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return !display.equalsIgnoreCase(material.name())
                && !display.equalsIgnoreCase(materialKey);
    }

    public static ItemStack actionButton(MessageService messages, Player player, String titleKey, String loreKey) {
        return actionButton(messages, player, "CHEST", titleKey, loreKey);
    }

    public static ItemStack actionButton(
            MessageService messages,
            Player player,
            String materialName,
            String titleKey,
            String loreKey
    ) {
        Material material = Material.matchMaterial(materialName);
        if (material == null || material.isAir()) {
            material = Material.CHEST;
        }
        ItemStack item = new ItemStack(material);
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

    public static ItemStack pageButton(
            MessageService messages,
            Player player,
            boolean previous,
            int currentPage,
            int totalPages
    ) {
        Material material = previous ? Material.LIGHT_GRAY_DYE : Material.GRAY_DYE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messages.component(
                    player.getUniqueId(),
                    previous ? "gui-page-previous-title" : "gui-page-next-title"
            ));
            meta.lore(List.of(
                    messages.component(
                            player.getUniqueId(),
                            previous ? "gui-page-previous-lore" : "gui-page-next-lore"
                    ),
                    messages.component(
                            player.getUniqueId(),
                            "gui-page-indicator",
                            messages.placeholder("page", Integer.toString(currentPage)),
                            messages.placeholder("pages", Integer.toString(totalPages))
                    )
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack pageButton(
            MessageService messages,
            Player player,
            String materialName,
            boolean previous,
            int currentPage,
            int totalPages
    ) {
        Material material = Material.matchMaterial(materialName);
        if (material == null || material.isAir()) {
            return pageButton(messages, player, previous, currentPage, totalPages);
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messages.component(
                    player.getUniqueId(),
                    previous ? "gui-page-previous-title" : "gui-page-next-title"
            ));
            meta.lore(List.of(
                    messages.component(
                            player.getUniqueId(),
                            previous ? "gui-page-previous-lore" : "gui-page-next-lore"
                    ),
                    messages.component(
                            player.getUniqueId(),
                            "gui-page-indicator",
                            messages.placeholder("page", Integer.toString(currentPage)),
                            messages.placeholder("pages", Integer.toString(totalPages))
                    )
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack cancelButton(MessageService messages, Player player) {
        return actionButton(messages, player, "BARRIER", "confirm-cancel-title", "confirm-cancel-lore");
    }

    public static ItemStack cancelButton(MessageService messages, Player player, String materialName) {
        return actionButton(messages, player, materialName, "confirm-cancel-title", "confirm-cancel-lore");
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
