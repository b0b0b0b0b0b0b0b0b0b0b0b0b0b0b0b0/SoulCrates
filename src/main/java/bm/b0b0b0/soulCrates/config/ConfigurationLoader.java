package bm.b0b0b0.soulCrates.config;

import bm.b0b0b0.soulCrates.config.settings.CrateDefinitionSettings;
import bm.b0b0b0.soulCrates.config.settings.CrateShopSettings;
import bm.b0b0b0.soulCrates.config.settings.CratesSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiClaimSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiConfirmSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiEditorSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiGeneralSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiPreviewSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiRerollSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiShopSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiSelectSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiVirtualKeysSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiSpinnerSettings;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigurationLoader {

    private final Path dataFolderPath;

    public ConfigurationLoader(JavaPlugin plugin) {
        this.dataFolderPath = plugin.getDataFolder().toPath();
    }

    public PluginConfig load() {
        ensureDirectories();
        CratesSettings cratesSettings = SerializedConfigReloader.reload(
                new CratesSettings(),
                dataFolderPath.resolve("config.yml")
        );
        GuiGeneralSettings guiGeneralSettings = SerializedConfigReloader.reload(
                new GuiGeneralSettings(),
                dataFolderPath.resolve("gui").resolve("general.yml")
        );
        GuiPreviewSettings guiPreviewSettings = SerializedConfigReloader.reload(
                new GuiPreviewSettings(),
                dataFolderPath.resolve("gui").resolve("preview.yml")
        );
        GuiConfirmSettings guiConfirmSettings = SerializedConfigReloader.reload(
                new GuiConfirmSettings(),
                dataFolderPath.resolve("gui").resolve("confirm.yml")
        );
        GuiSpinnerSettings guiSpinnerSettings = SerializedConfigReloader.reload(
                new GuiSpinnerSettings(),
                dataFolderPath.resolve("gui").resolve("spinner.yml")
        );
        GuiEditorSettings guiEditorSettings = SerializedConfigReloader.reload(
                new GuiEditorSettings(),
                dataFolderPath.resolve("gui").resolve("editor.yml")
        );
        GuiRerollSettings guiRerollSettings = SerializedConfigReloader.reload(
                new GuiRerollSettings(),
                dataFolderPath.resolve("gui").resolve("reroll.yml")
        );
        GuiShopSettings guiShopSettings = SerializedConfigReloader.reload(
                new GuiShopSettings(),
                dataFolderPath.resolve("gui").resolve("shop.yml")
        );
        GuiSelectSettings guiSelectSettings = SerializedConfigReloader.reload(
                new GuiSelectSettings(),
                dataFolderPath.resolve("gui").resolve("select.yml")
        );
        GuiVirtualKeysSettings guiVirtualKeysSettings = SerializedConfigReloader.reload(
                new GuiVirtualKeysSettings(),
                dataFolderPath.resolve("gui").resolve("virtual_keys.yml")
        );
        GuiClaimSettings guiClaimSettings = SerializedConfigReloader.reload(
                new GuiClaimSettings(),
                dataFolderPath.resolve("gui").resolve("claim.yml")
        );
        CrateShopSettings crateShopSettings = SerializedConfigReloader.reload(
                new CrateShopSettings(),
                dataFolderPath.resolve("shop.yml")
        );
        List<CrateDefinition> crateDefinitions = CrateDefinitionLoader.loadDirectory(
                dataFolderPath.resolve(cratesSettings.cratesDirectory)
        );
        return new PluginConfig(
                cratesSettings,
                guiGeneralSettings,
                guiPreviewSettings,
                guiConfirmSettings,
                guiSpinnerSettings,
                guiEditorSettings,
                guiRerollSettings,
                guiClaimSettings,
                guiShopSettings,
                guiSelectSettings,
                guiVirtualKeysSettings,
                crateShopSettings,
                crateDefinitions
        );
    }

    public Path crateFilePath(String crateId) {
        return dataFolderPath.resolve("crates").resolve(crateId + ".yml");
    }

    public CrateDefinitionSettings loadCrateSettings(String crateId) {
        return CrateYamlLoadGuard.reloadCrateSettings(new CrateDefinitionSettings(), crateFilePath(crateId));
    }

    public void saveCrateSettings(CrateDefinitionSettings settings) {
        Path path = crateFilePath(settings.id);
        settings.save(path);
        CrateYamlPresenter.polish(path);
    }

    private void ensureDirectories() {
        try {
            Files.createDirectories(dataFolderPath.resolve("gui"));
            Files.createDirectories(dataFolderPath.resolve("crates"));
            Files.createDirectories(dataFolderPath.resolve("lang"));
            Files.createDirectories(dataFolderPath.resolve("data"));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create plugin directories", exception);
        }
    }
}
