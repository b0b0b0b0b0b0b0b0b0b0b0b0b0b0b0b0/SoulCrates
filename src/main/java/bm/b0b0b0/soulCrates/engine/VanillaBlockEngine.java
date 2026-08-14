package bm.b0b0b0.soulCrates.engine;

import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.DisplayEngineKind;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

public final class VanillaBlockEngine implements DisplayEngine {

    @Override
    public DisplayEngineKind kind() {
        return DisplayEngineKind.VANILLA_BLOCK;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public DisplayComponent createComponent(CrateDefinition crateDefinition, Location location, Player viewer) {
        return new VanillaBlockComponent(crateDefinition, location);
    }

    private static final class VanillaBlockComponent implements DisplayComponent {

        private final CrateDefinition crateDefinition;
        private final Location location;
        private Material previousType;
        private BlockData previousData;

        private VanillaBlockComponent(CrateDefinition crateDefinition, Location location) {
            this.crateDefinition = crateDefinition;
            this.location = location;
        }

        @Override
        public void create() {
            if (location.getWorld() == null) {
                return;
            }
            Block block = location.getBlock();
            previousType = block.getType();
            previousData = block.getBlockData().clone();
            Material material = crateDefinition.blockMaterial() == null ? Material.CHEST : crateDefinition.blockMaterial();
            block.setType(material, false);
        }

        @Override
        public void destroy() {
            if (location.getWorld() == null || previousType == null) {
                return;
            }
            Block block = location.getBlock();
            block.setType(previousType, false);
            if (previousData != null) {
                block.setBlockData(previousData, false);
            }
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
