package bm.b0b0b0.soulCrates.engine;

import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.DisplayEngineKind;
import com.ticxo.modelengine.api.model.ModeledEntity;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ModelEngineDisplayEngine implements DisplayEngine {

    private final boolean available;

    public ModelEngineDisplayEngine(JavaPlugin plugin) {
        this.available = plugin.getServer().getPluginManager().isPluginEnabled("ModelEngine")
                && ModelEngineSupport.isAvailable();
    }

    @Override
    public DisplayEngineKind kind() {
        return DisplayEngineKind.MODEL_ENGINE;
    }

    @Override
    public boolean available() {
        return available;
    }

    @Override
    public DisplayComponent createComponent(CrateDefinition crateDefinition, Location location, Player viewer) {
        return new ModelEngineComponent(crateDefinition, location, viewer);
    }

    private static final class ModelEngineComponent implements DisplayComponent {

        private final CrateDefinition crateDefinition;
        private final Location location;
        private final Player viewer;
        private ModeledEntity modeledEntity;
        private String currentAnimation = "";

        private ModelEngineComponent(CrateDefinition crateDefinition, Location location, Player viewer) {
            this.crateDefinition = crateDefinition;
            this.location = location;
            this.viewer = viewer;
        }

        @Override
        public void create() {
            if (crateDefinition.modelId().isBlank()) {
                return;
            }
            modeledEntity = ModelEngineSupport.spawnModel(crateDefinition.modelId(), location, viewer);
            if (modeledEntity != null && crateDefinition.idleAnimation() != null && !crateDefinition.idleAnimation().isBlank()) {
                playAnimation(crateDefinition.idleAnimation());
            }
        }

        @Override
        public void destroy() {
            ModelEngineSupport.destroy(modeledEntity);
            modeledEntity = null;
        }

        @Override
        public void show(Player player) {
        }

        @Override
        public void hide(Player player) {
        }

        @Override
        public void playAnimation(String animationId) {
            if (animationId == null || animationId.isBlank() || animationId.equals(currentAnimation)) {
                return;
            }
            currentAnimation = animationId;
            ModelEngineSupport.playAnimation(modeledEntity, crateDefinition.modelId(), animationId);
        }

        @Override
        public Location anchor() {
            return location.clone();
        }
    }
}
