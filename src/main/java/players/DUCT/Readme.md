# Folding decoupled UCT into `players.mcts.SingleTreeNode`

*Draft 3. Revised in the light of JG's review comments on draft 2, and of a line-by-line
verification pass over `SingleTreeNode` and every one of its consumers. JG's comments are answered in
place rather than left inline; the review history is in §12. Stage A and Stage B have both since
landed on `duct`; §8 and §9 each end with a record of what was actually built.*

Line references are against the `duct` branch at the time of writing.
Anything marked **verified** was checked against the working tree while writing draft 3.

---

## 1. Where we are

Branch `duct` carries two separable things.

**Core simultaneous-move support** (merged from `simultaneous-moves`). The API is small:

- `AbstractGameState.getCurrentSimultaneousPlayers()` (`AbstractGameState:175-180`) returns
  `List<Integer>`, defaulting to `singletonList(getCurrentPlayer())`. Games opt in by overriding it,
  and, where necessary, by overriding `IExtendedSequence.getCurrentSimultaneousPlayers`.
- `AbstractForwardModel.computeAvailableActions(state, actionSpace, activePlayer)`
  (`AbstractForwardModel:177-191`) — a 3-arg form that asks for a specific player's actions rather
  than the current player's.
- `core.actions.SimultaneousAction` wraps `Map<Integer, AbstractAction>`, executes the components in
  insertion order, and delegates `equals`/`hashCode` to the map — so it is usable as a tree key.
  `AbstractForwardModel.next` unpacks it and records one history entry *per player*
  (`AbstractForwardModel:143-146`).
- `Game.oneAction()` polls every acting player against its own pre-move observation
  (`gameState.copy(p)`, `Game:367`), calls `getAction` per player, then fuses the choices into one
  `SimultaneousAction` (`Game:460-462`) and calls `forwardModel.next` **once**. That "each agent
  sees a state copy taken before anyone moved" property is what makes the turn genuinely
  simultaneous rather than sequential-with-hidden-information.

Only SushiGo implements the game side (`SGGameState.getCurrentSimultaneousPlayers`, :41-63).

**A standalone decoupled-UCT agent** in this package: `BasicDUCTPlayer`, `DUCTNode`, `DUCTParams`,
~570 lines. It is not registered in `games.GameType`, `PlayerFactory`, or any JSON config; you reach
it via the class-name fallback in `PlayerFactory.createPlayer`. `ForwardModelTestsWithMCTS`
exercises it end to end via `testSushiGoWithDUCT` and `SushiGoDUCT.json`, alongside
`testSushiGoWithSeqUCT`.

Both `DUCTNode` and `SingleTreeNode` are open-loop searchers over actions: one state copy per
iteration, mutated in place by the forward model on the way down. The **only** algorithmic
difference is that `DUCTNode` lets several players decide at one node, each from its own action
statistics, and keys the child by the resulting joint action. `SingleTreeNode` is hard-bound to one
acting player per node.

### Why merge

`players/DUCT` has no MAST, no action heuristics, no progressive bias or widening, no
EXP3/regret-matching, no tree reuse, no paranoid or MaxN backups, only three budget types, and
uniform-random rollouts. It is also invisible to `TreeStatistics`, `MCTSMetrics`, `TreeRecorder`,
`MCTSDecisionRecorder`, the expert-iteration listener, and the NTBEA search spaces. None of that
will ever arrive unless the two implementations become one.

### Scope of this document

- The switch is a tunable `boolean MCTSParams.decoupled`, default `true` — mirroring
  `DUCTParams.decoupled`. Not a new `OpponentTreePolicy` value.
- `players/DUCT` stays as an independently verifiable reference baseline. Retiring it is not covered.
- MCGS is out of scope.
- §8 (Stage A) and §9 (Stage B) are both implemented; each ends with what actually landed.

---

## 2. The sequential-play contract

**This is the governing constraint, and it precedes the design.** JG's review made the point that
draft 2 got backwards: `getCurrentPlayer()` is not being demoted to a tie-break. It stays
load-bearing, for every agent in the framework that is not decoupled MCTS.

> **A game that supports simultaneous moves must still fully support sequential play.**
> Simultaneity is additional information layered on top of the sequential API; it never replaces it.

The four obligations:

1. **`redeterminise(playerId)` must set `turnOwner` to the observing player.** `getCurrentPlayer()`
   is what every sequential agent reads, and — this is the mechanical reason, verified —
   `computeAvailableActions(state, actionSpace)` *is defined as*
   `computeAvailableActions(state, actionSpace, state.getCurrentPlayer())`
   (`AbstractForwardModel:173-175`). A game that leaves `turnOwner` pointing at someone else hands
   every 2-arg caller another player's action list. SushiGo discharges this at
   `SGGameState.redeterminise` → `setTurnOwner(playerId)` (~:155), which works because
   `AbstractGameState.copy(playerId)` assigns `s.turnOwner = turnOwner` (:349) *before* calling
   `s.redeterminise(playerId)` (:377-379), and only calls it when
   `playerId != -1 && coreGameParameters.partialObservable`.

2. **The forward model must accept single-player actions applied one at a time**, and sequence them
   itself. `SGForwardModel._afterAction` cycles to the next player who has not yet chosen and only
   reveals once everyone has. DiamantForwardModel does the same (and every other game that has simultaneous moves) This is what makes MCTS rollouts, `advanceToTurnOfPlayer`, opponent
   models, OSLA, RHEA and every non-decoupled agent work on the game at all — and it is why
   rollouts stay sequential (§4.5) rather than building joint actions for symmetry.

3. **`getCurrentSimultaneousPlayers()` has exactly two consumers:** `Game.oneAction` and decoupled
   MCTS. Sequential UCT must never call it.

4. **Therefore the merge is switch-shaped.** With `decoupled=false`, `SingleTreeNode` reproduces
   today's sequential UCT line for line and asks only `getCurrentPlayer()`. With `decoupled=true`,
   the acting set is the only thing it asks about who is moving. There is no third mode.

**Checklist for the next game that adds simultaneous moves.** Override
`getCurrentSimultaneousPlayers()`; override `_computeAvailableActions(state, activePlayer)`; set
`turnOwner` in `redeterminise`; keep `_afterAction` able to sequence one player at a time; and
verify a `RandomPlayer`-only game runs end to end before touching any search agent.

---

## 3. The two hard constraints in `SingleTreeNode`

Everything in §4 follows from these.

**Constraint 1 — one acting player per node.** `setActionsFromOpenLoopState:199`:

```java
if (actionState.getCurrentPlayer() == this.decisionPlayer && actionState.isNotTerminalForPlayer(decisionPlayer)) {
    actionsFromOpenLoopState = forwardModel.computeAvailableActions(actionState, params.actionSpace);
    ...
} else if (!params.opponentTreePolicy.selfOnlyTree) {
    throw new AssertionError("Expected?");   // :297-299
}
```

**Constraint 2 — one action per trajectory step.** `backUp:997-1005`:

```java
for (int i = root.currentNodeTrajectory.size() - 1; i >= 0; i--) {
    int actingPlayer = root.actionsInTree.get(i).a;
    AbstractAction action = root.actionsInTree.get(i).b;
    SingleTreeNode n = root.currentNodeTrajectory.get(i);
    if (n.decisionPlayer != actingPlayer)
        throw new AssertionError("We have a mismatch between the player who took the action and the player who should be acting");
    result = n.backUpSingleNode(action, result);
}
```

Structurally, `children` is `Map<AbstractAction, SingleTreeNode[]>` where the array is indexed by
the player who acts *next* — that models uncertainty about who moves next, not concurrency.

---

## 4. Design: per-player statistics on the node

Draft 1's "extra `SingleTreeNode` instances as statistics holders" is dropped (JG's review of draft 1,
§12). Draft 2's replacement — make the statistics themselves per-player, as `DUCTNode` already does
with `playerActionStats` (`DUCTNode:30`) — stands. Three details of draft 2 are corrected below:
§4.2 (how the acting player is threaded), §4.3 (the acting-player rule), and §4.4 (the call-site
counts and the migration recipe).

### 4.1 What actually has to become per-player

Not just `actionValues`. Reading `setActionsFromOpenLoopState` (:197-301), `treePolicyAction`
(:629-697) and `backUpSingleNode` (:1054-1147) together, **five** fields are per-acting-player, and
they co-vary — written together in one pass, read together in the next:

| Field | Line | Type | Role                                                                           |
|---|---|---|--------------------------------------------------------------------------------|
| `actionsFromOpenLoopState` | :48 | `List<AbstractAction>` | the candidates this player has *this iteration* (i.e. in this determinisation) |
| `actionValues` | :72 | `Map<AbstractAction, ActionStats>` | visits/value per candidate                                                     |
| `actionValueEstimates` | :49 | `Map<AbstractAction, Double>` | action-heuristic scores (move ordering, progressive bias)                      |
| `actionPDFEstimates` | :50 | `Map<AbstractAction, Double>` | pUCT prior                                                                     |
| `regretMatchingAverage` | :62 | `Map<AbstractAction, Double>` | RM average policy                                                              |

Group them into one small container, `PlayerDecisionStats`, a new package-visible class in
`players.mcts`:

```java
/** The candidate actions, and the statistics over them, for ONE player at ONE tree node. */
class PlayerDecisionStats {
    final int player;
    List<AbstractAction> actionsFromOpenLoopState = new ArrayList<>();   // reassigned at :200 - not final
    final Map<AbstractAction, ActionStats> actionValues = new HashMap<>();
    final Map<AbstractAction, Double> actionValueEstimates = new HashMap<>();
    Map<AbstractAction, Double> actionPDFEstimates = new HashMap<>();    // reassigned at :246 - not final
    final Map<AbstractAction, Double> regretMatchingAverage = new HashMap<>();
    PlayerDecisionStats(int player) { this.player = player; }
}
```

It is a data holder with no behaviour and no back-reference to the node — selection logic stays on
`SingleTreeNode`, where it needs `params`, `root.highReward`, `parent`, `children` and `nVisits`
anyway.

`SingleTreeNode` then holds `Map<Integer, PlayerDecisionStats> statsByPlayer`, and the five fields
are deleted.

**Two properties of the maps are load-bearing and must be preserved exactly.** Construct with
`new HashMap<>()` and **no initial-capacity argument**, and never substitute `LinkedHashMap` or
`TreeMap`. `HashMap` iteration order is a deterministic function of hash codes, insertion sequence
and capacity trajectory — and it is *observed*: `nodeValue:468-471` sums over `values()` (so the
order fixes the floating-point summation and can flip a UCB comparison), `bestAction():1243` does
`keySet().stream().findFirst()`, and `toString():1381-1384` sorts stably so ties fall back to table
order. `new HashMap<>(4)` changes the table length and therefore the order.
[JG: This outright ban of LinkedHashMap is downright wrong. Sometimes this is required to ensure that the order of iteration though the Actions is i insertion order an dnot in hash code order, as an Action can get different hashcodes (due to changing Component Ids) depending on details of the game state]
[JG: Follow the existing conventions in SingleTreeNode - they are there for a reason]

**`nVisits` stays a single node-level counter.** 

### 4.2 Two different meanings of "player" — and how to thread them safely

`actionTotValue(action, playerId)` (:463), `nodeValue(playerId)` (:468) and
`actionSquaredValue(action, playerId)` (:473) already take a player id — but it is the **reward
index** into `ActionStats.totValue[]`, and today it is always passed as `decisionPlayer`. The
decoupled version introduces a second, distinct meaning: **whose `ActionStats` to read**.
They coincide at every current call site, but they are not the same concept, and paranoid backup
already makes `totValue[i]` meaningful for `i != decisionPlayer`.

Draft 2 proposed distinguishing them by name —
`actionTotValue(int actingPlayer, AbstractAction action, int playerId)`. **That is withdrawn.** It
produces `nodeValue(int, int)`: two `int` parameters of different meaning, adjacent, silently
swappable, with no compiler help. It also breaks `toString():1374`, where `this::nodeValue` becomes
an ambiguous method reference — a compile error, which at least fails loudly, but the swap hazard
does not.

**Instead, thread the `PlayerDecisionStats` object, never a second int.** Each internal fetches it
once at the top:

```java
private double ucbValue(int actingPlayer, AbstractAction action) {
    PlayerDecisionStats pds = statsFor(actingPlayer);
    ...
}
```

and reads through `pds`. Exactly one `int` is ever threaded, and the reward index never sits beside
it. `nodeValue`, `actionTotValue` and `actionSquaredValue` keep their present signatures unchanged.

`ActionStats` is still sized from `getNPlayers()` (:265) / `result.length` (:1080) — the array is
indexed by reward player, not by acting player. This is the one place where the two meanings meet,
and it does not change.

### 4.3 The acting-player rule

JG's formulation, adopted:

- **Not decoupled** → `getCurrentPlayer()` is correct. That is sequential UCT, unchanged.
- **Decoupled** → the search asks `getCurrentSimultaneousPlayers()` and never `getCurrentPlayer()`.
- **Decoupled, and this node has several acting players** → `decisionPlayer` **is not a valid
  concept**, and the design must not manufacture one. Draft 2's `primaryActor` helper (which fell
  back to `root.decisionPlayer`) is dropped.

The consequences, in full:

- `decisionPlayer` is retained only in its two *other* meanings: the **reward index** (§4.2), and
  **the search owner at the root**.
- "Whose `ActionStats`" becomes a separate, explicitly threaded concept — `actingPlayer`
  selecting a `PlayerDecisionStats`.
- At a multi-actor node Stage B set `decisionPlayer = -1`, and the no-arg compatibility accessors
  threw there, so that any accidental "whose `ActionStats`?" use failed loudly.
  *Revised after Stage B (JG, September 2026):* a multi-actor node now carries the **root's**
  decision owner in `decisionPlayer`, so the field means one thing everywhere - the player the
  search is on behalf of - and the no-arg accessors read that player's `ActionStats`. At a multi-actor
  node where the root player is not acting (Diamant, once the searching player has left the cave
  while others continue) there are no such `ActionStats`: `nodeValue(playerId)` consults no player's
  `ActionStats` at all, but the node's own per-player `totValue` (§9 item 7), while `getActionValues()` and the other
  no-arg readers create and return an empty `ActionStats` map rather than throwing.
  The tree policy and the backup only ever use the named `actingPlayer` form, so the search itself
  never reads it. This is, in effect, draft 2's `primaryActor` fallback adopted after all.
- The **root** always has a valid `decisionPlayer`, because we only search when we have a decision
  to make. That is what keeps `bestAction()`, `getValue()`, `MCTSPlayer.getDecisionStats`,
  `MCTSMetrics` and `MCTSDecisionRecorder` correct with no change at all.

The seam is therefore one method, added in **Stage B**. (Stage A landed it as a bare
`getCurrentPlayer()` pass-through; that was reverted on review as indirection with no value until
the body changes - see §8, "What actually landed", item 6. The six sites that ask "who acts here?"
call `getCurrentPlayer()` directly today, exactly as on master.)

```java
// As it stands (after Stage B). There is no soleActingPlayer helper any more: the seam is
// actingPlayersAt(), and the three sites that need "one player or several" ask it directly.
protected List<Integer> actingPlayersAt(AbstractGameState s) {
    if (!params.decoupled) return Collections.singletonList(s.getCurrentPlayer());
    return s.getCurrentSimultaneousPlayers();
}
// instantiate(), for a child node - the root keeps state.getCurrentPlayer():
decisionPlayer = terminalStateInSelfOnlyTree(state) ? parent.decisionPlayer
        : actingPlayersAt(state).size() > 1 ? root.decisionPlayer : state.getCurrentPlayer();
// advanceState() and the Closed_Loop bypass in treePolicy() record (-1, jointAction) at a
// multi-actor node; that is the one place -1 still stands for "several players acted".
```

**Correction to draft 2.** It cited `MultiTreeNode:31` (`decisionPlayer = player.getPlayerID()`) as
precedent for setting the root's decision player from the agent's id. It is not precedent:
`instantiate:144` immediately overwrites it with `state.getCurrentPlayer()`, and
`ToadMCTSPlayer:75-76` documents that the resulting value is deliberately relied on. Likewise
`createRootNode:89` is dead — `instantiate` at :101 overwrites it with the identical expression at
:144. None of this is tidied in Stage A.

**Correction to draft 2 on SushiGo.** Draft 2 attributed the `turnOwner` override to
"`SGGameState:150`, `copy.turnOwner = playerId` in the partially-observable branch". Wrong location
and wrong framing: it is `setTurnOwner(playerId)` inside `SGGameState.redeterminise`, and per §2 it
is **mandatory**, not a SushiGo convenience the search might stop depending on.

### 4.4 Keeping the external call sites cheap

Two mechanical steps cover all of it, but **not** with `sed` — draft 2's
`sed 's/\.actionValues/.actionValues()/'` recipe is unsafe in four separate ways. Verified counts:

| | Count | Notes |
|---|---|---|
| `actionValues` **field** reads outside `SingleTreeNode`, `src/main` | **21** | `TreeStatistics` x10, `MCTSMetrics` x3, `PIMCTSMetrics` x2, `MCTSPlayer:376-377`, `OMATreeNode:84`, `MCTSExpertIterationListener:135`, `ForestNode:121`, and **`MCGSNode:70`, a bare inherited `actionValues.get(...)` with no receiver that any `.actionValues` regex misses** |
| ...`src/test` | **39** | `BackupTests` x32, `MCTSNodesAndVisitsTests` x3, `OMATests` x2, `MultiTreeMCTSTests` x1, `MCGSTests` x1 |
| `.actionValues(` that is the **method** `actionValues(List)` | **17** | all in `MCTSTreeSelectionTests` - a blind sed corrupts every one |
| dotted `.actionValues` on **unrelated classes** | **5** | `FeatureListener:128,129,256`, `ActionValueHeuristic:59` x2 - must not be touched |
| bare `actionValues` inherited from `ActionFeatureListener:31` | **24** | `MCTSExpertIterationListener:116-155`; only its `:135` is the node's field |

- `actionsFromOpenLoopState` has **28** external uses draft 2 never counted: `MCTSPlayer:388`,
  `MCTSMetrics:32,170`, and 25 in `MCTSTreeSelectionTests:539-630`.
- `actionValueEstimates`, `actionPDFEstimates` and `regretMatchingAverage` have **zero** external
  uses. (`MaxNSearchPlayer`'s ten `actionValueEstimates` are its own field; `MCTSMetrics`' ten are a
  local variable misleadingly named after it but initialised from `root.actionValues`.)
- Four of the five fields are package-private and `regretMatchingAverage` is protected, so every
  field consumer is inside `players.mcts`. No visibility work is needed.
- Consumers draft 2 missed, all safe because they use only public methods:
  `players/observers/MCTSDecisionRecorder` (14 calls), `games/toads/ToadMCTSPlayer`,
  `players/heuristics/LearnedValue`, `PIMCTSMetrics`, `games/toads/TestUndoOpponentFlank:137`.

**Step 1 — field to accessor.** Add `public Map<AbstractAction, ActionStats> getActionValues()`
returning `statsByPlayer.get(decisionPlayer).actionValues`, plus `getActionValues(int actingPlayer)`.
Migrate the 60 field reads by hand, then make the field `private` so the compiler proves the
migration complete. `actionsFromOpenLoopState`'s 28 reads go to the existing public
`getActionsFromOpenLoopState()`.

Note `getActionValues()` — not draft 2's `actionValues()`, which would sit beside the existing
`double[] actionValues(List)` as a confusing overload. `getActionValues` matches the naming of
`getActionStats`, `getActionsFromOpenLoopState` and `getChildren`.

**Step 2 — overloads for the public per-player methods.** `actionVisits`, `actionValidVisits`,
`getActionsFromOpenLoopState`, `getActionValues`, `actionsToConsider`, `treePolicyAction`,
`actionValues(List)`, `exp3Value`, `rmValue`, `validVisitsFor`, `updateRegretMatchingAverage` and
`regretMatchingAverage()` each gain an `int actingPlayer` first parameter, keeping the existing
signature as a one-line overload delegating with `decisionPlayer`. **No external caller or test
changes at all.** `actionsToConsider(List)` in particular must keep its 1-arg form —
`MCTSMetrics:32,170` call it cross-class.

Per §4.2, `nodeValue`, `actionTotValue` and `actionSquaredValue` get **no** such overload.

This is a compatibility layer of *default arguments*, not an alias for mutable state: there is one
copy of every map, reached by one path.

### 4.5 Rollouts stay sequential — deliberately

It is tempting to build joint rollout actions for symmetry. Don't. This is obligation 2 of §2 seen
from the search side.

`DUCTNode.rollout()` advances **one player at a time** and lets `SGForwardModel._afterAction`
sequence them — it cycles to the next player who has not yet chosen and only reveals once everyone
has. `SingleTreeNode.rollout()` (:920-959) already does exactly the same thing, so it needs no
change at all.

Making rollouts joint would break `finishRollout` (:967-987), which relies on `lastActorInRollout`
and `getTurnOwner()` being meaningful single values, and would change forward-model call accounting
relative to the DUCT baseline we want to compare against (§11).

---

## 5. Changes outside `SingleTreeNode` (Stage B)

**`MCTSParams`** — `public boolean decoupled` beside `paranoid` (:41),
`addTunableParameter("decoupled", ...)` in the constructor, and the matching `_reset()` read.

> On the default: `decoupled=true` only alters behaviour where
> `getCurrentSimultaneousPlayers().size() > 1`, which today means SushiGo alone — so the blast
> radius is `ForwardModelTestsWithMCTS.testSushiGoWithSeqUCT`, not the whole suite. Stage A needs no
> flag at all, since it is a provable no-op.

*Default changed to `false` (JG, 8 September 2026).* Stage B shipped with `decoupled = true`.
With the node-level gate gone (below), a test or caller that sets `opponentTreePolicy` directly
without going through `_reset()` was left decoupled under `MultiTree` and failed on Diamant and
SushiGo (`RolloutTerminationTests`). Sequential search is the safe default; the decoupled test
classes now set `decoupled = true` themselves.

**`MCTSParams._reset()` guards**, following the clamp precedent at `MCTSParams:152-156`
(auto-correct plus a console warning): force `decoupled = false` unless
`opponentTreePolicy == OneTree`; force `reuseTree = false` while §6.1 is outstanding. `Closed_Loop`
and `RegretMatching` are **not** clamped - both are in Stage B (§6, §9).

*As landed:* `_reset()` clamps `decoupled` off, **silently**, when `opponentTreePolicy != OneTree`
or `numDeterminizations > 1`. Silently because `decoupled` then defaulted to true, so the warning
would have fired for every MCGS, SelfOnly or forest configuration on every game, simultaneous or
not, to report a default being overridden by a deliberate choice. Stage B repeated the same test on the node
(`SingleTreeNode.decoupled()`), because the tests set parameter fields directly and never pass
through `_reset()`. *Removed after Stage B (JG):* `actingPlayersAt` now reads `params.decoupled`
alone, so parameters set directly are not clamped at the node.
The test that asserted the node-level gate, `nonOneTreePolicyIsNeverDecoupled`, went with it;
the `_reset()` clamp is the only gate.
`reuseTree` is **not** clamped in `_reset()` at all: that would switch reuse off
for every sequential game whenever `decoupled` is at its default. Instead `MCTSPlayer.newRootNode`
skips reuse, silently, only when the new root state is itself
a simultaneous decision under `decoupled` - the one case §6.1 has not yet made work.

**`MCTSPlayer:307`** — the assertion

```java
if (root.children.size() > 3 * actions.size() && ...)
    throw new AssertionError("Unexpectedly large number of children: ...");
```

fires immediately under joint keys, where child count is bounded by iteration count rather than by
action count. Exempt multi-actor roots.

**`SingleTreeNode.toString():1381-1394`** iterates `actionValues.keySet()` and does
`children.get(action).length` — a *component* action against a joint-keyed `children`. Once the
placeholder `children.put(..., null)` is suppressed on multi-actor nodes, that is an NPE whenever
`state == null`, which is the default at depth > 0 with `discardStateAfterEachIteration=true`.
Reached via `TreeRecorder` and the debug print at `MCTSPlayer:303`. With per-player `ActionStats`,
`toString` should also print one block per entry in `statsByPlayer` — `DUCTNode.toString():346-367`
is the model to copy.

---

## 6. What is gated off, what is deferred, and why

Gated off under `decoupled` (clamped in `MCTSParams._reset()`, §5). None of these makes sense with
simultaneous moves, so the gate is permanent:

| Feature | Reason |
|---|---|
| `SelfOnly` | `treePolicyAction:630` throws whenever the current player is not `decisionPlayer`, and `advanceToTurnOfPlayer:603-628` advances players one at a time and never builds a joint action. |
| `MultiTree` | `MultiTreeNode:141` throws on `p != currentLocation[p].decisionPlayer`. |
| `OMA` / `OMA_All` | `OMATreeNode.backUp:78-86` walks `selfActionsOnly` and asserts `currentNode.actionValues.containsKey(actionTakenFromParent)`. Those entries would be *joint* actions while `actionValues` is keyed by *components*, so :84 fires; `OMAChildren` is likewise keyed by single actions. |
| `numDeterminizations > 1` | `ForestNode:121` reads `roots[i].actionValues` and `totValue[decisionPlayer]`; correct for the primary player only, so a decoupled forest would silently ignore the other players' `ActionStats`. The `OneTree`-only gate covers it. |

Two features that draft 3 had in that table are **in Stage B** on re-examination (review in §12):

- **`Closed_Loop` needs one change, not a gate.** Draft 3's objection was that `treePolicy` never
  advances the state, so decoupled selection had "nothing to select against". That is wrong: the
  candidates at a multi-actor node come from the 3-arg `computeAvailableActions` applied to the
  state stored against the node when it was created (§9 step 3), and a stored, never-advanced state
  is precisely what `Closed_Loop` keeps. The second objection, that the `Closed_Loop` branch of
  `nextNodeInTree` (:806-808) returns the first non-null child "regardless of actor", is no
  objection either: a deterministic joint transition has exactly one child. Expansion
  (`expandNode:641`) already applies the chosen action to a copy and stores the result, and
  `AbstractForwardModel.next:143-146` already applies a `SimultaneousAction` component by component.
  The one site that does need work is the `Closed_Loop` bypass at `treePolicy:594-597`, which
  appends a single `(getCurrentPlayer(), chosen)` pair to `actionsInTree` without going through
  `advanceState`: at a multi-actor node it must record the same thing `advanceState` records there,
  a `(-1, jointAction)` pair, so that `backUp` and `MASTBackup` see the identical shape on both
  paths. That is §9 step 4a, with its own test (§10.2, item 14). *As landed:* exactly that, one
  line, keyed off `isMultiActor()`.
- **`RegretMatching` needs a small extension, not a gate.** Two sites act on `decisionPlayer`
  alone, and both simply loop over the acting players in Stage B (§9 step 5): the average-policy
  refresh in `backUpSingleNode` (:1257-1263), which is keyed off node `nVisits` with `updateEvery`
  derived from *that player's* `actionsToConsider`; and `initialiseRootMetrics` (:339), which
  clears only that player's `regretMatchingAverage`. `bestAction` (:1361-1365) is right as it
  stands, since the action returned from the root is always the `decisionPlayer`'s. Test at §10.2,
  item 15.

**One limitation introduced by Stage B, documented rather than gated.** The `Lambda`, `MaxLambda`
and `MaxMC` backup tails in `backUpSingleNode` interpolate the result with the running mean of
*the* action taken, or of the best action - a single-actor notion. At a multi-actor node there is
one such quantity per acting player and no principled way to combine them, so at those nodes the
backup is plain Monte Carlo whatever `backupPolicy` says; single-actor nodes in the same tree still
apply the configured policy. `StageBDecoupledTests.nonMonteCarloBackupIsMonteCarloAtMultiActorNodes`
pins this (on the all-multi-actor fixture the Lambda and MaxMC trees are byte-identical to the
Monte Carlo one). A per-player generalisation is a possible follow-up; it is not needed for the
MCTS-vs-DUCT comparison, which uses Monte Carlo backups on both sides.

### 6.1 Deferred, not gated: `reuseTree` with simultaneous moves

`reuseTree` is clamped to `false` under `decoupled` **for the moment only**. Tree reuse has to work
with simultaneous moves, and it is the first item after Stage B; the clamp exists so that Stage B
can land without it, and must come out when it does. Nothing here is hard, but it is three
coordinated changes in `MCTSPlayer` plus a test, and it is not needed to compare MCTS against DUCT
(§11), which is Stage B's purpose.

**Why it does not work today.** Under joint keys the reuse path degrades silently rather than
crashing. `AbstractForwardModel.next:141-149` records *component* actions in history, one per
player, whether the game applied a `SimultaneousAction` or the players' actions one at a time; so
`backtrack`'s backward search for `lastAction` (`MCTSPlayer:209`) still succeeds. But the forward
walk then looks up each component in `children` (`MCTSPlayer:227`), which at a multi-actor node
holds *joint* keys, so it always misses, `newRoot` is `null`, and the tree is rebuilt every turn -
which is exactly what `reuseTree=false` does, at the cost of the backtrack.

**What already works, verified.** The history is in the right shape: components are recorded per
player in both application paths, and the entries for one simultaneous turn are contiguous.
`SimultaneousAction` equality and hash code are those of its `Map<Integer, AbstractAction>`, so the
order in which the components appear in history does not matter (SushiGo starts each simultaneous
turn with whichever player chose last, so that order is *not* by player id). And the miss case is
already graceful: when the real joint action was never expanded in the tree - likely under
`Information_Set`, where the tree's joint children were built against redeterminised opponent
hands - the lookup returns `null` and the tree is rebuilt, as it is today when a child is absent.

**What has to change** (line references current on `duct` at the time of writing):

1. `MCTSPlayer.backtrack:201-245` - the forward walk. On reaching a node whose `actingPlayers`
   has more than one entry, consume the next `actingPlayers.size()` history entries, check their
   player ids equal that acting set, build a `SimultaneousAction` from them, and look up
   `children.get(joint)`. A multi-actor successor lives in the extra slot at index `nPlayers` of
   the child array (`childSlot`, §9 item 1), not under any player; a single-actor successor is
   under its current player. Backtrack must index the same way. Any mismatch (a missing
   entry, an unexpected player) sets `newRoot = null` and falls through to the existing rebuild.
2. `SingleTreeNode.rootify:160` and the re-key block in `instantiate:145-166` - the new root
   after a real simultaneous turn *is* a multi-actor node (that is what a decoupled SushiGo root
   is), with one `PlayerDecisionStats` per acting player and, since the post-Stage-B revision of
   §4.3, `decisionPlayer` already equal to the old root's owner - the same agent. The re-key block
   leaves a multi-entry `statsByPlayer` alone, and `instantiate` recomputes the owner from the new
   root state, so `rootify` has only to leave the other players' `ActionStats` undisturbed.
3. `MCTSPlayer:189-190` - the assertion that `newRoot.decisionPlayer == gameState.getCurrentPlayer()`
   fails by construction for a reused multi-actor root and must be relaxed to "the current player
   is in the root's acting set".

**Test** (to add alongside §10.2): 3-player SushiGo, `reuseTree=true`, `decoupled=true`; play one
full simultaneous turn with all three seats MCTS; assert that the second search's root has
`inheritedVisits > 0` and that its `statsByPlayer` keys equal the new acting set. A second case
under `Information_Set` asserts only that the search completes, since reuse there is best-effort.

Unaffected, verified: `TreeStatistics:87-102`, `TreeRecorder`, `MCTSExpertIterationListener`,
`MCTSDecisionRecorder`, `allNodesInTree:1347-1363` — all traverse `children` or read
`getActor()`/`getActionValues()`, and all remain valid once §4.4's accessor migration lands.
`TreeStatistics`' `meanActionsAtNode` / `meanActionsExpanded` become hard to interpret
(primary-player action counts against joint child counts) but do not break; a follow-up could sum
over `statsByPlayer`.

One pre-existing gap worth knowing about: `MCTSMetrics` only ever inspects
`players.get(e.state.getCurrentPlayer())`, so in a simultaneous game only the turn owner's tree is
measured.

---

## 7. Hazards, and defects found during review

### 7.1 Defects that must be fixed, not worked around

**D1 — `SGGameState.redeterminise` discarded the observer's own card choice.** Fixed; this was the
Step 0 commit. The code was:

```java
setTurnOwner(playerId);
cardChoices = new ArrayList<>();                    // field reassigned...
for (int i = 0; i < getNPlayers(); i++) {
    cardChoices.add(new ArrayList<>());
    if (i == playerId)
        for (ChooseCard cc : cardChoices.get(i))    // ...so this iterates the NEW empty list
            cardChoices.get(i).add(cc.copy());
}
```

The preservation loop always iterates the freshly-created empty list, so the observing player's
committed choice is silently dropped (and were the list non-empty, the loop would throw
`ConcurrentModificationException` — proof it has never run non-empty). Because
`getCurrentSimultaneousPlayers()` decides who still has to move from `cardChoices.get(p).isEmpty()`
(:53-56), a redeterminised state reports an already-committed player as still to act — exactly the
double-play the comment there warns against. **Fix: snapshot the old list before reassigning.** This
is Step 0 of §8, because it is a behaviour change and must precede the no-op refactor.

**D2 — acting-set divergence between state and forward model.**
`SGGameState.getCurrentSimultaneousPlayers` selects only on `cardChoices.get(p).isEmpty()`, while
`SGForwardModel._afterAction` additionally skips players whose hand is empty. Reconcile before
Stage B.

**Resolved (JG, on review of Stage A):** the hand-empty clause was removed from
`SGForwardModel._afterAction`, so both sides now select on `cardChoices` alone. It was an incorrect
silent failure rather than a real case: SushiGo hands are always equal in size (a chopsticks play
takes two cards and returns one, net one, like everyone else), so a player with an empty hand
while others still hold cards cannot arise, and had it arisen the old clause would have declared
"all chosen" with that player never asked. Verified: the seven SushiGo unit tests and both
`ForwardModelTestsWithMCTS` SushiGo runs pass with the change.

**D3 — `ForestNode` had no test coverage at all** (now fixed - see §8). Nothing in
`src/test/java/players/mcts/` exercised `numDeterminizations > 1`. That matters here because `ForestNode` never calls
`instantiate` (§8, A3) — a refactor that assumes it does will NPE on every forest run and ship
undetected. Stage A adds a smoke test.

**D4 (cosmetic) — unreachable guard.** `backUpSingleNode:1129-1133`'s `if (bestAction == null)` is
dead: :1106 already dereferences `bestAction`.

### 7.2 Hazards to keep in view

- **Branching factor.** At a simultaneous node the child count is the *product* of the acting
  players' action counts, so child reuse is rare at low budgets. `DUCTNode` has exactly the same
  property; it is inherent to keying children by the joint action.
- **`SimultaneousAction.getString()` returns `""`**, so the debug output at `Game:425` and any
  listener logging the applied joint action produce blank lines. `AbstractGameState.recordAction` is
  unaffected, since only components are recorded.
- **`Game:462` builds the `SimultaneousAction` from a `HashMap`**, so its internal `LinkedHashMap`
  records hash order rather than player order. `equals`/`hashCode` delegate to `Map`, so child keys
  stay stable, but `execute` applies components in hash order.
- **`ChooseCard.equals`/`hashCode`** include the *mutable* `chopstickChooseDone` field. Safe today
  because `advanceState:589` and :265 both copy before use — but `SingleTreeNode:268` carries an
  explicit equals/hashCode-contract assertion aimed at precisely this shape of action, and
  `SimultaneousAction` delegates `hashCode` to its component map, so a mutating component makes the
  *joint* key unstable too.
- **Two players using chopsticks in one joint action.** `ChooseCard.execute` calls
  `setActionInProgress(this)`, so two `IExtendedSequence`s get pushed while only the top receives
  `_afterAction`, and `SGForwardModel._afterAction` returns early while any sequence is in progress.
  It *probably* unwinds correctly via the `playerId` filter in `ChooseCard` — but it is untested,
  and it is the only route by which `getCurrentSimultaneousPlayers()` returns a singleton mid-tree.
- **`SGGameState.getCurrentSimultaneousPlayers` throws from a getter** (:57-60) if it is ever called
  when every player has already chosen. Search drives copied states hard, so do not reorder the
  `isNotTerminalForPlayer` check at `SingleTreeNode:199` ahead of it.
- **`actionsFromOpenLoopState` goes stale.** :199 is an `if` with no `else` that clears the list.
  When the node's own player is not acting this iteration, the node keeps the *previous* iteration's
  actions, `treePolicy`'s loop condition sees a non-empty list, and `treePolicyAction` selects an
  action that is illegal in the current state. Fixing it is a behaviour change, so it belongs in
  Stage B, where the `else` branch drops the entry from `statsByPlayer` outright.
  *As landed in Stage B:* at a multi-actor node the candidate list of every player not acting on
  this visit was emptied, but the entry itself kept, so as not to discard that player's accumulated
  statistics. *Revised after Stage B (JG):* nothing is cleared. The tree policy's loop condition
  (`hasDecisionToMakeInTree`), `jointTreePolicyAction` and the backup all iterate the *current*
  visit's `actingPlayers`, so a player who acted here on an earlier visit keeps a candidate list
  that is never consulted until they act again, when `setActionsForPlayer` replaces it. A player
  who is in the acting set but for whom the game is over now throws instead of being given an
  empty list; `treePolicy` never expands a terminal state, so that can only fire if a game reports
  an eliminated player as still deciding. The sequential path is unchanged and still throws.
- **The turn owner after a simultaneous turn depends on how it was applied.** Applied one
  component at a time, `SGForwardModel._afterAction` hands the next turn to whoever chose last;
  applied as one `SimultaneousAction`, the turn owner is unchanged. Harmless, since the acting set
  is everyone either way, and `games.sushigo.SimultaneousActionTests` records it - but it means
  the *current player* of a state after a joint action in the tree is the searching player, while
  after the same turn in a rollout it is not. Since 8 September 2026 the tree no longer depends on
  it: a multi-actor successor is filed in the extra child slot (§9 item 1).

---

## 8. Stage A — the refactor, step by step

**Stage A is a provable no-op.** No new parameter, no behaviour change, no new concept — only the
storage shape and the seams that Stage B will edit. Each step below is one commit that compiles on
its own, and the full check set in §10.1 is rerun after **every** commit so a bisect is useful.

JG suggested two stages, and asked whether the first could be "keep `actionValues` as it is, add a
second structure for the other players". That variant is not taken: per §4.1 the conditional
`p == decisionPlayer ? ... : ...` would have to be repeated across five fields, not one, and it is a
shape that has to be unpicked again later. Splitting refactor-from-behaviour gets the same property
JG was after — a first stage that is provably behaviour-preserving — without the asymmetry.

### Step 0 — pre-Stage-A behaviour fix (own commit, before anything else)

Fix **D1** in `games/sushigo/SGGameState.java`: snapshot `cardChoices` before reassigning it. Add a
test in `src/test/java/games/sushigo/` asserting that after `state.copy(playerId)` on a state where
player *p* has committed a `ChooseCard`, *p*'s own choice survives and
`getCurrentSimultaneousPlayers()` no longer lists *p*.

This is a behaviour change, so it lands before any baseline is captured.

### Step A1 — capture the proof, on the otherwise untouched tree

New `src/test/java/players/mcts/StageAGoldenTests.java`, committed **before** step A2. Two
independent mechanisms:

**(i) RNG canary — the cheapest near-complete proof.** `rnd` is `root.rnd`, one `Random` shared by
every node in the tree (assigned at `instantiate:118`), so any draw added, removed or reordered
anywhere in the search diverges the next one. After each seeded `mctsSearch`, assert all of:

```java
root.getVisits()             // node accounting
root.fmCallsCount            // forward-model calls - catches rollout / opponent-model divergence
root.copyCount               // state copies
root.rolloutActionsTaken     // rollout length accounting
rnd.nextLong()               // the canary
```

The three counters matter because the rollout policy has its own separately-seeded `Random`, which
`rnd` alone would not observe.

**(ii) Golden tree digest — on `LMRGame` only.** Walk `root.allNodesInTree()` (BFS over the
`LinkedHashMap` `children`, deterministic) emitting per node
`depth | decisionPlayer | nVisits | terminalNode`, then for each `getActionValues()` entry **in
iteration order** — do not sort; the order is exactly what is being pinned —
`action | nVisits | validVisits | doubleToLongBits(totValue[i]) | doubleToLongBits(squaredTotValue[i])`.
SHA-256 the result and assert the literal.

Restricted to `LMRGame`, and this turned out to matter for a different and more fundamental reason
than anticipated. **`AbstractGameState.redeterminisationRnd` (:107) is deliberately unseeded, and
`copy()` reseeds that copy's RNG from it (:354)** - so any game whose forward model consumes
`gs.rnd` takes a different trajectory on every run of the same build. That is a framework design
decision (hidden information must not be predictable from the game seed), not something a test
should work around. LMRGame is reproducible precisely because it never consumes `gs.rnd`;
Dominion, which shuffles, is not - verified by capturing twice in separate JVMs and diffing, which
is worth doing before pinning any value.

So the Dominion scenarios are checked structurally (node type, sub-root count, total visits) rather
than exactly. That is still enough to catch a refactor that drops statistics, mis-keys an
`ActionStats` map or
throws; exact coverage comes from the LMR set, which runs Information_Set with full digests.

Configurations to cover, reusing the existing `TestMCTSPlayer` / `new Random(303897)` /
`new LMTParameters(302)` harness from `MCTSTreeSelectionTests`: `UCB`, `UCB_Tuned`, `EXP3`,
`RegretMatching`, `progressiveWideningConstant=1.5`, `initialiseVisits=3`, `pUCTTemperature=1.0`,
`progressiveBias=0.5`, each crossed with `backupPolicy` in
{`MonteCarlo`, `Lambda`, `MaxLambda`, `MaxMC`} — those four are what step A6 can break.

Apply mechanism (i) to the real-game configurations Stage A touches: Dominion built the way
`MCTSNodesAndVisitsTests.createGame` does, under `OneTree`, `SelfOnly`, `MultiTree`, `OMA`, `MCGS`,
`paranoid` and `reuseTree`.

Plus a two-line **`ForestNode` smoke test** (`numDeterminizations > 1`) — see **D3**.

*Acceptance: passes on the unmodified tree.*

### Step A2 — field to accessor, migrate external readers, then make the fields private

- Add `public Map<AbstractAction, ActionStats> getActionValues()` returning the existing field.
- Migrate the 21 `src/main` + 39 `src/test` field reads **by hand**, per §4.4. Includes the bare
  `actionValues` at `MCGSNode:70`, `currentNode.actionValues` at `OMATreeNode:84` (write it as
  `currentNode.getActionValues()` — a cross-node read, not `getActionValues(decisionPlayer)`), and
  `roots[i].actionValues` at `ForestNode:121`.
- Migrate the 28 `actionsFromOpenLoopState` reads to the existing public
  `getActionsFromOpenLoopState()`.
- **In the same commit**, make both fields `private`, so the compiler proves the migration complete
  and no intermediate state leaves two reachable paths to the same storage.
- Hand-check that `FeatureListener:128,129,247,256` and `MCTSExpertIterationListener:116-134` are
  untouched. They will not produce a compile error, which is precisely the risk.

*Acceptance: A1 unchanged.*

### Step A3 — move the storage into `PlayerDecisionStats`

- Add `players/mcts/PlayerDecisionStats.java` exactly as in §4.1 — package-visible, not a `record`,
  no `equals`/`hashCode`, `new HashMap<>()` with no capacity argument.
- `SingleTreeNode`: delete the five fields; add
  `protected final Map<Integer, PlayerDecisionStats> statsByPlayer = new HashMap<>();` and route
  *every* read through one accessor:

  ```java
  private PlayerDecisionStats statsFor(int actingPlayer) {
      if (actingPlayer != decisionPlayer)     // Stage A only - first line deleted in Stage B
          throw new AssertionError("Node has statistics only for P" + decisionPlayer);
      return statsByPlayer.computeIfAbsent(actingPlayer, PlayerDecisionStats::new);
  }
  ```

  `computeIfAbsent` is what covers `ForestNode`, whose constructor sets `decisionPlayer`, `params`,
  `forwardModel`, `rnd`, `MASTStatistics` and `roots` and returns — **it never calls `instantiate`**
  — and then calls `initialiseRootMetrics()`, whose :308 is `regretMatchingAverage.clear()`. With a
  bare `statsByPlayer.get(...)` that is an NPE on every forest run (**D3**).

  The one-line assertion is the highest-value part of this step. It converts the entire "wrong
  receiver" error class into a loud failure under tests already being run: `treePolicy` executes on
  the **root**, so a bare `decisionPlayer` at :502-505 is the root's and not `cur`'s; and
  `MCGSNode:140` calls `node.backUpSingleNode(...)` with no `decisionPlayer` guard, unlike
  `backUp:1002`.

- **`instantiate` must re-key, never allocate.** Today none of the five fields is touched by
  `instantiate` (:112-150) — they are initialised once by the field initialisers and **survive
  `rootify`** (:152-164, which calls `instantiate(null, null, newState)`). That is exactly how tree
  reuse keeps the root's statistics. Allocating a fresh `PlayerDecisionStats` there silently empties
  every reused root while leaving `nVisits` and `children` intact, and nothing throws because
  :263-296 repopulates `actionValues` for the currently available actions. Reached from `MCTSPlayer:196`
  (`reuseTree`), `MCTSPlayer:152-162` (MCGS reuse — and that path has **no** `decisionPlayer` guard,
  unlike `MCTSPlayer:189`, so the key itself can change), and `ToadMCTSPlayer:67`. Insert between
  :145 and :148:

  ```java
  // reproduces today's behaviour, where the five fields survive a re-instantiate
  PlayerDecisionStats mine = statsByPlayer.size() == 1
          ? statsByPlayer.values().iterator().next()
          : new PlayerDecisionStats(decisionPlayer);
  statsByPlayer.clear();
  statsByPlayer.put(decisionPlayer, mine);
  ```

- `initialiseRootMetrics:308` becomes `statsFor(decisionPlayer).regretMatchingAverage.clear()`.
  Keep `.clear()`; do **not** assign a fresh map — clear retains table capacity, and therefore order.
- Accessors return the **live** map and list, never a copy and never `unmodifiableMap`: :1074-1075
  mutates through `values()`, and `MCTSPlayer:388` / `MCTSMetrics:32,170` hold the list directly and
  depend on today's staleness semantics.
- Do not reorder anything inside :263-296. `nActions` at :282 is
  `Math.max(actionValues.size(), actionsFromOpenLoopState.size())` computed *mid-loop*, so it
  depends on the action's position in the iteration and feeds `stats.validVisits` (:284) and node
  `nVisits` (:291-292) whenever `initialiseVisits > 0`. Keep `children.put(action.copy(), null)` at
  :266 exactly where it is, inside the `if (!actionValues.containsKey(action))` guard, or
  `allNodesInTree()`'s BFS order changes and the digest is invalidated. Do not hoist the `params`
  mutation at :232-235 out of its guard.

*Acceptance: A1 unchanged; `TreeReuseTests` and `TreeReuseMCGSTests` green — those are what catch the
re-key error.*

### Step A4 — additive `int actingPlayer` overloads

Purely additive, so nothing can break. Per §4.4 step 2, only for the methods a Stage B caller will
invoke with a non-`decisionPlayer` value: `actionsToConsider`, `getActionsFromOpenLoopState`,
`getActionValues`, `actionVisits(AbstractAction)`, `actionVisits()` (the `int[]` form),
`validVisitsFor`, `treePolicyAction`, `actionValues(List)`, `ucbValue`, `rmValue`, `exp3Value`,
`getActionValue`, `getFullValue`, `getBiasValue`, `updateRegretMatchingAverage`,
`regretMatchingAverage()`.

Existing signatures stay as one-line delegates passing `decisionPlayer`. `actionsToConsider(List)`
must keep its 1-arg form — `MCTSMetrics:32,170` call it cross-class.

**Do not** add an `actingPlayer` parameter to `nodeValue(int)`, `actionTotValue(action, int)` or
`actionSquaredValue(action, int)` (§4.2).

### Step A5 — thread `actingPlayer` through the internals

Each internal fetches `PlayerDecisionStats pds = statsFor(actingPlayer);` once at the top and reads
through it. Arity errors catch every miss inside the class; the `statsFor` assertion from A3 catches
receiver confusion at test time.

### Step A6 — split `backUpSingleNode`

```java
protected double[] backUpSingleNode(AbstractAction actionTaken, double[] result) {
    ... :1055-1060 unchanged (discardStateAfterEachIteration) ...
    nVisits++;                                                        // node-level: stays here, runs once
    List<AbstractAction> actionsToConsider =
            updateActionStats(decisionPlayer, actionTaken, result);   // computes :1064 and RETURNS it
    ... :1092 onwards unchanged, using the returned list ...
}
```

`updateActionStats(int actingPlayer, AbstractAction actionTaken, double[] result)` is the current
:1064 plus :1067-1090, returning the `actionsToConsider` list. The tail re-fetches
`stats = getActionValues(decisionPlayer).get(actionTaken)`.

**The tail must use the returned list, not recompute it.** :1064 computes `actionsToConsider` after
`nVisits++`, and the tail reuses that same object at :1093, :1096 and :1105. `actionsToConsider`
(:533-549) is a function of node `nVisits` (:535) *and* of each action's `nVisits` via the tie-break
term at :545 — which `stats.update(result)` at :1090 has just incremented. Recomputing it after
`updateActionStats` returns can, with `progressiveWideningConstant >= 1.0`, change the list's size
or order and therefore: `updateEvery` at :1093 and whether `updateRegretMatchingAverage` fires;
`bestAction(actionsToConsider)` at :1105, which can reach `rnd.nextInt` at :1162 and **shift the
shared RNG stream for the rest of the search**; and `maxValue` at :1106-1109 under
Lambda/MaxLambda/MaxMC.

*Acceptance: `BackupTests` unchanged and green; A1 unchanged.*

### Step A7 — the acting-player seam

> **First bullet reverted on review.** JG: "It just hides the call to getCurrentPlayer(). Remove
> this from Stage A; I fail to see the value." The seam landed in `8ec3b5ac5` and was taken out
> again; the six sites call `getCurrentPlayer()` directly, as on master, and the helper arrives
> with its real body in Stage B (§9 step 2). The remaining bullets of this step stand.

- ~~Add `protected int soleActingPlayer(AbstractGameState s) { return s.getCurrentPlayer(); }` (§4.3)
  and route the six sites that ask "who acts here"~~: `instantiate:141`, `instantiate:144`,
  `treePolicy:513`, `expandNode:554`, `advanceState:587`, `nextNodeInTree:717`. The site list is
  kept because it is what Stage B routes through the helper.

  `treePolicy:513` is easy to miss: it is the `Closed_Loop` bypass that appends to `actionsInTree`
  without going through `advanceState`, and it is what `backUp:997-1005` asserts against.

- Do **not** add `actingPlayers` or an `actingPlayersAt(...)` helper yet. Nothing in Stage A consumes
  a `List<Integer>`, and dead code that merely looks correct is worse than none. They arrive in
  Stage B, where `setActionsFromOpenLoopState` actually loops over the acting set.
- Do **not** tidy the dead `decisionPlayer` assignment at :89, or `MultiTreeNode:31` (§4.3).
- Extract the guarded body of `setActionsFromOpenLoopState` into
  `private void setActionsForPlayer(AbstractGameState actionState, int actingPlayer)` writing into
  `statsFor(actingPlayer)`, so Stage B only has to wrap it in a loop. The :199 guard and the missing
  `else`-clear (§7.2) stay exactly as they are.
- Convert `computeAvailableActions(actionState, params.actionSpace)` at :200 to the 3-arg form with
  `actingPlayer`. This is a provable no-op: the 2-arg form *is* the 3-arg form with
  `getCurrentPlayer()` (`AbstractForwardModel:173-175`), and the :199 guard establishes
  `actionState.getCurrentPlayer() == decisionPlayer == actingPlayer`.
- **Leave `bestAction():1207` as the 2-arg form.** Draft 2 wanted it converted. It is in fact equal —
  `instantiate:144` assigns `decisionPlayer` from the same `state` that :1207 reads, and
  `bestAction()` is only called on roots — but the argument rests on a whole-program invariant
  (nobody reassigns `root.state`) rather than on a guard five lines above, and the conversion buys
  nothing until Stage B. Instead leave a tripwire immediately before it:

  ```java
  if (state.getCurrentPlayer() != decisionPlayer)
      throw new AssertionError("root state / decisionPlayer mismatch in bestAction()");
  ```

*Acceptance: A1 unchanged.*


### What actually landed, and where it differed from the plan above

Stage A is implemented, as seven commits on `duct`, in the order given. Every one of them was
checked against the full set in §10.1 plus `ForwardModelTestsWithMCTS#testSushiGoWithSeqUCT` and
`#testSushiGoWithDUCT`, and the golden values captured at A1 have not moved since.

Six things came out differently from the plan, five of them discovered by doing it and one by
review:

1. **The Dominion scenarios cannot be pinned exactly, for a better reason than the one predicted.**
   Not enum hash codes, but the unseeded `redeterminisationRnd` described in A1 above. Worth
   knowing generally: *no* test in this repo can pin exact search behaviour for a game that shuffles.

2. **`statsFor` creating on demand is the only population path, and `instantiate` never allocates.**
   The plan had `instantiate` re-key an object it might also have to create. Making `statsFor` the
   sole creation point is simpler and removes the failure mode outright: there is no code path that
   can hand a reused root an empty `ActionStats` map. `instantiate` only re-keys, for the case where
   `decisionPlayer` changes (the MCGS reuse path permits this; the standard path asserts against it).

3. **The reward-index / `ActionStats`-selector split is threaded as an object, not a second int** (§4.2), and
   that decision was forced rather than merely preferred: `nodeValue(int, int)` breaks `toString`'s
   `this::nodeValue` reference outright.

4. **Two API consequences of adding overloads**, neither anticipated:
   - the `int[]` form of `actionVisits` had to become `actionVisitCounts(int)`, since overloading on
     `int` collides with `actionVisits(AbstractAction)`;
   - adding two-argument `actionVisits` overloads makes `root::actionVisits` an *inexact* method
     reference, so javac stops inferring a comparator's type variable through it where the target
     type is not already fixed. Two call sites in `MCTSDecisionRecorder` became explicit lambdas.
     (A third, in `TestUndoOpponentFlank`, still compiles - its target type is fixed by `Stream.max`.)

5. **The `backUpSingleNode` recomputation hazard is real but not demonstrable.** Reusing the list
   returned by `updateActionStats` keeps the split identical by construction, but no configuration
   could be found where recomputing actually differs - with distinct action-heuristic values the
   widening sort is stable under the tie-break. Recorded as correctness by construction rather than
   as a fixed bug. A `progressiveWideningTight` scenario was added to the golden set anyway, chosen
   so the widened subset genuinely grows during the search (139 nodes rather than 201).

6. **The `soleActingPlayer` seam was reverted after review.** It landed as planned in
   `8ec3b5ac5`, a `getCurrentPlayer()` pass-through routed through six sites. JG's objection: it
   hides the call and has no value until its body changes. Removed; the six sites are back to
   direct `getCurrentPlayer()` calls, identical to master, and the helper is now a Stage B item
   (§9 step 2). The full Stage A test set (88 tests across the §10.1 classes) and both SushiGo
   forward-model runs pass after the revert. The `setActionsForPlayer` extraction and the 3-arg
   `computeAvailableActions` call from the same step are unchanged.

Two claims from §7.1 were confirmed by experiment rather than left as assertions: swapping
`actionValues` to a `LinkedHashMap` - a change to iteration order and nothing else - moves the LMR
digests, and the `SGGameState.redeterminise` defect (D1) fails the new test without its fix.

`ForestNode` (D3) now has coverage: it is exercised as a golden scenario, and confirmed to be the
one root that reaches `initialiseRootMetrics()` without ever calling `instantiate()`.

### Explicitly out of Stage A

`MCTSParams.decoupled` and its `_reset` clamps; `soleActingPlayer` (reverted, see item 6 above);
`actingPlayers` / `actingPlayersAt`; `jointTreePolicyAction`; the backup fan-out; MAST expansion into a fresh list; suppressing the child
placeholder; the `MCTSPlayer:307` assertion; the gating in §6; the `setActionsFromOpenLoopState`
stale-list `else`-clear; converting `bestAction():1207`; per-player `toString()` and its
`children.get(action).length` NPE; setting the root's `decisionPlayer` from `player.getPlayerID()`.

---

## 9. Stage B

Behaviour, behind `MCTSParams.decoupled`. The outline as reviewed comes first; what actually landed,
and where it differed, is at the end of the section.

1. Add `MCTSParams.decoupled` and the `_reset()` clamps (§5): `OneTree` only, and `reuseTree`
   off until §6.1 is done. `Closed_Loop` and `RegretMatching` are not clamped.
2. Add `List<Integer> actingPlayers` and `actingPlayersAt(state)`; add `soleActingPlayer` with its
   Stage B body (§4.3) and route the six sites listed under Step A7 through it, including
   `decisionPlayer = -1` and the throwing no-arg accessors at multi-actor nodes. Delete the Stage A
   assertion in `statsFor`.
3. Make `setActionsFromOpenLoopState` loop `setActionsForPlayer` over `actingPlayers`, using the
   3-arg `computeAvailableActions`, and **clear explicitly** when we are not acting — dropping the
   entry from `statsByPlayer` rather than resetting four fields individually (§7.2).
4. Add `jointTreePolicyAction(boolean explore)`: returns `treePolicyAction(decisionPlayer, explore)`
   unchanged when `actingPlayers.size() == 1`; otherwise calls `treePolicyAction(p, explore)` once
   per acting player and wraps the results in a `SimultaneousAction`. Suppress the
   `children.put(action.copy(), null)` placeholder on multi-actor nodes, so `children` only ever
   holds joint keys.

   4a. **`Closed_Loop` bypass.** `treePolicy:594-597` skips `advanceState` under `Closed_Loop` and
   appends `(getCurrentPlayer(), chosen)` to `actionsInTree` itself. At a multi-actor node
   `chosen` is a `SimultaneousAction`, and this must append the same `(-1, joint)` pair that
   `advanceState` appends there, so that `backUp` and `MASTBackup` see identical shapes on both
   paths and `actionsInTree` stays index-aligned with `currentNodeTrajectory`. That is the whole
   of the `Closed_Loop` work (§6); selection and `nextNodeInTree` need nothing.
5. Fan out the backup: for each acting player, `updateActionStats(p, componentAction, result)`; then
   run the policy tail once. The `RegretMatching` refresh (:1257-1263) goes *inside* the fan-out:
   `updateEvery` and `updateRegretMatchingAverage(p, actionsToConsider)` are per player, since each
   player's `actionsToConsider` list is their own, while the `nVisits` cadence it keys off is the
   shared node counter. `initialiseRootMetrics:339` likewise clears `regretMatchingAverage` for
   every entry in `statsByPlayer`, not only `decisionPlayer`'s (§6).
6. Expand `SimultaneousAction` in `MASTBackup` rather than in `advanceState`, so `actionsInTree`
   stays index-aligned with `currentNodeTrajectory` — but build a **fresh list**:
   `updateMASTStatistics:401-419` (`case Both`) aliases `rollout` and mutates it in place
   (`MASTActions = rollout; MASTActions.addAll(tree);`), so expanding joint actions there would
   corrupt `actionsInRollout`. Separately, `params.MASTActionKey.key(action)` at :1171 will fail on a
   `SimultaneousAction` for most game-specific `IActionKey` implementations, which is why the
   expansion has to happen before the key is taken.
7. Convert `bestAction():1207` to the 3-arg form with `decisionPlayer`, replacing the Stage A
   tripwire, and relax the `MCTSPlayer:307` assertion.
8. Add the §6 gating and the `toString()` work from §5.
9. ~~Reconcile **D2**~~ (done, see §7.1), and settle the two-chopsticks case in §7.2.

After Stage B, first: `reuseTree` under `decoupled` (§6.1), and remove its clamp.

### What actually landed, and where it differed from the outline above

Stage B is implemented on `duct`, as one change to `SingleTreeNode`, `MCTSParams`, `MCTSPlayer` and
`TreeStatistics`. The full Stage A check set (§10.1), the new Stage B classes (§10.2), both SushiGo
`ForwardModelTestsWithMCTS` runs and the seven SushiGo unit tests pass; every Stage A golden value
is unchanged, so the sequential path is still bit-identical to master. Where it differed:

1. **The root keeps `getCurrentPlayer()` as its decision owner.** In Stage B `soleActingPlayer`
   returned -1 at a multi-actor state, which was right for a child but not for the root, which is
   multi-actor *and* has an owner (§4.3), so three of the six A7 sites routed through the helper
   and the root assignment in `instantiate` stayed as it was. *After Stage B (JG)* the helper is
   gone: a multi-actor child takes the root's owner as its `decisionPlayer` (§4.3, revised), and
   `advanceState` and the `Closed_Loop` bypass record -1 for a joint action directly.
   In Stage B `expandNode` and `nextNodeInTree` stayed as they were, indexing the child array by
   the next state's `getCurrentPlayer()`, which after a joint action in SushiGo is still the
   searching player (§7.2, turn-owner note). *Revised 8 September 2026 (§12):* that made the tree
   depend on which of several simultaneously acting players a game reports as current, which
   Diamant decides differently from SushiGo and no game guarantees. The child array now has
   `nPlayers + 1` slots, and a successor in which several players decide lives in the extra one
   (`childSlot`), whatever its current player; a single-actor successor is filed under its
   current player as before. It is a separate slot, not any player's, because the same action can
   lead by chance to either kind of successor (Diamant: the cave continues with one player in it,
   or collapses and everyone re-enters), and filing the joint successor under the root player's
   slot let those two collide. One consequence: two joint successors of the same action with
   *different* acting sets (the cave continues with two players, or collapses and all four
   re-enter) now share a node, as open-loop chance outcomes normally do; in Stage B the turn-owner
   index happened to keep them apart.
2. **The one real bug was the reward index.** The selection internals (`ucbValue`, `exp3Value`,
   `rmValue`, `getActionValue`, and `bestAction(pds, ...)`) read `totValue[decisionPlayer]`, the
   §4.2 confusion made concrete: at a multi-actor node that is `totValue[-1]`. Each player's
   `ActionStats` are now indexed by their own `pds.player`, which coincides with `decisionPlayer` at every sequential node
   (the goldens prove it) and is each player's own reward at a decoupled one. Found by
   `testSushiGoWithSeqUCT` on the first run.
3. **Step 3's "drop the entry" became "empty the candidate list"** (§7.2).
4. **Steps 4a and 6 became simpler than planned.** `actionsInTree` holds one `(-1, joint)` pair
   per multi-actor node, from `advanceState` and the `Closed_Loop` bypass alike. `backUp` checks
   a joint entry against `isMultiActor()` rather than `decisionPlayer`, `backUpSingleNode` fans
   `updateActionStats` out per component, and `MASTBackup` expands the joint action per component
   *as it reads it* - no fresh list, because nothing ever mutates the list it is given.
5. **The non-Monte-Carlo backup tails fall back to Monte Carlo at multi-actor nodes** (§6).
6. **Step 1's clamps are as described in §5**: `decoupled` silently off for non-`OneTree` or
   forest configurations, no longer repeated on the node (§5); `reuseTree` skipped at a
   simultaneous root by `MCTSPlayer` rather than clamped in `_reset()`.
7. **Step 8's accessors.** In Stage B the no-argument accessors threw at a multi-actor node, via
   `statsFor`, and `getActor()` returned -1 there. *After Stage B (JG)* both read the root
   player's `ActionStats` (§4.3, revised). `nodeValue(playerId)` reads a per-player total kept on the
   node itself (`totValue`, accumulated in `backUpSingleNode` beside `nVisits`) rather than summing any
   player's `ActionStats`: with joint successors sharing one child slot (item 1) a node's acting set
   can differ between visits, so no single player's `ActionStats` need cover every visit, and "any
   acting player's `ActionStats` sum to the same total" no longer holds. `toString` prints one block per acting player; `TreeStatistics` sums action
   counts across players. `TreeRecorder`, `MCTSExpertIterationListener`, `MCTSDecisionRecorder`
   and `LearnedValue`, which all call `getActionValues()` per node, no longer throw on a decoupled
   tree, but at a node where the root player is not acting they see an empty `ActionStats` map. Follow-up.
8. **The two-chopsticks case works** (§7.2): `SushiGoDecoupledTests.chopsticksInsideTree` forces
   two players to hold Chopsticks, finds joint actions in which both use them, and checks that the
   node after any chopsticks joint action has a single acting player who is one of the users.
9. **`MCTSParams._reset()` order.** The `decoupled` read sits at the end of `_reset()`, after
   `numDeterminizations`, because the clamp depends on it.

Decoupled behaviour is now pinned the way Stage A pinned sequential behaviour:
`StageBDecoupledTests.decoupledTreesUnchanged` holds digests for eleven decoupled scenarios on the
simultaneous LMR fixture, and `decoupledOffIsExactlySequentialSearch` checks that the same fixture
searched with `decoupled=false` reproduces the Stage A `lmr.ucb` digest byte for byte - the §2
contract as a test.

---

## 10. Tests

JUnit 4, `@Before`, standard assertions, in `src/test/java/players/mcts/`, using the existing
`TestMCTSPlayer` node-factory hook the way `OMATests` and `MCTSNodesAndVisitsTests` do.

### 10.1 Stage A

1. **`StageAGoldenTests`** (§8, A1) — the RNG canary plus the LMR digest. This is what turns Stage
   A's "provable no-op" claim into an actual proof rather than a hope, and it is committed before
   the first refactoring commit.
2. **`ForestNode` smoke test** — `numDeterminizations > 1`, two lines, closing **D3**.
3. **SushiGo redeterminisation test** (§8, Step 0) — closing **D1**.

Existing classes to rerun after **every** Stage A commit, in descending order of value:

- `StageAGoldenTests` (new)
- `MCTSTreeSelectionTests` — asserts exact doubles out of `actionValues(List)`, `treePolicyAction`,
  `backUpSingleNode` and `backUp`, on the deterministic LMR substrate. The strongest single guard.
- `BackupTests` — exact `nVisits`/`totValue` on hand-built trees; catches the A6 recomputation error
  under Lambda/MaxLambda/MaxMC.
- `TreeReuseTests`, `TreeReuseMCGSTests` — catch the A3 re-key error. Non-negotiable.
- `MCTSNodesAndVisitsTests` — the only `Closed_Loop` coverage; asserts
  `sum(actionValues.nVisits) == nVisits`.
- `MCGSTests` — `MCGSNode:70` and the unguarded `backUpSingleNode` at `MCGSNode:140`.
- `OMATests` — `OMATreeNode:84` and the `getOMAValue` threading.
- `MultiTreeMCTSTests`, `MultiTreeMASTRolloutTest`.
- `RewardsForParanoiaTests` — the only place `totValue[i]` for `i != decisionPlayer` is asserted, so
  it is the guard on the §4.2 confusion.
- `RolloutTerminationTests` — already includes `GameType.SushiGo`, so it is the canary that Stage A
  did not accidentally start calling `getCurrentSimultaneousPlayers()`.
- `MASTRolloutMixture`, `UtilsTests`, `LoadFromJSON`, `players.DUCT.DUCTNodeTests`.
- `games.fmtester.ForwardModelTestsWithMCTS` — `testSushiGoWithSeqUCT` end to end, and
  `testSushiGoWithDUCT` as the untouched reference baseline.

Per `AGENTS.md`, run classes individually — never the full suite. `mvn test` is additionally broken
here (surefire's `argLine` references a JaCoCo `@{argLine}` token that is not configured):

```bash
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
mvn test-compile -Dmaven.test.skip=false
java -cp "target/classes;target/test-classes;$(cat cp.txt)" \
     org.junit.runner.JUnitCore players.mcts.StageAGoldenTests
```

### 10.2 Stage B

4. **`rootReturnsOwnAction`** — 3-player SushiGo, all three seats MCTS:
   `((ChooseCard) player.getAction(...)).playerId == player.getPlayerID()`. The regression test for
   §4.3, and the single most important test here.
5. **Synthetic decoupled fixture** — `LMRGame`/`LMRForwardModel` is a trivial stateless 2-player
   fixture with immutable actions. A `SimultaneousLMRGame` variant (~15 lines) overriding
   `getCurrentSimultaneousPlayers()` to return `[0, 1]`, plus the per-player
   `_computeAvailableActions`, lets the decoupled path be tested deterministically without SushiGo's
   chopsticks-and-reveal complexity. It must live in package `players.mcts`: `LMRGame` is in
   `src/test/java/players/mcts`, is hard-wired to 2 players, and `LMRAction`/`LMTParameters` are
   package-private.
6. **`jointChildKeys`** — every `root.children` key is a `SimultaneousAction` whose
   `getPlayerActions().keySet()` equals `actingPlayers`; no null placeholders survive.
7. **`visitAccounting`** — for **each** acting player,
   `sum(getActionValues(p).values().nVisits) == node.nVisits` and `validVisits >= nVisits`. The
   single node-level `nVisits` (§4.1) is what makes this meaningful across players.
8. **`statsTablesAreIndependent`** — after a seeded search on the synthetic fixture,
   `statsByPlayer.keySet().equals(actingPlayers)`, and the two players' `actionValues` maps are
   distinct objects with independently-moving counts.
9. **`backupCreditsOwnActionOnly`** — port of `DUCTNodeTests.backUpCreditsOnlyTheActingPlayer`, with
   a stubbed heuristic returning a distinct value per player.
10. **`chopsticksInsideTree`** — force one, then two players to hold played Chopsticks; 200
    iterations; assert no exception, and that the node after the joint action has
    `actingPlayers.size() == 1`.
11. **`actingPlayersMatchGameState`** — mirrors `DUCTNodeTests.decoupledStatsAtSimultaneousNode`.
12. **`toStringAndTreeStatisticsSurvive`** — with `discardStateAfterEachIteration=true`, both
    `new TreeStatistics(root)` and `root.toString()` run without NPE on a joint-keyed tree (§5).
13. In `src/test/java/games/sushigo/`: apply a hand-built `SimultaneousAction` covering all players
    and assert `cardChoices` cleared, hands rotated and equal-sized, and
    `getCurrentSimultaneousPlayers()` again returning everyone.
14. **`closedLoopJointTrajectory`** — the synthetic fixture (item 5) under `Closed_Loop`,
    `decoupled=true`; after a seeded search assert that every `root.children` key is a
    `SimultaneousAction` with exactly one non-null child, that `actionsInTree` after an iteration
    holds one `(player, component)` pair per acting player per multi-actor node visited (§9 step
    4a), and `visitAccounting` (item 7) holds. `MCTSNodesAndVisitsTests` stays the sequential
    `Closed_Loop` guard.
15. **`regretMatchingPerPlayer`** — the synthetic fixture with `treePolicy=RegretMatching`,
    `decoupled=true`; after enough iterations for at least one refresh, assert
    `statsByPlayer.get(p).regretMatchingAverage` is non-empty and sums to the refresh count for
    **each** acting player, and that `initialiseRootMetrics()` empties all of them.

*As landed:* `players.mcts.StageBDecoupledTests` (items 5 to 9, 12, 14 and 15, the sequential
cross-check, the non-OneTree gate, the backup-tail limitation, and the pinned decoupled digests),
`players.mcts.SushiGoDecoupledTests` (items 4, 10, 11 and 12 on real SushiGo, plus MAST, EXP3,
`Closed_Loop`, `RegretMatching`, the reuse skip through the real game loop, and a whole game with
mixed seats) and `games.sushigo.SimultaneousActionTests` (item 13, and joint-versus-sequential
application). The fixture is `players.mcts.SimultaneousLMRGame`.

`ForwardModelTestsWithMCTS.testSushiGoWithSeqUCT` runs MCTS over SushiGo with
`json/players/gameSpecific/SushiGo/SushiGo.json`, which sets `OneTree`, `MAST: Both`, `UCB_Tuned`
and `Information_Set`. It therefore exercises the joint path *with MAST active* and directly
validates the `MASTBackup` fan-out. It must keep passing.

---

## 11. Confounds that would invalidate an MCTS-vs-DUCT comparison

These matter as much as the merge itself, because the whole point of keeping `players/DUCT` is to
have something to compare against.

- **Forward-model call budgets are not comparable.** A decoupled tree makes *one* `fm.next` call per
  simultaneous turn while advancing three players; a sequential tree makes three. At the default
  `BUDGET_FM_CALLS` with `budget = 4000` (`PlayerParameters:16-17`) the decoupled agent therefore
  gets roughly 3x the effective depth, and that confound will dominate any `decoupled` ablation.
  `DUCTNode` is internally inconsistent about this too: `treePolicy` counts 1 per joint action,
  while `rollout` counts 1 per player. Use `BUDGET_ITERATIONS`, or count *n* forward-model calls per
  joint action. Note both SushiGo JSON configs currently use `BUDGET_TIME`, which is worse still.
- **The plain-UCB exploration term.** `SingleTreeNode:843` and `DUCTNode.computeUCB` must be
  reconciled before any comparison across `K`: they differ in whether `K` multiplies the term and in
  the denominator used.
- **Different exploration denominators.** DUCT uses `node.nVisits`; MCTS uses
  `validVisitsFor(action)`, which is `stats.validVisits` in open loop (:452-457). The numbers differ
  even at `K = 1`.
- **Redeterminisation must match.** DUCT re-copies with `gameState.copy(getPlayerID())` every
  iteration (`BasicDUCTPlayer:72-73`). That matches MCTS `Information_Set` — the default,
  `MCTSParams:31` — at `SingleTreeNode:344`, but **not** `Open_Loop` (:338, `copy(-1)`, no
  redeterminisation). Set `information` explicitly and identically on both sides.
- **No search over a shuffling game is reproducible between runs.**
  `AbstractGameState.redeterminisationRnd` (:107) is unseeded, and `copy()` reseeds that copy's RNG
  from it (:354), so any game whose forward model consumes `gs.rnd` takes a different trajectory
  every run. Any head-to-head between MCTS and DUCT therefore has to average over many games; a
  single seeded game proves nothing, and no test can pin exact search behaviour for such a game.
- **`DUCTNode.backUp` never increments `nVisits` on the newly expanded leaf**, because the leaf is
  created after `next()` returns and is not in `currentNodeTrajectory`. `SingleTreeNode` does count
  it. Node counts are therefore off by one per iteration between the two implementations.

---

## 12. Review history

**Draft 1 to 2.** Draft 1 proposed giving each node a set of extra `SingleTreeNode` instances — one
per acting player — used purely as statistics holders.

> **JG:** I like the approach to minimise intervention (and hence risk), but I am a little
> uncomfortable with this dual-use of SingleTreeNode, both as a node that can be traversed in a
> tree, and also as a store for statistics for other players. This could get confusing. The change
> to actionValues to make it a Map does introduce many changes...but these are all syntactically
> straightforward and I do not see that they require tricky logic to implement. Hence, I would far
> prefer this approach and avoid the use of SingleTreeNode for purposes beyond its primary
> responsibility.

Accepted; the sibling design was dropped and §4 rewritten around per-player statistics. One
refinement on the letter of the suggestion: it is not only `actionValues` that is per-player — four
other fields co-vary with it, so they are grouped into `PlayerDecisionStats` rather than becoming
five parallel maps (§4.1). Dropping the siblings also removed the `nVisits`-parity trap outright.

> **JG:** One possible stepping stone is to keep actionValues as is, and add a new
> `Map<Integer, Map<AbstractAction, ActionStats>>` that only applies for non-decision players. This
> would reduce the impact on the sequential code...but be a little messy with if..then... loops. It
> may (this is NOT an instruction) be something you want to do in two stages?

Two stages, yes — but split refactor-vs-behaviour rather than decision-player-vs-others; the
reasoning is at the head of §8.

> **JG:** [on the SushiGo `turnOwner` claim] This is not accidental, but was inserted deliberately
> as part of the conversion of Sushi Go to simultaneous actions. [...] turnOwner is really the same
> as 'currentPlayer'. Previously it has been overridden only by Extended Action Sequences.

Corrected; draft 1's "survives only by accident" framing is gone.

**Draft 2 to 3.** JG's three comments on draft 2:

> **JG (on §3.4):** I am a little wary of this (but it may be the best option). Could we amend MCTS
> (if we are in decoupled mode) to never use getCurrentPlayer(), and always use
> getCurrentSimultaneousPlayers(). If we are not decoupled, then getCurrentPlayer() is correct, as we
> are using Sequential UCT. If we are decoupled AND this node has multiple players, then we need to
> revert to referring to the decisionPlayer.
>
> **JG (follow-up):** If we have a simultaneous node in the tree for which the root player is not
> acting (totally possible), then the decisionPlayer is not a valid concept.

Adopted, including the follow-up, which is the sharper point: draft 2's `primaryActor` manufactured
a single decision player for nodes that do not have one. §4.3 now separates the three meanings that
`decisionPlayer` was carrying, keeps only two of them, and makes the third fail loudly.

> **JG (on §3.4's `currentPlayer`/`turnOwner` discussion):** In the case of Sushi Go, we have to set
> the turnOwner when we redeterminise the state, because agents may be playing with only a
> sequential understanding. Any game that implements simultaneous turns *must* still support
> sequential turns too, and provide the relevant support in redeterminise() etc. It will also have
> to support agents who use the existing computeAvailableActions(state)...which explicitly assumes
> it can use getCurrentPlayer(); if redeterminise does not override turnOwner, then this will not
> work and return actions for the wrong player. So getCurrentPlayer() is *NOT* a tie-break...it
> remains load-bearing for sequential agents.

Accepted in full. Draft 2's "demoted to a tie-break" framing is deleted, and the point is promoted
from a remark inside the design to §2, the contract that governs it.

> **JG (on §3.7, rollouts):** This is all fine I think — it emphasizes the point that games must
> support sequential play.

Agreed; §4.5 now derives it from §2 rather than arguing it locally.

Draft 3 also corrects, from a verification pass over the source: the `.actionValues` call-site
counts and the unsafe `sed` recipe (§4.4); the `(actingPlayer, playerId)` signature shape (§4.2);
the `MultiTreeNode:31` and `SGGameState:150` attributions (§4.3); and three latent defects in draft
2's Stage A that would each have broken it — `instantiate` allocating instead of re-keying,
`ForestNode` never calling `instantiate`, and the `backUpSingleNode` split recomputing
`actionsToConsider` (§8, A3 and A6). It adds four defects found in the existing code (§7.1).

**Stage A review (JG, 6 September 2026).** Comments on the landed Stage A code and on §6, with
what was done about each:

> **JG (on §6, `Closed_Loop`):** Can you recheck on Closed Loop. The state is not advanced, but is
> instead stored against the node on first creation. I don't see why this should be a problem with
> simultaneous moves?

Correct; the gate is withdrawn. Draft 3's "nothing to select against" overlooked that per-player
candidates come from the 3-arg `computeAvailableActions` on the stored state, which is what
`Closed_Loop` has. The only real touchpoint is the `actionsInTree` bypass in `treePolicy`, now §9
step 4a. §6 has the full argument.

> **JG (on §6, `RegretMatching`):** Should be fine with simultaneous moves... mark this for the
> next phase.

Taken further: it is in Stage B rather than a later phase, because the change is two loops over
the acting players (§6, §9 step 5). SelfOnly, MultiTree, OMA and `numDeterminizations` stay gated,
as JG confirmed.

> **JG (on §6, `reuseTree`):** Ditto for reuseTree. This needs to work with simultaneous moves.

Agreed as a requirement; deferred as work. §6.1 records why it fails today, what already works on
the history side, the three changes needed in `MCTSPlayer` and `rootify`, and the test, and §9
names it as the first item after Stage B. The clamp stays until then.

> **JG (in `SingleTreeNode.instantiate`, on the re-key block):** This comment is gibberish to me. I
> would like an explanation of what this code is doing - and why we ignore this if we have
> multiple players.

The comment was rewritten in place. In short: a reused root goes through `instantiate` again, the
statistics have to survive that as they did when they were bare fields, and `decisionPlayer` has
just been recomputed, so the single entry is moved to the new key. The standard reuse path asserts
the player has not changed, so it is a no-op there; the MCGS transposition path can re-home a node
created for a different player, and the old code silently kept its statistics, which the re-key
reproduces. Multi-entry `statsByPlayer` maps are left alone because their keys are real player ids that do not
depend on the state's current player.

> **JG (on `soleActingPlayer`):** I do not like this at all. It just hides the call to
> getCurrentPlayer(). Remove this from Stage A; I fail to see the value.

Removed; §4.3 and §8 A7 updated, and the helper moved to §9 step 2. Tests unchanged and passing.

> **JG (on §7.1 D2):** I have amended this, and removed the check on empty player hands; which was
> an incorrect silent fail.

Verified and recorded under D2.

Also noted by JG and not yet acted on: the "never substitute `LinkedHashMap`" wording in §4.1 and
in the `PlayerDecisionStats` javadoc overstates a Stage A invariance rule as a design principle, and
should be reworded to "preserve the map types `SingleTreeNode` already used" (the code already
does). JG's inline comment there is left in place until that is done. *Partly done (JG,
September 2026):* the `PlayerDecisionStats` javadoc now just describes a data holder; the map-type
note survives only in §4.1, with JG's comment still against it.

**Post-Stage-B simplification (JG, September 2026) and its review (8 September 2026).** JG
removed `soleActingPlayer`, `anyStats`, `decoupled()` and the unused overloads, gave a multi-actor
child the root's owner as `decisionPlayer` instead of -1 (§4.3, revised), stopped clearing
non-acting players' candidate lists (§7.2), and dropped the reuse-skip warning (§5). The review
found four defects in that commit, all fixed in the same session:

1. The "node created for one player is now multi-actor" assertion still tested `decisionPlayer
   != -1`, so every non-root multi-actor node threw on creation - 27 failures across the three
   decoupled test classes. It now tests whether the *previous* visit had a single acting player.
2. `expandNode` filed children under `decisionPlayer` in a decoupled search, while
   `nextNodeInTree` and `backtrack` look them up under the next state's current player. Children
   were never found again and were re-expanded every iteration: of 201 nodes created in a
   200-iteration Diamant search, 19 to 23 survived. First reverted to `getCurrentPlayer()`; then,
   on JG's point that a game may report any of the simultaneously acting players as current, the
   lookup was made consistent with the store and joint successors given the extra child slot (§9,
   item 1). A first cut that filed them under the root player's own slot collided in Diamant
   whenever the root player continued alone and the cave then collapsed, which is what settled
   the separate slot. Verified over six whole 4-player games from every seat: every node created
   is in its tree, no slot disagrees with the rule, and no node mixes joint and component keys.
3. The non-decoupled branch of the same line used the first of `getCurrentSimultaneousPlayers()`,
   which in Diamant after a cave collapse is not the turn owner, so plain sequential MCTS on
   Diamant failed with "Unexpected non-self current player". Same fix.
4. `nodeValue(playerId)` read the root player's `ActionStats` unconditionally and so returned 0 at the
   quarter or so of Diamant nodes where the root player is not acting. It was first changed to
   read the first acting player's `ActionStats`; the regression test written for it then showed that
   a node visited with different acting sets (shared joint slot, item 2) can leave that player's
   `ActionStats` a partial sum - one visit's reward divided by twelve visits, in one case. The node now
   keeps its own per-player `totValue`, accumulated in the backup, and `nodeValue` reads that
   (§9 item 7). `DiamantDecoupledTests.nodeValueWhereTheRootPlayerIsNotActing` pins it, and
   fails against both earlier bodies.

The `Closed_Loop` bypass was also brought back to recording -1 for a joint action, matching
`advanceState`. The eleven `decoupledTreesUnchanged` digests were regenerated: with the per-node
actor field masked, every tree is byte-identical to the Stage B one, so only the recorded owner of
multi-actor children changed. Two Stage B tests that asserted -1 were rewritten to the new
convention. `nonOneTreePolicyIsNeverDecoupled`, which asserted the removed node-level gate, was
deleted (§5).

**One node value, not two (JG, 9 September 2026).** `SingleTreeNode` carried two `nodeValue`
methods: the public one reading the node's own `totValue`, and a private one summing an acting
player's `ActionStats`, used as the baseline in `rmValue` and `exp3Value`. Two definitions of one
quantity had already drifted. `initialiseVisits` seeds each action's `ActionStats` with
`estimate * initialiseVisits` of fake value and raises node `nVisits` to match, but never touched
node `totValue`, so the public method divided an unseeded numerator by a seeded denominator and
returned a value no consumer wanted - notably `LearnedValue`, which subtracts it from action means
that do carry the seed. The seeding block now mirrors what it writes into the `ActionStats` onto
node `totValue`, and the private method is gone; `actionValues` calls `nodeValue(pds.player)`.

At a multi-actor node each acting player seeds only their own slot of `totValue`, so the seeds land
in disjoint slots and nothing is counted twice; the paranoid cross-terms are left out there, having
no principled owner. One approximation remains there: `nVisits` is clamped to the largest acting
player's fake visit count rather than the sum of them, so all slots share a denominator sized by
whichever player has the most actions, and a player with fewer stays a little diluted. It is exact
where the acting players have equally many actions, which covers Diamant (two each) and the
simultaneous LMR fixture (three each).

The sequential path no longer clamps at all. `nVisits` gains `initialiseVisits` per seeded action,
which reaches the same total as the clamp on a node's first visit and, unlike the clamp, also
credits an action discovered once the node already has real visits - an open-loop action set that
grows, as in Dominion or Diamant, used to add such an action's fake value to the numerator while the
clamp silently declined to fire. No pinned digest moved: the only scenarios that seed are the two
LMR fixtures, whose action set is the same on every visit.

With that in place the two methods agree exactly at every sequential node: node `totValue` and the
acting player's `ActionStats` receive the same seeds and the same per-visit result. A probe over the
LMR and simultaneous-LMR fixtures found visit counts identical everywhere and value differences of
at most 2.8e-14, purely the reassociation of one accumulated sum against a sum over a `HashMap`.
Regret matching amplifies that: `Math.max(0.0, regret)` and the pdf that follows turn a last-bit
difference into a different action, so `lmr.regretMatching` and the decoupled `regretMatching`
digests moved. Both were regenerated. No other scenario changed, `initialiseVisits` included, since
it runs UCB and never consults node value.
