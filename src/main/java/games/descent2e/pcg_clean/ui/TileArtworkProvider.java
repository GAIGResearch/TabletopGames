package games.descent2e.pcg_clean.ui;

import java.awt.image.BufferedImage;
import java.util.Optional;

/** Supplies presentation assets without coupling the domain model to files or AWT images. */
public interface TileArtworkProvider {
    Optional<BufferedImage> artwork(String tileId, int quarterTurns);

    static TileArtworkProvider none() { return (tileId, quarterTurns) -> Optional.empty(); }
}
