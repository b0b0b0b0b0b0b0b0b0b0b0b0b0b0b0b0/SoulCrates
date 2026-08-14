package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.language.object.YamlSerializable;

public final class GuiConfirmSettings extends YamlSerializable {

    public int size = 27;
    public int confirmSlot = 11;
    public int cancelSlot = 15;
    public int previewSlot = 13;
    public String fillerMaterial = "GRAY_STAINED_GLASS_PANE";
}
