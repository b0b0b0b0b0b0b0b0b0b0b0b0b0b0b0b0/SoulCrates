package bm.b0b0b0.soulCrates.gui;

import bm.b0b0b0.soulCrates.config.settings.CrateShopSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiShopSettings;
import bm.b0b0b0.soulCrates.config.settings.ShopEntrySettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.service.CrateRegistry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class KeyShopMenu extends SoulMenu {

    private final MessageService messageService;
    private final GuiShopSettings guiShopSettings;
    private final CrateShopSettings shopSettings;
    private final CrateRegistry crateRegistry;
    private final BiConsumer<Player, ShopEntrySettings> purchaseAction;

    public KeyShopMenu(
            UUID viewerId,
            MessageService messageService,
            GuiShopSettings guiShopSettings,
            CrateShopSettings shopSettings,
            CrateRegistry crateRegistry,
            BiConsumer<Player, ShopEntrySettings> purchaseAction
    ) {
        super(viewerId, normalizeSize(guiShopSettings.size), messageService.component(viewerId, "shop-title"));
        this.messageService = messageService;
        this.guiShopSettings = guiShopSettings;
        this.shopSettings = shopSettings;
        this.crateRegistry = crateRegistry;
        this.purchaseAction = purchaseAction;
        refresh();
    }

    @Override
    public void refresh() {
        getInventory().clear();
        Player player = Bukkit.getPlayer(viewerId());
        if (player == null) {
            return;
        }
        for (int slot = 0; slot < getInventory().getSize(); slot++) {
            getInventory().setItem(slot, GuiItemFactory.filler(guiShopSettings.fillerMaterial));
        }
        List<ShopEntrySettings> entries = shopSettings.entries;
        List<Integer> slots = guiShopSettings.entrySlots;
        for (int index = 0; index < entries.size() && index < slots.size(); index++) {
            ShopEntrySettings entry = entries.get(index);
            if (!entry.enabled) {
                continue;
            }
            getInventory().setItem(slots.get(index), entryItem(player, entry));
        }
        getInventory().setItem(guiShopSettings.closeSlot, GuiItemFactory.cancelButton(messageService, player));
    }

    @Override
    public void handleClick(SoulMenuClick click) {
        if (!click.clickedTop()) {
            return;
        }
        Player player = click.player();
        if (click.slot() == guiShopSettings.closeSlot) {
            player.closeInventory();
            return;
        }
        List<ShopEntrySettings> entries = shopSettings.entries;
        List<Integer> slots = guiShopSettings.entrySlots;
        for (int index = 0; index < entries.size() && index < slots.size(); index++) {
            if (click.slot() == slots.get(index)) {
                purchaseAction.accept(player, entries.get(index));
                refresh();
                return;
            }
        }
    }

    private ItemStack entryItem(Player player, ShopEntrySettings entry) {
        Material material = Material.matchMaterial(entry.displayMaterial);
        if (material == null || material.isAir()) {
            material = Material.TRIPWIRE_HOOK;
        }
        Optional<CrateDefinition> crateOptional = crateRegistry.find(entry.crateId);
        String crateName = crateOptional.map(CrateDefinition::displayName).orElse(entry.crateId);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messageService.component(
                    player.getUniqueId(),
                    "shop-entry-name",
                    messageService.placeholder("crate", crateName),
                    messageService.placeholder("amount", Integer.toString(Math.max(1, entry.keyAmount)))
            ));
            Component priceLine;
            if (entry.vaultPrice > 0.0) {
                priceLine = messageService.component(
                        player.getUniqueId(),
                        "shop-entry-price-vault",
                        messageService.placeholder("price", formatMoney(entry.vaultPrice))
                );
            } else {
                priceLine = messageService.component(player.getUniqueId(), "shop-entry-price-free");
            }
            if (entry.itemCost != null && !entry.itemCost.isBlank()) {
                meta.lore(List.of(
                        priceLine,
                        messageService.component(
                                player.getUniqueId(),
                                "shop-entry-price-item",
                                messageService.placeholder("item", entry.itemCost)
                        )
                ));
            } else {
                meta.lore(List.of(priceLine));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String formatMoney(double amount) {
        if (amount >= 10.0) {
            return String.format("%.0f", amount);
        }
        return String.format("%.2f", amount);
    }

    private static int normalizeSize(int size) {
        if (size < 9 || size % 9 != 0) {
            return 27;
        }
        return size;
    }
}
