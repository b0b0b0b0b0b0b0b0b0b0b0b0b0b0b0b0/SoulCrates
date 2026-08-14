package bm.b0b0b0.soulCrates.config.settings;

import java.util.List;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class GuiSpinnerSettings extends YamlSerializable {

    public int size = 27;
    public List<Integer> carouselSlots = List.of(9, 10, 11, 12, 13, 14, 15, 16, 17);
    public String fillerMaterial = "BLACK_STAINED_GLASS_PANE";
    public int spinIntervalTicks = 2;
}
