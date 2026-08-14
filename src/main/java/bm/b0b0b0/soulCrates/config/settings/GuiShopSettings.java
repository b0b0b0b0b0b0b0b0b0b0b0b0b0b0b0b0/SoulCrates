package bm.b0b0b0.soulCrates.config.settings;

import java.util.List;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class GuiShopSettings extends YamlSerializable {

    public int size = 27;

    public String fillerMaterial = "GRAY_STAINED_GLASS_PANE";

    public List<Integer> entrySlots = List.of(10, 11, 12, 13, 14, 15, 16);

    public int closeSlot = 22;
}
