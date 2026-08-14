package bm.b0b0b0.soulCrates.gui;

import bm.b0b0b0.soulCrates.config.settings.GuiSpinnerSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class CsgoSpinnerMenu extends SoulMenu {

    private final MessageService messageService;
    private final GuiSpinnerSettings spinnerSettings;
    private final List<RewardDefinition> pool;
    private boolean locked;

    public CsgoSpinnerMenu(
            UUID viewerId,
            MessageService messageService,
            GuiSpinnerSettings spinnerSettings,
            CrateDefinition crateDefinition,
            RewardDefinition winner
    ) {
        super(viewerId, normalizeSize(spinnerSettings.size), messageService.component(viewerId, "spinner-title"));
        this.messageService = messageService;
        this.spinnerSettings = spinnerSettings;
        this.pool = crateDefinition.rewards();
        refresh();
    }

    @Override
    public void refresh() {
        getInventory().clear();
        for (int slot = 0; slot < getInventory().getSize(); slot++) {
            getInventory().setItem(slot, GuiItemFactory.filler(spinnerSettings.fillerMaterial));
        }
        spin(0);
    }

    public void spin(int cursor) {
        if (pool.isEmpty()) {
            return;
        }
        List<Integer> slots = spinnerSettings.carouselSlots;
        Player player = Bukkit.getPlayer(viewerId());
        if (player == null) {
            return;
        }
        for (int index = 0; index < slots.size(); index++) {
            int slot = slots.get(index);
            RewardDefinition reward = pool.get(Math.floorMod(cursor + index, pool.size()));
            getInventory().setItem(slot, GuiItemFactory.rewardPreview(messageService, player, reward, weightPercent(reward)));
        }
    }

    public void lockWinner(RewardDefinition reward) {
        if (locked || spinnerSettings.carouselSlots.isEmpty()) {
            return;
        }
        locked = true;
        Player player = Bukkit.getPlayer(viewerId());
        if (player == null) {
            return;
        }
        int centerSlot = spinnerSettings.carouselSlots.get(spinnerSettings.carouselSlots.size() / 2);
        getInventory().setItem(centerSlot, GuiItemFactory.rewardPreview(messageService, player, reward, weightPercent(reward)));
    }

    private double weightPercent(RewardDefinition reward) {
        double total = 0.0;
        for (RewardDefinition entry : pool) {
            total += Math.max(0.0, entry.weight());
        }
        if (total <= 0.0) {
            return 0.0;
        }
        return reward.weight() * 100.0 / total;
    }

    private static int normalizeSize(int size) {
        if (size < 9 || size % 9 != 0) {
            return 27;
        }
        return size;
    }
}
