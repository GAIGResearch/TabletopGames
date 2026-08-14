package games.descent2e.pcg_clean.ui;

import games.descent2e.pcg_clean.data.*;
import games.descent2e.pcg_clean.spatial.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class SpatialBoardViewerApplication {
    private SpatialBoardViewerApplication() {}

    public static void main(String[] args) throws IOException {
        SpatialRunSettings settings = SpatialRunSettings.parse(args);
        Path tilePath = settings.tilePath();
        TileCatalog catalog = new TileCatalogLoader().load(tilePath);
        SpatialConfig config = settings.toConfig();
        System.out.println("Spatial board generator: " + settings.description());
        SpatialEvolutionResult result = new SpatialBoardGenerator(catalog).generate(config);
        List<BoardViewModel> boards = result.viewingArchive().stream().limit(20)
                .map(candidate -> BoardViewModel.from(candidate, catalog)).toList();
        TileArtworkProvider artwork = new FileTileArtworkProvider(tilePath.resolveSibling("img").resolve("tiles"));
        BoardViewer.show(boards, artwork);
        EvolutionHistoryPanel.showInWindow(result.history());
        System.out.printf("Complete: evaluated %,d placements; Pareto front: %d; minimum violations: %d%n",
                result.evaluations(), result.paretoFront().size(),
                result.finalPopulation().stream().mapToInt(c -> c.evaluation().violationCount()).min().orElse(0));
    }
}
