package bm.b0b0b0.soulCrates.engine;

import bm.b0b0b0.soulCrates.hook.modelengine.ModelEngineHookProvider;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

public final class ModelEngineSupport {

    private ModelEngineSupport() {
    }

    public static boolean isAvailable() {
        return ModelEngineAPI.getAPI() != null;
    }

    public static Optional<ModelBlueprint> findBlueprint(String modelId) {
        if (!isAvailable() || modelId == null || modelId.isBlank()) {
            return Optional.empty();
        }
        ModelBlueprint blueprint = ModelEngineAPI.getBlueprint(modelId);
        return Optional.ofNullable(blueprint);
    }

    public static ModeledEntity spawnModel(String modelId, Location location, Player viewer) {
        if (!isAvailable() || location.getWorld() == null) {
            return null;
        }
        Location anchor = location.clone();
        anchor.setX(anchor.getBlockX() + 0.5);
        anchor.setY(anchor.getBlockY());
        anchor.setZ(anchor.getBlockZ() + 0.5);
        ArmorStand stand = anchor.getWorld().spawn(anchor, ArmorStand.class, entity -> {
            entity.setInvisible(true);
            entity.setMarker(true);
            entity.setGravity(false);
            entity.setBasePlate(false);
            entity.setPersistent(false);
        });
        ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(stand);
        modeledEntity.setBaseEntityVisible(false);
        ActiveModel activeModel = ModelEngineAPI.createActiveModel(modelId);
        modeledEntity.addModel(activeModel, true);
        activeModel.setCanHurt(false);
        activeModel.setHitboxVisible(false);
        if (viewer != null) {
            modeledEntity.setBaseEntityVisible(false);
        }
        return modeledEntity;
    }

    public static void playAnimation(ModeledEntity modeledEntity, String modelId, String animationId) {
        if (modeledEntity == null || animationId == null || animationId.isBlank()) {
            return;
        }
        ActiveModel activeModel = modeledEntity.getModel(modelId).orElse(null);
        if (activeModel == null && !modeledEntity.getModels().isEmpty()) {
            activeModel = modeledEntity.getModels().values().iterator().next();
        }
        if (activeModel == null) {
            return;
        }
        activeModel.getAnimationHandler().playAnimation(animationId, 0.0, 0.0, 1.0, true);
    }

    public static void destroy(ModeledEntity modeledEntity) {
        if (modeledEntity == null || modeledEntity.isDestroyed()) {
            return;
        }
        Object base = modeledEntity.getBase().getOriginal();
        modeledEntity.destroy();
        if (base instanceof org.bukkit.entity.Entity entity && !entity.isDead()) {
            entity.remove();
        }
    }
}
