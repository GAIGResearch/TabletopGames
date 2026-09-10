# `players.DUCT`: the standalone decoupled-UCT baseline

`BasicDUCTPlayer`, `DUCTNode` and `DUCTParams` are a small, self-contained decoupled UCT agent for
simultaneous-move games. Decoupled UCT is also available inside the main MCTS agent, as
`MCTSParams.decoupled`; that integration is documented in `players/mcts/DecoupledUCT.md`, and it is
what should be used and extended. This package is kept as an independently verifiable reference to
compare against, and is deliberately minimal: uniform-random rollouts, three budget types, no MAST,
no action heuristics, no tree reuse and no alternative backup policies.

It is not registered in `PlayerFactory` by name; it is reached through the class-name fallback in
`PlayerFactory.createPlayer`, as `json/players/gameSpecific/SushiGo/SushiGoDUCT.json` does.
`DUCTNodeTests` covers the tree, and `ForwardModelTestsWithMCTS.testSushiGoWithDUCT` runs it end to
end.

## Comparing it with decoupled `MCTSPlayer`

Several differences between the two implementations will dominate a naive head-to-head and have to
be controlled for:

- **Budgets.** Use `BUDGET_ITERATIONS`. Under `BUDGET_FM_CALLS` a decoupled tree makes one forward
  model call per simultaneous turn while a sequential rollout makes one per player, and `DUCTNode`
  itself counts one per joint action in the tree but one per player in the rollout. Both SushiGo
  JSON configurations currently use `BUDGET_TIME`, which is worse still.
- **The UCB exploration term.** `DUCTNode.computeUCB` and `SingleTreeNode.ucbValue` differ in
  whether `K` multiplies the term and in the denominator (`DUCTNode` uses the node's visits,
  `SingleTreeNode` the action's valid visits). The numbers differ even at `K = 1`.
- **Redeterminisation.** `BasicDUCTPlayer` copies with `gameState.copy(getPlayerID())` on every
  iteration, which matches `MCTSParams.information = Information_Set` and not `Open_Loop`. Set it
  explicitly and identically on both sides.
- **Visit counts differ by one per iteration.** `DUCTNode.backUp` does not increment `nVisits` on
  the newly expanded leaf, because the leaf is created after the trajectory is recorded;
  `SingleTreeNode` does count it.
- **Nothing on a shuffling game is reproducible between runs.** `AbstractGameState.copy()` reseeds
  from an unseeded source, so a single seeded game proves nothing and comparisons must average over
  many games.
