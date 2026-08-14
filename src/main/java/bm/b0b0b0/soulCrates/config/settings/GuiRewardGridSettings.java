package bm.b0b0b0.soulCrates.config.settings;

import java.util.ArrayList;
import java.util.List;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class GuiRewardGridSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Outer frame slots. Slots 45 and 53 are filled automatically when page arrows are hidden."),
    })
    public List<Integer> borderSlots = defaultBorderSlots();

    @Comment({
            @CommentValue("Reward workspace: rows at slots 10-16, 19-25, 28-34 (21 per page)."),
    })
    public List<Integer> rewardSlots = defaultRewardSlots();

    @Comment({
            @CommentValue("Border / frame filler."),
    })
    public String borderFillerMaterial = "BLACK_STAINED_GLASS_PANE";

    @Comment({
            @CommentValue("Empty slots inside the reward workspace."),
    })
    public String contentFillerMaterial = "WHITE_STAINED_GLASS_PANE";

    public boolean paginationEnabled = true;

    public int previousPageSlot = 45;

    public int nextPageSlot = 53;

    public String previousPageMaterial = "LIGHT_GRAY_DYE";

    public String nextPageMaterial = "GRAY_DYE";

    public static List<Integer> defaultBorderSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot <= 8; slot++) {
            slots.add(slot);
        }
        slots.add(9);
        slots.add(17);
        slots.add(18);
        slots.add(26);
        slots.add(27);
        slots.add(35);
        for (int slot = 36; slot <= 44; slot++) {
            slots.add(slot);
        }
        slots.add(46);
        slots.add(50);
        slots.add(52);
        return List.copyOf(slots);
    }

    public static List<Integer> defaultRewardSlots() {
        return List.of(
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        );
    }
}
