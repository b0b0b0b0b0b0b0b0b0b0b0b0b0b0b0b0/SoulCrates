package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.language.object.YamlSerializable;

public final class GuiRerollSettings extends YamlSerializable {

    public int size = 27;
    public int acceptSlot = 11;
    public int rerollSlot = 15;
    public int rewardSlot = 13;
    public String fillerMaterial = "GRAY_STAINED_GLASS_PANE";
}
