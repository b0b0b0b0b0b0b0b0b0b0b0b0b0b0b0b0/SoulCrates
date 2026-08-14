package bm.b0b0b0.soulCrates.service.reward;

import bm.b0b0b0.soulCrates.model.RewardDefinition;

public record DeliveryResult(boolean queued, boolean deliveredItems, boolean deliveredCommands) {

    public static DeliveryResult direct() {
        return new DeliveryResult(false, true, true);
    }

    public static DeliveryResult queued(RewardDefinition reward) {
        return new DeliveryResult(true, true, false);
    }

    public static DeliveryResult failure() {
        return new DeliveryResult(false, false, false);
    }

    public boolean fullyDirect() {
        return !queued;
    }

    public boolean isFailed() {
        return !queued && !deliveredItems && !deliveredCommands;
    }
}
