# Descent 2e Procedural Board Generator: Code Review

## Scope and method

This review covers the ten Java files in `games.descent2e.pcg`. The package contains approximately 4,100 physical lines and 3,327 non-comment source lines.

Cyclomatic complexity was measured with Lizard. Lizard found 90 methods, with an average cyclomatic complexity (CC) of 8.8. CC is not a complete measure of quality, but it is a useful indication of how many independent control-flow paths a method contains and, consequently, how difficult it will be to understand and test.

As a rough interpretation:

- CC 1–5 is normally straightforward.
- CC 6–10 deserves some care.
- CC above 10 is increasingly difficult to test thoroughly.
- CC above 20 is a strong refactoring candidate.

## Worst methods by cyclomatic complexity

| CC | NLOC | Method |
|---:|---:|---|
| 94 | 331 | `CreateOffspring.addEndcaps()` |
| 62 | 98 | `FitnessFunction.addConnectionsAtOpeningOnSide()` |
| 52 | 75 | `FitnessFunction.findOpenings()` |
| 45 | 111 | `CreateOffspring.mutatePositions()` |
| 39 | 121 | `FitnessFunction.addTilesToBoard()` |
| 36 | 85 | `FitnessFunction.legalSpawns()` |
| 35 | 173 | `CreateOffspring.createOffspring()` |
| 29 | 53 | `FitnessFunction.consistency()` |
| 29 | 110 | `CreateOffspring.assembleBoard()` |
| 26 | 83 | `CreateOffspring.crossoverMutate()` |
| 24 | 66 | `CreateOffspring.rotateMutate()` |
| 17 | 49 | `CreateOffspring.mutateTraits()` |
| 15 | 77 | `FitnessFunction.createBoard()` |
| 15 | 49 | `NumberField.propertyChange()` |
| 14 | 163 | `CreateOffspring.exportPCGToJSON()` |
| 13 | 55 | `CreateOffspring.feasibleParents()` |

`addEndcaps()` is the clearest outlier. At CC 94, comprehensive path testing is not realistic in its current form. Its directional cases should be expressed through shared direction-independent operations rather than separate north, south, east and west branches.

### Core-class summary

| Class | NLOC | Methods | Average CC |
|---|---:|---:|---:|
| `CreateOffspring` | 1,586 | 31 | 12.7 |
| `FitnessFunction` | 773 | 18 | 16.1 |
| `GenerateBoardsGUI` | 388 | 12 | 3.1 |

`CreateOffspring` and `FitnessFunction` contain about 71% of the package's non-comment code. Their high average CC indicates an architectural concentration of responsibilities, not just one or two unfortunate methods.

## Why the current implementation can exhaust the heap

The implementation retains every generated candidate, and retains several representations of it. `CreateOffspring` owns all of the following growing collections:

```java
Map<Integer, GridBoard> boards;
Map<Integer, Map<Integer, GridBoard>> boardTiles;
Map<Integer, int[][]> tileRefs;
Map<Integer, Map<String, Map<Vector2D, Vector2D>>> gridRefs;

List<Pair<Quest, GraphBoard>> feasible;
List<Pair<Quest, GraphBoard>> infeasible;
List<HashMap<String, Float>> feasibleFitness;
List<HashMap<String, Float>> infeasibleFitness;
List<Boolean> feasibleList;
```

For each candidate, `FitnessFunction.createBoard()` allocates an oversized `BoardNode[][]` and `int[][]`, creates trimmed copies, constructs a `GridBoard`, and stores the resulting grid and supporting maps in `CreateOffspring`. The original `Quest` and `GraphBoard` genome are also retained.

Consequently, live memory grows approximately linearly with the total number of candidates evaluated. The initial two-dimensional arrays also produce considerable temporary allocation and garbage-collection pressure.

MAP-Elites itself is not causing this growth. The two elite maps contain only one candidate ID and score for each occupied feature cell. The problem is that all candidates remain available behind those IDs, including candidates that are neither current elites nor needed as parents.

Increasing `-Xmx` would postpone the failure but would not address this scaling behaviour.

## How many well-represented boards could be retained?

The answer depends much more on representation than on the nominal 32×32 limit. There are 1,024 cells, but "1 KiB per board" is true only for something close to a flat `byte[1024]`. It is not true for a Java object graph containing an object per cell.

### Dense representations

Approximate raw payloads for a 32×32 board are:

| Representation | Raw cell payload | Comments |
|---|---:|---|
| Occupied/unoccupied bit set | 128 B | Records occupancy only; it cannot identify tiles or orientations. |
| Flat `byte[1024]` | 1 KiB | Up to 256 cell values; approximately 1.0–1.1 KiB including the array object. |
| Flat `short[1024]` | 2 KiB | Up to 65,536 values; approximately 2.0 KiB including the array object. |
| Flat `int[1024]` | 4 KiB | Approximately 4.0 KiB including the array object. |
| `byte[32][32]` | 1 KiB payload | More expensive than a flat array because it creates 33 array objects. |
| `Cell[32][32]` with cell objects | potentially tens of KiB | The reference grid alone is about 4 KiB with compressed references, before any `Cell` objects. |

These estimates assume a typical 64-bit HotSpot JVM with compressed ordinary object pointers. Exact values depend on JVM configuration and object alignment.

A practical board needs more than cell occupancy: dimensions, tile IDs, orientations, graph connections, quest data, fitness values and collection/index overhead must also be counted. A compact immutable candidate might therefore occupy 2–8 KiB even if its raw spatial grid is close to 1 KiB.

### Sparse or genome representations

Most Descent boards are sparse and are assembled from a relatively small number of physical tiles. Storing 1,024 cells is therefore not necessarily the best primary representation.

For example, a packed tile placement could record:

- tile type;
- orientation;
- x and y position;
- connection or parent information.

If that is packed into one or two integers per tile, a 20-tile board has only 80–160 bytes of raw placement data. In ordinary Java objects and collections it may occupy 0.5–2 KiB; in primitive flat arrays it can remain much closer to the raw size. The 32×32 cell grid can then be generated temporarily for validation or display and discarded.

This is not a particularly clever compression scheme: it is simply storing the natural genome rather than a rendered board. More specialised techniques—bit-packing, interning common layouts, off-heap storage, delta encoding or compression—could reduce it further, but should not be necessary for 26,000 candidates.

### Capacity estimates

The following estimates reserve 30% of the heap for the JVM, active generation, indexes, temporary evaluation data and garbage-collection headroom. They show retained-board capacity, not the theoretical maximum number of allocations that can be forced into the heap.

| Effective retained size per candidate | 1 GiB heap | 4 GiB heap | 8 GiB heap |
|---:|---:|---:|---:|
| 1.25 KiB | about 587,000 | about 2.35 million | about 4.70 million |
| 2 KiB | about 367,000 | about 1.47 million | about 2.94 million |
| 4 KiB | about 184,000 | about 734,000 | about 1.47 million |
| 8 KiB | about 92,000 | about 367,000 | about 734,000 |
| 32 KiB | about 23,000 | about 92,000 | about 184,000 |
| 100 KiB | about 7,300 | about 29,000 | about 59,000 |

The calculation is:

```text
capacity ≈ heap bytes × 0.70 / retained bytes per candidate
```

Thus, the naive 1 KiB intuition is reasonable for the raw payload of a flat byte grid, but a realistic compact Java candidate is more likely to be a few KiB. Even at 8 KiB each, retaining 26,000 candidates requires only about 203 MiB of candidate payload. At 32 KiB each it requires about 813 MiB. Both are manageable on a normal desktop JVM.

Conversely, the observation that the current program fails around 26,000 candidates is consistent with retaining object-heavy graphs, multiple grids and several maps per candidate. It does not imply that retaining 26,000 well-represented boards is intrinsically difficult.

These are design estimates rather than heap measurements. The correct follow-up would be to measure retained sizes with a heap dump, Java Flight Recorder, or an object-layout tool after a controlled run. It is important to measure *retained* size after a full GC, rather than allocation rate or shallow size alone.

## Performance scaling

`infeasibleParents()` scans the complete infeasible pool, recalculates a selection value for every member and inserts every result into a sorted `ArrayList`. This is approximately quadratic for one selection event, since insertion itself may scan and shift much of the list.

Suitable replacements include:

- fixed-size tournament selection;
- a bounded priority queue;
- weighted sampling without sorting the full pool;
- cached selection values that are updated only when necessary.

There are also repeated linear searches for tiles, boards and candidates by name. Immutable lookup maps should be prepared once rather than scanning collections inside evaluation and mutation loops.

## Class decomposition

`CreateOffspring` currently coordinates the evolutionary loop, selects parents, stores candidates, performs crossover, performs several kinds of mutation, repairs boards, assembles graph connections, manages MAP-Elites, writes JSON and launches a GUI.

`FitnessFunction` calculates fitness but also renders layouts, validates geometry, checks monster rules, discovers connections and stores rendering data back in `CreateOffspring`.

A better decomposition would be:

```text
EvolutionEngine
 ├── ParentSelector
 ├── OffspringFactory
 │    ├── BoardCrossover
 │    ├── BoardMutator
 │    └── QuestMutator
 ├── BoardRepairer
 ├── BoardAssembler
 ├── CandidateEvaluator
 │    ├── GeometryEvaluator
 │    ├── SpawnEvaluator
 │    └── FitnessCalculator
 ├── EliteArchive
 └── ResultWriter
```

A typed candidate would keep related values together and eliminate parallel lists:

```java
record Candidate(
        CandidateId id,
        Quest quest,
        GraphBoard board,
        FitnessResult fitness) {
}
```

The engine could then choose explicitly between two valid storage policies:

1. Keep only a bounded parent population and current elites.
2. Keep every compact candidate genome, but do not keep every rendered grid and evaluation workspace.

## Hard-coded strings and weak types

The core classes contain hundreds of identifier-like string literals. Important examples are parent-pool values (`"feasible"`, `"infeasible"`, `"null"`), fitness keys (`"Fitness"`, `"Geometry"`, `"Total Health"`), node-property keys, directions and special tile names.

Important domain concepts should become types rather than merely being moved to a constants file:

```java
enum ParentPool { ORIGINAL, FEASIBLE, INFEASIBLE }
enum Direction { NORTH, EAST, SOUTH, WEST }

record FitnessResult(
        boolean feasible,
        float fitness,
        int size,
        int groups,
        float totalHealth,
        float geometry,
        float consistency) {
}
```

`HashMap<String, Float>` permits misspelled keys, absent values and nonsensical combinations, while boxing every numeric value. The string `"null"` is also used as a sentinel; absence should instead be represented by an explicit state, `Optional`, or an empty connection slot.

## Other correctness and design concerns

### Parent-selection fallback is overwritten

In `begin()` the code contains:

```java
if (infeasible.size() < 2)
    parents = new Pair<>(infeasible.get(0).a, infeasible.get(0).a);
parents = feasibleParents();
```

The fallback assignment is always overwritten. It also accesses element zero when the collection may be empty. The condition may have been intended to inspect the feasible pool, or the second assignment may have been intended as an `else` branch.

### Randomised loops need attempt budgets

Several mutation and repair operations repeat until random changes satisfy a condition. Some have local limits, while others do not have a clear global limit. Each operation should accept an explicit attempt budget and return success or failure instead of being capable of running indefinitely.

### Shared tile templates appear mutable

Board construction obtains tiles from the global `GenerateBoards.tiles` collection and then calls methods such as `setProperty()` and `setComponentName()` on them. If these are the original templates rather than copies, evaluation is order-dependent, mutates global data and cannot safely be parallelised.

Templates should be immutable or copied before candidate-specific state is applied.

### The EA blocks Swing's event thread

The Generate button calls `CreateOffspring.begin()` directly from `actionPerformed()`. The GUI will remain unresponsive throughout a long run. The engine should execute in a `SwingWorker` or executor and publish progress through a small callback interface.

### JSON is constructed and parsed twice

`exportPCGToJSON()` manually constructs JSON strings, parses them with Jackson and then serialises them again. DTO records plus Jackson's streaming `JsonGenerator` would be safer and would allow results to be written incrementally.

### Global mutable state obscures dependencies

Loaded boards, quests, tiles and monsters are mutable static fields in `GenerateBoards` and are statically imported elsewhere. An immutable `DescentGenerationData` object should instead be supplied to the engine and evaluator through their constructors.

### Randomness is not reproducible

The implementation mixes `com.google.crypto.tink.subtle.Random` with newly created `java.util.Random` objects. A single seeded `RandomGenerator` should be injected into the engine and shared by the operators. This would make failures and experiments reproducible.

## Recommended order of work

1. Introduce a compact immutable candidate/genome type and decide explicitly what must be retained.
2. Stop retaining rendered `GridBoard`, `int[][]` and coordinate maps for every candidate; generate them temporarily or lazily.
3. Replace parallel candidate and fitness lists with typed candidate collections.
4. Separate the headless evolution engine from Swing and JSON output.
5. Extract board assembly, repair, mutation operators and evaluators into focused classes.
6. Replace fitness/property string keys and parent-pool strings with records and enums.
7. Replace full-pool infeasible sorting with bounded or sampled selection.
8. Rewrite `addEndcaps()` around generic directional operations.
9. Add deterministic, seeded unit tests for each operator and evaluator.
10. Add an automated complexity threshold: initially CC ≤ 15 for modified code, moving toward CC ≤ 10.

The first architectural goal should be to make rendered board state an evaluation product rather than the candidate's primary retained representation. After that change, retaining 26,000 candidates is entirely reasonable even if the project deliberately chooses not to use a bounded population.
