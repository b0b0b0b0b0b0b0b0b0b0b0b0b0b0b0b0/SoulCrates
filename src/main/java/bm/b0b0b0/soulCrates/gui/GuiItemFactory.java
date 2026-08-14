package bm.b0b0b0.soulCrates.gui;

import bm.b0b0b0.soulCrates.config.settings.RarityTierSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.service.reward.RewardDisplayService;
import bm.b0b0b0.soulCrates.util.ItemDisplayNames;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import java.util.UUID;
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
        return buildRewardPreview(
                messages,
                player,
                crate.id(),
                reward,
                crate.rarities(),
                chancePercent
        );
    }

    public static ItemStack rewardPreview(MessageService messages, Player player, RewardDefinition reward, double chancePercent) {
        return rewardPreview(messages, player, "", reward, chancePercent);
    }

    public static ItemStack rewardPreview(
            MessageService messages,
            Player player,
            String crateId,
            RewardDefinition reward,
            double chancePercent
    ) {
        return buildRewardPreview(messages, player, crateId, reward, null, chancePercent);
    }

    public static ItemStack rewardDisplayItem(MessageService messages, Player player, String crateId, RewardDefinition reward) {
        Material material = RewardDisplayService.material(reward);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            applyRewardAppearance(item, meta, messages, player.getUniqueId(), crateId, reward, material);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack rewardDisplayItem(MessageService messages, Player player, RewardDefinition reward) {
        return rewardDisplayItem(messages, player, "", reward);
    }

    private static ItemStack buildRewardPreview(
            MessageService messages,
            Player player,
            String crateId,
            RewardDefinition reward,
            List<RarityTierSettings> rarities,
            double chancePercent
    ) {
        Material material = RewardDisplayService.material(reward);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            applyRewardAppearance(item, meta, messages, player.getUniqueId(), crateId, reward, material);
            List<Component> lore = new ArrayList<>();
            if (!RewardDisplayService.shouldUseMaterialName(reward, material)) {
                lore.add(ItemDisplayNames.materialName(item));
            }
            appendRarityLore(lore, messages, player.getUniqueId(), reward, rarities);
            lore.add(messages.component(
                    player.getUniqueId(),
                    "reward-preview-chance",
                    messages.placeholder("chance", formatChance(chancePercent))
            ));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void applyRewardAppearance(
            ItemStack item,
            ItemMeta meta,
            MessageService messages,
            UUID playerId,
            String crateId,
            RewardDefinition reward,
            Material material
    ) {
        if (RewardDisplayService.shouldUseMaterialName(reward, material)) {
            RewardDisplayService.applyPreviewStackSize(item, reward, material);
        } else {
            meta.displayName(RewardDisplayService.displayName(messages, playerId, crateId, reward));
        }
        applyRewardItemMeta(meta, reward);
    }

    public static Component rewardDisplayName(MessageService messages, Player player, String crateId, RewardDefinition reward) {
        return RewardDisplayService.displayName(messages, player.getUniqueId(), crateId, reward);
    }

    public static Component rewardDisplayName(MessageService messages, Player player, RewardDefinition reward) {
        return rewardDisplayName(messages, player, "", reward);
    }

    private static void applyRewardItemMeta(ItemMeta meta, RewardDefinition reward) {
        if (reward.customModelData() >= 0) {
            meta.setCustomModelData(reward.customModelData());
        }
        if (reward.pityEligible()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
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

    public static void fillPreviewActionSlots(
            org.bukkit.inventory.Inventory inventory,
            List<Integer> slots,
            String fillerMaterial
    ) {
        ItemStack filler = filler(fillerMaterial);
        for (int slot : slots) {
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private static void appendRarityLore(
            List<Component> lore,
            MessageService messages,
            UUID playerId,
            RewardDefinition reward,
            List<RarityTierSettings> tiers
    ) {
        if (reward.rarityId() == null || reward.rarityId().isBlank()) {
            return;
        }
        RarityTierSettings matched = null;
        if (tiers != null) {
            for (RarityTierSettings tier : tiers) {
                if (tier.id != null && tier.id.equalsIgnoreCase(reward.rarityId())) {
                    matched = tier;
                    break;
                }
            }
        }
        lore.add(rarityComponent(messages, playerId, reward.rarityId(), matched));
    }

    private static Component rarityComponent(
            MessageService messages,
            UUID playerId,
            String rarityId,
            RarityTierSettings tier
    ) {
        String key = "rarity-" + rarityId.toLowerCase(Locale.ROOT);
        if (messages.hasKey(playerId, key)) {
            return messages.component(playerId, key);
        }
        if (tier != null) {
            return messages.parse(tier.color + tier.displayName);
        }
        return messages.component(playerId, "rarity-fallback", messages.placeholder("rarity", rarityId));
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
