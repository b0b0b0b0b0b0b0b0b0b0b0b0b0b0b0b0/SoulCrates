package bm.b0b0b0.soulCrates.gui.editor;

import bm.b0b0b0.soulCrates.config.settings.CrateDefinitionSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiEditorSettings;
import bm.b0b0b0.soulCrates.gui.GuiItemFactory;
import bm.b0b0b0.soulCrates.gui.SoulMenu;
import bm.b0b0b0.soulCrates.gui.SoulMenuClick;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
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

public final class CrateEditorMenu extends SoulMenu {

    private final JavaPlugin plugin;
    private final MessageService messageService;
    private final GuiEditorSettings editorSettings;
    private final CrateDefinition crateDefinition;
    private final CrateDefinitionSettings mutableSettings;
    private final Consumer<CrateDefinitionSettings> saveAction;
    private final Runnable backAction;

    public CrateEditorMenu(
            JavaPlugin plugin,
            UUID viewerId,
            MessageService messageService,
            GuiEditorSettings editorSettings,
            CrateDefinition crateDefinition,
            CrateDefinitionSettings mutableSettings,
            Consumer<CrateDefinitionSettings> saveAction,
            Runnable backAction
    ) {
        super(viewerId, 27, messageService.component(viewerId, "editor-crate-title"));
        this.plugin = plugin;
        this.messageService = messageService;
        this.editorSettings = editorSettings;
        this.crateDefinition = crateDefinition;
        this.mutableSettings = mutableSettings;
        this.saveAction = saveAction;
        this.backAction = backAction;
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
            getInventory().setItem(slot, GuiItemFactory.filler(editorSettings.grid.borderFillerMaterial));
        }
        getInventory().setItem(4, GuiItemFactory.actionButton(messageService, player, "editor-rewards-open-title", "editor-rewards-open-lore"));
        getInventory().setItem(10, toggleItem(player, "editor-toggle-preview-title", mutableSettings.opening.previewEnabled));
        getInventory().setItem(12, toggleItem(player, "editor-toggle-confirm-title", mutableSettings.opening.confirmEnabled));
        getInventory().setItem(14, toggleItem(player, "editor-toggle-pity-title", mutableSettings.pity.enabled));
        getInventory().setItem(16, valueItem(player, "editor-pity-threshold-title", Integer.toString(mutableSettings.pity.threshold)));
        getInventory().setItem(22, GuiItemFactory.actionButton(messageService, player, "editor-save-title", "editor-save-lore"));
        getInventory().setItem(18, GuiItemFactory.cancelButton(messageService, player));
    }

    @Override
    public void handleClick(SoulMenuClick click) {
        if (!click.clickedTop()) {
            return;
        }
        Player player = click.player();
        if (click.slot() == 4) {
            CrateRewardListMenu menu = new CrateRewardListMenu(
                    plugin,
                    player.getUniqueId(),
                    messageService,
                    editorSettings,
                    crateDefinition,
                    mutableSettings,
                    saveAction,
                    () -> PluginSchedulers.run(plugin, player, () -> {
                        refresh();
                        player.openInventory(getInventory());
                    })
            );
            player.openInventory(menu.getInventory());
            return;
        }
        if (click.slot() == 10) {
            mutableSettings.opening.previewEnabled = !mutableSettings.opening.previewEnabled;
            refresh();
            return;
        }
        if (click.slot() == 12) {
            mutableSettings.opening.confirmEnabled = !mutableSettings.opening.confirmEnabled;
            refresh();
            return;
        }
        if (click.slot() == 14) {
            mutableSettings.pity.enabled = !mutableSettings.pity.enabled;
            refresh();
            return;
        }
        if (click.slot() == 16) {
            mutableSettings.pity.threshold = Math.max(1, mutableSettings.pity.threshold + (click.clickType().isShiftClick() ? -1 : 1));
            refresh();
            return;
        }
        if (click.slot() == 22) {
            saveAction.accept(mutableSettings);
            messageService.send(player.getUniqueId(), "editor-save-success");
            refresh();
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
}
