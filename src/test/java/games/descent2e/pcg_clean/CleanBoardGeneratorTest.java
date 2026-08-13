package games.descent2e.pcg_clean;

import games.descent2e.pcg_clean.data.*;
import games.descent2e.pcg_clean.domain.*;
import games.descent2e.pcg_clean.evaluation.*;
import games.descent2e.pcg_clean.evaluation.constraints.*;
import games.descent2e.pcg_clean.evaluation.criteria.*;
import games.descent2e.pcg_clean.evolution.*;
import games.descent2e.pcg_clean.layout.BoardLayout;
import games.descent2e.pcg_clean.layout.BoardLayoutEngine;
import games.descent2e.pcg_clean.mapelites.*;
import games.descent2e.pcg_clean.spatial.*;
import games.descent2e.pcg_clean.ui.*;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.util.Optional;
import java.nio.file.Path;
import java.util.List;
import java.util.random.RandomGeneratorFactory;

import static org.junit.Assert.*;

public class CleanBoardGeneratorTest {
    private final Path tiles = Path.of("data/descent2e/tiles.json");

    @Test
    public void loadsVariableSizedTilesAndDiscoversPorts() throws Exception {
        TileCatalog catalog = new TileCatalogLoader().load(tiles);
        TileDefinition tile = catalog.require("1A");
        assertEquals(10, tile.width());
        assertEquals(8, tile.height());
        assertEquals(3, tile.ports().size());
        assertEquals(2, tile.ports().get(0).cells().size());
    }

    @Test
    public void rotationPreservesCellsAndRotatesPorts() throws Exception {
        TileDefinition tile = new TileCatalogLoader().load(tiles).require("1A");
        RotatedTile rotated = tile.rotate(1);
        assertEquals(tile.height(), rotated.width());
        assertEquals(tile.width(), rotated.height());
        assertEquals(tile.ports().size(), rotated.ports().size());
        assertEquals(tile.ports().get(0).direction().rotate(1), rotated.ports().get(0).direction());
    }

    @Test
    public void factoryCreatesConnectedEntranceToExitGenome() throws Exception {
        TileCatalog catalog = new TileCatalogLoader().load(tiles);
        BoardGenome genome = new PortGraphGenomeFactory(catalog).create(8,
                RandomGeneratorFactory.of("L64X128MixRandom").create(42));
        assertEquals(8, genome.tiles().size());
        assertEquals(7, genome.connections().size());
        Evaluation evaluation = new BoardEvaluator(catalog, List.of(
                new ConnectedPieceGraphConstraint(),
                new RequiredTileRoleConstraint("entrance", 1),
                new RequiredTileRoleConstraint("exit", 1)), List.of()).evaluate(genome);
        assertTrue(evaluation.feasible());
    }

    @Test
    public void layoutFindsDisconnectedPiece() throws Exception {
        TileCatalog catalog = new TileCatalogLoader().load(tiles);
        BoardGenome genome = new BoardGenome(List.of(
                new PlacedTile(0, "entrance1A", 0),
                new PlacedTile(1, "exit1A", 0)), List.of());
        BoardLayout layout = new BoardLayoutEngine(catalog).layout(genome);
        assertFalse(layout.assembled());
        assertEquals(1, layout.origins().size());
    }

    @Test
    public void evolutionIsBoundedAndSeeded() throws Exception {
        TileCatalog catalog = new TileCatalogLoader().load(tiles);
        GeneratorConfig config = new GeneratorConfig(8, 2, 3, 6, 3, 99);
        BoardEvaluator evaluator = new BoardEvaluator(catalog,
                List.of(new ConnectedPieceGraphConstraint()), List.of());
        EvolutionResult result = new EvolutionEngine(config, new PortGraphGenomeFactory(catalog),
                new CompatibleTileMutation(catalog), evaluator).run();
        assertEquals(8, result.finalPopulation().size());
        assertEquals(14, result.evaluations());
        assertTrue(result.best().evaluation().feasible());
        assertTrue(result.finalPopulation().stream().map(Candidate::genome).distinct().count() > 1);
    }

    @Test
    public void boardCanvasCanRenderWithoutCreatingAWindow() throws Exception {
        TileCatalog catalog = new TileCatalogLoader().load(tiles);
        GeneratorConfig config = new GeneratorConfig(4, 0, 1, 5, 2, 123);
        BoardEvaluator evaluator = new BoardEvaluator(catalog,
                List.of(new ConnectedPieceGraphConstraint()), List.of());
        Candidate candidate = new EvolutionEngine(config, new PortGraphGenomeFactory(catalog),
                new CompatibleTileMutation(catalog), evaluator).run().best();
        BoardViewModel model = BoardViewModel.from(candidate, catalog);
        BoardCanvas canvas = new BoardCanvas(BoardPalette.defaultPalette());
        canvas.setSize(640, 480);
        canvas.display(model);
        BufferedImage image = new BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB);
        canvas.paint(image.getGraphics());
        assertTrue(model.summary().contains("Candidate"));
        assertEquals(candidate.evaluation().fitness(), model.fitness(), 0.000001);
        assertEquals(candidate.evaluation().criteria().size(), model.criteria().size());
        assertNotEquals(0, image.getRGB(320, 240));
    }

    @Test
    public void artworkProviderFindsMixedCaseNamesAndRotatesImages() throws Exception {
        FileTileArtworkProvider artwork = new FileTileArtworkProvider(Path.of("data/descent2e/img/tiles"));
        Optional<BufferedImage> original = artwork.artwork("1A", 0);
        Optional<BufferedImage> rotated = artwork.artwork("1A", 1);
        assertTrue(original.isPresent());
        assertTrue(rotated.isPresent());
        assertEquals(original.get().getWidth(), rotated.get().getHeight());
        assertEquals(original.get().getHeight(), rotated.get().getWidth());
        assertSame(rotated.get(), artwork.artwork("1A", 1).orElseThrow());
        assertTrue(artwork.artwork("tile-that-does-not-exist", 0).isEmpty());
    }

    @Test
    public void boardCanvasRendersTileArtwork() throws Exception {
        TileCatalog catalog = new TileCatalogLoader().load(tiles);
        GeneratorConfig config = new GeneratorConfig(4, 0, 1, 5, 2, 321);
        BoardEvaluator evaluator = new BoardEvaluator(catalog,
                List.of(new ConnectedPieceGraphConstraint()), List.of());
        Candidate candidate = new EvolutionEngine(config, new PortGraphGenomeFactory(catalog),
                new CompatibleTileMutation(catalog), evaluator).run().best();
        BoardCanvas canvas = new BoardCanvas(BoardPalette.defaultPalette(),
                new FileTileArtworkProvider(Path.of("data/descent2e/img/tiles")));
        canvas.setSize(640, 480);
        canvas.display(BoardViewModel.from(candidate, catalog));
        BufferedImage image = new BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB);
        canvas.paint(image.getGraphics());
        long distinctColours = java.util.Arrays.stream(
                image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()))
                .distinct().count();
        assertTrue("Artwork should introduce a rich colour range", distinctColours > 100);
    }

    @Test
    public void terrainCriterionDistinguishesDifferentTileFaces() throws Exception {
        TileCatalog catalog = new TileCatalogLoader().load(tiles);
        FitnessCriterion terrain = new TerrainCompositionCriterion(
                java.util.Map.of(Cell.WATER, 0.15, Cell.PIT, 0.05, Cell.BLOCK, 0.05), 1.0);
        BoardEvaluator evaluator = new BoardEvaluator(catalog, List.of(), List.of(terrain));
        BoardGenome water = new BoardGenome(List.of(new PlacedTile(0, "1A", 0)), List.of());
        BoardGenome blocked = new BoardGenome(List.of(new PlacedTile(0, "1B", 0)), List.of());
        assertNotEquals(evaluator.evaluate(water).fitness(), evaluator.evaluate(blocked).fitness(), 0.000001);
    }

    @Test
    public void configuredGeneratorProducesMoreThanOneFitnessValue() throws Exception {
        TileCatalog catalog = new TileCatalogLoader().load(tiles);
        EvolutionResult result = new DescentBoardGenerator(catalog).generate(
                new GeneratorConfig(20, 10, 5, 8, 3, 7654));
        long distinctFitness = result.viewingArchive().stream()
                .map(candidate -> Math.round(candidate.evaluation().fitness() * 1_000_000.0))
                .distinct().count();
        assertTrue("Viewing archive should not be a single fitness plateau", distinctFitness > 1);
        assertEquals(distinctFitness, result.viewingArchive().size());
    }

    @Test
    public void fixedSeedProducesIdenticalRuns() throws Exception {
        TileCatalog catalog = new TileCatalogLoader().load(tiles);
        GeneratorConfig config = new GeneratorConfig(12, 5, 4, 7, 3, 987654321L);
        EvolutionResult first = new DescentBoardGenerator(catalog).generate(config);
        EvolutionResult second = new DescentBoardGenerator(catalog).generate(config);
        assertEquals(first.finalPopulation(), second.finalPopulation());
        assertEquals(first.viewingArchive(), second.viewingArchive());
        assertEquals(first.evaluations(), second.evaluations());
    }

    @Test
    public void factoryCanCreateAValidPieceGraphLoop() throws Exception {
        TileCatalog catalog = new TileCatalogLoader().load(tiles);
        PortGraphGenomeFactory factory = new PortGraphGenomeFactory(catalog);
        var random = RandomGeneratorFactory.of("L64X128MixRandom").create(24680);
        boolean foundLoop = false;
        for (int attempt = 0; attempt < 300 && !foundLoop; attempt++) {
            BoardGenome genome = factory.create(12, random);
            foundLoop = genome.connections().size() >= genome.tiles().size();
            if (foundLoop) assertTrue(new BoardLayoutEngine(catalog).layout(genome).assembled());
        }
        assertTrue("Generator should be capable of physically valid cyclic piece graphs", foundLoop);
    }

    @Test
    public void physicalCatalogGroupsOppositeFaces() throws Exception {
        TileCatalog catalog = new TileCatalogLoader().load(tiles);
        PhysicalPieceCatalog physical = new PhysicalPieceCatalog(catalog);
        PhysicalPiece pieceOne = physical.pieces().stream().filter(piece -> piece.id().equals("1")).findFirst().orElseThrow();
        assertEquals(List.of("1A", "1B"), pieceOne.faces());
        assertTrue(physical.pieces().size() < catalog.all().size());
    }

    @Test
    public void uniformCrossoverTakesEachWholeGeneFromAParent() {
        SpatialChromosome first = new SpatialChromosome(List.of(
                new PieceGene(true, 0, 1, 2, 0), new PieceGene(false, 1, 3, 4, 1)));
        SpatialChromosome second = new SpatialChromosome(List.of(
                new PieceGene(false, 1, 8, 7, 2), new PieceGene(true, 0, 6, 5, 3)));
        SpatialChromosome child = new UniformPieceCrossover().cross(first, second,
                RandomGeneratorFactory.of("L64X128MixRandom").create(55));
        for (int i = 0; i < child.genes().size(); i++)
            assertTrue(child.genes().get(i).equals(first.genes().get(i))
                    || child.genes().get(i).equals(second.genes().get(i)));
    }

    @Test
    public void spatialEvolutionIsDeterministicAndRecordsProgress() throws Exception {
        TileCatalog catalog = new TileCatalogLoader().load(tiles);
        SpatialConfig config = new SpatialConfig(30, 15, 20, 3, -14, 14, 8, 0.10, 112233L);
        SpatialEvolutionResult first = new SpatialBoardGenerator(catalog).generate(config);
        SpatialEvolutionResult second = new SpatialBoardGenerator(catalog).generate(config);
        assertEquals(first.finalPopulation(), second.finalPopulation());
        assertEquals(config.generations() + 1, first.history().size());
        assertEquals(config.populationSize() + (long) config.generations() * config.offspringPerGeneration(),
                first.evaluations());
        assertTrue(first.history().get(first.history().size() - 1).minimumViolations()
                <= first.history().get(0).minimumViolations());
        assertTrue(first.viewingArchive().stream()
                .map(candidate -> candidate.evaluation().violationCount() + ":"
                        + Math.round(candidate.evaluation().quality().fitness() * 1_000_000))
                .distinct().count() > 1);
    }

    @Test
    public void spatialRepairRestoresMinimumSizeEntranceAndExit() throws Exception {
        TileCatalog catalog = new TileCatalogLoader().load(tiles);
        PhysicalPieceCatalog pieces = new PhysicalPieceCatalog(catalog);
        SpatialConfig config = new SpatialConfig(10, 0, 1, 2, -14, 14, 8, 0.1, 44);
        List<PieceGene> empty = pieces.pieces().stream()
                .map(piece -> new PieceGene(false, 0, 0, 0, 0)).toList();
        SpatialDecoder decoder = new SpatialDecoder(catalog, pieces, -14, 14, 8);
        SpatialChromosome repaired = new SpatialRepairOperator(catalog, pieces, config, decoder)
                .repair(new SpatialChromosome(empty),
                        RandomGeneratorFactory.of("L64X128MixRandom").create(44));
        SpatialPhenotype phenotype = decoder.decode(repaired);
        assertTrue(phenotype.genome().tiles().size() >= 8);
        assertTrue(phenotype.genome().tiles().stream()
                .anyMatch(tile -> tile.tileId().toLowerCase().startsWith("entrance")));
        assertTrue(phenotype.genome().tiles().stream()
                .anyMatch(tile -> tile.tileId().toLowerCase().startsWith("exit")));
    }

    @Test
    public void spatialApplicationSettingsAreNamedAndParseable() {
        SpatialRunSettings defaults = SpatialRunSettings.defaults();
        assertTrue(defaults.minimumPieces() > 0);
        assertEquals(defaults.population() + (long) defaults.generations() * defaults.offspring(),
                defaults.evaluations());
        assertEquals(RepairMode.BASIC, defaults.repairMode());
        SpatialRunSettings custom = SpatialRunSettings.parse(new String[]{
                "--seed", "77", "--population", "20", "--generations", "5",
                "--offspring", "6", "--pieces", "12", "--mutation", "0.04",
                "--repair", "topology"});
        assertEquals(77, custom.seed());
        assertEquals(50, custom.evaluations());
        assertEquals(12, custom.minimumPieces());
        assertEquals(0.04, custom.mutationRate(), 0.000001);
        assertEquals(RepairMode.TOPOLOGY, custom.repairMode());
    }

    @Test
    public void spatialEvolutionIsDeterministicForAFixedSeed() throws Exception {
        SpatialConfig config = new SpatialConfig(20, 8, 10, 3, -14, 14, 8, 0.08, 24680L);
        TileCatalog catalog = new TileCatalogLoader().load(tiles);
        SpatialEvolutionResult first = new SpatialBoardGenerator(catalog).generate(config);
        SpatialEvolutionResult second = new SpatialBoardGenerator(catalog).generate(config);

        assertEquals(first.history(), second.history());
        assertEquals(first.viewingArchive().stream().map(SpatialCandidate::chromosome).toList(),
                second.viewingArchive().stream().map(SpatialCandidate::chromosome).toList());
        assertEquals(first.viewingArchive().stream().map(SpatialCandidate::evaluation).toList(),
                second.viewingArchive().stream().map(SpatialCandidate::evaluation).toList());
    }

    @Test
    public void spatialViolationsUseTheSamePieceLabelsAsTheCanvas() throws Exception {
        TileCatalog catalog = new TileCatalogLoader().load(tiles);
        PhysicalPieceCatalog pieces = new PhysicalPieceCatalog(catalog);
        List<PieceGene> genes = pieces.pieces().stream()
                .map(piece -> new PieceGene(false, 0, 0, 0, 0)).collect(java.util.stream.Collectors.toList());
        for (int i = 0; i < pieces.pieces().size(); i++) {
            String id = pieces.pieces().get(i).id();
            if (id.startsWith("entrance")) genes.set(i, new PieceGene(true, 0, -10, -10, 0));
            if (id.startsWith("exit")) genes.set(i, new PieceGene(true, 0, 10, 10, 0));
        }
        SpatialPhenotype phenotype = new SpatialDecoder(catalog, pieces, -20, 20, 2)
                .decode(new SpatialChromosome(genes));
        List<String> pieceViolations = phenotype.violations().stream().filter(v -> v.contains("piece")).toList();
        assertFalse(pieceViolations.isEmpty());
        assertTrue(pieceViolations.stream().allMatch(v -> v.matches(".*\\d+:[^ ]+.*")));
    }

    @Test
    public void mapElitesIsDeterministicAndOccupiesStructuralNiches() throws Exception {
        TileCatalog catalog = new TileCatalogLoader().load(tiles);
        SpatialConfig config = new SpatialConfig(20, 8, 10, 3, -14, 14, 8, 0.10, 13579L);
        GraphStructureDescriptor descriptor = new GraphStructureDescriptor();
        MapElitesResult first = new SpatialMapElitesGenerator(catalog)
                .generate(config, descriptor, MapElitesListener.none());
        MapElitesResult second = new SpatialMapElitesGenerator(catalog)
                .generate(config, descriptor, MapElitesListener.none());
        assertEquals(first, second);
        assertEquals(config.populationSize() + (long) config.generations() * config.offspringPerGeneration(),
                first.archive().evaluations());
        assertTrue(first.archive().elites().size() > 1);
        assertTrue(first.archive().elites().keySet().stream().allMatch(cell ->
                cell.branchingBin() < descriptor.branchingBins() && cell.cycleBin() < descriptor.cycleBins()));
    }
}
