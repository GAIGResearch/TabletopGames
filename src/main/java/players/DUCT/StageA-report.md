# Stage A — completion report

Stage A of folding decoupled UCT into `players.mcts.SingleTreeNode` (see `Readme.md` §8) is
complete: eight commits on `duct`, working tree clean, everything green from a clean rebuild.

Stage B — the `decoupled` parameter, joint actions, the backup fan-out and the gating — is
deliberately untouched.

## Commits

| Commit | Step | Content |
|---|---|---|
| `5f6182b6b` | Step 0 | `SGGameState.redeterminise` fix + two tests in `shuffleTests` |
| `97ecb94b9` | A1 | `StageAGoldenTests` — the characterisation baseline |
| `8eadcab33` | A2 | Fields → accessors; all five per-player fields made `private` |
| `5484e08fa` | A3 | `PlayerDecisionStats` + `statsByPlayer` |
| `1a2c355dd` | A4/A5 | Acting player threaded through the selection internals |
| `b0761a45d` | A6 | `updateActionStats` split out of `backUpSingleNode` |
| `8ec3b5ac5` | A7 | `soleActingPlayer` seam + `setActionsForPlayer` extraction |
| `f2d7a0fda`, `46e6d62ef` | — | `Readme.md` draft 3, then updated with what Stage A turned up |

A4 and A5 were planned as separate commits but collapse naturally into one: the overload bodies
*are* the threaded implementations, so splitting them would have meant writing each method twice.

## Verification

Run after **every** commit: 122 tests over 18 classes, plus `ForwardModelTestsWithMCTS`
`#testSushiGoWithSeqUCT` and `#testSushiGoWithDUCT`. The golden values captured at A1 never moved.
The final run was from a deleted-and-rebuilt `target/`, to rule out stale classes.

`mvn` is **not installed** on this machine — not on PATH in Bash or PowerShell, and not bundled with
the IntelliJ install. The build loop used instead was `javac` against the existing
`target/TAG.jar` plus the test-scope jars from `~/.m2`, with the classpath passed in a java argfile
(Git Bash mangles a `;`-separated `-cp`). Details are in the user-level memory note.

## Findings worth attention beyond this refactor

**No search over a shuffling game is reproducible between runs.** I had planned to pin exact search
behaviour on Dominion, and when that failed I first blamed enum hash codes. The real cause is that
`AbstractGameState.redeterminisationRnd` (`:107`) is unseeded, and `copy()` reseeds each copy's RNG
from it (`:354`). That is deliberate — hidden information must not be predictable from the game
seed — but it has two consequences: no test can pin exact search behaviour for a game that
shuffles, and **any MCTS-vs-DUCT comparison has to average over many games**, since a single seeded
game proves nothing. Recorded in `Readme.md` §11. LMR is pinned exactly because it never consumes
`gs.rnd`; the Dominion scenarios are checked structurally (node type, sub-root count, total visits).

**Two claims were verified rather than asserted.** Swapping `actionValues` to a `LinkedHashMap` — a
change to iteration order and nothing else — moves the LMR digests, confirming that the order is
load-bearing and that the golden tests are sensitive. And the SushiGo `redeterminise` defect (D1)
does fail the new test without its fix.

**One planned claim I had to walk back.** The plan said the tail of `backUpSingleNode` *must not*
recompute `actionsToConsider`, because the visit counts it depends on have just been incremented. I
wrote that as fact in a comment, then tried to demonstrate it and could not: even with a widening
schedule tuned to sit on a boundary (139 nodes rather than 201), recomputing gives byte-identical
results, because distinct action-heuristic values make the widening sort stable under the tie-break.
The comment and commit message now say correctness-by-construction, not a fixed bug.

**Two API consequences of adding overloads,** neither anticipated:

- the `int[]` form of `actionVisits` had to become `actionVisitCounts(int)`, since overloading on
  `int` collides with `actionVisits(AbstractAction)`;
- adding two-argument `actionVisits` overloads makes `root::actionVisits` an *inexact* method
  reference, so javac stops inferring a comparator's type variable through it where the target type
  is not already fixed. Two call sites in `MCTSDecisionRecorder` became explicit lambdas. A third,
  in `TestUndoOpponentFlank`, still compiles, because `Stream.max` fixes its target type.

**`ForestNode` has coverage for the first time.** It is now a golden scenario, and confirmed to be
the one root that reaches `initialiseRootMetrics()` without ever calling `instantiate()` — which is
why `statsFor` creates its statistics on demand rather than `instantiate` allocating them.

## Design decisions taken during implementation

- **`statsFor` is the sole creation point, and `instantiate` never allocates.** The plan had
  `instantiate` re-key an object it might also have to create; making `statsFor` the only creator is
  simpler and removes the failure mode outright — there is no path that can hand a reused root an
  empty `ActionStats` map. `instantiate` only re-keys, for the case where `decisionPlayer` changes.
- **The reward-index / `ActionStats`-selector distinction is threaded as an object, not a second int.** This
  was forced rather than merely preferred: `nodeValue(int, int)` breaks `toString`'s
  `this::nodeValue` reference outright.
- **`soleActingPlayer` returns an `int`, not a list.** Nothing in Stage A consumes a list, and dead
  code that merely looks correct is worse than none. Its Stage B body returns `-1` where several
  players decide at once, per JG's point that a multi-actor node has no valid `decisionPlayer`.
- **The `statsFor` guard is temporary scaffolding.** It asserts the player asked for is the node's
  own, so that threading the wrong id — which compiles cleanly, an int being just an int — fails
  loudly under the existing suite. It comes out at Stage B step 2.
