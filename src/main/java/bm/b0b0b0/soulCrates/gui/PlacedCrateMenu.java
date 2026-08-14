package bm.b0b0b0.soulCrates.gui;

import bm.b0b0b0.soulCrates.config.settings.GuiPreviewSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.CrateInstance;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.service.reward.RewardRollService;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class PlacedCrateMenu extends SoulMenu {

    private final MessageService messageService;
    private final GuiPreviewSettings previewSettings;
    private final CrateDefinition crateDefinition;
    private final CrateInstance crateInstance;
    private final Location blockLocation;
    private final RewardRollService rewardRollService;
    private final BiConsumer<Player, UUID> openAction;
    private final Runnable abandonAction;
    private final Runnable openCloseAction;
    private int page;
    private boolean openRequested;
    private boolean closeHandled;

    public PlacedCrateMenu(
            UUID viewerId,
            MessageService messageService,
            GuiPreviewSettings previewSettings,
            CrateDefinition crateDefinition,
            CrateInstance crateInstance,
            Location blockLocation,
            RewardRollService rewardRollService,
            BiConsumer<Player, UUID> openAction,
            Runnable abandonAction,
            Runnable openCloseAction
    ) {
        super(
                viewerId,
                normalizeSize(previewSettings.size),
                messageService.component(
                        viewerId,
                        "placed-crate-title",
                        Placeholder.parsed("crate", crateDefinition.displayName())
                )
        );
        this.messageService = messageService;
        this.previewSettings = previewSettings;
        this.crateDefinition = crateDefinition;
        this.crateInstance = crateInstance;
        this.blockLocation = blockLocation.clone();
        this.rewardRollService = rewardRollService;
        this.openAction = openAction;
        this.abandonAction = abandonAction;
        this.openCloseAction = openCloseAction;
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
                GuiItemFactory.actionButton(
                        messageService,
                        player,
                        previewSettings.openMaterial,
                        "placed-crate-open-title",
                        "placed-crate-open-lore"
                )
        );
        int infoSlot = previewSettings.backSlot >= 0 ? previewSettings.backSlot : 4;
        getInventory().setItem(infoSlot, infoItem(player));
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
            openRequested = true;
            player.closeInventory();
            openAction.accept(player, crateInstance.instanceId());
        }
    }

    @Override
    public void onClose(Player player) {
        if (closeHandled) {
            return;
        }
        closeHandled = true;
        if (openRequested) {
            if (openCloseAction != null) {
                openCloseAction.run();
            }
            return;
        }
        if (abandonAction != null) {
            abandonAction.run();
        }
    }

    private ItemStack infoItem(Player player) {
        Material material = Material.matchMaterial(previewSettings.backMaterial);
        if (material == null || material.isAir()) {
            material = Material.PAPER;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messageService.component(
                    player.getUniqueId(),
                    "placed-crate-info-title",
                    messageService.placeholder("crate", crateDefinition.displayName())
            ));
            meta.lore(List.of(
                    messageService.component(
                            player.getUniqueId(),
                            "placed-crate-info-lore1",
                            messageService.placeholder("serial", crateInstance.serial())
                    ),
                    messageService.component(player.getUniqueId(), "placed-crate-info-lore2"),
                    messageService.component(player.getUniqueId(), "placed-crate-info-lore3")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static int normalizeSize(int size) {
        if (size < 9 || size % 9 != 0) {
            return 54;
        }
        return size;
    }
}
