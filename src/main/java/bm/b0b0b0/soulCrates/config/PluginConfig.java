package bm.b0b0b0.soulCrates.config;

import bm.b0b0b0.soulCrates.config.settings.CrateShopSettings;
import bm.b0b0b0.soulCrates.config.settings.CratesSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiClaimSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiConfirmSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiEditorSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiGeneralSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiPreviewSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiRerollSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiShopSettings;
import bm.b0b0b0.soulCrates.config.settings.GuiSpinnerSettings;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import java.util.List;

public final class PluginConfig {

    private final CratesSettings cratesSettings;
    private final GuiGeneralSettings guiGeneralSettings;
    private final GuiPreviewSettings guiPreviewSettings;
    private final GuiConfirmSettings guiConfirmSettings;
    private final GuiSpinnerSettings guiSpinnerSettings;
    private final GuiEditorSettings guiEditorSettings;
    private final GuiRerollSettings guiRerollSettings;
    private final GuiClaimSettings guiClaimSettings;
    private final GuiShopSettings guiShopSettings;
    private final CrateShopSettings crateShopSettings;
    private final List<CrateDefinition> crateDefinitions;

    public PluginConfig(
            CratesSettings cratesSettings,
            GuiGeneralSettings guiGeneralSettings,
            GuiPreviewSettings guiPreviewSettings,
            GuiConfirmSettings guiConfirmSettings,
            GuiSpinnerSettings guiSpinnerSettings,
            GuiEditorSettings guiEditorSettings,
            GuiRerollSettings guiRerollSettings,
            GuiClaimSettings guiClaimSettings,
            GuiShopSettings guiShopSettings,
            CrateShopSettings crateShopSettings,
            List<CrateDefinition> crateDefinitions
    ) {
        this.cratesSettings = cratesSettings;
        this.guiGeneralSettings = guiGeneralSettings;
        this.guiPreviewSettings = guiPreviewSettings;
        this.guiConfirmSettings = guiConfirmSettings;
        this.guiSpinnerSettings = guiSpinnerSettings;
        this.guiEditorSettings = guiEditorSettings;
        this.guiRerollSettings = guiRerollSettings;
        this.guiClaimSettings = guiClaimSettings;
        this.guiShopSettings = guiShopSettings;
        this.crateShopSettings = crateShopSettings;
        this.crateDefinitions = crateDefinitions;
    }

    public CratesSettings cratesSettings() {
        return cratesSettings;
    }

    public GuiGeneralSettings guiGeneralSettings() {
        return guiGeneralSettings;
    }

    public GuiPreviewSettings guiPreviewSettings() {
        return guiPreviewSettings;
    }

    public GuiConfirmSettings guiConfirmSettings() {
        return guiConfirmSettings;
    }

    public GuiSpinnerSettings guiSpinnerSettings() {
        return guiSpinnerSettings;
    }

    public GuiEditorSettings guiEditorSettings() {
        return guiEditorSettings;
    }

    public GuiRerollSettings guiRerollSettings() {
        return guiRerollSettings;
    }

    public GuiClaimSettings guiClaimSettings() {
        return guiClaimSettings;
    }

    public GuiShopSettings guiShopSettings() {
        return guiShopSettings;
    }

    public CrateShopSettings crateShopSettings() {
        return crateShopSettings;
    }

    public List<CrateDefinition> crateDefinitions() {
        return crateDefinitions;
    }
}
