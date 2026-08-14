package bm.b0b0b0.soulCrates.gui.editor;

import bm.b0b0b0.soulCrates.config.settings.CrateDefinitionSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiEditorSettings;
import bm.b0b0b0.soulCrates.config.settings.RewardEntrySettings;
import bm.b0b0b0.soulCrates.gui.GuiItemFactory;
import bm.b0b0b0.soulCrates.gui.PagedRewardGuiRenderer;
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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class CrateRewardListMenu extends SoulMenu {

    private final JavaPlugin plugin;
    private final MessageService messageService;
    private final GuiEditorSettings editorSettings;
    private final CrateDefinition crateDefinition;
    private final CrateDefinitionSettings mutableSettings;
    private final Consumer<CrateDefinitionSettings> saveAction;
    private final Runnable backAction;
    private int page;

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
        super(viewerId, normalizeSize(editorSettings.size), messageService.component(viewerId, "editor-rewards-title"));
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
        List<RewardEntrySettings> rewards = mutableSettings.rewards;
        PagedRewardGuiRenderer.PageView pageView = PagedRewardGuiRenderer.normalizePage(
                page,
                rewards.size(),
                editorSettings.grid.rewardSlots.size()
        );
        page = pageView.page();
        PagedRewardGuiRenderer.applyGrid(getInventory(), editorSettings.grid, messageService, player, pageView);
        List<Integer> rewardSlots = editorSettings.grid.rewardSlots;
        for (int index = 0; index < rewardSlots.size(); index++) {
            int rewardIndex = pageView.pageStartIndex() + index;
            if (rewardIndex >= rewards.size()) {
                break;
            }
            RewardEntrySettings entry = rewards.get(rewardIndex);
            getInventory().setItem(
                    rewardSlots.get(index),
                    GuiItemFactory.rewardPreview(
                            messageService,
                            player,
                            toDefinition(entry),
                            chancePercent(entry, rewards)
                    )
            );
        }
        getInventory().setItem(
                editorSettings.addRewardSlot,
                GuiItemFactory.actionButton(
                        messageService,
                        player,
                        editorSettings.addRewardMaterial,
                        "editor-reward-add-title",
                        "editor-reward-add-lore"
                )
        );
        if (editorSettings.backSlot >= 0) {
            getInventory().setItem(
                    editorSettings.backSlot,
                    GuiItemFactory.cancelButton(messageService, player, editorSettings.backMaterial)
            );
        }
    }

    @Override
    public void handleClick(SoulMenuClick click) {
        if (!click.clickedTop()) {
            return;
        }
        Player player = click.player();
        if (PagedRewardGuiRenderer.isPreviousPageSlot(editorSettings.grid, click.slot())) {
            page = Math.max(0, page - 1);
            refresh();
            return;
        }
        if (PagedRewardGuiRenderer.isNextPageSlot(editorSettings.grid, click.slot())) {
            page++;
            refresh();
            return;
        }
        if (editorSettings.backSlot >= 0 && click.slot() == editorSettings.backSlot) {
            player.closeInventory();
            if (backAction != null) {
                backAction.run();
            }
            return;
        }
        if (click.slot() == editorSettings.addRewardSlot) {
            RewardEntrySettings entry = new RewardEntrySettings();
            entry.id = "reward_" + (mutableSettings.rewards.size() + 1);
            entry.displayName = "New Reward";
            entry.material = Material.PAPER.name();
            mutableSettings.rewards.add(entry);
            page = PagedRewardGuiRenderer.normalizePage(
                    Integer.MAX_VALUE,
                    mutableSettings.rewards.size(),
                    editorSettings.grid.rewardSlots.size()
            ).page();
            openEdit(player, mutableSettings.rewards.size() - 1);
            return;
        }
        int slotIndex = PagedRewardGuiRenderer.rewardSlotIndex(editorSettings.grid, click.slot());
        if (slotIndex < 0) {
            return;
        }
        PagedRewardGuiRenderer.PageView pageView = PagedRewardGuiRenderer.normalizePage(
                page,
                mutableSettings.rewards.size(),
                editorSettings.grid.rewardSlots.size()
        );
        int rewardIndex = pageView.pageStartIndex() + slotIndex;
        if (rewardIndex >= 0 && rewardIndex < mutableSettings.rewards.size()) {
            openEdit(player, rewardIndex);
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
                entry.rarity == null ? "" : entry.rarity.toLowerCase(Locale.ROOT),
                entry.weight,
                entry.displayName,
                entry.material,
                entry.customModelData,
                List.copyOf(entry.grants),
                List.copyOf(entry.commands),
                entry.pityEligible,
                entry.broadcast,
                entry.enabled,
                null
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

    private static int normalizeSize(int size) {
        if (size < 9 || size % 9 != 0) {
            return 54;
        }
        return size;
    }
}
