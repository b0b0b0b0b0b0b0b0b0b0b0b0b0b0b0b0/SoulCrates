package bm.b0b0b0.soulCrates.config.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class WorldGuardPhysicalCrateSettings extends YamlSerializable {

    @Comment({
            @CommentValue("Use WorldGuard when the plugin is installed."),
    })
    public boolean enabled = true;

    @Comment({
            @CommentValue("WorldGuard region ids where SoulCrates ignores WG block/interact denial for physical crates."),
            @CommentValue("Only these regions get the bypass. Everywhere else WG and normal build rules apply as usual."),
            @CommentValue("Example: spawn — place and open crates on spawn even when block-place is denied."),
    })
    public List<String> bypassRegions = defaultBypassRegions();

    private static List<String> defaultBypassRegions() {
        List<String> regions = new ArrayList<>(1);
        regions.add("spawn");
        return regions;
    }

    public boolean matchesBypassRegion(String regionId) {
        if (regionId == null || regionId.isBlank() || bypassRegions == null || bypassRegions.isEmpty()) {
            return false;
        }
        String normalized = regionId.trim().toLowerCase(Locale.ROOT);
        for (String configured : bypassRegions) {
            if (configured == null || configured.isBlank()) {
                continue;
            }
            if (normalized.equals(configured.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
