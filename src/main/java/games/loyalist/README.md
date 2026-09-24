# Loyalist (충신: 안개 속의 왕권)

An experimental native TAG implementation of the **five-player** Loyalist rules. One public king governs with four ministers: one loyalist, two mutually unacquainted magnates, and one spy. Ministers control hidden treasuries, make potentially false public reports, and privately alter a communal bag of resources and signed power cards. The king rewards, exiles, and reorganises the court while attempting ten successful affairs before four failures.

The implementation is suitable for headless games using TAG's normal player and tournament APIs. It is a new Java implementation, not a wrapper around the original Python engine. There is no bespoke Swing GUI, web hosting service, account system, or matchmaking service in this contribution.

## Rules and provenance

The authoritative rules for this implementation are the included [five-player Korean rulebook](RULEBOOK_KO.md), version **0.6.9**. SHA-256 of that source document:

`b51c3c7402d423c7cdec7a6927d85ea4afa00dd7fc19d6b18955ba0689aea563`

The pre-existing Python state machine was consulted as a secondary implementation reference. The Java implementation has targeted rules tests and information-boundary tests; this does **not** establish complete semantic equivalence with every possible original-engine execution.

| Rulebook section | Java implementation |
| --- | --- |
| 3–4: five roles, royal office assignments, starting resources, private cards | `LoyalistForwardModel._setup`, `ASSIGN_OFFICES`; `LoyalistGameState.Role` |
| 5: role, treasury, wealth, hand, and bag information boundaries | `LoyalistGameState._copy`, per-seat knowledge ledgers, private/public transcripts |
| 6: three event stages and 3/3/4 successful affairs | `eventDecks`, `eventDiscards`, `stage`, `prosperity` |
| Phase 1: reform start effects, two-card event choice, one-time reform proposals, ordered purchases, disaster draw | `startRound`, `EVENT_DRAW`, `REFORM_PROPOSAL`, `PURCHASE`, `DISASTER` |
| Phase 2: P/W/M supply, potentially false report, audit | `SUPPLY`, `SUPPLY_REPORT`, `AUDIT` |
| Phase 3: first nomination, each active minister once, theft limits, contributions, card play, inspection, testimony | `FIRST_NOMINATION`, `INTRIGUE_*`, `TESTIMONY`, `NEXT_NOMINATION` |
| Phase 4: score, reform confirmation/rejection, optional reward, return and independent success bonus | `settle`, `REWARD`, `returnResources` |
| Phase 5: optional exile, half confiscation, blind card selection, office reassignment including multiple offices | `exile`, `EXILE_CARDS`, `REASSIGN_OFFICE` |
| Phase 6: optional treasury reports, W→M before P→W, exchange restrictions, salary, inspector appointment, return from exile | `TREASURY_PLAN`, `CONVERT_*`, `EXCHANGE`, `SALARY`, `ROUND_END` |
| 8: crisis-dependent buy/sell exchange rates | `buyRate`, `sellRate` |
| 9–10: final purge, minimum/strict magnate wealth threshold, exiled-player exclusions, shared magnate victory | `FINAL_PURGE`, `finish` |
| 11–13: all 17 event cards, 11 reforms, and the exact 40-card signed deck | `LoyalistData`; reform checks in the forward model |

### Reform coverage

1. **Land reform:** creates one W token at each subsequent round start.
2. **Three-army command:** adds ceil(original military supply / 2); later embezzlement does not reduce it.
3. **Daedong tax:** allows W supply even when the event would normally exclude it.
4. **Border council:** king chooses one of two events; the other returns to the bottom.
5. **Impartiality:** removes one most-negative card before scoring.
6. **Centralisation:** owner selects the two cards retained; theft caps become 2 per minister and 3 per round.
7. **Royal inspector:** king appoints the next inspector; that minister can inspect wealth instead of manipulating the bag, sharing the result only with the king.
8. **Informants:** only the king sees the chosen minister's hand sum and the chosen target.
9. **Treasury audit:** only the king sees the selected exact treasury balance and target.
10. **Forced levy:** optional royal order for an active minister to transfer one private token to W, if able.
11. **Sogo militia:** on an M-eligible affair, P may be supplied in pairs for one bag token per pair.

## Agent observation contract

TAG passes `state.copy(activePlayer)` to a player. This implementation enables `CoreParameters.competitionMode` during setup because TAG's generic debug action history otherwise includes private action parameters. An agent should use the observation supplied by `core.Game`, not a retained reference to the authoritative game.

`copy(-1)` is a full simulator copy. `copy(playerId)` generates an alternative world:

- The viewer keeps their own role, hand, wealth, assigned treasury balances, public facts, private transcript, and still-current private observations.
- Hidden roles are reassigned while retaining exactly one loyalist, two magnates, and one spy. Magnates do not learn one another's identities.
- Unseen cards are redistributed from the exact forty-card multiset. Known hand sums and known card subsets constrain this redistribution. Unseen event-deck order is shuffled while retaining the bottom-card suffix that the king personally placed there under Border council.
- Hidden token holdings are resampled under a public interval for the total physical token inventory. Before any hidden multiplier return, the total is exactly conserved; any sampled total is fully allocated across the unseen holdings. The exact unseen original treasury, wealth, and bag quantities are not supplied to the player.
- A minister sees the current bag only at their own permitted supply/intrigue turn. Earlier observations remain historical memories rather than following later players' changes. Bag card values are an unordered multiset; neither the observation nor its transcript exposes insertion order or identifies the disaster card by position.
- The current theft action menu reveals the legal maximum. The precise earlier cumulative theft is sampled among values compatible with that maximum, not exposed as a more precise hidden fact.
- Public reports and testimony remain **claims**, never substituted for authoritative quantities.
- Private reform outcomes do not enter another seat's current knowledge or private transcript.

The sampler is **approximate information-set determinization**, not a Bayesian posterior reconstructed from the entire game. It preserves directly represented observations and physical card inventory, but does not infer every historical resource-flow, deception, or strategic-policy constraint. Sampled holdings and generic AI performance must not be treated as a validated balance model. Ended games expose the complete final state and mechanical private transcripts for retrospective analysis; this implementation does not store private human conversations.

The default framework UCT/MCTS players are search agents. They do not call an LLM and do not train the original game's PPO model. Information-set MCTS is the appropriate mode for this implementation; copying a hidden-information state does not make a perfect-information search setting an equivalent experiment.

## Explicit modelling choices and differences

### Communication

The physical game permits unrestricted public speech and lies. TAG search requires a finite action space, so numeric supply/treasury reports use integers `0..maximumReport` (default 30). Intrigue testimony has eight fixed statements: help, no theft, sufficient/insufficient bag, and suspicion of one of the four ministers. Reports are chosen independently of the actual private action; false claims are legal.

These statements are an **AI communication abstraction**, not a faithful model of natural-language persuasion, memory, or deception. The Java classes support the mechanical game, but this contribution does not implement a human chat interface. Human participants would need a separate shared conversation channel and an adapter/UI to use this implementation interactively. No claim is made that automated play estimates the full social game's experience.

### Resource orders and ambiguous details

- Intrigue uses the reference controller's order: **steal → contribute private wealth → play cards → testify**. The physical rulebook permits freely combining these operations; donation-before-theft and repeated intra-turn reorderings are not modelled here. For example, donating and immediately reclaiming one's own tokens to consume the round theft budget is not an available sequence. This is an explicit controller-order abstraction, not a claim of exhaustive action-sequence equivalence.
- When ordinary P supply and Sogo pair conversion are both available, a supplier chooses one of those modes for that supply action, following the reference controller. Mixing both modes within the same supply action is not represented.

- The king can request any whole-number treasury conversion from zero through a public upper bound. The executor applies as many pairs as exist. Thus the legal action list does not reveal hidden stocks. W→M is completed before P→W; freshly created W cannot be chained into M in the same round.
- **Sogo militia follows the written 2 P→1 token rule.** The reference Python code also accepted odd P quantities and placed the odd remainder into the bag at 1:1. That undocumented odd remainder is not implemented here.
- Exile card confiscation is an explicit royal selection of facedown card positions. The reference Python controller selected those positions randomly. Both discard floor(hand size / 2) without revealing values to the king.
- Recovered offices may be left vacant or assigned independently by the king, including multiple offices on one active minister. This follows the rulebook's optional wording; the reference Python controller automatically found a replacement when none was specified.
- Treasury-report requests are optional. When requested, existing active P/W/M officers respond in that order.
- Centralisation choices are resolved after Phase 1 purchases, after successful Phase 4 return, after Phase 6 exchanges, and after salary, following the reference controller's batching.
- Role 0 is always the public king. The four hidden roles are shuffled among seats 1–4. Three-/four-player variants, hidden bloodline roles, original dialogue policies, original artwork, and PPO checkpoints are outside this five-player port.

## Tests

The added JUnit 4 tests are deliberately scoped to this game:

- `LoyalistRulesTest`: supply and theft limits; specific reforms; event returns and success-bonus exceptions; market restrictions; exile; final-purge and victory conditions.
- `LoyalistInformationTest`: paired hidden states must yield indistinguishable observations under the same sampler seed; private memories, temporal bag visibility, role/card/token isolation, exact card inventory, and repeated copying.
- `LoyalistIntegrationTest`: seeded random and information-set MCTS games through TAG, including legal-action agreement between the real state and repeated player observations.

Run from the TAG checkout with Java 21 and Maven:

```sh
mvn test -Dmaven.test.skip=false \
  -Dtest=LoyalistRulesTest,LoyalistInformationTest,LoyalistIntegrationTest
```

Build the standard runtime and run random self-play:

```sh
mvn package
java -jar target/TAG.jar RunGames game=Loyalist nPlayers=5 \
  playerDirectory=json/players/random.json matchups=5 byTeam=false
```

For the supplied, untuned information-set MCTS baseline, replace the player file with `json/players/gameSpecific/Loyalist.json`. It uses 32 iterations per decision and short random rollouts. Use the full `TAG.jar` for `RunGames` with the default metrics listener: the slim `TAG-pytag.jar` excludes the Tablesaw dependency that listener requires.

Passing these tests supports the tested rules and observation boundaries; it is not a strength, fun, balance, or complete rule-equivalence claim.
