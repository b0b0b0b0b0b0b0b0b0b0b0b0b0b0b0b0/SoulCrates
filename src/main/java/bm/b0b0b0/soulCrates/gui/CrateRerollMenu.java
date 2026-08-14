package bm.b0b0b0.soulCrates.gui;

import bm.b0b0b0.soulCrates.config.settings.GuiRerollSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.service.reward.RewardRollService;
import bm.b0b0b0.soulCrates.service.reroll.RerollService;
import bm.b0b0b0.soulCrates.session.CrateOpeningSession;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class CrateRerollMenu extends SoulMenu {

    private final MessageService messageService;
    private final GuiRerollSettings rerollSettings;
    private final CrateOpeningSession session;
    private final RewardRollService rewardRollService;
    private final RerollService rerollService;
    private final Consumer<Player> acceptAction;
    private final Consumer<Player> rerollAction;

    public CrateRerollMenu(
            UUID viewerId,
            MessageService messageService,
            GuiRerollSettings rerollSettings,
            CrateOpeningSession session,
            RewardRollService rewardRollService,
            RerollService rerollService,
            Consumer<Player> acceptAction,
            Consumer<Player> rerollAction
    ) {
        super(viewerId, normalizeSize(rerollSettings.size), messageService.component(viewerId, "reroll-title"));
        this.messageService = messageService;
        this.rerollSettings = rerollSettings;
        this.session = session;
        this.rewardRollService = rewardRollService;
        this.rerollService = rerollService;
        this.acceptAction = acceptAction;
        this.rerollAction = rerollAction;
        refresh();
    }

    @Override
    public void refresh() {
        getInventory().clear();
        Player player = Bukkit.getPlayer(viewerId());
        if (player == null) {
            return;
        }
        CrateDefinition crate = session.crateDefinition();
        for (int slot = 0; slot < getInventory().getSize(); slot++) {
            getInventory().setItem(slot, GuiItemFactory.filler(rerollSettings.fillerMaterial));
        }
        double chance = rewardRollService.chancePercent(crate, session.rolledReward());
        getInventory().setItem(
                rerollSettings.rewardSlot,
                GuiItemFactory.rewardPreview(messageService, player, session.rolledReward(), chance)
        );
        getInventory().setItem(
                rerollSettings.acceptSlot,
                GuiItemFactory.actionButton(messageService, player, "reroll-accept-title", "reroll-accept-lore")
        );
        int remaining = rerollService.rerollsRemaining(session);
        double cost = rerollService.nextRerollCost(session);
        if (remaining > 0) {
            getInventory().setItem(
                    rerollSettings.rerollSlot,
                    GuiItemFactory.rerollButton(messageService, player, remaining, cost)
            );
        }
    }

    @Override
    public void handleClick(SoulMenuClick click) {
        if (!click.clickedTop()) {
            return;
        }
        Player player = click.player();
        if (click.slot() == rerollSettings.acceptSlot) {
            player.closeInventory();
            acceptAction.accept(player);
            return;
        }
        if (click.slot() == rerollSettings.rerollSlot && rerollService.canReroll(player, session)) {
            rerollAction.accept(player);
        }
    }

    @Override
    public void onClose(Player player) {
        if (session.state() == bm.b0b0b0.soulCrates.model.OpeningSessionState.AWAITING_REROLL) {
            acceptAction.accept(player);
        }
    }

    private static int normalizeSize(int size) {
        if (size < 9 || size % 9 != 0) {
            return 27;
        }
        return size;
    }
}
