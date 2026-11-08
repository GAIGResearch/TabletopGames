# Ventlife agent search log

Goal: the strongest agent for 4-player Ventlife at 64ms per decision. Baseline: `Ventlife_baseline.json` (MCTS,
rolloutLength 0, MaxMC backup, K 0.01, LeaderHeuristic, tree reuse).

Experiments are run with `scripts/runExp.sh` (several RunGames processes in `mode=random`, 4 players, `budget=64`,
listener `json/listeners/VentlifeMetrics.json`) and aggregated by `scripts/aggregate.py`. Each `expNN` directory holds
the agent pool for that experiment. Win rates count a shared first place as a fraction of a win; +- is one standard
error.

## Exp00 - sanity check (8 games: baseline, osla, random)

`osla` (one-step look-ahead on the game's default heuristic: own score as a share of all scores) beat the baseline
in 10 of 13 shared games. MCTS tree statistics (`MCTSMetricsListener`, 2 games) show why:

| decision | median actions at root | median iterations in 64ms |
|:--|--:|--:|
| tile placement | 363 | 70 |
| creature placement | 131 | 86 |

The baseline cannot even expand every root action once: with FPU on, it chooses among the first ~70 actions in list
order. Each iteration costs ~0.9ms, dominated by `computeAvailableActions` (~250us per call on average, against
~30us for a copy, ~30us for `next` and ~30us for four `getGameScore` calls; profiled over 180 random games).

Conclusions: (1) action ordering/pruning matters far more than search depth; (2) forward-model speed is the main
limit on iterations.

## Forward-model speed-ups - REVERTED

I briefly optimised the forward model (cheaper tile-placement generation, cached `Hex` neighbours, an early exit in
`CreatureStep.hasPlacement`; behaviour-identical, verified by trajectory hashes and the unit tests). The game code is
off limits, so all of it has been reverted: the game package is now identical to the original apart from the new
`ai` and `metrics` sub-packages. Exp02-04 were run with the faster forward model; that only affects the MCTS
agents (baseline, mctsH, mctsHPW), which then had roughly twice as many iterations as they get now. The greedy agents
barely depend on speed. From Exp05 on, everything runs on the original game code.

## Exp01 - baseline, osla, random (90 games)

| agent | win | ordinal | score |
|:--|--:|--:|--:|
| osla | 0.59 | 1.59 | 63.0 |
| baseline | 0.16 | 2.60 | 41.7 |
| random | 0.00 | 3.30 | 29.5 |

Vent Sponges are the biggest earner (osla 44 points when in play, out of a maximum of 48); Fish and Tube Worms
contribute almost nothing for any agent.

## Exp02 - greedy agents (300 games)

New code in `games.ventlife.ai`: `VentlifeScorer` (all players' scores in one pass, unit-tested against
`getGameScore`), `VentlifeHeuristic` (own score - oppMaxWeight * best opponent - oppMeanWeight * mean opponent),
`VentlifeActionHeuristic` (one-step look-ahead on a state heuristic) and `VentlifeGreedyPlayer` (one-step greedy;
for a tile, optionally plays the best `tileLookahead` tiles out to the end of the turn greedily).

| agent | win | ordinal | score |
|:--|--:|--:|--:|
| osla | 0.435 | 1.91 | 58.7 |
| greedy k0, own - max opp | 0.383 | 2.11 | 58.3 |
| greedy k10, own - max opp | 0.333 | 2.25 | 56.8 |
| greedy k0, own only | 0.140 | 2.53 | 53.7 |
| greedy k10, own only | 0.122 | 2.78 | 51.7 |
| baseline | 0.085 | 3.36 | 45.0 |

* Taking the opponents' scores into account matters a lot: it makes the agent place plateau tiles (45-63% of tiles,
  against 15% for own-score greedy) that cover opponents' creatures (0.5-0.8 per tile against 0.1).
* osla's default heuristic (own share of total score) is to first order own score - mean opponent score.
* Playing the turn out after the tile (k10) did not help (differences within noise).

## Exp03 - heuristic weights and an exposure penalty (600 greedy games, k0)

Exposure: the points of each player's creatures on positions some plateau tile could now cover (sponges and worms
lose everything, other species `exposureMoveFactor` (0.5) of their score; a crab counts 3, a fish 5 if in a shoal)
are subtracted, times `exposure`, before the opponent weighting.

| agent | win | ordinal | score |
|:--|--:|--:|--:|
| mean opp, exposure 0.5 | 0.446 | 1.89 | 58.5 |
| mean opp, exposure 1.0 | 0.439 | 1.83 | 58.3 |
| max opp, exposure 0.5 | 0.396 | 2.08 | 56.5 |
| 0.5 max + 0.5 mean | 0.125 | 2.83 | 50.2 |
| mean opp | 0.124 | 2.84 | 49.5 |
| osla | 0.123 | 2.86 | 48.5 |
| max opp | 0.096 | 3.13 | 45.9 |

The exposure penalty is a big gain (beats the same agent without it ~75-80% head to head): the agents now avoid
leaving creatures where they can be covered, and value covering opponents. Weighting the mean opponent beats the
max opponent.

## Exp04 - MCTS with the Ventlife heuristic (300 games; faster forward model, see above)

`mctsH` is the baseline with the heuristic swapped for VentlifeHeuristic (mean opp, exposure 0.5). `mctsHPW` adds
`VentlifeActionHeuristic` with progressive widening (C 2, exponent 0.5). `k10` is the greedy agent playing out the 10
best tiles to the end of the turn.

| agent | win | ordinal | score |
|:--|--:|--:|--:|
| greedy, mean opp, exposure 0.5, k10 | 0.400 | 1.91 | 63.2 |
| mctsHPW | 0.371 | 1.98 | 61.6 |
| greedy, mean opp, exposure 0.5, k0 | 0.342 | 2.03 | 60.6 |
| mctsH | 0.125 | 2.75 | 50.0 |
| baseline | 0.013 | 3.82 | 30.1 |

* Every agent using the new heuristic beats the baseline 86-97% head to head.
* mctsHPW manages only ~6 iterations per decision, because the one-step action heuristic is evaluated for every
  action at each new node (~450 tile placements at ~60us each): it is effectively greedy with overheads.
* mctsH gets ~130 iterations but cannot even try each root action once; it needs action ordering.
* With the exposure term, playing out the turn after the tile (k10) now helps a little (55% head to head against k0).

## Faster evaluation inside the agents (game code untouched)

* `VentlifeFastEval`: the heuristic value after a creature placement, from the local changes it makes (own score,
  shoal merges, plateau majorities, neighbouring Octopuses, and their exposure), without copying the state.
  Unit-tested equal to copy + `next` + heuristic on 563k placements.
* `VentlifeTileSim`: applies a tile to a copy without the forward model's bookkeeping (which lists every creature
  placement after the tile). Unit-tested to give the same field, creatures and supplies on 215k placements.
* Exposure computed with one pass per Black Smoker (unit-tested against the definition).
* `VentlifeGreedyPlayer` uses these, and now respects the budget: it values tiles in random order up to 70% of the
  budget, and starts no play-out after 90%.

Late in the game a tile decision has ~620 options; each costs ~17us (copy) + 9us (scores) + 16us (exposure).

## NTBEA round 1 - greedy heuristic weights (3 runs x 1500 games, opponents 3 x `g_mean1_exp05`, ordinal)

Search space `ntbea/greedyHeuristicSS.json` (48000 settings). Recommendations (final estimate over 300 games):

| run | ordinal | oppMax | oppMean | exposure | oppExposure | moveFactor | crabValue | endScaling | fishSeed |
|:--|--:|--:|--:|--:|--:|--:|--:|:--|--:|
| r0 | 1.61 | 0 | 1.5 | 0.75 | 0.25 | 1.0 | 4 | true | 0 |
| r1 | 1.60 | 0 | 1.5 | 1.5 | 0.25 | 0.75 | 6 | false | 1 |
| r2 | 1.65 | 0.25 | 1.25 | 1.0 | 0.5 | 0.75 | 4 | true | 2 |

Consistent: a heavy weight on the mean opponent (at the top of the range), a high penalty on own exposure and a low
one on the opponents'.

## Exp05 - tuned weights and tile look-ahead in a mixed pool (360 games)

| agent | win | ordinal | score |
|:--|--:|--:|--:|
| g_mean1_exp05_k10 (previous best) | 0.319 | 2.24 | 56.2 |
| t0_k0 | 0.311 | 2.28 | 56.3 |
| t0_k10 | 0.293 | 2.22 | 57.4 |
| t0_k50 | 0.278 | 2.28 | 56.6 |
| t1_k0 | 0.271 | 2.23 | 56.4 |
| t2_k0 | 0.268 | 2.30 | 55.5 |
| baseline | 0.010 | 3.92 | 27.3 |

All within noise of each other (pairwise 0.41-0.59). The NTBEA gains (ordinal 1.6 against three copies of the old
agent) do not carry over to a mixed pool: the tuned, more spiteful weights exploit a field of identical, less spiteful
opponents. Wider tile look-ahead (k50) does not help either. Lesson: evaluate against mixed opponents, not copies of
one agent. Greedy play with this heuristic looks to be on a plateau.

## Diag01 - can search beat greedy given time? (60 games, 1000ms per decision)

`mctsHPW` (MCTS, rollout 0, MaxMC, VentlifeHeuristic mean opp + exposure 0.5, progressive widening on the new fast
`VentlifeActionHeuristic`, which uses VentlifeTileSim and VentlifeFastEval) as focus player against three
`g_mean1_exp05` greedy agents, everyone at 1000ms:

| agent | win | ordinal | score |
|:--|--:|--:|--:|
| mctsHPW @1000ms | 0.533 | 1.77 | 58.5 |
| g_mean1_exp05 (x3) | 0.156 | 2.74 | 50.5 |

MCTS beats each greedy agent 74% of the time head to head. So search adds a lot over greedy play; at 64ms the same
agent gets only ~10 iterations (each new opponent tile node costs ~13ms of action-heuristic evaluation), which is why
it was only level with greedy in Exp04. Making search cheaper is the main lever.

## Covering risk model

`CoverageData` plays greedy agents against each other and records, at each tile placement, features of every creature
on the field and whether it is later covered. 150 games, 192k samples, 18.5% covered. A logistic model
(`scripts/fitCoverage.py`; level, edge, same/higher/lower-level neighbours, nearby same-level Smokers, tiles left,
species, the current exposed flag and interactions with tiles left), trained on 4/5 of the games:

| model | held-out log-loss | AUC |
|:--|--:|--:|
| constant | 0.480 | 0.5 |
| exposed flag only | 0.418 | 0.648 |
| full | 0.340 | 0.847 |

Higher hexes and hexes surrounded by higher ones are much safer; risk rises with the tiles left; Tube Worms are rarely
covered (they climb).

## Exp06 - greedy agents with the risk model (600 games)

The heuristic now subtracts `risk` (own) / `oppRisk` (opponents) times each creature's CoverageModel probability of
being covered times what it would lose. Move factor 0.75, crab value 4, no max-opponent weight.

| agent | oppMean | exposure | risk | oppRisk | k | win | ordinal | score |
|:--|--:|--:|--:|--:|--:|--:|--:|--:|
| r4 | 1.25 | 0 | 1.5 | 0.5 | 0 | 0.391 | 2.07 | 59.2 |
| r2 | 1.0 | 0 | 1.0 | 1.0 | 0 | 0.310 | 2.29 | 57.4 |
| r1_k10 | 1.0 | 0 | 1.0 | 0.5 | 10 | 0.301 | 2.33 | 57.0 |
| r1 | 1.0 | 0 | 1.0 | 0.5 | 0 | 0.287 | 2.31 | 57.0 |
| r3 | 1.0 | 0.25 | 0.75 | 0.5 | 0 | 0.231 | 2.48 | 55.7 |
| t0_k0 (exposure only) | | | | | | 0.119 | 3.00 | 50.1 |
| g_mean1_exp05_k10 (exposure only) | | | | | | 0.112 | 2.99 | 50.6 |

Every risk-model agent beats both exposure-only agents 64-75% head to head; r4 beats the previous best 72%.
The strongest has the largest own-risk weight.

## Exp07 - around r4 (750 games)

| agent | oppMean | risk | oppRisk | moveFactor | k | win | ordinal |
|:--|--:|--:|--:|--:|--:|--:|--:|
| r9 | 1.5 | 2.0 | 0.5 | 0.75 | 0 | 0.300 | 2.40 |
| r4 | 1.25 | 1.5 | 0.5 | 0.75 | 0 | 0.277 | 2.36 |
| r5 | 1.25 | 2.0 | 0.5 | 0.75 | 0 | 0.261 | 2.39 |
| r4_k10 | 1.25 | 1.5 | 0.5 | 0.75 | 10 | 0.260 | 2.53 |
| r7 | 1.25 | 1.5 | 0.25 | 0.75 | 0 | 0.235 | 2.49 |
| r10 | 1.25 | 2.0 | 0.5 | 1.0 | 0 | 0.233 | 2.57 |
| r6 | 1.25 | 2.5 | 0.5 | 0.75 | 0 | 0.228 | 2.54 |
| r8 | 1.25 | 1.5 | 1.0 | 0.75 | 0 | 0.205 | 2.70 |

r4, r5 and r9 are level (pairwise 0.50). oppRisk 1.0 and risk 2.5 are worse; tile look-ahead still adds nothing.
Greedy play has plateaued again around r4. Diag01 says search is where the gains are, and its cost is dominated by
valuing every tile placement (~600 late in the game, ~40us each). Next: an exact, incremental tile valuation.

## Incremental tile valuation

`VentlifeTileEval` values a tile placement without copying the state: a tile changes the field only under itself and
moves covered creatures one step (an Octopus along a line), so only creatures within two steps of the tile, or next to
where a covered creature went, need their terms recomputed, on an overlay view of the field (`Board`); shoals and
plateau majorities are recomputed in full when a Fish is covered or a plateau tile is placed with Crabs about.
Unit-tested equal to `VentlifeTileSim` + heuristic on 864k tile placements. Late-game cost 7.3us per tile against
50us; the greedy agent's tile decision now takes 1.6ms (median, 4.5ms at the 95th percentile).

## Game bug found and fixed (with the user's go-ahead)

`VentlifeUtils.plateaus` returned single raised hexes as plateaus, so a lone Crab on an isolated raised hex scored 1;
the rules (and the method's own javadoc) say a plateau is 2 or more connected hexes. Two existing scoring tests had
encoded the old behaviour; they were updated and a new test added, then the method fixed. Experiments up to Exp08 ran
with the old scoring (a small effect: such hexes are uncommon).

## Exp08 - MCTS at 64ms with the fast evaluators (300 games)

MCTS as `mctsHPW`, with the r4 heuristic (risk model) as both state and action heuristic. With TileEval the MCTS gets
~20 iterations per decision (~10 before): each new node still costs a full action-heuristic pass, and every
iteration recomputes the available tile placements (~0.4ms in the forward model).

| agent | K | PW C | PW exp | win | ordinal | score |
|:--|--:|--:|--:|--:|--:|--:|
| mR_pw3 | 0.01 | 3 | 0.3 | 0.321 | 2.26 | 60.9 |
| r4 (greedy) | | | | 0.308 | 2.28 | 60.0 |
| r4_k30 (greedy, 30-tile look-ahead) | | | | 0.242 | 2.50 | 58.5 |
| mR | 0.01 | 2 | 0.5 | 0.217 | 2.65 | 56.4 |
| mR_K01 | 0.1 | 2 | 0.5 | 0.163 | 2.79 | 55.5 |

MCTS with narrow widening (C 3, exponent 0.3: 3-5 actions per node) draws level with the best greedy agent; wider
widening or more exploration is worse with so few iterations. Look-ahead within the turn again does not help.

## Exp09 - playing out the opponents' replies (420 games)

New option `opponentReplies` in `VentlifeGreedyPlayer`: each of the best `tileLookahead` tiles (by one-step value) is
played out greedily to the end of the turn and then through the next three players' turns, each of whom values every
plateau placement (the ones that can cover creatures) and a random sample of 20 seafloor placements with their own
heuristic and plays the rest of their turn greedily. The plan whose state is best after the round is chosen. Each
plan costs ~5.5ms, so about 9 fit in the budget.

| agent | win | ordinal | score |
|:--|--:|--:|--:|
| r4rep (k30, risk 1.5) | 0.366 | 2.19 | 59.1 |
| r4rep_k8 | 0.330 | 2.18 | 59.4 |
| r4rep_r1 (risk 1.0) | 0.241 | 2.51 | 56.5 |
| mR_pw3 (MCTS) | 0.179 | 2.81 | 54.5 |
| r4 (greedy) | 0.134 | 2.80 | 54.2 |

r4rep beats r4 65% and the best MCTS 64% head to head: anticipating the covering replies pays, as Diag01 suggested.
Lowering the risk weight hurts even though covering in the next round is now simulated (the risk model covers the
rest of the game too).

## Exp10 - reply play-out variants (600 games)

Reply candidates are now generated directly (all plateau placements plus a random seafloor sample; unit-tested legal
and complete for plateaus) instead of listing every legal placement: ~24 play-outs fit in the budget instead of ~9.

| agent | change from r4rep | win | ordinal |
|:--|:--|--:|--:|
| r4rep_sea50 | 50 seafloor candidates per reply | 0.296 | 2.44 |
| r4rep_m1 | oppMean 1.0 | 0.284 | 2.40 |
| r4rep_sea5 | 5 seafloor candidates | 0.273 | 2.43 |
| r4rep_k8 | 8 play-outs at most | 0.268 | 2.37 |
| r4rep_r2 | risk 2.0 | 0.268 | 2.44 |
| r4rep | (k30, 20 seafloor) | 0.234 | 2.50 |
| r4 (greedy) | | 0.128 | 2.90 |

All reply variants are within noise of each other and beat greedy r4 61-67% head to head. More play-outs (k30 vs
k8), a wider seafloor sample or different weights make no difference, so the budget is better spent elsewhere.

## Exp11 - creature choices and deeper replies (600 games)

New options: `creatureLookahead` (the creature decision itself is made by reply play-outs of its best N placements)
and `creatureAlternatives` (when valuing a tile, branch on its best M first creature placements); `replyRounds` 2
plays out a second full round (every player, this one too, playing a reply turn).

| agent | change from rep_k8 | win | ordinal |
|:--|:--|--:|--:|
| rep_c | creatureAlternatives 3, creatureLookahead 8 | 0.328 | 2.20 |
| rep_cl | creatureLookahead 8 | 0.316 | 2.28 |
| rep_d2_c | 2 rounds, creatureAlternatives 2, creatureLookahead 6 | 0.295 | 2.36 |
| rep_k8 | (tileLookahead 8, replies) | 0.241 | 2.49 |
| rep_d2 | 2 rounds | 0.212 | 2.60 |
| r4 (greedy) | | 0.109 | 3.06 |

Searching the creature placement helps (rep_c beats rep_k8 57%, r4 73%); a second round of replies does not (the
greedy model of everyone's play two turns out is too crude, and it halves the plans).

## Risk model 2 and Exp12 (600 games)

`CoverageData` on 183 games between the Exp11 reply agents (276k samples, 17.9% covered); refitted model held-out
AUC 0.865. On that data model 1 over-predicts covering (mean 0.232 against 0.179 actual; log-loss 0.365) while model 2
is calibrated (log-loss 0.332). Heuristic parameter `riskModel` selects the model.

| agent | change from rep_c | win | ordinal |
|:--|:--|--:|--:|
| rep_c_l16 | creatureLookahead 16 | 0.291 | 2.35 |
| rep_c | | 0.271 | 2.40 |
| rep_c_m2_r2 | risk model 2, risk 2.0 | 0.250 | 2.51 |
| rep_c_m2 | risk model 2 | 0.228 | 2.60 |
| rep_k8 | no creature search | 0.210 | 2.63 |

The better-calibrated model plays worse (rep_c beats rep_c_m2 56%): over-estimating the risk of being covered makes
the agent usefully more defensive, so model 1 stays. Searching 16 creature placements instead of 8 adds nothing.

## Confirm01 - standard RunGames tournament against the baseline (180 games, 4 agents, one seat each)

`Ventlife_best.json` is rep_c. TournamentResults.txt per process in `metrics/out/vl/confirm01/p*`; combined:

| agent | win | ordinal | score |
|:--|--:|--:|--:|
| rep_c (best) | 0.606 | 1.52 | 67.0 |
| mcts_r4 (MCTS with the r4 heuristic, mR_pw3) | 0.231 | 2.14 | 59.9 |
| greedy_r4 | 0.164 | 2.33 | 58.4 |
| baseline | 0.000 | 3.99 | 23.7 |

rep_c beats the baseline in all 180 games, the MCTS 70% and greedy r4 78% head to head.

## NTBEA round 2 - reply agent against a mixed pool (3 runs x 600 games)

Search space `ntbea/repAgentSS.json` (play-out sizes and heuristic weights); opponents drawn from `ntbea/opp02`
(rep_c, rep_k8, r4, t0_k0). All three runs chose tileLookahead 6 and exposureMoveFactor 0.5; the other settings varied.

## Exp13 - NTBEA round 2 recommendations (600 games)

| agent | settings | win | ordinal |
|:--|:--|--:|--:|
| n1 | k6, sea 40, creatureAlt 3, creatureLook 8; risk 2.0, oppRisk 0.75, oppMean 1.25, exposure 0.25, mf 0.5, crab 6 | 0.293 | 2.32 |
| rep_c | | 0.279 | 2.39 |
| n2 | k6, sea 20, creatureAlt 2, creatureLook 4; risk 1.5, oppRisk 0.25, exposure 0.25, mf 0.5, crab 6 | 0.272 | 2.44 |
| n3 | k6, sea 40, creatureAlt 3, creatureLook 4; risk 2.5, oppRisk 0.75, oppMean 1.5, mf 0.5, crab 4 | 0.257 | 2.46 |
| rep_k8 | | 0.149 | 2.87 |

n1 is marginally ahead of rep_c (53% head to head, within noise).

## Duel01 - rep_c against n1 (600 games, two seats each)

| agent | win | ordinal | score |
|:--|--:|--:|--:|
| rep_c | 0.278 | 2.43 | 56.8 |
| n1 | 0.223 | 2.57 | 55.6 |

rep_c beats n1 55% head to head over 600 games: the Exp13 lead of n1 was noise, and NTBEA round 2 found nothing
better. rep_c (`Ventlife_best.json`) remains the best agent; its settings look near a local optimum.

## Current best and next ideas

Best: `Ventlife_best.json` (rep_c): VentlifeGreedyPlayer with opponentReplies, tileLookahead 8, creatureAlternatives 3,
creatureLookahead 8, replySeafloorSample 20; VentlifeHeuristic oppMean 1.25, risk 1.5, oppRisk 0.5 (model 1),
exposureMoveFactor 0.75, exposureCrabValue 4.

Ideas not yet tried: a better opponent model in the replies (one fitted to how strong agents actually play rather than
greedy on our own heuristic); sampling several reply sequences per plan; learning the state value (rather than hand
features + risk model) from game outcomes; using the play-out evaluations as priors for a proper search tree.

