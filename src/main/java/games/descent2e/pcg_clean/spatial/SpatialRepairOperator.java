package games.descent2e.pcg_clean.spatial;

import games.descent2e.pcg_clean.data.TileCatalog;
import games.descent2e.pcg_clean.domain.*;
import games.descent2e.pcg_clean.layout.PortGeometry;

import java.util.*;
import java.util.random.RandomGenerator;

/** Directed feasibility repair; it changes only chromosome genes, never the evaluator. */
public final class SpatialRepairOperator {
    private static final int PROPOSAL_BUDGET = 12;
    private record PortRef(PlacedTile tile, int port) {}
    private record BridgePlacement(int pieceId, int face, int turns, PlacedTile tile,
                                   Port firstPort, Port secondPort) {}

    private final TileCatalog tiles;
    private final PhysicalPieceCatalog pieces;
    private final SpatialConfig config;
    private final SpatialDecoder decoder;

    public SpatialRepairOperator(TileCatalog tiles, PhysicalPieceCatalog pieces,
                                 SpatialConfig config, SpatialDecoder decoder) {
        this.tiles = tiles;
        this.pieces = pieces;
        this.config = config;
        this.decoder = decoder;
    }

    public SpatialChromosome repair(SpatialChromosome source, RandomGenerator random) {
        SpatialChromosome repaired = ensureRequiredSelections(source, random);
        for (int attempt = 0; attempt < 2; attempt++) {
            SpatialChromosome improvement = connectOneComponent(repaired, random);
            if (improvement.equals(repaired)) break;
            repaired = improvement;
        }
        if (config.repairMode() == RepairMode.TOPOLOGY) {
            repaired = closeOneLoop(repaired, random);
            repaired = completeOneOpenPort(repaired, random);
        }
        return repaired;
    }

    private SpatialChromosome ensureRequiredSelections(SpatialChromosome source, RandomGenerator random) {
        List<PieceGene> genes = new ArrayList<>(source.genes());
        selectRole(genes, "entrance");
        selectRole(genes, "exit");
        while (genes.stream().filter(PieceGene::selected).count() < config.initialSelectedPieces()) {
            List<Integer> available = new ArrayList<>();
            for (int i = 0; i < genes.size(); i++) if (!genes.get(i).selected()) available.add(i);
            if (available.isEmpty()) break;
            int index = available.get(random.nextInt(available.size()));
            genes.set(index, genes.get(index).withSelected(true));
        }
        return new SpatialChromosome(genes);
    }

    private void selectRole(List<PieceGene> genes, String role) {
        for (int i = 0; i < pieces.pieces().size(); i++) {
            if (pieces.pieces().get(i).id().startsWith(role)) {
                genes.set(i, genes.get(i).withSelected(true));
                return;
            }
        }
    }

    private SpatialChromosome connectOneComponent(SpatialChromosome chromosome, RandomGenerator random) {
        SpatialPhenotype phenotype = decoder.decode(chromosome);
        List<Set<Integer>> components = components(phenotype);
        if (components.size() <= 1) return chromosome;
        Set<Integer> main = components.stream().max(Comparator.comparingInt(Set::size)).orElseThrow();
        List<Set<Integer>> outside = components.stream().filter(component -> component != main).toList();
        List<SpatialChromosome> proposals = new ArrayList<>();
        Set<PortRef> open = openPorts(phenotype);
        for (Set<Integer> movingComponent : outside)
            for (int fixedId : main) for (int movingId : movingComponent)
                proposals.addAll(componentTranslationProposals(
                        chromosome, phenotype, fixedId, movingId, movingComponent, open));
        // A singleton may also change face and rotation, which is useful early in a run.
        outside.stream().filter(component -> component.size() == 1).forEach(component -> {
            int movingId = component.iterator().next();
            main.forEach(fixedId -> proposals.addAll(
                    attachmentProposals(chromosome, phenotype, fixedId, movingId, open)));
        });
        return bestImprovement(chromosome, phenotype, proposals, PROPOSAL_BUDGET, random);
    }

    private List<SpatialChromosome> attachmentProposals(SpatialChromosome chromosome, SpatialPhenotype phenotype,
                                                        int fixedId, int movingId, Set<PortRef> open) {
        PlacedTile fixed = tile(phenotype, fixedId);
        PieceGene fixedGene = chromosome.genes().get(fixedId);
        PhysicalPiece movingPiece = pieces.pieces().get(movingId);
        List<SpatialChromosome> result = new ArrayList<>();
        for (int face = 0; face < movingPiece.faces().size(); face++) for (int turns = 0; turns < 4; turns++) {
            PlacedTile moving = new PlacedTile(movingId, movingPiece.faces().get(face), turns);
            for (Port fixedPort : tiles.require(fixed.tileId()).rotate(fixed.quarterTurns()).ports())
                for (Port movingPort : tiles.require(moving.tileId()).rotate(turns).ports()) {
                    if (!open.contains(new PortRef(fixed, fixedPort.index()))) continue;
                    if (fixedPort.direction() != movingPort.direction().opposite()
                            || fixedPort.cells().size() != movingPort.cells().size()) continue;
                    GridPoint origin = attachedOrigin(fixedPort, new GridPoint(fixedGene.x(), fixedGene.y()), movingPort);
                    if (origin == null || !inRange(origin)) continue;
                    List<PieceGene> genes = new ArrayList<>(chromosome.genes());
                    genes.set(movingId, new PieceGene(true, face, origin.x(), origin.y(), turns));
                    result.add(new SpatialChromosome(genes));
                }
        }
        return result;
    }

    /** Translate a whole component, preserving all of its internal connections. */
    private List<SpatialChromosome> componentTranslationProposals(
            SpatialChromosome chromosome, SpatialPhenotype phenotype, int fixedId, int movingId,
            Set<Integer> movingComponent, Set<PortRef> open) {
        PlacedTile fixed = tile(phenotype, fixedId);
        PlacedTile moving = tile(phenotype, movingId);
        PieceGene fixedGene = chromosome.genes().get(fixedId);
        PieceGene movingGene = chromosome.genes().get(movingId);
        List<SpatialChromosome> result = new ArrayList<>();
        for (Port fixedPort : tiles.require(fixed.tileId()).rotate(fixed.quarterTurns()).ports())
            for (Port movingPort : tiles.require(moving.tileId()).rotate(moving.quarterTurns()).ports()) {
                if (!open.contains(new PortRef(fixed, fixedPort.index()))
                        || !open.contains(new PortRef(moving, movingPort.index()))) continue;
                if (fixedPort.direction() != movingPort.direction().opposite()
                        || fixedPort.cells().size() != movingPort.cells().size()) continue;
                GridPoint origin = attachedOrigin(fixedPort,
                        new GridPoint(fixedGene.x(), fixedGene.y()), movingPort);
                if (origin == null) continue;
                int dx = origin.x() - movingGene.x();
                int dy = origin.y() - movingGene.y();
                translateComponent(chromosome, movingComponent, dx, dy).ifPresent(result::add);
            }
        return result;
    }

    private Optional<SpatialChromosome> translateComponent(SpatialChromosome chromosome,
                                                            Set<Integer> component, int dx, int dy) {
        List<PieceGene> genes = new ArrayList<>(chromosome.genes());
        for (int id : component) {
            PieceGene gene = genes.get(id);
            GridPoint translated = new GridPoint(gene.x() + dx, gene.y() + dy);
            if (!inRange(translated)) return Optional.empty();
            genes.set(id, new PieceGene(gene.selected(), gene.face(), translated.x(), translated.y(),
                    gene.quarterTurns()));
        }
        return Optional.of(new SpatialChromosome(genes));
    }

    /** Uses the unique one-port end cap when it can strictly reduce violations. */
    private SpatialChromosome completeOneOpenPort(SpatialChromosome chromosome, RandomGenerator random) {
        int endCap = pieceWithPrefix("endcap");
        if (endCap < 0 || chromosome.genes().get(endCap).selected()) return chromosome;
        SpatialPhenotype phenotype = decoder.decode(chromosome);
        Set<PortRef> open = openPorts(phenotype);
        List<SpatialChromosome> proposals = new ArrayList<>();
        phenotype.genome().tiles().forEach(fixed -> proposals.addAll(attachmentProposals(
                chromosome, phenotype, fixed.instanceId(), endCap, open)));
        return bestImprovement(chromosome, phenotype, proposals, PROPOSAL_BUDGET, random);
    }

    private int pieceWithPrefix(String prefix) {
        for (int i = 0; i < pieces.pieces().size(); i++)
            if (pieces.pieces().get(i).id().startsWith(prefix)) return i;
        return -1;
    }

    /**
     * Adds one unused two-port piece only when it joins two exposed ports already in the
     * same component. That consumes two violations and increases the cyclomatic number,
     * rather than producing an accidental "cycle" through overlap or a multiply-used port.
     */
    private SpatialChromosome closeOneLoop(SpatialChromosome chromosome, RandomGenerator random) {
        SpatialPhenotype phenotype = decoder.decode(chromosome);
        Set<PortRef> open = openPorts(phenotype);
        if (open.size() < 2) return chromosome;
        List<SpatialChromosome> proposals = loopProposals(chromosome, phenotype, new ArrayList<>(open));
        return bestCycleImprovement(chromosome, phenotype, proposals, random);
    }

    private List<SpatialChromosome> loopProposals(SpatialChromosome chromosome,
                                                   SpatialPhenotype phenotype, List<PortRef> open) {
        List<SpatialChromosome> result = new ArrayList<>();
        for (int pieceId = 0; pieceId < pieces.pieces().size(); pieceId++) {
            if (chromosome.genes().get(pieceId).selected()) continue;
            result.addAll(loopProposalsForPiece(chromosome, phenotype, open, pieceId));
        }
        return result;
    }

    private List<SpatialChromosome> loopProposalsForPiece(SpatialChromosome chromosome,
                                                           SpatialPhenotype phenotype,
                                                           List<PortRef> open, int pieceId) {
        List<SpatialChromosome> result = new ArrayList<>();
        PhysicalPiece piece = pieces.pieces().get(pieceId);
        for (int face = 0; face < piece.faces().size(); face++) for (int turns = 0; turns < 4; turns++) {
            List<Port> ports = tiles.require(piece.faces().get(face)).rotate(turns).ports();
            if (ports.size() != 2) continue;
            result.addAll(loopProposalsForOrientation(chromosome, phenotype, open,
                    new BridgePlacement(pieceId, face, turns,
                            new PlacedTile(pieceId, piece.faces().get(face), turns), ports.get(0), ports.get(1))));
            result.addAll(loopProposalsForOrientation(chromosome, phenotype, open,
                    new BridgePlacement(pieceId, face, turns,
                            new PlacedTile(pieceId, piece.faces().get(face), turns), ports.get(1), ports.get(0))));
        }
        return result;
    }

    private List<SpatialChromosome> loopProposalsForOrientation(
            SpatialChromosome chromosome, SpatialPhenotype phenotype, List<PortRef> open,
            BridgePlacement bridge) {
        List<SpatialChromosome> result = new ArrayList<>();
        for (PortRef first : open) {
            GridPoint origin = bridgeOrigin(chromosome, first, bridge.firstPort);
            if (origin == null || !inRange(origin)) continue;
            open.stream().filter(second -> closesLoop(phenotype, first, second, bridge.secondPort, origin))
                    .map(second -> withBridge(chromosome, bridge, origin)).forEach(result::add);
        }
        return result;
    }

    private GridPoint bridgeOrigin(SpatialChromosome chromosome, PortRef target, Port bridgePort) {
        if (!compatible(port(target), bridgePort)) return null;
        PieceGene gene = chromosome.genes().get(target.tile.instanceId());
        return attachedOrigin(port(target), new GridPoint(gene.x(), gene.y()), bridgePort);
    }

    private boolean closesLoop(SpatialPhenotype phenotype, PortRef first, PortRef second,
                               Port bridgePort, GridPoint bridgeOrigin) {
        if (second.equals(first) || second.tile.instanceId() == first.tile.instanceId()
                || !compatible(port(second), bridgePort)) return false;
        GridPoint origin = phenotype.layout().origins().get(second.tile.instanceId());
        return PortGeometry.globalCells(bridgePort, bridgeOrigin).equals(
                PortGeometry.globalCells(port(second), origin))
                && sameComponent(phenotype, first.tile.instanceId(), second.tile.instanceId());
    }

    private SpatialChromosome withBridge(SpatialChromosome chromosome, BridgePlacement bridge, GridPoint origin) {
        List<PieceGene> genes = new ArrayList<>(chromosome.genes());
        genes.set(bridge.pieceId, new PieceGene(true, bridge.face, origin.x(), origin.y(), bridge.turns));
        return new SpatialChromosome(genes);
    }

    private SpatialChromosome bestCycleImprovement(SpatialChromosome original, SpatialPhenotype phenotype,
                                                    List<SpatialChromosome> proposals,
                                                    RandomGenerator random) {
        if (proposals.isEmpty()) return original;
        proposals = shuffledDistinct(proposals, PROPOSAL_BUDGET * 2, random);
        int currentCycles = cycleCount(phenotype);
        int currentViolations = phenotype.violations().size();
        Map<SpatialChromosome, SpatialPhenotype> valid = new LinkedHashMap<>();
        proposals.forEach(proposal -> {
            SpatialPhenotype repaired = decoder.decode(proposal);
            if (cycleCount(repaired) > currentCycles && repaired.violations().size() < currentViolations)
                valid.put(proposal, repaired);
        });
        int best = valid.values().stream().mapToInt(value -> value.violations().size()).min().orElse(currentViolations);
        List<SpatialChromosome> improvements = valid.entrySet().stream()
                .filter(entry -> entry.getValue().violations().size() == best).map(Map.Entry::getKey).toList();
        return improvements.isEmpty() ? original : improvements.get(random.nextInt(improvements.size()));
    }

    private Port port(PortRef ref) {
        return tiles.require(ref.tile.tileId()).rotate(ref.tile.quarterTurns()).ports().stream()
                .filter(port -> port.index() == ref.port).findFirst().orElseThrow();
    }

    private boolean compatible(Port first, Port second) {
        return first.direction() == second.direction().opposite()
                && first.cells().size() == second.cells().size();
    }

    private boolean sameComponent(SpatialPhenotype phenotype, int first, int second) {
        Set<Integer> reached = new LinkedHashSet<>();
        visit(first, phenotype, reached);
        return reached.contains(second);
    }

    private int cycleCount(SpatialPhenotype phenotype) {
        int vertices = phenotype.layout().graph().size();
        int edges = phenotype.layout().graph().values().stream().mapToInt(Set::size).sum() / 2;
        return Math.max(0, edges - vertices + components(phenotype).size());
    }

    private Set<PortRef> openPorts(SpatialPhenotype phenotype) {
        Set<PortRef> used = new HashSet<>();
        Map<Integer, PlacedTile> byId = new LinkedHashMap<>();
        phenotype.genome().tiles().forEach(tile -> byId.put(tile.instanceId(), tile));
        phenotype.genome().connections().forEach(edge -> {
            used.add(new PortRef(byId.get(edge.firstTile()), edge.firstPort()));
            used.add(new PortRef(byId.get(edge.secondTile()), edge.secondPort()));
        });
        Set<PortRef> open = new LinkedHashSet<>();
        phenotype.genome().tiles().forEach(tile -> tiles.require(tile.tileId()).rotate(tile.quarterTurns()).ports()
                .forEach(port -> {
                    PortRef ref = new PortRef(tile, port.index());
                    if (!used.contains(ref)) open.add(ref);
                }));
        return open;
    }

    private SpatialChromosome bestImprovement(SpatialChromosome original, SpatialPhenotype phenotype,
                                               List<SpatialChromosome> proposals, int budget,
                                               RandomGenerator random) {
        if (proposals.isEmpty()) return original;
        proposals = shuffledDistinct(proposals, budget, random);
        int current = phenotype.violations().size();
        Map<SpatialChromosome, Integer> scores = new LinkedHashMap<>();
        proposals.forEach(proposal -> scores.put(proposal, decoder.decode(proposal).violations().size()));
        int best = scores.values().stream().mapToInt(Integer::intValue).min().orElse(current);
        if (best >= current) return original;
        List<SpatialChromosome> improvements = scores.entrySet().stream()
                .filter(entry -> entry.getValue() == best).map(Map.Entry::getKey).toList();
        return improvements.get(random.nextInt(improvements.size()));
    }

    private List<SpatialChromosome> shuffledDistinct(List<SpatialChromosome> proposals, int budget,
                                                     RandomGenerator random) {
        List<SpatialChromosome> shuffled = new ArrayList<>(new LinkedHashSet<>(proposals));
        Collections.shuffle(shuffled, new Random(random.nextLong()));
        return shuffled.stream().limit(budget).toList();
    }

    private GridPoint attachedOrigin(Port fixed, GridPoint fixedOrigin, Port moving) {
        GridPoint target = fixed.cells().get(0).plus(fixedOrigin);
        for (GridPoint anchor : moving.cells()) {
            GridPoint candidate = new GridPoint(target.x() - anchor.x(), target.y() - anchor.y());
            if (PortGeometry.globalCells(fixed, fixedOrigin).equals(PortGeometry.globalCells(moving, candidate)))
                return candidate;
        }
        return null;
    }

    private boolean inRange(GridPoint origin) {
        return origin.x() >= config.minCoordinate() && origin.x() <= config.maxCoordinate()
                && origin.y() >= config.minCoordinate() && origin.y() <= config.maxCoordinate();
    }

    private PlacedTile tile(SpatialPhenotype phenotype, int id) {
        return phenotype.genome().tiles().stream().filter(tile -> tile.instanceId() == id).findFirst().orElseThrow();
    }

    private List<Set<Integer>> components(SpatialPhenotype phenotype) {
        Set<Integer> remaining = new LinkedHashSet<>(phenotype.layout().graph().keySet());
        List<Set<Integer>> result = new ArrayList<>();
        while (!remaining.isEmpty()) {
            Set<Integer> component = new LinkedHashSet<>();
            visit(remaining.iterator().next(), phenotype, component);
            remaining.removeAll(component);
            result.add(component);
        }
        return result;
    }

    private void visit(int id, SpatialPhenotype phenotype, Set<Integer> reached) {
        if (!reached.add(id)) return;
        phenotype.layout().graph().getOrDefault(id, Set.of()).forEach(next -> visit(next, phenotype, reached));
    }
}
