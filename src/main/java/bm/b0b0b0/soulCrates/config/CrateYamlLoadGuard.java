package bm.b0b0b0.soulCrates.config;

import java.nio.file.Path;
import net.elytrium.serializer.language.object.YamlSerializable;

final class CrateYamlLoadGuard {

    private CrateYamlLoadGuard() {
    }

    static <T extends YamlSerializable> T reloadCrateSettings(T settings, Path path) {
        try {
            settings.reload(path);
            return settings;
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Cannot load crate " + path.getFileName()
                            + ". Check YAML indentation — commands/grants must be inside the reward entry (4 spaces).",
                    exception
            );
        }
    }
}
