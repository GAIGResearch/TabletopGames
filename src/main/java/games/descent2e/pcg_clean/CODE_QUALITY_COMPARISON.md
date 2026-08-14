# Descent PCG Code-Quality Comparison

## Scope and method

This report compares:

- the ten Java files directly in `games.descent2e.pcg` (the same scope used by the original review); and
- all Java files recursively under the standalone `games.descent2e.pcg_clean` package.

Cyclomatic complexity (CC) and non-comment lines of code (NLOC) were measured with Lizard 1.23.0 in Java mode. Physical lines were counted with `wc -l`. Tests are discussed separately and are not included in either production-code total. The measurements were taken on 13 August 2026 from the working-tree versions, including the live MAP-Elites UI and the latest topology-aware repairs.

Lizard reports compact constructors and some methods against an enclosing record name, so names such as `PortRef::closeOneLoop` below are really private methods of `SpatialRepairOperator`. This affects presentation, not the measured CC or NLOC.

As a rough interpretation:

- CC 1–5 is normally straightforward;
- CC 6–10 deserves some care;
- CC above 10 is a refactoring signal; and
- CC above 20 is a strong refactoring candidate.

## Headline comparison

| Metric | Original `pcg` | `pcg_clean` | Change |
|---|---:|---:|---:|
| Java source files | 10 | 83 | +730% |
| Declared types, including nested types | 14 | 84 | +500% |
| Physical source lines | 4,096 | 3,429 | **−16.3%** |
| Lizard NLOC | 3,327 | 2,923 | **−12.1%** |
| Methods | 90 | 271 | +201.1% |
| Average method NLOC | 33.3 | 7.6 | **−77.2%** |
| Average method CC | 8.81 | 2.29 | **−74.0%** |
| Median method CC | 4 | 1 | **−75.0%** |
| Maximum method CC | 94 | 10 | **−89.4%** |
| Methods with CC > 10 | 17 | 0 | **−100%** |
| Methods with CC > 15 | 12 | 0 | **−100%** |
| Methods with CC > 20 | 11 | 0 | **−100%** |
| Methods over 50 NLOC | 17 | 0 | **−100%** |
| Total method CC | 793 | 621 | **−21.7%** |

The clean implementation therefore does not obtain its decomposition merely by spreading the same code over more files. It contains 404 fewer non-comment lines while supporting more explicit functionality: a typed standalone domain, graph-first generation, absolute-coordinate NSGA-II generation, MAP-Elites, multiple repair operators, deterministic runs, artwork rendering, progress charts and a live clickable heatmap.

The increase from 90 to 271 methods alongside a reduction in total CC is particularly revealing. Responsibilities have been divided into smaller named operations; there are about three times as many methods, but fewer control-flow decisions overall.

## Worst complexity offenders

### Original package

| CC | NLOC | Method |
|---:|---:|---|
| 94 | 331 | `CreateOffspring.addEndcaps()` |
| 62 | 98 | `FitnessFunction.addConnectionsAtOpeningOnSide()` |
| 52 | 75 | `FitnessFunction.findOpenings()` |
| 45 | 111 | `CreateOffspring.mutatePositions()` |
| 39 | 121 | `FitnessFunction.addTilesToBoard()` |
| 36 | 85 | `FitnessFunction.legalSpawns()` |
| 35 | 173 | `CreateOffspring.createOffspring()` |
| 29 | 110 | `CreateOffspring.assembleBoard()` |
| 29 | 53 | `FitnessFunction.consistency()` |
| 26 | 83 | `CreateOffspring.crossoverMutate()` |
| 24 | 66 | `CreateOffspring.rotateMutate()` |

### Clean package

| CC | NLOC | Method |
|---:|---:|---|
| 10 | 22 | `SpatialRepairOperator.attachmentProposals()` |
| 9 | 25 | `LoopCloser.closeAvailableLoops()` |
| 9 | 18 | `BoardLayoutEngine.render()` |
| 8 | 23 | `SpatialRepairOperator.componentTranslationProposals()` |
| 7 | 24 | `MapElitesEngine.run()` |
| 7 | 24 | `SpatialDecoder.inferConnections()` |
| 7 | 20 | `PortGraphGenomeFactory.findSecondConnections()` |

The two distributions are qualitatively different. In the original, eleven methods exceed CC 20 and seven exceed CC 30. In the clean package, no method exceeds the chosen threshold of CC 10. The clean maximum is almost one tenth of the original maximum of 94.

The loop-repair search has been decomposed into the following cooperating operations:

```text
LoopRepair
 ├── BridgePieceSelector
 ├── OpenPortPairFinder
 ├── BridgePlacementGenerator
 └── CycleImprovementPolicy
```

Proposal generation is now separate from decoding and improvement selection. This also removes duplicate decoding during loop scoring. The older graph-first bridge builder was similarly separated into candidate discovery, second-connection discovery and application.

## Lines of code and concentration

The original package's two largest files are:

| File | Physical lines |
|---|---:|
| `CreateOffspring.java` | 1,917 |
| `FitnessFunction.java` | 962 |

Together they contain 2,879 lines, or 70.3% of the entire original package. Both mix several concerns, so changing evolution, board assembly, repair, evaluation or storage commonly requires editing the same files.

The clean package's two largest files are:

| File | Physical lines |
|---|---:|
| `SpatialRepairOperator.java` | 355 |
| `SpatialEvolutionEngine.java` | 193 |

Together they contain 548 lines, or 16.0% of clean production code. No other file reaches 200 lines. The largest clean file is less than one fifth the size of `CreateOffspring`.

The current clean code is distributed by responsibility as follows:

| Area | Files | Physical lines | NLOC | Methods | Average CC |
|---|---:|---:|---:|---:|---:|
| Root composition | 1 | 59 | 51 | 4 | 1.50 |
| Data loading | 2 | 89 | 75 | 7 | 2.57 |
| Domain records/types | 9 | 141 | 114 | 15 | 2.13 |
| Evaluation and criteria | 17 | 328 | 267 | 34 | 1.74 |
| Graph-first evolution | 10 | 470 | 402 | 35 | 2.89 |
| Layout | 3 | 168 | 144 | 13 | 3.08 |
| MAP-Elites | 8 | 195 | 160 | 16 | 2.06 |
| Spatial generation | 20 | 1,064 | 918 | 78 | 2.59 |
| Swing UI | 13 | 915 | 792 | 69 | 1.83 |

This also puts the headline LOC result in context: 915 physical lines—26.7% of the clean package—are the richer demonstration UI. The algorithm, domain and evaluation layers still remain smaller in total than the legacy implementation.

## Architectural comparison

### Separation of concerns

The original `CreateOffspring` coordinates the evolutionary loop, selects parents, crosses and mutates candidates, repairs and assembles boards, stores every intermediate representation, manages MAP-Elites, exports JSON and launches UI behavior. `FitnessFunction` performs evaluation but also constructs grids, discovers connections and mutates shared storage.

The clean package instead has explicit boundaries:

- immutable domain records describe tiles, placements, ports, genomes and layouts;
- `SpatialDecoder` is the single genotype-to-phenotype boundary;
- criteria implement `FitnessCriterion` and remain independent of the EA;
- NSGA-II and MAP-Elites are separate engines sharing the same representation and evaluation;
- `BehaviorDescriptor` defines MAP-Elites dimensions independently of archive storage;
- repair changes chromosomes but does not change evaluation rules;
- immutable snapshots cross from the evolution thread to Swing; and
- rendering consumes `BoardViewModel` rather than reaching into the generator.

This is why the larger class count is beneficial. Most clean files represent a domain concept or one reason to change, rather than fragments arbitrarily extracted to reduce file length.

### Types and constants

The legacy package relies heavily on string keys and sentinels for fitness components, directions, candidate pools and component properties. A rough lexical count finds 649 string-literal occurrences in the ten legacy files versus 175 in the entire clean package. This is only an indicator—both counts include user-facing text—but is notable given that the clean package has eight times as many files and substantially more UI diagnostics.

The clean implementation replaces important strings with `Direction`, `Cell`, records such as `SpatialConfig`, `PieceGene`, `SpatialEvaluation` and `EliteCell`, and interfaces such as `Constraint`, `FitnessCriterion` and `BehaviorDescriptor`. Remaining strings are mainly JSON identifiers, diagnostic messages and UI labels.

### State, memory and concurrency

The original retains every candidate plus several rendered representations in global or long-lived collections. Its live memory therefore grows with total evaluations. The clean engines retain bounded populations and/or a small elite archive; rendered state is an evaluation product attached only to retained candidates. MAP-Elites has at most 30 current elites for its present descriptor.

The original generation work is launched on Swing's event thread. The clean live MAP-Elites application runs evolution on a worker thread and publishes immutable, coalesced snapshots to Swing. UI repaint timing cannot consume random numbers or alter the fixed-seed result.

### Testability and reproducibility

The original package has mixed random sources and global mutable inputs. The clean algorithms accept explicit configuration, create one named seeded `RandomGenerator`, preserve deterministic collection ordering, and have regression tests comparing complete fixed-seed results. Small interfaces allow criteria, descriptors and listeners to be tested independently.

## Remaining clean-code risks

The clean implementation is substantially better, but it is not finished:

1. **`SpatialRepairOperator` remains the largest algorithmic class.** Its methods are now individually below the threshold, but its repair phases could still become separate `SpatialRepairStrategy` implementations if they need to evolve independently.
2. **Two generator tracks coexist.** The graph-first `evolution` package and newer absolute-coordinate `spatial` package are useful experiments, but their intended status should be documented. If the former is no longer part of the demonstration, moving it to an examples or experimental namespace would reduce conceptual surface area.
3. **Fitness composition is currently fixed in a composition root.** The criteria themselves are extensible, but a typed `BoardGenerationProfile` could make weights, constraints and behavioral descriptors configurable without editing Java.
4. **Metrics are not yet enforced.** Adding a build check that rejects CC > 10 for new or modified code would prevent another repair monolith from developing.

## Conclusion

The clean package demonstrates that stronger decomposition did not cause code bloat. Despite growing from 10 files to 83 and adding considerably more functionality, it is 667 physical lines and 404 NLOC smaller. Average method complexity fell by 74%, every method above CC 10 was eliminated, and maximum method complexity fell from 94 to 10.

The results support the design assessment: the many classes mostly represent meaningful boundaries and make the system easier to understand, test, extend and run safely. The main qualification is that spatial repair has become the new concentration point. Extracting its repair phases behind a small strategy interface would bring the newest code back within the clean package's otherwise strong complexity profile.

## Reproduction

From the repository root, with Lizard 1.23.0 installed:

```bash
lizard -l java src/main/java/games/descent2e/pcg/*.java
lizard -l java src/main/java/games/descent2e/pcg_clean

find src/main/java/games/descent2e/pcg -maxdepth 1 -name '*.java' -print0 \
  | xargs -0 wc -l
find src/main/java/games/descent2e/pcg_clean -name '*.java' -print0 \
  | xargs -0 wc -l
```

CSV output (`lizard --csv`) was used to calculate medians and threshold counts.
