package bm.b0b0b0.soulCrates.util;

import bm.b0b0b0.soulCrates.model.RewardDefinition;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class RewardSnapshotCodec {

    private static final Gson GSON = new GsonBuilder().create();

    private RewardSnapshotCodec() {
    }

    public static String encode(RewardDefinition reward) {
        return GSON.toJson(reward);
    }

    public static RewardDefinition decode(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return GSON.fromJson(json, RewardDefinition.class);
    }
}
