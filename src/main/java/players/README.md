# ComparisonPlayer

`players.ComparisonPlayer` seats several agents in one position and records how similar their action preferences are, decision by decision. It writes a pairwise agreement matrix as CSV, which is what the policy-distance analysis in the JSON-Bag paper runs on.

It is an `AbstractPlayer`, so a run seats it like any other agent, but it plays a random legal action rather than the action its members prefer. Use it to measure agents, not to play well.

## What happens at each decision

1. If there is exactly one legal action, it takes it and records nothing.
2. Otherwise it asks every wrapped agent for a policy over the legal actions:
   - a wrapped `MCTSPlayer` is asked to search the state, then the policy is `getSoftmaxPolicy(actions, 1)`, a softmax over root visit counts;
   - any other agent is sampled `nSamples = 100` times through `copy()`, so the policy is a count per action, plus one decision from the live agent so its own state advances as it would in a real game.
3. Each pair of policies updates the agreement matrix with their Jensen-Shannon distance. The diagonal counts decisions.
4. It returns a uniformly random legal action, then calls `setLastAction(chosenAction)` on any wrapped `MCTSPlayer`, so a tree built for an action nobody played is not left stale. That call is why `MCTSPlayer.setLastAction` exists.

## Output

`<outputPath>/<csvName>`, written when the game finalises. The first row holds the agent names when they came from files, then one row per agent, with the diagonal holding the number of decisions and the off-diagonal entries the summed JSD to each other agent.

`initializePlayer` looks for that CSV first: if it exists, accumulation continues from it instead of zero, which lets several runs feed one matrix. Delete the file to start a fresh measurement.

`toCSV` rewrites the whole file at the end of a game, so an interrupted run leaves whatever the previous run wrote.

## Constructing it

```java
// From instances. Copies are taken, so the agents you pass in are untouched.
ComparisonPlayer cp = new ComparisonPlayer(List.of(mcts, osla, randomPlayer), "Experiments/agreement");

// From a directory of player JSONs. Every file becomes one member, named after the file.
ComparisonPlayer cp2 = new ComparisonPlayer("json/players/gameSpecific/Wonders7", "Experiments/agreement", "Wonders7Agreement.csv");
```

The first constructor names the output `AgreementMatrix.csv`. The second takes the name as an argument and prints the member names as it loads them.

## Wiring it into a run

Wrapped agents need a forward model, and `ComparisonPlayer` forwards what the framework gives it, so seating it through `Game` or `RunGames` is enough:

- `setForwardModel` propagates to every member;
- `initializePlayer`, `registerUpdatedObservation`, `onEvent` and `finalizePlayer` are forwarded too.

## Cost and caveats

- A decision costs one search per MCTS member and 100 sampled games per other member, so a run is roughly two orders of magnitude slower than a normal game per decision point.
- The action actually played is random, so the trajectory is nobody's policy. The matrix measures agreement at the states the run happens to visit.
- Only the policy-JSD update is wired in. `simpleAgreementMatrixUpdate` (exact action equality) and `jaccardSimilarity` exist but nothing calls them; the exact-equality path is described in the code as unreliable for high-variance agents.
- The JSD helpers here predate the ones now in `utilities.Utils`; that pair should be folded into `Utils.jensenShannonDistance(double[], double[])` when someone next touches this class.
- `copy()` prints `AAAAA` to stdout, left over from debugging.
- `nSamples` is a field, not a constructor argument.

---

Other classes in this package are the individual agents and `PlayerFactory`, which builds them from JSON.
