# Decoupled UCT in `players.mcts`

This document details how `SingleTreeNode` searches a simultaneous-move turn when `MCTSParams.decoupled` is on, how that
path shares the tree, the selection policies and the backup with ordinary sequential UCT, and what
it does not yet do. It assumes
familiarity with `SingleTreeNode` as a sequential open-loop searcher.

---

## 1. What decoupled UCT is

On a **simultaneous turn** several players choose at once, with none seeing the others' choices. The
game resolves all the choices together. Sequential UCT can still search such a turn: it asks the
game who the "current" player is, gives that player a node, applies their action, asks again, and
so on, so the second player's node sits *below* the first player's action and can condition on it.
This is fine when run from the main Game loop, because that hides the unknown information about what
actions the previous players took, so there is no information leakage. However, this cannot happen in 
MCTS search, which leads to conservative play as we assume that other players can condition their actions
based on ours, when in fact this is simultaneous.

**Decoupled UCT** puts every player who decides at the same moment on the *same* node. Each of them
keeps their own set of action statistics there and selects their own action from these, without
reference to what the others select on this iteration. The chosen actions are combined into one
joint action, the child is keyed by that joint action, and on backup each player credits only
their own component. "Decoupled" refers to the fact that the players' statistics are independent,
so the search treats each player's choice as a bandit problem against the aggregate behaviour of
the others rather than as a reply to their specific move.

Decoupled UCT is not a separate agent. It is a mode of MCTS switched on by one tunable boolean:

```java
MCTSParams.decoupled   // default false
```

With it off, nothing described here runs and the search is sequential UCT as the default. With it
on, the search behaves differently only at states the *game* reports as simultaneous; every other node
is still a sequential node, and a single tree freely mixes the two kinds.

It is up to a Game implementation to provide support for simultaneous moves. It does this by implementing
the `AbstractGameState.getCurrentSimultaneousPlayers()` method.

---

## 2. What the framework provides on the game side

Decoupled search depends on a small simultaneous-move API in `core`. 

- **`AbstractGameState.getCurrentSimultaneousPlayers()`** returns the list of players who must decide
  now. The default is the singleton `getCurrentPlayer()`. A simultaneous-move game overrides it to
  list every player who has not yet committed a choice this turn. While an `IExtendedSequence` is in
  progress the default implementation defers to that sequence's own
  `getCurrentSimultaneousPlayers(state)`, so a multi-step action taken by one player narrows the
  acting set to that player until it completes (SushiGo's chopsticks are an example).
- **`AbstractForwardModel.computeAvailableActions(state, actionSpace, activePlayer)`**, the
  three-argument form, computes a *named* player's actions. The two-argument form is defined as this
  with `state.getCurrentPlayer()`, which is why the current player must remain meaningful (section
  3).
- **`core.actions.SimultaneousAction`** wraps a `Map<Integer, AbstractAction>` from player to
  component. `execute` applies the components in map order; `equals` and `hashCode` are those of the
  map, which is what makes it usable as a key in `children`. `AbstractForwardModel.next` recognises it
  and records one history entry per component, against that component's player, before applying it.
- **`Game.oneAction()`** asks `getCurrentSimultaneousPlayers()`,
  gives each of those players their own observation copied *before anyone has moved*, collects one
  action each, fuses them into a `SimultaneousAction` when there is more than one, and calls
  `forwardModel.next` once.

Two games implement the game side today: **SushiGo** (`SGGameState`, `SGForwardModel`) and
**Diamant** (`DiamantGameState`, `DiamantForwardModel`).

The test fixture `players.mcts.SimultaneousLMRGame` is a third implementation: the stateless
two-player LMR game with both players deciding on every turn. This is only used for unit tests.

---

## 3. The sequential-play contract

This is the constraint that shapes everything else, and is required for backwards compatibility.

> **A game that supports simultaneous moves must still fully support sequential play.**
> Simultaneity is additional information layered on top of the sequential API; it never replaces it.

Every agent in the framework other than decoupled MCTS uses `getCurrentPlayer()` and applies
single-player actions one at a time. The game has four obligations, all of which decoupled search
also relies on:

1. **`redeterminise(playerId)` must set the turn owner to the observing player.** The two-argument
   `computeAvailableActions` reads `getCurrentPlayer()`, so a redeterminised state that still points
   at another player hands every sequential caller the wrong action list. SushiGo and Diamant both do
   this in their `redeterminise`.
2. **The forward model must accept single-player actions one at a time and sequence them itself.**
   `SGForwardModel._afterAction` and `DiamantForwardModel` cycle to the next player who has not yet
   chosen and resolve the turn only once everyone has. This keeps rollouts, opponent models,
   `advanceToTurnOfPlayer`, OSLA, RHEA and sequential MCTS working on the game, and it is why
   decoupled search keeps its rollouts sequential (section 5.5).
3. **`getCurrentSimultaneousPlayers()` currently has just two consumers**: `Game.oneAction` and decoupled
   search. Sequential UCT never calls it. 
4. With `decoupled=false`, `SingleTreeNode` uses only `getCurrentPlayer()`. 

**Checklist for a new simultaneous-move game.** Override `getCurrentSimultaneousPlayers()`; implement
`_computeAvailableActions(state, activePlayer)` for a named player; set the turn owner in
`redeterminise`; keep the forward model able to sequence one player at a time; make sure a game of
`RandomPlayer`s runs end to end before involving any search agent. Then run the sequential and
decoupled MCTS end-to-end tests in `games.fmtester.ForwardModelTestsWithMCTS` against it.

---

## 4. What a node holds

`SingleTreeNode` is still one class with one set of fields. The fields that matter here:

| Field | Meaning                                                                                                                                                                                                                                                                                                                                |
|---|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `actingPlayers` | The players who decide at this node, as reported by the game state on the most recent visit. One entry at a sequential node; several at a multi-actor node. Refreshed every visit in open loop, because the acting set at a node can change between iterations (section 6.4).                                                          |
| `statsByPlayer` | `Map<Integer, PlayerDecisionStats>`: one statistics container per player who has ever acted at this node. Entries are created on demand by `statsFor(player)` and never removed. A sequential node has exactly one entry, keyed by `decisionPlayer`.                                                                                   |
| `decisionPlayer` | At a sequential node, the player who acts there. At the root, always the player the search is on behalf of. At a multi-actor child, also the root's owner, so the field has one meaning everywhere: the player this search is for. It is also the index used to read that player's reward out of a result vector.                      |
| `children` | `Map<AbstractAction, SingleTreeNode[]>`. At a sequential node the keys are that player's actions; at a multi-actor node they are `SimultaneousAction`s over the acting set, and no other keys are ever added there. The array has `nPlayers + 1` slots (section 4.2). The last slot is used if we transition to a Simultaneous Action. |
| `nVisits` | A single node-level counter, incremented once per backup whoever acted.                                                                                                                                                                                                                                                                |
| `totValue` | The node's own per-player accumulated result over every visit, kept beside `nVisits`. `nodeValue(playerId)` reads this and nothing else (section 6.5).                                                                                                                                                                                 |

`isMultiActor()` is `actingPlayers.size() > 1`; this predicate is the branch point at every site
that behaves differently under decoupling.

### 4.1 `PlayerDecisionStats`

The five pieces of per-player state co-vary: they are written together when a node is visited and
read together by the tree policy. They are grouped into one package-visible data holder with no
behaviour and no back-reference to the node:

```java
class PlayerDecisionStats {
    final int player;
    List<AbstractAction> actionsFromOpenLoopState;        // this player's candidates on this iteration
    final Map<AbstractAction, ActionStats> actionValues;  // visits and value per candidate
    final Map<AbstractAction, Double> actionValueEstimates;  // action-heuristic scores
    Map<AbstractAction, Double> actionPDFEstimates;       // pUCT prior
    final Map<AbstractAction, Double> regretMatchingAverage;
}
```

Selection logic stays on `SingleTreeNode`, which needs `params`, `root`, `children` and `nVisits`
anyway. Every internal that reads these tables (`ucbValue`, `exp3Value`, `rmValue`,
`getActionValue`, `actionsToConsider`, `treePolicyAction`, `updateActionStats`, ...) takes an
`int actingPlayer`, fetches `statsFor(actingPlayer)` once at the top, and reads through that object.
The public no-argument forms (`getActionValues()`, `getActionsFromOpenLoopState()`,
`getActionStats(action)`, `actionVisits(action)`, ...) delegate with `decisionPlayer`, so every
external consumer written for sequential search still compiles and still reads the root player's
table.

The map types are those `SingleTreeNode` used before the container existed. `nodeValue` used to sum over 
`actionValues.values()`, `bestAction` still falls back to
`keySet().stream().findFirst()`, and the golden tests (section 8) pin the resulting iteration order.

**Two meanings of "player" are kept apart on purpose.** `actionTotValue(action, playerId)` and
`nodeValue(playerId)` take the *reward index* into a `double[]`. This asks for the reward/value that the player will 
receive on this action (which is distinct to the player who took the action).

The player who took the decision is never a second`int` parameter beside it; it is instead defined via the 
`PlayerDecisionStats` object.

### 4.2 The child array and `childSlot`

The child array is always indexed by "who acts next", because in open loop the same action can
lead to different players' turns on different iterations. A decoupled tree needs one more case: the
successor may be a state where *several* players act. Such a successor is filed in the extra slot
at index `nPlayers`, whatever the game reports as its current player:

```java
protected int childSlot(AbstractGameState nextState) {
    return actingPlayersAt(nextState).size() > 1 ? nextState.getNPlayers() : nextState.getCurrentPlayer();
}
```

`expandNode` and `nextNodeInTree` use this childSlot, so the store and the lookup
should agree. A separate slot is used to indicate that we transition to a multi-actor simultaneous decision, not any 
player's, because the same action can by chance lead
to either kind of successor (in Diamant the cave continues with one player in it, or collapses and
everyone re-enters). Filing the joint successor under the root player's slot let those two
collide. The consequence is that two joint successors of one action with *different* acting sets
share a node, but this is what open-loop chance outcomes normally do (section 6.4).

`actingPlayersAt(state)` is a link between the two modes, and is the main point at which MCTS picks one or other of 
the get...Players methods:

```java
protected List<Integer> actingPlayersAt(AbstractGameState s) {
    if (!params.decoupled) return Collections.singletonList(s.getCurrentPlayer());
    return s.getCurrentSimultaneousPlayers();
}
```

---

## 5. The search, one iteration

Everything uses `SingleTreeNode`; there is no separate decoupled node class.

### 5.1 Root creation and node instantiation

`instantiate` sets `decisionPlayer`: at the root, `state.getCurrentPlayer()`, which after
`Game.oneAction`'s per-player observation copy is the player being asked for an action. At a child, the state's
current player if one player acts there, otherwise the root's `decisionPlayer`. It then calls
`setActionsFromOpenLoopState`.

### 5.2 Arriving at a node: `setActionsFromOpenLoopState`

Called on every visit in open loop (and once, at creation, in closed loop). It records the state,
refreshes `actingPlayers` from `actingPlayersAt`, and then:

- **sequential node** (one acting player): no change. The state's
  current player must equal `decisionPlayer` or the tree policy is `SelfOnly`; otherwise it throws an error.
- **multi-actor node**: for each acting player, `setActionsForPlayer(state, p)` computes that
  player's candidates with the three-argument `computeAvailableActions`, creates an `ActionStats`
  entry for any new action, and applies move ordering, pUCT priors and `initialiseVisits` seeding
  to that player's table. A player in the acting set but for whom the game is over throws an error:
  the tree policy never expands a terminal state, so this can only fire if a game reports an
  eliminated player as still deciding. Nothing is added to `children` here (at a sequential node a
  `null` placeholder is put under each new action; at a multi-actor node the keys are joint actions
  that do not yet exist).

A player who acted here on an earlier visit but not on this one keeps their statistics untouched. They is
simply not consulted until they act again, when `setActionsForPlayer` replaces their candidate list.

### 5.3 Descending: `treePolicy`

The loop condition is `hasDecisionToMakeInTree()`: at a sequential node, the decision player is not
terminal and has candidates; at a multi-actor node, the same for every acting player.

Each step calls `jointTreePolicyAction(true)`:

```java
if (!isMultiActor()) return treePolicyAction(decisionPlayer, explore);
Map<Integer, AbstractAction> choices = new LinkedHashMap<>();
for (int p : actingPlayers) choices.put(p, treePolicyAction(p, explore));
return new SimultaneousAction(choices);
```

`treePolicyAction(p, explore)` is the ordinary per-player selection (UCB, UCB_Tuned, EXP3,
RegretMatching, Greedy, Uniform, with progressive widening, pUCT and progressive bias) over that
player's own statistics. At a multi-actor node it runs once per acting player, and the players' choices
do not inform each other. That is the "decoupling".

The chosen action is then applied with `advanceState`, which appends to `root.actionsInTree` the
pair `(actingPlayer, action)` used later by the backup. At a multi-actor node the pair is
`(-1, jointAction)`: this is the one place `-1` stands for "several players acted", and `backUp`
and `MASTBackup` expand it. Under `Closed_Loop` the state is not advanced, but the same pair is
recorded so that the two paths leave `actionsInTree` in the same shape and index-aligned with
`currentNodeTrajectory`.

`nextNodeInTree(chosen)` then looks up `children.get(chosen)[childSlot(openLoopState)]` (or the
single non-null child under `Closed_Loop`) and, if found, calls `setActionsFromOpenLoopState` on it.
If not found, `expandNode` creates the child and files it under `childSlot`.

### 5.4 Closed loop

Nothing special is needed for selection: per-player candidates come from the three-argument
`computeAvailableActions` on the state stored against the node at creation, which is exactly what
`Closed_Loop` keeps. A deterministic joint transition has one child, so `nextNodeInTree`'s
"first non-null" rule is right. The only closed-loop touch point is the `actionsInTree` record
described above.

### 5.5 Rollouts stay sequential

`rollout` is unchanged. It advances one player at a time through `getCurrentPlayer()` and lets the
forward model sequence the simultaneous turn (contract obligation 2). Joint rollout actions were
considered and rejected: `finishRollout` relies on `lastActorInRollout` and the turn owner being
meaningful single values, and joint rollouts would change forward-model call accounting.

### 5.6 Backup

`backUp` walks `currentNodeTrajectory` and `actionsInTree` in step, from the leaf up. For each
entry it checks the shape: a `SimultaneousAction` must be paired with a multi-actor node, and a
plain action's recorded player must equal the node's `decisionPlayer`. It then calls
`backUpSingleNode(action, result)`, which:

1. increments `nVisits` and adds `result` into the node's `totValue` (both modes);
2. at a multi-actor node, for each acting player `p`, takes the component
   `joint.getPlayerActions().get(p)` and calls `updateActionStats(p, component, result)`, which
   updates that player's `ActionStats` for that component only. The regret-matching average-policy
   refresh runs inside this loop, per player, on the shared `nVisits` cadence. 
3. at a sequential node, runs `updateActionStats(decisionPlayer, ...)` and then the configured
   backup tail (`MonteCarlo`, `Lambda`, `MaxLambda`, `MaxMC`) as before.

`MASTBackup` receives the same `actionsInTree` list and expands each `SimultaneousAction` into
`(player, component)` pairs as it reads it, so MAST statistics are per player per component action
and `MASTActionKey` is never asked to key a joint action. `actionsInTree` itself is never rewritten;
it must stay index-aligned with the trajectory.

### 5.7 Choosing the move: `bestAction`

Only ever called on the root, which always has a valid `decisionPlayer`. It reads that player's
table (`statsFor(decisionPlayer)`), recomputing the legal actions with the three-argument
`computeAvailableActions` for `decisionPlayer` where the root state has to be re-checked
(redeterminised roots, MCGS). The returned action is a component action, never a joint one, which
is what `Game.oneAction` expects from each player. `MCTSPlayer` records
`(getCurrentPlayer(), bestAction())` as `lastAction` for tree reuse.

---

## 6. Design decisions, and why

**6.1 A new set of PlayerDecisionStats in a Map keyed by the Acting player. This is the main change to support the 
new paradigm of more than one player taking an action at a node.

**6.2 One switch, not an `OpponentTreePolicy` value.** Decoupling composes with the tree policy,
information model, MAST and the action heuristics rather than replacing them, and it only engages at
simultaneous states. A boolean lets every existing configuration be tried decoupled by flipping one
field.

**6.3 A multi-actor child takes the root's owner as `decisionPlayer`.** A multi-actor node where the
root player is not acting has no natural decision player.
The tree policy and the backup only
ever use the named `actingPlayer` forms, so the search itself never reads decisionPlayer.

**6.4 A separate child slot for joint successors.** Filing the joint successor under the successor
state's current player made the tree depend on which of several simultaneously acting players a game
happens to report as current, which SushiGo and Diamant decide differently and no game guarantees.
Filing it under the root player's slot collided with the genuine single-actor successor in Diamant.
The extra slot removes both problems, at the cost that a node can be visited with different acting
sets on different iterations. 

**6.5 `nodeValue` reads the node's own `totValue`.** It used to sum one player's `ActionStats`.
At a multi-actor node visited with different acting sets, no single player's statistics need cover every
visit, so that sum can be a partial one. 
The node now accumulates its own per-player total in the backup, mirrored by
`initialiseVisits` seeding so the numerator and denominator stay in step. At every sequential node
the two definitions agree exactly.

**6.6 `-1` in `actionsInTree`, joint actions expanded at the consumer.** Expanding a joint action into
per-player entries at record time would break the index alignment between `actionsInTree` and
`currentNodeTrajectory` that `backUp` depends on. Recording one `(-1, joint)` pair per multi-actor
node and expanding in `backUpSingleNode` and `MASTBackup` keeps the alignment and needs no fresh
list.

**6.7 `statsFor` creates on demand, so that `instantiate` never allocates or creates statistics.**
`ForestNode` constructs a root without ever calling `instantiate`, and a reused root goes through `instantiate` a 
second time but must keep its statistics. Making `statsFor` the sole creation point removes both failure modes.
`instantiate` only re-keys a single-entry map when `decisionPlayer` has changed (possible on the
MCGS transposition path); a multi-entry map is left alone, since its keys are real player ids.

---

## 7. Where decoupling is controlled, and current limitations

### 7.1 Gated in `MCTSParams._reset()`

`decoupled` is silently forced to `false` when `opponentTreePolicy != OneTree` or
`numDeterminizations > 1`. Every other opponent tree policy assumes one actor per node:

| Feature | Why it cannot be decoupled as it stands                                                                                                                                                                   |
|---|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `SelfOnly` | `treePolicyAction` throws whenever the current player is not `decisionPlayer`, and `advanceToTurnOfPlayer` advances players one at a time and never builds a joint action.                                |
| `MultiTree` | `MultiTreeNode` asserts that each player's current node belongs to that player.                                                                                                                           |
| `OMA` / `OMA_All` | `OMATreeNode.backUp` walks the trajectory expecting the parent's `actionValues` to contain the action taken; those would be joint actions against component-keyed tables.                                 |
| `MCGS` / `MCGSSelfOnly` | Not decoupled: the transposition map and its backup path are single-actor. There is no <br/>need for explicit decoupling here - we just need to make sure the state definition does not leak information. |
| `numDeterminizations > 1` | `ForestNode` reads each sub-root's table for the primary player only and would silently ignore the others.                                                                                                |

The clamp is in `_reset()`. Tests that set `MCTSParams` fields directly bypass it, which is why
the default is `false` and the decoupled test classes set `decoupled = true` themselves.
(But best practice is to use the proper API, setParameterValue(), to set these values.)

### 7.2 Backup policies other than Monte Carlo

The `Lambda`, `MaxLambda` and `MaxMC` tails interpolate the result with the running mean of the
action taken, or of the best action: a single-actor notion. At a multi-actor node there is one such
quantity per acting player and no principled way to combine them, so at those nodes the backup is
plain Monte Carlo whatever `backupPolicy` says. Sequential nodes in the same tree still apply the
configured policy. A per-player generalisation (compute each player's best action marginalising
over the others, then interpolate per player) is a possible follow-up; the `TODO` is in
`backUpSingleNode`.

### 7.3 Tree reuse across a simultaneous turn

`reuseTree` is not clamped, but `MCTSPlayer.newRootNode` starts afresh, silently, whenever the new
root state is itself a simultaneous decision under `decoupled`. Reuse still works for the sequential
nodes of a mixed tree.

Why it does not work yet: the game history records component actions one per player, so
`backtrack`'s backward search for `lastAction` succeeds, but its forward walk then looks each
component up in `children`, which at a multi-actor node holds joint keys, so it always misses. To
make it work: (1) on reaching a multi-actor node, `backtrack` must consume the next
`actingPlayers.size()` history entries, check their player ids equal the acting set, build a
`SimultaneousAction`, and look it up in the extra child slot; (2) `rootify` must leave the other
players' tables undisturbed, which it already does; (3) the assertion in `newRootNode` that the new
root's `decisionPlayer` equals the current player must be relaxed to "the current player is in the
root's acting set". `SushiGoDecoupledTests.reuseTreeIsSkippedAtASimultaneousRoot` pins the current
behaviour and should be replaced when this is done.

### 7.4 Consumers that read the root player's table

`TreeRecorder`, `MCTSExpertIterationListener`, `MCTSDecisionRecorder` and `LearnedValue` call the
no-argument `getActionValues()` per node. On a decoupled tree that is the root player's table, which
at a multi-actor node where the root player is not acting is an empty map (created on the read).
They do not throw, but they see nothing at those nodes. `TreeStatistics` and `toString` were
converted to iterate `getActingPlayers()` and sum or print one block per player; the others have
not been. `MCTSMetrics` has a separate pre-existing gap: it inspects only the turn owner's tree, so
in a simultaneous game only one of the several searches per turn is measured.

### 7.6 Other things to keep in view

- **Branching factor.** At a multi-actor node the number of distinct children is the *product* of
  the acting players' action counts. Child reuse is therefore rare at low iteration budgets. This is
  inherent to keying by joint action.
- **The `MCTSPlayer` child-count assertion is skipped at a multi-actor root.** The sequential check
  (`children.size() > 3 * actions.size()`) is meaningless against joint keys. A replacement bound of
  three times the cross product of the acting players' action counts has been suggested and not yet
  written.
- **Forward-model call budgets are not comparable between modes.** A decoupled tree makes one
  `next` call per simultaneous turn; a sequential tree makes one per player. Under
  `BUDGET_FM_CALLS` the decoupled agent therefore gets roughly *n* times the effective depth in an
  *n*-player simultaneous turn. Compare under `BUDGET_ITERATIONS`, or count *n* calls per joint
  action.
- **The turn owner after a simultaneous turn depends on how it was applied.** Applied one component
  at a time, the game's forward model hands the next turn to whoever chose last; applied as one
  `SimultaneousAction`, the turn owner is unchanged. The tree no longer depends on this (section
  6.4), but anything that reads `getCurrentPlayer()` after a joint action in the tree, or after the
  same turn in a rollout may see different players. (There is no material difference in the underlyign game state, 
  as it does not matter what order the players choose their actions.)
- **Mutable action fields.** `SimultaneousAction` delegates `hashCode` to its components, so a
  component whose `equals`/`hashCode` includes mutable state makes the joint key unstable too. The
  search copies actions before applying them, and `setActionsForPlayer` asserts the contract on each
  component, which is the existing guard.
- **No search over a shuffling game is reproducible between runs.**
  `AbstractGameState.redeterminisationRnd` is deliberately unseeded and every `copy()` reseeds from
  it. Tests that expect exact behaviour therefore fail. This is one reason for the LMR test game (which has no 
  determinisation); searches on SushiGo and
  Diamant are checked structurally, and any head-to-head comparison has to average over many games.

---

## 8. Tests

All JUnit 4, in `src/test/java/players/mcts/` unless stated.

| Class | What it pins |
|---|---|
| `SequentialMCTSGoldenTests` | Sequential search is byte-identical to what it was before any of this work: exact tree digests and an RNG canary over sixteen LMR configurations, plus structural checks over the Dominion tree shapes (OneTree, SelfOnly, MultiTree, OMA, paranoid, Closed_Loop, MAST, forest, tree reuse). Any change to a value means sequential behaviour changed. |
| `DecoupledUCTTests` | The decoupled path on `SimultaneousLMRGame`, where every node is multi-actor: joint child keys, per-player visit accounting, independent tables, backup crediting own component only, the `-1` record shape, closed loop, per-player regret matching, MAST expansion, the Monte-Carlo-only backup limitation, `toString` and `TreeStatistics`; and eleven pinned decoupled digests. It also checks that the same fixture searched with `decoupled=false` reproduces the sequential `lmr.ucb` digest byte for byte, which is contract obligation 4 as a test. |
| `SushiGoDecoupledTests` | Three-player SushiGo through the real player API: every seat returns its *own* action from a multi-actor root, joint keys over everyone, MAST, EXP3, regret matching, closed loop, chopsticks inside the tree (an extended sequence narrowing the acting set to one player), the reuse skip, and a whole game with mixed seats. |
| `DiamantDecoupledTests` | Four-player Diamant, where the acting set shrinks as players leave the cave: joint keys over everyone in the cave, the acting set after two players leave, `nodeValue` at nodes where the root player is not acting, and whole games through `Game.oneAction`. |
| `games.sushigo.SimultaneousActionTests`, `games.diamant.DiamantSimultaneousTests` | The game side, with no search. |
| `games.fmtester.ForwardModelTestsWithMCTS` | End to end: `testSushiGoWithSeqUCT`, `testDiamant` (sequential, via the game-specific JSON), `testDiamantDUCT` (`Diamant_DUCT.json`, which is `MCTSParams` with `decoupled: true`), and `testSushiGoWithDUCT` against the `players.DUCT` baseline. |
| `players.DUCT.DUCTNodeTests` | The standalone baseline. |

Run classes individually rather than the whole suite:

```bash
mvn test -Dmaven.test.skip=false -Dtest=SequentialMCTSGoldenTests
mvn test -Dmaven.test.skip=false -Dtest=DecoupledUCTTests+SushiGoDecoupledTests+DiamantDecoupledTests
```

**Re-baselining.** Both golden classes have a `main` that prints the current values in the form of
the static `EXPECTED` block. Do this only after a deliberate behaviour change, and record why in the
commit. A change that moves the sequential digests is a change to sequential UCT, whatever it was
meant to be. Regret-matching digests are the most sensitive: a last-bit floating-point difference in
a value becomes a different sampled action.

The existing sequential test classes remain the guard on the shared code: `MCTSTreeSelectionTests`,
`BackupTests`, `TreeReuseTests`, `TreeReuseMCGSTests`, `MCTSNodesAndVisitsTests`, `MCGSTests`,
`OMATests`, `MultiTreeMCTSTests`, `RewardsForParanoiaTests` (the only place a reward index other
than the decision player's is asserted) and `RolloutTerminationTests` (which includes SushiGo and
Diamant, and so is the canary that sequential search never calls `getCurrentSimultaneousPlayers()`).
