package games.descent2e.pcg_clean.ui;

import games.descent2e.pcg_clean.data.TileCatalog;
import games.descent2e.pcg_clean.data.TileCatalogLoader;
import games.descent2e.pcg_clean.mapelites.GraphStructureDescriptor;
import games.descent2e.pcg_clean.spatial.SpatialConfig;
import games.descent2e.pcg_clean.spatial.SpatialMapElitesGenerator;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;

/** Live, interactive MAP-Elites demonstration entry point. */
public final class SpatialMapElitesViewerApplication {
    private SpatialMapElitesViewerApplication() {}

    public static void main(String[] args) throws IOException {
        SpatialRunSettings settings = SpatialRunSettings.parse(args);
        Path tilePath = settings.tilePath();
        TileCatalog catalog = new TileCatalogLoader().load(tilePath);
        SpatialConfig config = settings.toConfig();
        GraphStructureDescriptor descriptor = new GraphStructureDescriptor();
        TileArtworkProvider artwork = new FileTileArtworkProvider(tilePath.resolveSibling("img").resolve("tiles"));
        MapElitesHeatmapPanel heatmap = new MapElitesHeatmapPanel(descriptor, catalog, artwork);

        SwingUtilities.invokeLater(() -> showWindow(heatmap));
        System.out.println("Spatial MAP-Elites generator: " + settings.description());
        Thread evolution = new Thread(() -> {
            try {
                var result = new SpatialMapElitesGenerator(catalog).generate(config, descriptor, heatmap::submit);
                System.out.printf("Complete: evaluated %,d placements; occupied %d / %d niches%n",
                        result.archive().evaluations(), result.archive().elites().size(),
                        descriptor.branchingBins() * descriptor.cycleBins());
            } catch (RuntimeException error) {
                error.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(heatmap, error.getMessage(),
                        "MAP-Elites failed", JOptionPane.ERROR_MESSAGE));
            }
        }, "descent-map-elites");
        evolution.setDaemon(true);
        evolution.start();
    }

    private static void showWindow(MapElitesHeatmapPanel heatmap) {
        JFrame frame = new JFrame("Descent MAP-Elites — live behavioral archive");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(heatmap, BorderLayout.CENTER);
        frame.pack();
        frame.setMinimumSize(new Dimension(700, 520));
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }
}
