package bm.b0b0b0.soulCrates.engine;

import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.DisplayEngineKind;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public final class VanillaDisplayEngine implements DisplayEngine {

    @Override
    public DisplayEngineKind kind() {
        return DisplayEngineKind.VANILLA_DISPLAY;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public DisplayComponent createComponent(CrateDefinition crateDefinition, Location location, Player viewer) {
        return new VanillaDisplayComponent(crateDefinition, location);
    }

    private static final class VanillaDisplayComponent implements DisplayComponent {

        private final CrateDefinition crateDefinition;
        private final Location location;
        private BlockDisplay display;

        private VanillaDisplayComponent(CrateDefinition crateDefinition, Location location) {
            this.crateDefinition = crateDefinition;
            this.location = location;
        }

        @Override
        public void create() {
            if (location.getWorld() == null) {
                return;
            }
            display = location.getWorld().spawn(location, BlockDisplay.class, entity -> {
                Material material = crateDefinition.blockMaterial() == null ? Material.ENDER_CHEST : crateDefinition.blockMaterial();
                entity.setBlock(material.createBlockData());
                entity.setTransformation(new Transformation(
                        new Vector3f(0.0f, 0.5f, 0.0f),
                        new AxisAngle4f(0.0f, 0.0f, 1.0f, 0.0f),
                        new Vector3f(1.0f, 1.0f, 1.0f),
                        new AxisAngle4f(0.0f, 0.0f, 1.0f, 0.0f)
                ));
                entity.setBrightness(new Display.Brightness(15, 15));
            });
        }

        @Override
        public void destroy() {
            if (display != null && !display.isDead()) {
                display.remove();
            }
            display = null;
        }

        @Override
        public void show(Player player) {
        }

        @Override
        public void hide(Player player) {
        }

        @Override
        public void playAnimation(String animationId) {
        }

        @Override
        public Location anchor() {
            return location.clone();
        }
    }
}
