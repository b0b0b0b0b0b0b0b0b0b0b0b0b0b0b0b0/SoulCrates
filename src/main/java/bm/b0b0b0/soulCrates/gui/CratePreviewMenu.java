package bm.b0b0b0.soulCrates.gui;

import bm.b0b0b0.soulCrates.config.settings.GuiPreviewSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.service.reward.RewardRollService;
import bm.b0b0b0.soulCrates.service.key.KeyService;
import bm.b0b0b0.soulCrates.util.KeyCountLabels;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class CratePreviewMenu extends SoulMenu {

    private final MessageService messageService;
    private final GuiPreviewSettings previewSettings;
    private final CrateDefinition crateDefinition;
    private final RewardRollService rewardRollService;
    private final KeyService keyService;
    private final boolean boundStaticCrate;
    private final BiConsumer<Player, Integer> openAction;
    private final Runnable backAction;
    private int page;

    public CratePreviewMenu(
            UUID viewerId,
            MessageService messageService,
            GuiPreviewSettings previewSettings,
            CrateDefinition crateDefinition,
            RewardRollService rewardRollService,
            KeyService keyService,
            boolean boundStaticCrate,
            BiConsumer<Player, Integer> openAction,
            Runnable backAction
    ) {
        super(
                viewerId,
                normalizeSize(previewSettings.size),
                messageService.component(viewerId, "preview-title", Placeholder.parsed("crate", crateDefinition.displayName()))
        );
        this.messageService = messageService;
        this.previewSettings = previewSettings;
        this.crateDefinition = crateDefinition;
        this.rewardRollService = rewardRollService;
        this.keyService = keyService;
        this.boundStaticCrate = boundStaticCrate;
        this.openAction = openAction;
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
        List<RewardDefinition> rewards = crateDefinition.rewards();
        PagedRewardGuiRenderer.PageView pageView = PagedRewardGuiRenderer.normalizePage(
                page,
                rewards.size(),
                previewSettings.grid.rewardSlots.size()
        );
        page = pageView.page();
        PagedRewardGuiRenderer.applyGrid(getInventory(), previewSettings.grid, messageService, player, pageView);
        List<Integer> rewardSlots = previewSettings.grid.rewardSlots;
        for (int index = 0; index < rewardSlots.size(); index++) {
            int rewardIndex = pageView.pageStartIndex() + index;
            if (rewardIndex >= rewards.size()) {
                break;
            }
            RewardDefinition reward = rewards.get(rewardIndex);
            getInventory().setItem(
                    rewardSlots.get(index),
                    GuiItemFactory.rewardPreview(
                            messageService,
                            player,
                            crateDefinition,
                            reward,
                            rewardRollService.chancePercent(crateDefinition, reward)
                    )
            );
        }
        getInventory().setItem(
                previewSettings.openSlot,
                openButton(player)
        );
        GuiItemFactory.fillPreviewActionSlots(
                getInventory(),
                previewSettings.multiOpenSlots,
                previewSettings.grid.borderFillerMaterial
        );
        if (previewSettings.backSlot >= 0) {
            getInventory().setItem(
                    previewSettings.backSlot,
                    GuiItemFactory.cancelButton(messageService, player, previewSettings.backMaterial)
            );
        }
    }

    @Override
    public void handleClick(SoulMenuClick click) {
        if (!click.clickedTop()) {
            return;
        }
        Player player = click.player();
        if (PagedRewardGuiRenderer.isPreviousPageSlot(previewSettings.grid, click.slot())) {
            page = Math.max(0, page - 1);
            refresh();
            return;
        }
        if (PagedRewardGuiRenderer.isNextPageSlot(previewSettings.grid, click.slot())) {
            page++;
            refresh();
            return;
        }
        if (click.slot() == previewSettings.openSlot) {
            openAction.accept(player, 1);
            return;
        }
        if (previewSettings.backSlot >= 0 && click.slot() == previewSettings.backSlot) {
            player.closeInventory();
            if (backAction != null) {
                backAction.run();
            }
        }
    }

    private ItemStack openButton(Player player) {
        ItemStack item = GuiItemFactory.actionButton(
                messageService,
                player,
                previewSettings.openMaterial,
                "preview-open-title",
                "preview-open-lore"
        );
        if (!boundStaticCrate || !shouldShowKeyCount()) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        int keysOwned = keyService.totalKeys(player, crateDefinition.id());
        String localeId = messageService.resolveLocaleId(player);
        List<Component> lore = new ArrayList<>(2);
        lore.add(messageService.component(player.getUniqueId(), "preview-open-lore"));
        lore.add(messageService.component(
                player.getUniqueId(),
                "preview-open-keys-owned",
                messageService.placeholder("count", Integer.toString(keysOwned)),
                messageService.placeholder("keys_word", KeyCountLabels.word(localeId, keysOwned))
        ));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private boolean shouldShowKeyCount() {
        return crateDefinition.opening().requireKey || crateDefinition.keys().enabled;
    }

    private static int normalizeSize(int size) {
        if (size < 9 || size % 9 != 0) {
            return 54;
        }
        return size;
    }
}
