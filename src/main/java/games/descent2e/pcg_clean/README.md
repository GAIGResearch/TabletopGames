# Clean Descent board generator

This package is a standalone starting point for a Descent 2e evolutionary board generator. It deliberately has no dependency on `core`, `GraphBoard`, game state, Swing, or the old `pcg` package. Its only external dependency is Gson, used to read the existing `data/descent2e/tiles.json` file.

## Architecture

```text
data        JSON loading and immutable tile catalogue
domain      compact board genome and physical tile definitions
layout      deterministic genome-to-grid assembly and piece graph
evaluation  independent hard constraints and soft fitness criteria
evolution   bounded population, initialisation, mutation and selection
ui          Swing rendering and navigation, isolated from generation
```

The principal separation is between the board problem and the search algorithm:

- `BoardGenome` stores tile instances and explicit port connections. It does not store a rendered 64×64 grid.
- `BoardLayoutEngine` derives tile coordinates, atomic cells and an undirected piece graph.
- `Constraint` models hard feasibility rules and returns explanatory violations.
- `FitnessCriterion` models independently weighted soft objectives in `[0,1]`.
- `EvolutionEngine` knows only how to initialise, select, mutate, evaluate and truncate a population. It contains no Descent-specific rule.
- `DescentBoardGenerator` is the composition root where the chosen constraints and objectives are assembled.

Data transferred between stages uses immutable records. Collections are defensively copied at record boundaries.

## Implemented rules

This first version implements:

- tiles loaded directly from the existing JSON atomic-cell grids;
- connection ports discovered as contiguous `open` cells on tile boundaries;
- rotation of variable-sized pieces and ports;
- a connected explicit piece graph;
- geometry-aware loop closure between coincident unused ports;
- geometric assembly by matching opposite equal-width ports;
- detection of unreachable pieces and non-opening overlaps;
- exactly one entrance and one exit;
- a configurable maximum board extent;
- soft objectives for traversable cell count, grid compactness, graph branching,
  graph cycles, entrance-to-exit distance, and terrain composition;
- a bounded, seeded, mutation-based EA with tournament selection.

## Intentional extension points

The next rules can be added as classes implementing `Constraint` or `FitnessCriterion`, without changing the EA:

- atomic-cell walkability connectivity, distinct from piece-graph connectivity;
- unused/open-port rules and automatic end-cap repair;
- physical component inventory (including mutually exclusive A/B faces);
- minimum entrance-to-exit path length;
- dead-end and graph-diameter objectives;
- terrain balance and inaccessible terrain checks;
- monster groups, spawn footprints, quest rules and Act-specific limits.

The current mutation replaces a non-terminal tile with another tile that preserves all of its incident port contracts. Further operators can implement graph rewiring, subtree replacement, insertion/deletion, and crossover behind `MutationOperator` or a future `CrossoverOperator` interface.

## Running

Run `games.descent2e.pcg_clean.DescentBoardGenerator`. An optional first argument supplies a different tile JSON path. The default seed is fixed, so results are reproducible.

The demonstration configuration uses a population of 50, 100 generations, ten offspring per generation, and ten pieces per board. Memory is bounded by the population size: evaluated candidates that do not survive truncation are not retained.

Survivor selection keeps the best copy of each distinct `BoardGenome` before allowing duplicate genomes to fill any remaining population slots. This is a minimal diversity policy: it prevents clones from crowding the viewer and breeding population without mixing visual concerns into fitness. More sophisticated novelty or quality-diversity selection can be introduced separately later.

Generation is deterministic for a fixed seed. Collections that participate in random choice or layout use explicit stable ordering; do not replace them with unspecified-order `HashMap`/`HashSet` iteration. Supplying a newly sampled seed is the intended way to request a different run.

### Visual viewer

Run `games.descent2e.pcg_clean.ui.GeneratedBoardViewerApplication` to generate a population and browse 20 candidates from its bounded viewing archive in a Swing window. An optional first argument supplies the tile JSON path.

The viewing archive is separate from survivor selection. It retains at most 200 candidates, with one representative for each fitness value at six-decimal precision. This makes the viewer useful for inspecting how board properties affect fitness instead of showing only a converged final plateau.

The initial renderer includes:

- terrain-coloured atomic grid cells;
- cached, rotated artwork from `data/descent2e/img/tiles`;
- coloured physical-piece boundaries and piece IDs;
- an optional overlay of the piece-connection graph;
- automatic zoom-to-fit;
- previous/next navigation;
- prominent aggregate fitness and feasibility;
- a per-criterion table showing score, weight and weighted contribution;
- size and constraint diagnostics.

`BoardViewModel` is the immutable boundary between the generator and Swing. `BoardCanvas` only paints a supplied view model, while `BoardViewer` owns window lifecycle and navigation. This permits later features—port markers, path highlighting or export to PNG—to be added without changing the EA.

Tile artwork is supplied through `TileArtworkProvider`. `FileTileArtworkProvider` performs case-insensitive PNG lookup (needed because the legacy assets use mixed filename case), strips generated instance suffixes, rotates images, and caches both hits and misses. Artwork is drawn over a piece's occupied terrain rectangle rather than its outer `open` connector cells, matching the dimensions used by the original viewer. Missing artwork falls back cleanly to terrain-coloured cells.

## Framework integration

No framework objects should be introduced into these classes. Once the representation and rules are mature, add a separate adapter such as `GraphBoardAdapter` that translates a feasible `BoardGenome`/`BoardLayout` into Tabletop Games components.

## Alternative absolute-coordinate generator

The `spatial` package contains a second, independent evolutionary representation. It does not encode a connection graph. Instead, the chromosome has one fixed gene for every physical Descent component:

```text
selected, face, x, y, quarterTurns
```

The physical catalogue groups mutually exclusive A/B JSON entries into one piece gene. The decoder places selected variable-sized pieces on the atomic grid, infers connections from coincident opposite ports, derives the piece graph, and reports overlaps, unconnected ports, disconnected components, missing entrance/exit pieces, and out-of-range cells.

`SpatialEvolutionEngine` uses two-objective NSGA-II-style selection:

1. minimise the number of constraint violations;
2. maximise the weighted quality score.

Mutation toggles selection, changes face or rotation, or changes one coordinate. Coordinate step size is geometrically distributed, so single-cell changes are common and progressively larger moves are less likely. Uniform crossover chooses the complete state of every physical piece independently from either parent.

Selected candidates pass through a directed `SpatialRepairOperator`. Repair preserves the absolute-coordinate genotype while:

- forcing entrance and exit pieces to remain selected;
- restoring the configured minimum piece count after destructive mutation/crossover;
- identifying disconnected components in the inferred graph;
- trying compatible face, rotation and coordinate placements that attach one outside piece to the largest component;
- accepting only attachment changes that reduce the total violation count.

Repair is computationally bounded: it performs at most two component-attachment rounds and samples at most twelve compatible placements in each round. It is applied to the initial population, 20% of NSGA-II offspring and 35% of MAP-Elites emissions; ordinary mutation/crossover remains available to escape the repair operator's local preferences. This provides directed pressure without embedding an unbounded local optimiser inside every EA evaluation.

Two repair modes are available. `basic` is the default and performs required-piece and component-joining repair. The optional `topology` mode additionally runs a topology-aware loop closer and end-cap completion. The loop closer considers unused physical pieces with exactly two ports and places one only when its ports simultaneously match two exposed ports in the same connected component. The accepted move must strictly reduce violations and increase the graph's cyclomatic number (`edges - vertices + components`), so overlaps and multiply-used ports cannot masquerade as loops. These searches have fixed proposal budgets but cost extra phenotype decodes, so they are deliberately opt-in for long runs.

The minimum-piece rule contributes one violation for every missing piece. This prevents the optimiser from treating a one-piece board as preferable to a promising, nearly connected multi-piece board.

Run `games.descent2e.pcg_clean.ui.SpatialBoardViewerApplication` to launch this version. It opens both the board browser and a Java2D progress window containing:

- minimum and mean constraint violations per generation;
- best fitness among the least-violating candidates and mean population fitness.

The board viewer also includes the candidate's violation count and full diagnostics. Evolution history is returned as immutable `GenerationStatistics`, so CSV or other chart exporters can be added without changing the EA.

The application's experiment defaults live in `SpatialRunSettings` and are printed at startup, including the total evaluation count. They are deliberately easy to edit while tuning the demonstration. Avoid combining a tiny population with a high per-gene mutation rate: `population=10` and `mutation=0.5`, for example, destroys roughly half of every chromosome while retaining very little genetic diversity.

For a quicker exploratory run (with a lower chance of reaching a feasible board), use `--population 80 --generations 100 --offspring 40`.

Settings may be overridden explicitly:

```text
--seed 12345
--population 100
--generations 150
--offspring 50
--pieces 12
--mutation 0.06
--repair topology
```

Use `--repair basic` for faster experiments (also the default), or `--repair topology` when deliberate loop closure and exposed-port completion justify the extra evaluation cost. The chosen mode is included in the startup configuration line.

The optional non-flag argument remains the path to `tiles.json`. The application prints the complete effective configuration and evaluation count before starting, making stale builds and accidental parameter changes visible.

## Live MAP-Elites demonstration

Run `games.descent2e.pcg_clean.ui.SpatialMapElitesViewerApplication` for the diversity-oriented generator. It maintains a 6 by 5 behavioral archive whose axes are the number of branching pieces (graph degree at least three) and the number of independent graph cycles. These dimensions expose topology rather than simply sorting boards by size.

Each niche retains the board with the fewest constraint violations, breaking ties by weighted fitness. The live heatmap uses the perceptually uniform Matplotlib `viridis` colour scheme; colour represents `fitness / (1 + violations)`, while each occupied cell also displays both raw values. Click an occupied cell to open an immutable snapshot of its current elite in an independent board window. Later clicks may show a replacement elite as evolution progresses; closing a board window does not close the heatmap or other boards.

MAP-Elites predominantly uses a mutation emitter: it samples one archived elite and applies spatial mutation. Ten percent of emissions use uniform crossover and five percent use a random restart for exploration; using crossover on every child independently mixes absolute-position genes and normally destroys both co-adapted layouts. The mutation operator always makes at least one edit, so a configured rate such as `0.01` means a predominantly local one-gene search rather than no mutation.

Archive snapshots cross from the evolution thread to Swing through a coalescing update slot. Painting and user interaction therefore neither consume random numbers nor change the deterministic outcome for a fixed seed.
