package bm.b0b0b0.soulCrates.gui.editor;

import bm.b0b0b0.soulCrates.config.settings.CrateDefinitionSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiEditorSettings;
import bm.b0b0b0.soulCrates.config.settings.RewardEntrySettings;
import bm.b0b0b0.soulCrates.gui.GuiItemFactory;
import bm.b0b0b0.soulCrates.gui.SoulMenu;
import bm.b0b0b0.soulCrates.gui.SoulMenuClick;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class CrateRewardListMenu extends SoulMenu {

    private final JavaPlugin plugin;
    private final MessageService messageService;
    private final GuiEditorSettings editorSettings;
    private final CrateDefinition crateDefinition;
    private final CrateDefinitionSettings mutableSettings;
    private final Consumer<CrateDefinitionSettings> saveAction;
    private final Runnable backAction;

    public CrateRewardListMenu(
            JavaPlugin plugin,
            UUID viewerId,
            MessageService messageService,
            GuiEditorSettings editorSettings,
            CrateDefinition crateDefinition,
            CrateDefinitionSettings mutableSettings,
            Consumer<CrateDefinitionSettings> saveAction,
            Runnable backAction
    ) {
        super(viewerId, 54, messageService.component(viewerId, "editor-rewards-title"));
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
            getInventory().setItem(slot, GuiItemFactory.filler(editorSettings.fillerMaterial));
        }
        List<RewardEntrySettings> rewards = mutableSettings.rewards;
        for (int index = 0; index < rewards.size() && index < 45; index++) {
            RewardEntrySettings entry = rewards.get(index);
            getInventory().setItem(index, GuiItemFactory.rewardPreview(
                    messageService,
                    player,
                    toDefinition(entry),
                    chancePercent(entry, rewards)
            ));
        }
        getInventory().setItem(49, GuiItemFactory.actionButton(messageService, player, "editor-reward-add-title", "editor-reward-add-lore"));
        getInventory().setItem(53, GuiItemFactory.cancelButton(messageService, player));
    }

    @Override
    public void handleClick(SoulMenuClick click) {
        if (!click.clickedTop()) {
            return;
        }
        Player player = click.player();
        if (click.slot() == 53) {
            player.closeInventory();
            if (backAction != null) {
                backAction.run();
            }
            return;
        }
        if (click.slot() == 49) {
            RewardEntrySettings entry = new RewardEntrySettings();
            entry.id = "reward_" + (mutableSettings.rewards.size() + 1);
            entry.displayName = "New Reward";
            entry.material = Material.PAPER.name();
            mutableSettings.rewards.add(entry);
            openEdit(player, mutableSettings.rewards.size() - 1);
            return;
        }
        if (click.slot() >= 0 && click.slot() < mutableSettings.rewards.size() && click.slot() < 45) {
            openEdit(player, click.slot());
        }
    }

    private void openEdit(Player player, int index) {
        CrateRewardEditMenu menu = new CrateRewardEditMenu(
                plugin,
                player.getUniqueId(),
                messageService,
                editorSettings,
                crateDefinition,
                mutableSettings,
                index,
                saveAction,
                () -> PluginSchedulers.run(plugin, player, () -> player.openInventory(getInventory()))
        );
        PluginSchedulers.run(plugin, player, () -> player.openInventory(menu.getInventory()));
    }

    static RewardDefinition toDefinition(RewardEntrySettings entry) {
        return new RewardDefinition(
                entry.id.toLowerCase(Locale.ROOT),
                entry.weight,
                entry.displayName,
                entry.material,
                entry.customModelData,
                List.copyOf(entry.grants),
                List.copyOf(entry.commands),
                entry.pityEligible,
                entry.broadcast
        );
    }

    static double chancePercent(RewardEntrySettings entry, List<RewardEntrySettings> rewards) {
        double total = 0.0;
        for (RewardEntrySettings reward : rewards) {
            total += Math.max(0.0, reward.weight);
        }
        if (total <= 0.0) {
            return 0.0;
        }
        return Math.max(0.0, entry.weight) * 100.0 / total;
    }
}
