package bm.b0b0b0.soulCrates.gui;

import bm.b0b0b0.soulCrates.config.settings.GuiSpinnerSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class CsgoSpinnerMenu extends SoulMenu {

    private static final int TRACK_START = 10;
    private static final int TRACK_END = 16;
    private static final int TRACK_LENGTH = TRACK_END - TRACK_START + 1;
    private static final int CENTER_TRACK_INDEX = 3;

    private final MessageService messageService;
    private final GuiSpinnerSettings spinnerSettings;
    private final List<RewardDefinition> pool;
    private final RewardDefinition winner;
    private List<RewardDefinition> tape = List.of();
    private int stopOffset;
    private int scrollOffset;
    private boolean stopped;
    private RewardDefinition lastCenterReward;

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
        this.winner = winner;
        prepareRoulette();
        refresh();
    }

    public void prepareRoulette() {
        stopped = false;
        scrollOffset = 0;
        lastCenterReward = null;
        if (pool.isEmpty() || winner == null) {
            tape = winner == null ? List.of() : List.of(winner);
            stopOffset = 0;
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int extra = Math.max(0, spinnerSettings.maxSpinSteps - spinnerSettings.minSpinSteps);
        stopOffset = spinnerSettings.minSpinSteps + (extra == 0 ? 0 : random.nextInt(extra + 1));
        int winTapeIndex = stopOffset + CENTER_TRACK_INDEX;
        int tapeLength = winTapeIndex + TRACK_LENGTH + 6;
        List<RewardDefinition> cells = new ArrayList<>(Collections.nCopies(tapeLength, null));
        cells.set(winTapeIndex, winner);
        if (spinnerSettings.baitEnabled) {
            cells.set(winTapeIndex - 1, pickBait(random));
            if (winTapeIndex - 2 >= 0) {
                cells.set(winTapeIndex - 2, null);
            }
            if (winTapeIndex - 3 >= 0) {
                cells.set(winTapeIndex - 3, pickBait(random));
            }
            if (winTapeIndex + 1 < tapeLength) {
                cells.set(winTapeIndex + 1, null);
            }
        }
        for (int index = 0; index < tapeLength; index++) {
            if (cells.get(index) != null) {
                continue;
            }
            if (index % 2 == 1) {
                continue;
            }
            cells.set(index, pickTapeReward(random));
        }
        tape = cells;
    }

    @Override
    public void refresh() {
        getInventory().clear();
        for (int slot = 0; slot < getInventory().getSize(); slot++) {
            if (isTrackSlot(slot) || slot == spinnerSettings.pointerSlot) {
                continue;
            }
            getInventory().setItem(slot, GuiItemFactory.filler(spinnerSettings.fillerMaterial));
        }
        placePointer();
        renderOffset(0, true);
    }

    public boolean renderOffset(int offset) {
        return renderOffset(offset, false);
    }

    public boolean renderOffset(int offset, boolean force) {
        scrollOffset = Math.max(0, offset);
        Player player = Bukkit.getPlayer(viewerId());
        if (player == null || tape.isEmpty()) {
            return false;
        }
        for (int trackIndex = 0; trackIndex < TRACK_LENGTH; trackIndex++) {
            int slot = TRACK_START + trackIndex;
            int tapeIndex = scrollOffset + trackIndex;
            RewardDefinition reward = tapeIndex >= 0 && tapeIndex < tape.size() ? tape.get(tapeIndex) : null;
            if (reward == null) {
                getInventory().setItem(slot, null);
                continue;
            }
            getInventory().setItem(
                    slot,
                    GuiItemFactory.rewardPreview(messageService, player, reward, weightPercent(reward))
            );
        }
        RewardDefinition center = centerReward();
        boolean centerChanged = force
                || (center != null && (lastCenterReward == null || !center.id().equals(lastCenterReward.id())));
        lastCenterReward = center;
        if (scrollOffset >= stopOffset) {
            stopped = true;
        }
        return centerChanged;
    }

    public RewardDefinition centerReward() {
        if (tape.isEmpty()) {
            return null;
        }
        int tapeIndex = scrollOffset + CENTER_TRACK_INDEX;
        if (tapeIndex < 0 || tapeIndex >= tape.size()) {
            return null;
        }
        return tape.get(tapeIndex);
    }

    public boolean isNearMissStep() {
        return scrollOffset == stopOffset - 1;
    }

    public int stopOffset() {
        return stopOffset;
    }

    public int scrollOffset() {
        return scrollOffset;
    }

    public boolean isStopped() {
        return stopped;
    }

    private void placePointer() {
        if (spinnerSettings.pointerSlot < 0) {
            return;
        }
        Player player = Bukkit.getPlayer(viewerId());
        if (player == null) {
            return;
        }
        getInventory().setItem(
                spinnerSettings.pointerSlot,
                GuiItemFactory.actionButton(
                        messageService,
                        player,
                        spinnerSettings.pointerMaterial,
                        "carousel-pointer",
                        null
                )
        );
    }

    private RewardDefinition pickTapeReward(ThreadLocalRandom random) {
        return pickWeighted(random, spinnerSettings.tapeRareBoost, false);
    }

    private RewardDefinition pickBait(ThreadLocalRandom random) {
        return pickWeighted(random, spinnerSettings.baitRareBoost, true);
    }

    private RewardDefinition pickWeighted(ThreadLocalRandom random, double rareBoost, boolean bait) {
        double boost = Math.max(1.0, rareBoost);
        double total = 0.0;
        for (RewardDefinition entry : pool) {
            if (bait && entry.id().equals(winner.id())) {
                continue;
            }
            total += effectiveWeight(entry, boost);
        }
        if (total <= 0.0) {
            return pool.get(random.nextInt(pool.size()));
        }
        double roll = random.nextDouble(total);
        double cursor = 0.0;
        for (RewardDefinition entry : pool) {
            if (bait && entry.id().equals(winner.id())) {
                continue;
            }
            cursor += effectiveWeight(entry, boost);
            if (roll <= cursor) {
                return entry;
            }
        }
        return pool.get(pool.size() - 1);
    }

    private static double effectiveWeight(RewardDefinition entry, double rareBoost) {
        double weight = Math.max(0.0001, entry.weight());
        return Math.pow(1.0 / weight, Math.min(3.0, rareBoost * 0.35));
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

    private static boolean isTrackSlot(int slot) {
        return slot >= TRACK_START && slot <= TRACK_END;
    }

    private static int normalizeSize(int size) {
        if (size < 9 || size % 9 != 0) {
            return 27;
        }
        return size;
    }
}
