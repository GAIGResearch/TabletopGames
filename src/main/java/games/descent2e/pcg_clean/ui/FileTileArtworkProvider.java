package games.descent2e.pcg_clean.ui;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Case-insensitive PNG lookup plus decoding/rotation cache for Descent tile artwork. */
public final class FileTileArtworkProvider implements TileArtworkProvider {
    private record ArtworkKey(String tileId, int quarterTurns) {}

    private final Map<String, Path> imagesByLowerCaseName;
    private final Map<ArtworkKey, Optional<BufferedImage>> cache = new ConcurrentHashMap<>();

    public FileTileArtworkProvider(Path imageDirectory) throws IOException {
        if (!Files.isDirectory(imageDirectory))
            throw new IllegalArgumentException("Not a tile image directory: " + imageDirectory);
        try (var files = Files.list(imageDirectory)) {
            imagesByLowerCaseName = files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .collect(java.util.stream.Collectors.toUnmodifiableMap(
                            path -> path.getFileName().toString().toLowerCase(Locale.ROOT), path -> path,
                            (first, ignored) -> first));
        }
    }

    @Override
    public Optional<BufferedImage> artwork(String tileId, int quarterTurns) {
        ArtworkKey key = new ArtworkKey(baseTileId(tileId).toLowerCase(Locale.ROOT), Math.floorMod(quarterTurns, 4));
        return cache.computeIfAbsent(key, this::loadAndRotate);
    }

    private Optional<BufferedImage> loadAndRotate(ArtworkKey key) {
        Path path = imagesByLowerCaseName.get(key.tileId + ".png");
        if (path == null) return Optional.empty();
        try {
            BufferedImage source = ImageIO.read(path.toFile());
            if (source == null) return Optional.empty();
            return Optional.of(rotateClockwise(source, key.quarterTurns));
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot load tile artwork " + path, exception);
        }
    }

    private String baseTileId(String tileId) {
        int generatedSuffix = tileId.indexOf('-');
        return generatedSuffix < 0 ? tileId : tileId.substring(0, generatedSuffix);
    }

    static BufferedImage rotateClockwise(BufferedImage source, int quarterTurns) {
        int turns = Math.floorMod(quarterTurns, 4);
        if (turns == 0) return source;
        int width = turns % 2 == 0 ? source.getWidth() : source.getHeight();
        int height = turns % 2 == 0 ? source.getHeight() : source.getWidth();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            switch (turns) {
                case 1 -> { g.translate(width, 0); g.rotate(Math.PI / 2); }
                case 2 -> { g.translate(width, height); g.rotate(Math.PI); }
                case 3 -> { g.translate(0, height); g.rotate(-Math.PI / 2); }
                default -> throw new IllegalStateException();
            }
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return result;
    }
}
