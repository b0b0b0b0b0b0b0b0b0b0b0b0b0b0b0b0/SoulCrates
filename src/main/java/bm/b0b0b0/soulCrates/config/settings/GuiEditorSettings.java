package bm.b0b0b0.soulCrates.config.settings;

import java.util.List;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class GuiEditorSettings extends YamlSerializable {

    public int size = 54;
    public List<Integer> crateSlots = List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    );
    public int backSlot = 45;
    public int reloadSlot = 49;
    public String fillerMaterial = "BLACK_STAINED_GLASS_PANE";
}
