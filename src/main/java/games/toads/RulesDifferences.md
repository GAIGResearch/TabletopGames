# War of the Toads: moving to Rulebook 3

Source: `WotT_Rulebook3_NoBleed.pdf`, the final 20-page rulebook. This document is written so that a
developer can bring `games.toads` in line with that rulebook **without reading it**.

- §1 summarises the complete Rulebook 3 rules.
- §2 lists every difference from the current implementation, with the functional change required.
- §3–§6 say where in the code each change goes, what else is affected, and which tests to write.
- §7 records the rulebook ambiguities and how they were resolved.

"Current" means the code in `games.toads` running with the default `data/toads/cards.json` and the
defaults in `ToadParameters`.

---

## 1. Rulebook 3 in brief

### 1.1 Components and terms
- **Players and decks:** two players, each with an identical 9-card deck (listed in §2.1).
- **Battle and lanes:** in each Battle both players have one card in each of two lanes.
  - One lane holds the face-up card (the code's *field* card).
  - The other holds the face-down, or *hidden*, card (the code's *flank* card).
- **Attacker and Defender:** in each Battle one player is the Attacker and the other the Defender. The
  roles alternate every Battle.
- **Ally and Foe:**
  - A card's **Ally** is the other card on the same side, in the other lane.
  - A card's **Foe** is the opponent's card in the same lane as that Ally, i.e. the card the Ally is
    fighting.
- **Special Attribute (SpAt):** printed on some cards and always active, e.g. "Wins against General".
- **Tactic:** printed on every card, but **only the hidden card's Tactic activates**, once, when it is
  revealed. The face-up card's Tactic never activates.
  - A Tactic still activates if its card loses its lane.
  - A Tactic does not activate if it is blocked (see Bodyguard).
- **Hostage:** a captured opponent card, worth 1 point in the current War. This is the code's
  `battlesWon[war][player]`.
- **The Shrine:** holds cards that leave play without being captured (Monks). Every card beside the
  Shrine is shown as a **Flag** on its owner's side.
- **Calm / Angry:** you are **Angry** if you have fewer Hostages than your opponent in the current War.
  Otherwise you are **Calm**.

### 1.2 A War
1. **Setup, done at the start of each War:**
   - Each player shuffles their deck and draws **5** cards.
   - Each then chooses 1 of them and puts it **on the bottom of their deck**. This is mandatory, and the
     card stays secret from the opponent.
   - Maximum hand size is 4.
2. **Four Battles** follow. In each Battle:
   1. The Attacker plays 2 cards: one face-up, one face-down.
   2. The Defender plays 2 cards: face-up opposite face-up, face-down opposite face-down.
   3. Both players draw 2 cards, or 1 if only 1 remains in their deck.
   4. Both hidden cards are revealed simultaneously and their Tactics resolve in four stages:
      **Block → Start of Battle → During Battle → After Battle**.
      Tactics in the **same stage resolve simultaneously**.
   5. Each lane is resolved (§1.3).
   6. Hostages are captured (§1.4).
   7. Attacker and Defender swap roles for the next Battle.
3. **End of the War:** a War ends when a player can't fight another Battle.
   - At the end of War 1 each player has 1 card left in hand.
   - At the end of War 2 each player has none left.
   - The player with more Hostages wins the War. Equal Hostages is a **Stalemate**.

### 1.3 Resolving a lane
Apply these rules in order:
1. **Siege Cannon.** A Siege Cannon has no Strength and ignores all Strength bonuses. Its lane is
   decided purely by its Special Attribute:
   - In Defence it always loses.
   - In Attack it wins, unless the opposing card is a Saboteur, in which case the Saboteur wins.
2. **Assassin vs General.** An Assassin facing a General wins, whatever their Strengths.
3. **Strength.** Otherwise the higher Strength wins. Strength is the printed value plus Tactic bonuses,
   and can be fractional (the Assassin gives +2.5).
4. **Ties.** If Strengths are equal, the lane is tied unless exactly one side's card has been given
   "breaks ties" by a Saboteur. That side wins.

### 1.4 Capture, Calm/Angry and the Shrine
- **Tied lane:** both cards go to the Shrine, so each player gains 1 Flag.
- **Won lane:** the winning card captures the losing one. This is a Hostage stack, worth 1 point to the
  winner.
- **One player wins both lanes:**
  - **Calm:** they keep 1 stack, and the other stack goes to the Shrine, so each player gains 1 Flag.
    Which stack goes is purely cosmetic.
  - **Angry, or made Angry by a Berserker:** they keep both stacks. This is a "Leap-Frog".
- Calm/Angry is judged on the Hostage counts **before** the current Battle.

### 1.5 Between the Wars
1. **Casualty:** the card left in each player's hand at the end of War 1 is that player's **Casualty**.
   - It goes face-down beside the Shrine.
   - So **each player starts War 2 with 1 Flag**.
2. **Rebuild and swap:** each player rebuilds their own coloured deck from all their cards (Hostages,
   Captors and Monks), leaving out their Casualty. The players then **swap decks**, and each deck has 8
   cards.
3. **Reset:** the Shrine is reset to "both Calm". Hostage counts restart at 0 for War 2.
4. **War 2 first Attacker:** the player who **Defended** the first Battle of War 1.
5. **Setup:** setup (draw 5, return 1) is repeated.

### 1.6 Winning the game
- **Won War 2:** that player wins the game.
- **Won War 1 and Stalemated War 2:** that player wins.
- **Both Wars Stalemated:** the **lowest** Casualty wins.
  - A Siege Cannon is the lowest possible Casualty.
  - Equal Casualties (e.g. General vs General) make the game a genuine draw.
- The rulebook doesn't spell out "each player won one War", but the first rule covers it: the War 2
  winner wins. This matches the current code.

---

## 2. Required functional changes

### 2.1 Deck list

Every card keeps its Strength. Two cards are replaced, and most Tactics change.

| Str | Rulebook 3 card | Current card (`cards.json` → ability class) | Summary of change |
|-----|-----------------|---------------------------------------------|-------------------|
| 1 | Assassin | Assassin → `Assassin` | New Tactic |
| 2 | Scout | Scout → `Scout` | Unchanged apart from resolution order |
| 3 | Saboteur | Saboteur → `SaboteurII` | Breaks ties for its Ally only |
| 4 | Trickster | Trickster → `Trickster` | Loses the +½-Ally bonus; cannot be blocked |
| 5 | Berserker | Berserker → `Berserker` | New Tactic (the current `GeneralOne` effect) |
| 6 | **Bodyguard** | Icon Bearer → `IconBearer` | New card |
| 7 | General (Hostages) | General One → `GeneralOne` | New Tactic (roughly the current `Berserker` effect) |
| 7 | General (Flags) | General Two → `GeneralTwo` | Counts Flags, not just tied lanes |
| – | **Siege Cannon** | Assault Cannon → `AssaultCannon` | New Tactic; ignores bonuses; always loses in Defence |

Each entry in §2.2 gives the Tactic's stage, the current behaviour, and the required behaviour.
The stage names are the Rulebook's: Block / Start / During / After.

### 2.2 Card-by-card

#### Assassin (1): SpAt "Wins against General"; Tactic stage During
- **Rulebook:** *+2.5 to your Ally or their Foe, whoever is lower.*
- **Current:** copies its Ally's Tactic (priority -1). The SpAt is done as a `+20` CardModifier against
  any value-7 card.
- **Required:**
  - Compare the current Strengths of the Ally and the Foe as they stand at the **start** of the During
    stage, i.e. after all Start bonuses.
    - Add 2.5 to whichever is lower. This can be the opponent's card.
    - If they are **equal, do nothing**. Saboteur tie-breaking does not change this.
  - If either card is a Siege Cannon, do nothing. That lane ignores Strength (§1.3), so the bonus can't
    matter.
  - If both players reveal an Assassin, they sit opposite each other, so their Ally/Foe pair is the same
    two cards. Both Assassins read the same starting values, so the lower card gets **+5 in total**. This
    only comes out right if both effects read a snapshot taken at the start of the stage (see §3.1).
  - Remove the "copy Ally's Tactic" behaviour.
  - Assassin-vs-General is decided at lane resolution (§1.3 step 2), not by a `+20` modifier.

#### Scout (2): Tactic stage Start
- **Rulebook:** *+1 to your Ally, and the opponent shows you 3 cards from their hand.*
  - The opponent chooses which 3.
  - If both players reveal a Scout, both show their cards simultaneously.
- **Current:** the same effect, using `ScoutCards` → `ShowCards` as a post-battle decision.
- **Required:**
  - The effect itself is unchanged.
  - The Scout's reveal must resolve **before** a Siege Cannon's guess (Start precedes After). Today both
    are pushed onto the actions-in-progress **stack**, so the Cannon, pushed last, runs first. See §3.4.
  - *Approximation (accepted):* with two Scouts, the second player to choose will already have seen what
    the first showed. That is acceptable.
  - Showing the post-draw hand is correct: players draw in step 3, before the reveal in step 4. This is
    what the code does today.

#### Saboteur (3): SpAt "Wins against Siege Cannon"; Tactic stage During
- **Rulebook:** *your Ally breaks ties they are in.*
- **Current `SaboteurII`:** adds +1 at priority 7 to **both** its own lane and its Ally's lane when they
  are tied.
- **Required:**
  - Affects the Ally's lane **only**.
  - Implement it as a tie-break flag checked at lane resolution (§1.3 step 4), not as +1 Strength. That
    keeps it independent of fractional values and of the Assassin.
  - If both sides hold a tie-break flag for the same lane, it stays tied.
  - The SpAt is handled at lane resolution: the Saboteur beats an **attacking** Siege Cannon. A defending
    Siege Cannon loses to anything anyway.

#### Trickster (4): Tactic stage Start; cannot be blocked
- **Rulebook:** *switch lanes with your Ally. Cannot be blocked!*
  - After the switch, the Trickster fights in the face-up lane and its Ally fights in the hidden lane.
  - The Ally's Tactic is **not** activated by the move.
- **Current:**
  - Swaps lanes at priority -1, which is the part to keep.
  - Then, at priority 3, adds `ally.value / 2` to itself.
- **Required:**
  - Drop the `ally.value / 2` bonus.
  - Mark the Trickster as unblockable. A Bodyguard's Block has no effect on it.

#### Berserker (5): Tactic stage During
- **Rulebook:** *you become Angry: if you win both lanes, capture both.*
  - This overrides Calm for this Battle only.
- **Current:**
  - `Berserker` adds the opponent's Hostages this War to **itself** (priority 5).
  - The required behaviour is exactly what `GeneralOne` does today (`frogOverride`).
- **Required:** set `frogOverride` for the owner. There is no Strength change.

#### Bodyguard (6), a new card replacing Icon Bearer; Tactic stage Block
- **Rulebook:** *Block your opponent's Tactic.*
- **Current:**
  - There is no such card. Icon Bearer activates its Ally's Tactic, and gives its Ally +1 if that would
    make a tie.
  - The unused `Saboteur` ability class (priority -5) does something close: it calls
    `setActivation(!isAttacker, isFlank, false)` on the opposing card.
- **Required:**
  - Deactivate the opponent's **hidden** card, so that none of its Tactics resolve. The exception is a
    Trickster, which is unblockable.
  - Block is the first stage, so no Trickster has moved yet, and the target is always the card opposite
    in the hidden lane.
  - Two Bodyguards block each other **simultaneously**: both are blocked, so neither side's Tactic
    resolves beyond the Block itself.
    - Today the first to resolve would cancel the second.
    - Record the blocks during the stage and apply them at the end of it (§3.1).
  - Blocking a Siege Cannon cancels its guess. Blocking a Scout cancels both its +1 and the reveal.

#### General (Hostages) (7): SpAt "Loses against Assassin"; Tactic stage Start
- **Rulebook:** *+1 to your Ally per Hostage captured by your opponent.*
- **Current:** `GeneralOne` sets `frogOverride`. That behaviour moves to Berserker.
- **Required:** add `battlesWon[currentWar][opponent]` to the Ally, using the value **before** this
  Battle's captures.

#### General (Flags) (7): SpAt "Loses against Assassin"; Tactic stage Start
- **Rulebook:** *+1 to your Ally for each of your Flags in the Shrine.*
- **Current:** `GeneralTwo` adds `battlesTied[currentWar]` to the Ally. That counts tied lanes only.
- **Required:**
  - Add the owner's Flag count for the current War to the Ally. This needs new state (§3.3).
  - Flags are gained:
    - 1 per player for each tied lane (as today);
    - 1 per player for each Calm double win, where one stack goes to the Shrine;
    - 1 per player at the start of War 2, for the Casualties.
  - Both players always gain Flags together, so the two counts are always equal.

#### Siege Cannon, a new card replacing Assault Cannon: SpAt "Loses in Defence, wins in Attack except against Saboteur"; Tactic stage After
- **Rulebook:** *guess a card in your opponent's hand. If correct, they must say yes.*
- **Current `AssaultCannon`:**
  - Value 0, with a `+20` CardModifier when attacking and not facing a Saboteur.
  - It can take Tactic bonuses, so a defending Cannon can tie or win, and an attacking Cannon with bonuses
    can beat a Saboteur.
  - Its Tactic (`AssaultCannonInterrupt` → `ForceOpponentDiscard`) makes the opponent put the named card
    on the bottom of their deck and draw a replacement.
- **Required:**
  - **Lane:** resolved only by §1.3 step 1. It ignores Strength and all bonuses on both sides. If both
    lanes' cards are Siege Cannons, the attacker's wins.
  - **Tactic:** information only. Nothing moves.
    - The Cannon's owner names a card by its **printed name**.
    - Both Generals are printed "General", so a guess of **General** is one option. It is correct if
      the opponent holds **either** General type (`GENERAL_ONE` or `GENERAL_TWO`).
    - Every other name corresponds to a single card type.
    - If the guess is correct, make one matching card visible to the guesser in the opponent's
      `PartialObservableDeck` hand. For General, if the opponent holds both, make the first one found
      visible.
    - If not, nothing changes. The engine can't represent "known not to be in hand", and that loss of
      negative information is accepted.
  - **Candidate guesses:** one option per printed name. Keep today's filtering: exclude the guesser's
    own Casualty and cards already in the opponent's discards.
    - Exclude General only when **both** Generals are ruled out in that way.
    - Offer a "no guess" action **only** if the candidate list would otherwise be empty.
  - *Approximation (accepted):* visibility is per card, so a correct General guess shows the guesser
    **which** General it is. In the physical game the opponent only says "yes". The engine can't
    represent the weaker "one of the Generals", so this small leak is accepted.
  - Keep the old classes for legacy decks (§4).

### 2.3 Game flow

| # | Topic | Current | Required |
|---|-------|---------|----------|
| F1 | Opening hand | Draw `handSize` (4). | Draw `handSize + 1` (5). Each player then **must** return 1 card to the bottom of their deck; there is no "keep all" option and no replacement draw. The returned card is visible to its owner only. This happens at the start of **each** War. |
| F2 | Per-Battle recycle | `discardOption = true` (the default): an optional `DISCARD` phase before **every** Battle. | Not in the rulebook. Change the default to `false`, but keep the option working for experiments. |
| F3 | War 2 first Attacker | `secondRoundStart = WINNER`. | The player who Defended the first Battle of War 1. Player 0 always attacks first in War 1, so that is player 1: change the default to `TWO`. |
| F4 | Tactic resolution | Sequential `PriorityQueue` with ad-hoc priorities; ties in priority run in arbitrary order. | Four stages, with simultaneous resolution inside each (§3.1). |
| F5 | Lane values | `int`. | `double` (the Assassin's +2.5). |
| F6 | Post-battle decisions | Pushed onto a stack, so they run in reverse order. | Run in stage order: Scout's reveal (Start) before the Siege Cannon's guess (After). |

### 2.4 Scoring

| # | Topic | Current | Required |
|---|-------|---------|----------|
| S1 | Hostages, Calm/Angry, Leap-Frog | The "overcommit rule" in `ToadForwardModel._afterAction`. | Unchanged, except that a Calm double win must also add 1 Flag per player. |
| S2 | Shrine Flags | Only `battlesTied` is tracked. | Track Flags per War and per player (§3.3). |
| S3 | Both-Stalemate tiebreak | `getTiebreak` returns the Casualty's `value`, and higher ranks better, so the **highest** Casualty wins. | The **lowest** wins: return `-value`. The Siege Cannon's value of 0 correctly makes it the lowest. Equal values are a draw. |
| S4 | War and game winner | `getGameScore`. | Unchanged. |

---

## 3. Implementation notes

### 3.1 `BattleResult`: stage-based Tactic resolution
- **Stage constants:** define them in `AbilityConstants`, which is currently empty, e.g.
  `BLOCK = -30`, `START = -20`, `DURING = -10`, `AFTER = 20`.
  - All new abilities use these.
  - Block, Start and During run before the CardModifier point (0). After runs following it, which is
    where post-battle actions are queued.
- **Group processing:** change the loop in `calculate()` so it takes **all tactics that share the lowest
  priority**, runs them as a group, and then commits:
  - Before the group runs, take a **snapshot** of the four lane values. Effects that need
    "simultaneous" reads use it; the Assassin is currently the only one.
  - **Blocks** record their target in a pending set during the group. After the group, call
    `setActivation(..., false)` on each target, skipping unblockable cards. This makes two Bodyguards
    cancel each other.
  - Tactics added to the queue while a group runs (legacy Icon Bearer or Assassin copying) should still
    be processed, as they are today.
- **Unblockable cards:** add `default boolean canBeBlocked() { return true; }` to `ToadAbility`.
  The new Trickster ability returns `false`.
- **Doubles:** change `AField`, `AFlank`, `DField` and `DFlank` to `double`, and likewise
  `getCurrentValue` and `addValue`.
  - `ToadCard.value` stays `int`.
  - Legacy abilities compile unchanged, since `int` widens to `double`.
- **Tie-break flags:** add `boolean[2][2] tieBreak[side][lane]`, which the Saboteur sets.
  - `swapFieldAndFlank` must swap these too, for completeness. In practice no flag exists yet when a
    Trickster swaps, because Start comes before During.
- **Lane resolution:** replace the two `if (AField > DField)` comparisons with one
  `resolveLane(lane)` that applies §1.3 in order (Siege Cannon → Assassin vs General → Strength →
  tie-break).
  - **Identifying cards:** use `ToadCardType`: `SIEGE_CANNON`, `SABOTEUR`, `ASSASSIN`, and
    `GENERAL_ONE`/`GENERAL_TWO`, or `value == ASSASSIN_KILLS`.
  - **Legacy decks:** keep applying `attributes()` CardModifiers, so old cards that rely on `+20`
    (`Bomb`, `AssaultCannon`, the old `Assassin`) still work.
  - **New abilities:** give them no CardModifiers.

### 3.2 New and changed ability classes (`games.toads.abilities`)

Keep every existing class unchanged so that the legacy card files still work (§4). Add:

| New class | Card | Stage | Behaviour |
|-----------|------|-------|-----------|
| `AssassinII` | Assassin | DURING | +2.5 to the lower of Ally and Foe, read from the stage snapshot. Does nothing if they are equal or either is a Siege Cannon. |
| `SaboteurIII` | Saboteur | DURING | Sets the tie-break flag for the Ally's lane. |
| `TricksterII` | Trickster | START | Swaps lanes (reuse `swapFieldAndFlank`). No bonus. `canBeBlocked() == false`. |
| `BerserkerII` | Berserker | DURING | Sets `frogOverride` for its owner (same body as `GeneralOne`). |
| `Bodyguard` | Bodyguard | BLOCK | Adds the opponent's hidden card to the pending block set. |
| `GeneralHostages` | General | START | Adds the opponent's `battlesWon` for this War to the Ally. |
| `GeneralFlags` | General | START | Adds the owner's Flags for this War to the Ally. |
| `SiegeCannon` | Siege Cannon | AFTER | Adds a `SiegeCannonGuess` post-battle action. |
| `Scout` (existing) | Scout | → START | Only the priority constant changes: 3 → `START`. Scout is the one existing class to edit in place, since its behaviour doesn't change. |

### 3.3 State: `ToadGameState`
- **Flags:** add `int[][] shrineFlags = new int[2][2]`, indexed `[war][player]`. Add an accessor
  `getShrineFlags(war, player)`, and include the field in `_copy`, `_equals`, `hashCode` and `toString`.
  It is public information, so it needs no redeterminisation.
- **`battlesTied`:** keep it. Metrics and features use it.
- **Tiebreak:** change `getTiebreak` to return `-tieBreakers[playerId].value`. Keep the existing `0`
  when there is no tiebreaker.

### 3.4 Forward model: `ToadForwardModel`
- **`_setup` (F1):** if `openingReturn` is set, draw `handSize + 1` and enter a new phase
  `ToadGamePhase.OPENING_RETURN`. Otherwise keep today's behaviour.
  - Player 0 chooses first, then player 1, then `PLAY` starts with player 0 attacking.
  - Reuse the `discardOptions` counter pattern from the `DISCARD` phase.
- **`_computeAvailableActions`:** in `OPENING_RETURN`, offer one `ReturnCardToDeck(card)` per distinct
  card in hand, with no null option.
- **New action `ReturnCardToDeck`:** like `RecycleCard`, but without the replacement draw. It removes the
  card from the hand, calls `addToBottom`, and sets the card visible to its owner only.
- **`_afterAction` (flags):** after `scoreDiff` is known:
  - Add the number of tied lanes to `shrineFlags[war][p]` for both players.
  - If the overcommit reduction fired (a Calm double win), add 1 more to each.
- **`_afterAction` (post-battle order, F6):** `battle.getPostBattleActions()` is in resolution order,
  because tactics are processed in priority order. Push them onto the stack **in reverse**, so that the
  first one queued runs first.
- **`afterBattle`, end of War 1:**
  - Set `shrineFlags[1][0] = shrineFlags[1][1] = 1` for the Casualties.
  - If `openingReturn`, draw `handSize + 1` rather than `handSize`, and enter `OPENING_RETURN` with the
    War 2 first Attacker choosing first.
  - `afterBattle` sets the phase at its start, so it needs a special case here. `endRound` doesn't touch
    the phase; it only sets the turn owner to `firstPlayerOfSecondRound`, who is the right player to
    choose first.
- **Deck arithmetic check:**
  - War 1: 9 cards → hand 4 + deck 5 after the return. The deck then drops to 3, 1, 0 as the Battles
    proceed, and 1 card is left in hand after Battle 4.
  - War 2: 8 cards → hand 4 + deck 4, dropping to 2, 0, 0, and the hand is empty after Battle 4.
  - Both match the existing end-of-War test (`playerHands.get(0).getSize() <= 1`).

### 3.5 Actions (`games.toads.actions`)
- **New `SiegeCannonGuess` (IExtendedSequence) + `GuessCard(type)` action:** the decision player is the
  Cannon owner.
  - The candidate list is as in §2.2.
  - **Grouping the Generals:** add a helper, e.g. `ToadCardType.guessName()` (or `guessGroup()`), that
    maps `GENERAL_ONE` and `GENERAL_TWO` to one shared key and every other type to itself.
    - Build the candidate list from the distinct keys.
    - `GuessCard` holds a key and matches any card whose type maps to it.
  - When the guess is right, `GuessCard` sets the first matching card in the opponent's hand visible to
    the guesser.
  - Pattern it on `AssaultCannonInterrupt`, including `copy`, `equals` and `hashCode`.
- **Existing actions:** `AssaultCannonInterrupt`, `ForceOpponentDiscard` and `RecycleCard` stay
  unchanged, for the legacy decks and `discardOption`.

### 3.6 Parameters, constants and data
- **`ToadParameters`:**
  - Add `boolean openingReturn` (default `true`).
  - Change the default of `discardOption` to `false` and of `secondRoundStart` to `TWO`.
  - Register each change with `addTunableParameter` and read it back in `_reset`.
- **`ToadConstants.ToadCardType`:**
  - Add `BODYGUARD` and `SIEGE_CANNON`.
  - Point the `defaultAbility` of `ASSASSIN`, `SABOTEUR`, `TRICKSTER`, `BERSERKER`, `GENERAL_ONE` and
    `GENERAL_TWO` at the new classes (`GENERAL_ONE` → `GeneralHostages`, `GENERAL_TWO` →
    `GeneralFlags`).
  - Leave `ICON_BEARER`, `ASSAULT_CANNON` and `BOMB` as they are.
  - `fromString` maps JSON names to types. A JSON name "Siege Cannon" maps to `SIEGE_CANNON`
    automatically.
- **`data/toads/cards.json`:**
  - Move the current contents to a new `cards_005.json`, the legacy default deck.
  - Replace `cards.json` with the Rulebook 3 deck:
    - Assassin 1 `AssassinII`; Scout 2 `Scout`; Saboteur 3 `SaboteurIII`; Trickster 4 `TricksterII`;
      Berserker 5 `BerserkerII`; Bodyguard 6 `Bodyguard`;
    - General One 7 `GeneralHostages`; General Two 7 `GeneralFlags`;
    - Siege Cannon 0 `SiegeCannon`.

---

## 4. Backwards compatibility

- **Legacy decks:** `cards_001` to `cards_005` must still load and play. They reference only the old
  ability classes, which are unchanged.
- **Behaviour that changes for legacy decks too:** some of the engine changes apply to every deck. These
  are acceptable:
  - grouped (simultaneous) resolution of tactics that share a priority;
  - `double` lane values;
  - the new post-battle order;
  - the lowest-Casualty tiebreak;
  - Flag tracking.
- **Legacy defaults:** to reproduce the old default game, set
  `cardFile=cards_005.json`, `openingReturn=false`, `discardOption=true` and
  `secondRoundStart=WINNER`.
  - There is no switch back to the highest-Casualty tiebreak. Add one only if an old experiment needs
    exact reproduction.

## 5. Other code affected

- **`metrics/ToadFeatures002.java`:** uses `instanceof GeneralOne` / `GeneralTwo` to spot the Generals,
  which won't match the new classes. Switch to `card.type == GENERAL_ONE/GENERAL_TWO`.
- **Feature names across the metrics:** the names in `ToadFeatures002`, `ToadFeatures001`,
  `ToadFeaturesTunable` and `ToadQFeatures001` (`ICONBEARER_*`, `AC_*`) should be renamed
  `BODYGUARD_*` / `SIEGE_CANNON_*`.
- **Tiebreak-value features:** `TIEBREAK_US` and its equivalents now mean "lower is better". Any
  heuristic weights trained on the old meaning are stale.
- **`TIED_BATTLES` features and `ToadMetrics`:** consider adding a Flags feature or metric alongside
  them.
- **`ToadGUIManager`:** check that the card names and phases display properly, including the new
  `OPENING_RETURN` phase.
- **`ToadMCTSPlayer`:** its `UndoOpponentFlank` logic is unaffected, but test it with the new
  `OPENING_RETURN` phase.
- **Tests in `src/test/java/games/toads`:**
  - `Tactics.java` builds cards via `ToadCardType`, so it picks up the `defaultAbility` values. Most of
    its tests encode the old Tactics: Icon Bearer, Assassin copying, the Trickster bonus, and
    `SaboteurII` in its own lane.
    - Either point those tests explicitly at the legacy classes, via the 4-argument `ToadCard`
      constructor, or retire them.
  - `CardRecycling`, `GameFlow`, `TestVisibility` and `TestUndoOpponentFlank` set `discardOption`
    explicitly. They will also need `openingReturn=false`, or updating for the extra phase.

## 6. Tests to add

Each of these corresponds to a rule above:
- **Assassin:**
  - its Ally 4 vs Foe 6: Ally becomes 6.5 and wins;
  - Ally 6 vs Foe 4: the Foe becomes 6.5 and wins;
  - Ally 5 vs Foe 5: no change, and the lane ties;
  - two Assassins, 3 vs 5: the 3 becomes 8 and wins.
- **Assassin + opposing Saboteur:** Ally and Foe tied; the Assassin does nothing and the Saboteur's side
  wins the lane.
- **Saboteur:** breaks a tie in its Ally's lane; a tie in its own lane stays tied.
- **Two Saboteurs:** a tied lane stays tied.
- **Trickster:** swaps with no bonus; an opposing Bodyguard does not stop the swap.
- **Bodyguard:**
  - blocks each of Scout (no +1, no reveal), both Generals, Assassin, Saboteur, Berserker and the Siege
    Cannon guess;
  - two Bodyguards block each other.
- **Berserker:** a Calm player wins both lanes and captures both. There are no extra Flags.
- **Calm double win without a Berserker:** 1 Hostage, and +1 Flag for each player.
- **General (Hostages):** gets +N, where N is the opponent's Hostages before this Battle.
- **General (Flags):**
  - +1 per tied lane and per Calm double win this War;
  - in the first Battle of War 2 it gives +1, from the Casualty.
- **Siege Cannon:**
  - attacking: beats a 7 even with bonuses on the 7; loses to a Saboteur even with bonuses on the
    Cannon;
  - defending: loses to an Assassin;
  - Cannon vs Cannon: the attacker wins.
- **Siege Cannon guess:**
  - a correct guess makes the card visible to the guesser, and the opponent's hand and deck are
    otherwise unchanged;
  - a guess of General succeeds against a hand holding only General (Hostages), and also against one
    holding only General (Flags);
  - there is a single General option in the candidate list, and it is missing only when both Generals
    are ruled out;
  - with Scout + Cannon, the Scout's `ShowCards` decision comes before `GuessCard`.
- **Opening return:**
  - 5 cards are drawn, and there are exactly 5 decision options with no null option;
  - afterwards the hand has 4 cards and the returned card is at the bottom of the deck, visible to its
    owner only;
  - this happens again at the start of War 2, with the War 2 first Attacker choosing first.
- **War 2 first Attacker:** player 1.
- **Both Wars Stalemated:**
  - a Casualty of 2 beats a 5;
  - a Siege Cannon Casualty beats an Assassin (1);
  - General vs General is a draw.

## 7. Clarifications (resolved)

These were gaps in the rulebook. The answers above reflect them.

1. **Casualty tiebreak:** a Siege Cannon counts as the lowest Casualty. Equal Casualties (e.g. two
   Generals) are a genuine draw.
2. **Casualties as Flags:** yes. Each player starts War 2 with 1 Flag in the Shrine.
3. **Assassin next to a Siege Cannon:** the bonus is irrelevant. A Siege Cannon lane is decided only by
   Attack/Defence and whether it faces a Saboteur.
4. **Assassin and Saboteur:** the Assassin reads values before any Saboteur tie-break, and does nothing
   if Ally and Foe are equal.
5. **Calm double win:** which stack goes to the Shrine is purely cosmetic, so no decision is needed.
6. **Recycle options:** add an "opening draw 5, return 1" parameter and keep the old
   `discardOption`/`handSize` variant.
7. **Siege Cannon guessing "General":** this matches either General type.
