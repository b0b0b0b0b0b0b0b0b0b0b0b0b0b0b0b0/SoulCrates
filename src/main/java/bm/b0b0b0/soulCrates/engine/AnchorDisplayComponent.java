package bm.b0b0b0.soulCrates.engine;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class AnchorDisplayComponent implements DisplayComponent {

    private final Location anchor;

    public AnchorDisplayComponent(Location anchor) {
        this.anchor = anchor.clone();
    }

    @Override
    public void create() {
    }

    @Override
    public void destroy() {
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
        return anchor.clone();
    }
}
