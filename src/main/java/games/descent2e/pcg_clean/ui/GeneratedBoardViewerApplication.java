package games.descent2e.pcg_clean.ui;

import games.descent2e.pcg_clean.DescentBoardGenerator;
import games.descent2e.pcg_clean.data.*;
import games.descent2e.pcg_clean.evolution.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Standalone visual demo; generation remains usable without loading Swing classes. */
public final class GeneratedBoardViewerApplication {
    private GeneratedBoardViewerApplication() {}

    public static void main(String[] args) throws IOException {
        Path tilePath = Path.of(args.length > 0 ? args[0] : "data/descent2e/tiles.json");
        TileCatalog catalog = new TileCatalogLoader().load(tilePath);
        GeneratorConfig config = new GeneratorConfig(50, 1000,
                10, 10, 4, 202601L);
        EvolutionResult result = new DescentBoardGenerator(catalog).generate(config);
        List<BoardViewModel> models = result.viewingArchive().stream()
                .limit(20)
                .map(candidate -> BoardViewModel.from(candidate, catalog))
                .toList();
        TileArtworkProvider artwork = new FileTileArtworkProvider(tilePath.resolveSibling("img").resolve("tiles"));
        BoardViewer.show(models, artwork);
    }
}
