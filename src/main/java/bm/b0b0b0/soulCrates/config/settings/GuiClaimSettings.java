package bm.b0b0b0.soulCrates.config.settings;

import java.util.List;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class GuiClaimSettings extends YamlSerializable {

    public int size = 54;

    public List<Integer> rewardSlots = List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    );

    public int claimAllSlot = 49;

    public int closeSlot = 45;

    public String fillerMaterial = "GRAY_STAINED_GLASS_PANE";
}
