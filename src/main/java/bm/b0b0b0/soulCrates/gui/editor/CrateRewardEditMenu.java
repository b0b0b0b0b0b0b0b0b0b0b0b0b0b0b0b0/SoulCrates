package bm.b0b0b0.soulCrates.gui.editor;

import bm.b0b0b0.soulCrates.config.settings.CrateDefinitionSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiEditorSettings;
import bm.b0b0b0.soulCrates.config.settings.RewardEntrySettings;
import bm.b0b0b0.soulCrates.gui.GuiItemFactory;
import bm.b0b0b0.soulCrates.gui.SoulMenu;
import bm.b0b0b0.soulCrates.gui.SoulMenuClick;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class CrateRewardEditMenu extends SoulMenu {

    private final MessageService messageService;
    private final GuiEditorSettings editorSettings;
    private final CrateDefinitionSettings mutableSettings;
    private final int rewardIndex;
    private final Consumer<CrateDefinitionSettings> saveAction;
    private final Runnable backAction;

    public CrateRewardEditMenu(
            JavaPlugin plugin,
            UUID viewerId,
            MessageService messageService,
            GuiEditorSettings editorSettings,
            CrateDefinition crateDefinition,
            CrateDefinitionSettings mutableSettings,
            int rewardIndex,
            Consumer<CrateDefinitionSettings> saveAction,
            Runnable backAction
    ) {
        super(viewerId, 27, messageService.component(viewerId, "editor-reward-title"));
        this.messageService = messageService;
        this.editorSettings = editorSettings;
        this.mutableSettings = mutableSettings;
        this.rewardIndex = rewardIndex;
        this.saveAction = saveAction;
        this.backAction = backAction;
        refresh();
    }

    private RewardEntrySettings entry() {
        return mutableSettings.rewards.get(rewardIndex);
    }

    @Override
    public void refresh() {
        getInventory().clear();
        Player player = Bukkit.getPlayer(viewerId());
        if (player == null) {
            return;
        }
        RewardEntrySettings entry = entry();
        for (int slot = 0; slot < getInventory().getSize(); slot++) {
            getInventory().setItem(slot, GuiItemFactory.filler(editorSettings.fillerMaterial));
        }
        getInventory().setItem(4, GuiItemFactory.rewardPreview(
                messageService,
                player,
                CrateRewardListMenu.toDefinition(entry),
                CrateRewardListMenu.chancePercent(entry, mutableSettings.rewards)
        ));
        getInventory().setItem(10, valueItem(player, "editor-reward-weight-title", Double.toString(entry.weight)));
        getInventory().setItem(12, toggleItem(player, "editor-reward-broadcast-title", entry.broadcast));
        getInventory().setItem(14, toggleItem(player, "editor-reward-pity-title", entry.pityEligible));
        getInventory().setItem(16, actionItem(player, "editor-reward-grant-title", "editor-reward-grant-lore"));
        getInventory().setItem(22, GuiItemFactory.actionButton(messageService, player, "editor-save-title", "editor-save-lore"));
        getInventory().setItem(18, GuiItemFactory.cancelButton(messageService, player));
    }

    @Override
    public void handleClick(SoulMenuClick click) {
        if (!click.clickedTop()) {
            return;
        }
        Player player = click.player();
        RewardEntrySettings entry = entry();
        if (click.slot() == 10) {
            double delta = click.clickType().isShiftClick() ? -1.0 : 1.0;
            entry.weight = Math.max(0.0, entry.weight + delta);
            refresh();
            return;
        }
        if (click.slot() == 12) {
            entry.broadcast = !entry.broadcast;
            refresh();
            return;
        }
        if (click.slot() == 14) {
            entry.pityEligible = !entry.pityEligible;
            refresh();
            return;
        }
        if (click.slot() == 16) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.isEmpty()) {
                messageService.send(player.getUniqueId(), "editor-reward-no-item");
                return;
            }
            entry.material = hand.getType().name();
            entry.displayName = hand.getType().name();
            if (hand.hasItemMeta() && hand.getItemMeta().hasDisplayName()) {
                entry.displayName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(hand.getItemMeta().displayName());
            }
            entry.grants = List.of(hand.getType().name() + ":" + Math.max(1, hand.getAmount()));
            refresh();
            return;
        }
        if (click.slot() == 22) {
            saveAction.accept(mutableSettings);
            messageService.send(player.getUniqueId(), "editor-save-success");
            player.closeInventory();
            if (backAction != null) {
                backAction.run();
            }
            return;
        }
        if (click.slot() == 18) {
            player.closeInventory();
            if (backAction != null) {
                backAction.run();
            }
        }
    }

    private ItemStack toggleItem(Player player, String titleKey, boolean enabled) {
        ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messageService.component(
                    player.getUniqueId(),
                    titleKey,
                    messageService.placeholder("state", enabled ? "ON" : "OFF")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack valueItem(Player player, String titleKey, String value) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messageService.component(
                    player.getUniqueId(),
                    titleKey,
                    messageService.placeholder("value", value)
            ));
            meta.lore(List.of(messageService.component(player.getUniqueId(), "editor-value-hint")));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack actionItem(Player player, String titleKey, String loreKey) {
        return GuiItemFactory.actionButton(messageService, player, titleKey, loreKey);
    }
}
