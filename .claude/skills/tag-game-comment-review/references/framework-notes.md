# TAG framework notes for new games

Paths are relative to the TAG repo root (parent of `claude_game_creator/`).

## Key files
| File | Why |
|---|---|
| `AGENTS.md` | Build/test commands and test conventions (JUnit 4 + Mockito, `@Before`, run single test classes only, never the full suite). `CLAUDE.md` just redirects here. |
| `src/main/java/gametemplate/GT*.java` | Templates to copy. `GTGameState` Javadoc is the main state-design guidance (redeterminisation, teams, tiebreaks, ordinal position, simultaneous play). |
| `src/main/java/games/GameType.java` | Game registration enum; `Category` and `Mechanic` enums at the bottom. |
| `src/main/java/core/components/` | `Deck`, `PartialObservableDeck`, `FrenchCard`, `Counter`, `Token`, `GridBoard`, `GraphBoard`. A board of squares of fixed size is a `GridBoard` (read by `get(x, y)`, a facade over a `SparseGrid`); a hex board, or one that grows as tiles are laid, is a `grid.SparseGrid` of `HexCoord`s or `SquareCoord`s with immutable values. |
| `src/main/java/utilities/DeterminisationUtilities.java` | Redeterminisation helper for `redeterminise(playerId)`. |
| `games/poker/PokerForwardModel.java` | Creating a standard 52-card deck. |
| `games/hearts/HeartsParameters.java` | Parameters for a FrenchCard game, incl. `TunableParameters`. |
| `games/uno/` | Hand / draw pile / discard pile structure; round-end and scoring patterns. |

## Naming
- The class prefix is the **full name of the game** (e.g. `AgramGameState`, `HeartsForwardModel`,
  `PickominoParameters`). Only when the name is too long for that to be readable, use a recognisable acronym or
  abbreviation (e.g. `CZE` for Crazy Eights, `SSP` for Sea Salt Paper). There is no rule that a prefix has 2-3
  letters: `AG` for Agram was rejected as needlessly cryptic.
- Prefixes live in separate packages so the compiler won't complain about reuse, but a reused prefix is confusing to
  readers. Check an acronym before choosing it: e.g. CE is taken by Colt Express.

## GameType
- Every enum value appears in the GUI Frontend list, so a stubbed game will show there.
- `RunGames` with `all` excludes only `GameTemplate` - a stub game would be included in "all" runs until implemented.

## Deck
- `add(c)` inserts at **index 0**; `draw()` and `peek()` use index 0. Index 0 is the **top**. `addToBottom(c)` exists.
- So cards added one by one with `add` end up in reverse order: the **first card added is at the last index**, and
  `get(0)`/`peek()` is the most recent. Read a first-dealt card (Blackjack's up card, a starter) with
  `get(getSize() - 1)`, and draw a hand in deal order from the last index down. (Now also in `Deck`'s Javadoc.)
- Constructors: `new Deck<>(name, VisibilityMode)` and `new Deck<>(name, ownerId, VisibilityMode)`.
- Visibility modes: `VISIBLE_TO_ALL`, `VISIBLE_TO_OWNER` (player hands - set ownerId), `HIDDEN_TO_ALL` (face-down draw deck),
  `TOP_VISIBLE_TO_ALL`. Use `PartialObservableDeck` when visibility varies per card per player.
- `Deck.copy()` copies contained components via their `copy()`.
- Deck equality (and `hashCode`) covers the cards in order, the owner, the visibility mode and, for a
  `PartialObservableDeck`, who can see each card. So visibility that is game state (face-up cards, cards shown to the
  table) needs no separate field or equality term in the GameState.

## FrenchCard
- `FrenchCard.generateDeck(name, VisibilityMode)` gives a standard 52-card deck.
- Fields: `suite` (enum spelled **Suite**: Diamonds, Hearts, Clubs, Spades), `type` (`FrenchCardType`: Jack, Queen, King, Ace, Number), `number`.
- **Ace has number 14** (Jack 11, Queen 12, King 13). Any game scoring or ranking aces low must special-case it.
- To test a rank, compare both `type` and `number` (for Number cards `number` is 2-10).
- `copy()` returns `this` (immutable), so copying a Deck of FrenchCards is cheap and safe.

## GameState `_copy(playerId)` and `redeterminise(playerId)`
- Return type should be the concrete class.
- **`_copy` is always a faithful deep copy; redeterminisation goes in an override of `redeterminise(int playerId)`,
  never in `_copy`** (user's rule, Ventlife). `AbstractGameState.copy(playerId)` calls `_copy`, then - only when
  `playerId != -1` and `getCoreGameParameters().partialObservable` - calls `redeterminise(playerId)` on the new
  copy. Keeping the two apart keeps the split clear: `_copy` copies, `redeterminise` hides, and neither checks the
  conditions the framework already checks. The `GTGameState` template has a stub of each with this split in its
  Javadoc; many older games do it inside `_copy` - don't follow them. Example: `VentlifeGameState.redeterminise`
  reshuffles the other players' tiles and the draw deck.
- Use `redeterminisationRnd`, **never** `getRnd()`, in `redeterminise`.
- `DeterminisationUtilities.reshuffle(playerId, decks, predicate, redeterminisationRnd)` shuffles hidden cards in situ across
  the given decks based on each deck's VisibilityMode (skips VISIBLE_TO_ALL and the observer's own VISIBLE_TO_OWNER deck,
  shuffles HIDDEN_TO_ALL and other players' hands, keeps index 0 of TOP_VISIBLE_TO_ALL). Deck sizes are preserved.
  This is simpler than hand-rolled pooling (as in Uno).
- Known voids etc.: the constrained overload `reshuffle(playerId, decks, predicate, rnd, permitted)` takes a
  `BiPredicate<Deck, card>` saying whether a card may go in a deck (Agram: `deck.getOwnerId() < 0 ||
  !knownVoids.get(deck.getOwnerId()).contains(card.suite)`; decks with no constraint return true). It returns false
  and falls back to an unconstrained shuffle if the constraints are inconsistent. The VISIBLE_TO_ALL skip applies
  here too, so adding a face-up deck (e.g. the current trick) to the list does nothing.
- Parameters: pass `gameParameters` straight through to the copy's constructor (shared), as the template does.
  Do **not** call `gameParameters.copy()` - with `TunableParameters` that is a significant performance drain on every
  state copy. Parameters (including the random seed) are only changed at game initialisation, so sharing is safe.
  Some older games (Uno, Poker) do copy them; don't follow that example.

## Parameters - always extend TunableParameters
- As a rule, `<P>Parameters extends TunableParameters<<P>Parameters>`, not `AbstractParameters` (the `gametemplate`
  stub extends `AbstractParameters`; change it when copying). Examples: `HeartsParameters`, `GoFishParameters`,
  `CantStopParameters`, `crazyeights/CZEParameters`.
- Keep a public field per parameter (read by the rules as `params.x`), register each in the constructor, and reload
  the fields in `_reset()`:
  ```java
  public CZEParameters() {
      addTunableParameter("nCardsPerPlayer", 5, Arrays.asList(3, 4, 5, 6, 7));   // tunable: a range of values
      addTunableParameter("acePenalty", 1);                                     // single value: JSON-settable only
      addTunableParameter("starterEightSuit", FrenchCard.Suite.Hearts, List.of(FrenchCard.Suite.values()));
  }
  public void _reset() { nCardsPerPlayer = (int) getParameterValue("nCardsPerPlayer"); ... }
  ```
  Types supported: Integer, Double, String, Enum, Boolean, List.
- `_copy()` just returns `new <P>Parameters()` and `_equals(o)` is just `o instanceof <P>Parameters`:
  `TunableParameters.copy()`/`equals`/`hashCode` handle every registered value. Do not override `hashCode`.
  Only a field that is *not* registered (rare - e.g. a data path) needs copying/comparing by hand.
- `instantiate()` returns `this`.
- **End the constructor with `_reset()`**, after the `addTunableParameter` calls, so a new Parameters object holds its
  registered values from the start rather than only after its first `copy()`. It matters most when `_reset()` does
  more than reload fields: Lawn and Order's `_reset()` passes `maxRounds` to `setMaxRounds`, and without the call a
  new object had no round limit until it was copied (found by a test of the default limit). Catan and Chess do the
  same; many older games (and most of those made with these skills before Lawn and Order) do not.
- **Keep each field initialiser equal to its registered default** (`public int targetScore = 121;` with
  `addTunableParameter("targetScore", 121, ...)`). A new Parameters object holds the initialiser value until `_reset()`
  reloads the registered ones (e.g. on `copy()`), so a mismatch gives different rules before and after a copy.
  Found in Cribbage when a rule-break changed only the registered default and no test noticed.
- **Set values with `params.setParameterValue("x", v)`, never `params.x = v`** (in tests and fmtester too). A direct
  field write is not in the tunable values, so it is silently lost the next time the parameters are copied
  (`copy()` calls `_reset()`), and `equals`/`hashCode` do not see it.
- **Do not copy parameter values into the GameState** (or a component, action or GUI field) without a good reason:
  read them from the Parameters where they are needed (`((<P>Parameters) state.getGameParameters()).gridSize`, or a
  state getter that does that lookup). A second copy is a source of future bugs - the two can drift apart, and the
  copy must be carried through `_copy`, `_equals` and `hashCode`, where nothing checks it was. "A copy of the
  parameters loses the value" is not a good reason: that is a test or fmtester setting the field directly instead of
  with `setParameterValue` - fix that. (User's rule, grid refactor: TicTacToe's `gridSize` and Connect 4's
  `width`/`height` had been copied onto the state for this reason.) A good reason is a value that changes in play
  from a starting value the parameter gives (a score that starts at `params.startingScore`) - that is state, not a
  copy. A component's own size given at construction (`new GridBoard(params.width, params.height)`) is part of the
  component, not a copy.

## Scores and results
- Framework defaults (ordinal position, tiebreak) treat a **higher `getGameScore` as better**. For penalty-point games
  return the negative penalty.
- `_getHeuristicScore`: a trivial value in [0, 1] is enough initially (see "Heuristics").
- **Custom end-of-game results belong in an `endGame(AbstractGameState)` override**, never in `_afterAction`.
  `Game.run()` calls `forwardModel.endGame(state)` once more after the game is over (`Game.terminate()`), so results set
  anywhere else are overwritten by the default, ordinal-based ones.
  - *Not needed* - the normal case - when "wins" means "best `getGameScore`". The default `endGame` gives ordinal
    position 1 `WIN_GAME`, everyone else `LOSE_GAME`, and `DRAW_GAME` to all players sharing 1st. Getting `getGameScore`
    right (plus `getOrdinalPosition`/`getTiebreak` if the ranking is not simply by score) is then enough.
  - *Needed* when the result cannot be derived from that ordering: a co-operative game (all win or all lose), an
    insta-win or elimination condition, several players genuinely winning together where the default would call it a
    draw, or a different ranking under one particular end condition (Crazy Eights: a blocked game is won by *every*
    player holding the fewest cards - `WIN_GAME` for all of them, not `DRAW_GAME`).
  - In the override, set `GAME_END` and each `setPlayerResult` yourself for those cases, and delegate to
    `super.endGame(gs)` for the end conditions that do follow the default.

## ForwardModel
- Extend `StandardForwardModel`. Implement `_setup`, `_computeAvailableActions`, `_afterAction` (end of turn/round/game checks).
- `endPlayerTurn(state[, nextPlayer])`, `endRound(state)` are provided. Multi-step decisions: `IExtendedSequence`.
- Simultaneous play: `getCurrentSimultaneousPlayers` in state + player-specific `_computeAvailableActions` (see Diamant).
- **The forward model orchestrates; the rules of a particular action live in the action.** The forward model owns
  setup, the legal actions, and what applies before or after *every* action: passing the turn, ending the round or the
  game, automatic start-of-turn steps. What happens because of a specific action - landing on a particular square,
  the effect of a drawn card - goes in that action's `execute`, so each rule is in one place and the forward model
  does not grow with every special case. When the effect depends on a type (a square, a card), prefer a public
  method on the type's enum that the action calls (`BOARD[to].landOn(state, player, paid)`), so the rules a type
  governs sit in the enum beside the values they distinguish. Choose by where the rule makes most sense: one that is
  really about the action rather than the type stays in the action, as private methods switching on the enum. An enum
  that would need methods for several unrelated areas of the rules is a hint that it mixes concepts and should be
  split into separate enums. Hare and Tortoise first drew and resolved hare cards in the forward model's
  `_afterAction` (`jugTheHare`, `moveByCard`, behind an `instanceof Move` check); now `Move.execute` calls
  `SquareType.landOn`, which handles HOME, LETTUCE and HARE, and a drawn card's effect is `HareCardType.apply`. The
  card effects are mostly runner movement, but they stay on the card enum: they depend only on the card, and
  `landOn` (on the square enum) has to call them, which it could not do if they were private to `Move`. If such an
  effect changes the turn order, the action records it in the state
  (`anotherTurn`, `missNextTurn`) and the forward model acts on that flag without knowing which card set it.
- **Type enums live in the components package, outside the component class.** A card or square type is a top-level
  enum in `components` (Hare and Tortoise's `HareCardType`, `SquareType`), not nested in the component class, so it
  is plain that the component itself (`HareCard`: its type, `copy`, `equals`, `hashCode`) carries no game logic. The
  enum holds its data, queries on that data (`SquareType.paysRacePosition`) and, where they fit best there, the rules
  the type governs (above). Only the forward model, state, parameters and utils sit in the game's root package.
- **No static methods on the forward model.** Helpers are private instance methods (`private boolean canGoHome(...)`):
  a static gives no performance gain worth having, and static or package-private helpers on the forward model invite
  other classes to call into it. A helper another class needs is a query on the state (`state.canLandOn(player,
  square)`) or, if it never reads the state, a static in `<P>Utils`.
- **`_computeAvailableActions` is the search's hot path** (called on every MCTS iteration), so keep it lean:
  - Work that is the same for every candidate action is done once per call, not inside each legality check. A
    placement game finds the frontier (empty positions next to the occupied ones) once, then tests each placement
    against that set, rather than rescanning neighbours for every candidate and orientation. For a stable order,
    collect candidates in a `HashSet` and sort the list once, rather than inserting every (often duplicate)
    candidate into a `TreeSet`. Ventlife's tile placements went from ~265us to ~95us this way.
  - To ask whether any legal action exists (e.g. in `_afterAction`, to skip a step the player cannot take), return
    at the first one found - do not build the full list and test `isEmpty()`.

## IExtendedSequence - multi-step decisions  (important)
- A sequence pushed onto the action stack is **part of the state**: `AbstractGameState.equals`/`hashCode` include
  `actionsInProgress`. So the sequence object must be **equal and hashed by value** (over its own fields), and its
  `copy()` must return a genuinely independent copy - **never `this`**. Otherwise two states that should be equal are
  not, a copy shares mutable state with the original, and everything hash- or copy-based (MCTS, `_copy`,
  redeterminisation) misbehaves silently. Keep the sequence's own data to the minimum it needs.
- **The forward model's `_afterAction` is not called while a sequence is in control.** Decisions inside a sequence go
  to the sequence's own `_afterAction`. The forward model is called once the last sequence on the stack has completed,
  and is given the decision that completed it, not the action that started it. So end-of-turn / end-of-round checks
  and counters there must work from the state, and must not ignore (or depend on) the type of a sequence's decisions.
- **A single follow-on decision by one player: extend `core.actions.OneShotExtendedAction`** rather than implementing
  `IExtendedSequence` from scratch. It is complete after one action, and supplies `getCurrentPlayer`, `copy`,
  `equals`/`hashCode` (by name, player, executed). The subclass is a constructor passing a name, the player and a
  (stateless) lambda computing the actions, plus a `copy()` returning its own type. See `crazyeights/CZEStarterSuitNomination`,
  `wonders7/actions/BuildFromDiscard`.
- **A sequence pushed from inside another action's `execute` is told about that action.** `StandardForwardModel._next`
  executes the action, then passes it to the `_afterAction` of whatever is on top of the stack - skipping it only if
  the action *is* that sequence. A `OneShotExtendedAction` pushed by, say, a `Move` therefore marks itself executed
  straight away and the player never gets the choice. Override its `_afterAction` to count only its own decisions
  (Hare and Tortoise's `DrawOrDiscardCarrots` passes on only a `ChewCarrot`). Until the `StandardForwardModel` fix
  described in the previous bullet lands, the forward model's `_afterAction` also runs after that `Move`, so it
  returns while `state.isActionInProgress()` (harmless once the fix is in).
- **A step with several decisions is one sequence that holds what has happened so far - never decided by inspecting
  the stack** (user's rule, Ventlife). Anti-pattern: an action (or the forward model) choosing what to do by checking
  what is on the action stack - `state.isActionInProgress()`, or `state.currentActionInProgress() instanceof X` - e.g.
  "push a follow-on only if this is the first Snail of the turn". Instead, whatever starts the step (usually the
  forward model's `_afterAction`) pushes one sequence for the whole step, and that sequence records in its own fields
  what has been done (Ventlife's `CreatureStep`: the player, whether a placement has been made, the Snails placed so
  far, whether the player stopped). It offers the next decisions from those fields and decides from them when it is
  complete; the actions it offers just execute and push nothing.
  - Why, beyond style: `isActionInProgress()` is not a pure query - it pops every completed sequence first. Called
    inside `execute`, it can pop a sequence that the action being executed has just completed (Ventlife: the last
    follow-on Snail left no more options, so `isActionInProgress()` popped the follow-on and the Snail looked like the
    first of a turn, starting another - four Snails in a turn, found by a random-game test).
  - `executionComplete` may be called at any time (each `isActionInProgress()` call), including before the
    sequence's `_afterAction` has seen the latest action, so it must be correct from the sequence's fields and the
    state alone.
- **Never push a sequence in `_setup`.** It would survive (`AbstractForwardModel.setup` calls `state.reset()`, which
  clears the stack, *before* `_setup`), but needing to is a sign the game is modelled wrongly: setup produces a state,
  not a pending decision. A decision that happens before normal play belongs to the first player's turn - set the
  current player in `_setup` and let `_computeAvailableActions` offer that decision.

## Actions - identify components by value, NOT by index  (important)
- Do **not** write actions like `PlayCard(cardIndex)` / `PlayCard(deckId, idx)`. This is common in existing games
  (e.g. Uno `PlayCard(deckFrom, deckTo, cardIdx)`) but is an anti-pattern:
  - the same decision ("play the 7 of Hearts") gets a different action whenever hand order differs, so nothing can
    generalise across states (MCTS statistics, action features/heuristics, logging, human-readable traces);
  - indices are fragile under copies, redeterminisation and any change to deck ordering;
  - what matters to the rules and to players is which card, not where it sits.
- Instead the action holds the **value/type** of the component and looks up the physical component on execution:
  - Dominion: actions carry a `CardType` enum (`DominionAction.type`); `execute` calls
    `state.moveCard(type, player, DeckType.HAND, player, DeckType.TABLE)`, which finds a card of that type in the hand.
  - Hearts: `Play(playerID, FrenchCard card)` removes the matching card via `hand.getComponents().remove(card)`
    (FrenchCard is immutable and `equals` by suit/type/number, so the value *is* the identity that matters).
- **Name the component even when it seems implied, and check it on execution.** When the player has only one
  candidate (the single tile in a Ventlife hand), it is tempting to write `PlaceTile(position, orientation)` and
  let `execute` take whatever the hand holds. Instead the action holds the component (`PlaceTile(tile, position,
  orientation)`), and `execute` checks the player really has it - throwing `IllegalStateException` if not - before
  using it (user's rule, Ventlife). This catches errors between computing the action and executing it (a state
  changed in between, a copy or redeterminisation mix-up), which taking "whatever is there" would hide.
- Make actions immutable with `equals`/`hashCode` over those values, so a hand-built action in a test equals the generated one.
- Prefer one parameterised action over near-duplicate classes (e.g. `PlayCard(card, nominatedSuit)` covering the
  wild-card case rather than a separate `PlayEight`).

## Actions - do not store the player id
- By convention an action does **not** hold the acting player. `execute` takes it from `state.getCurrentPlayer()`
  (which already returns the extended sequence's player when one is in progress). So `PlayCard(card, suit)`,
  `DrawCard()`, `Pass()` - not `PlayCard(player, card, suit)`. Hand-built test actions are then player-free too.
- Store a player only when there is a specific need: the action affects a player other than the one acting (a target
  or victim), or it is itself an extended action / sequence that must remember whose decision it is.
- `hashCode`: distinguish action classes with a **class-specific integer constant**, not a string - e.g.
  `return 382901;` for a field-less action, `Objects.hash(card, nominatedSuit) + 382903` otherwise. The framework
  prefers integers over hashing a class-name string on every call (a performance judgement, but the house style).
  `equals` for a field-less action is just `o instanceof DrawCard`.

## Actions - hidden information in `getString` and `toString`  (important)
- `toString` is for logs and debugging and may name everything. `getString(state)` is what the GUI shows to other
  players (the action history), so it must not reveal what the acting player's opponents cannot see. War of the
  Toads' `ReturnCardToDeck` puts a card from hand on the bottom of the deck: `toString` is "Return <card> to the
  bottom of the deck", `getString(state)` is "Player 1 puts a card at the bottom of their deck".
- The acting player still needs to tell their options apart, so also override `getString(state, int perspectivePlayer)`
  to return `toString()` when `perspectivePlayer == state.getCurrentPlayer()` and `getString(state)` otherwise. The
  action buttons are labelled from the deciding player's perspective, and the history is written (`ACTION_CHOSEN`)
  before the action executes, so the current player is still the actor in both.
- When hidden information is revealed later - especially by the forward model rather than by an action (a face-down
  card turned over at the end of a round, a hidden bid shown) - publish it as a game event:
  `state.logEvent(Event.GameEvent.GAME_EVENT, () -> "Player 1's hidden card was the 7 of Hearts")`. The GUI's action
  history shows `GAME_EVENT`s, so everyone sees the reveal at the point it happens, and the `Supplier` form costs
  nothing when no listener is attached.

## Comments
- Comment freely while implementing (intent, decisions, which phase added what) - it helps between phases.
- Once the game is finished, the `tag-game-comment-review` skill removes that scaffolding (plan/log references,
  phase labels) and cuts comments back to house style. Don't polish them phase by phase.
- Name things as the code does. A deck field `drawDeck` is "the draw deck" in comments, method names
  (`drawCard`, not `drawFromStock`) and GUI text - not a synonym from the rules source ("stock", "shoe", "talon").
- Refer to a parameter by class and field when a comment depends on it: "BlackjackParameters.dealerHitsSoft17",
  not "the dealerHitsSoft17 option" or "(see dealerHitsSoft17)" - and never by its default value ("draw or discard
  10" when the amount is `carrotsPerChew`).
- Public Javadoc states the contract. Implementation notes go inside the method (e.g. "FrenchCard numbers Aces as
  14" belongs in the body of `cardValue`, not its Javadoc), and facts that are true but not about what the method
  does are left out ("a total over 21 is bust" on `handValue`).
- Be direct: say what the method does, in the order it happens. Avoid packing two ideas round a colon ("The player
  has finished `finished` hands from the active one: they play...") and elliptical noun phrases; a reader should
  not have to decode the sentence.

## Non-player actors (dealer, bank, environment)
- A dealer or bank that follows fixed rules is part of the forward model, not a player: it has no decisions, and
  its steps happen automatically in `_afterAction`. Do not model its fixed steps as state queries or named stages
  (Blackjack's first version had `dealerPeeks()` on the state and a "peek" step in the FM - both removed: the FM just
  checks `dealerHasBlackjack()` once insurance is settled).
- Drop conditions that another condition implies: a dealer can only have Blackjack under an Ace or ten-value up card,
  so "peeks under an Ace or ten and has Blackjack" is just "has Blackjack".

## Code organisation
- **Do not fail silently** (user's rule, grid refactor). A call that cannot do what it was asked - a node put off a
  `GridBoard`, a card that is not in the hand - throws (`IllegalArgumentException` for a bad argument,
  `IllegalStateException` for a state it cannot act in) rather than returning false or doing nothing. A caller that
  expects the case checks it first (`GridBoard.inBounds(x, y)`).
- **A board that could vary is data, not code** (user's rule, Risk). Territories, connections and anything that
  belongs to one map (continent bonuses, the missions that name its continents, a rule number that depends on the
  map's size such as the 24 territories of Risk's backup mission, screen positions) go in a file under
  `data/<game>/`, chosen by a String parameter with the standard board as default, and loaded with the framework's
  JSON support (`GraphBoard.loadBoard`, `JSONUtils`) rather than a new parser. The board's items are immutable value
  objects made on loading (Java records), not enums - compare them with `equals`, never `==`, since two loads make
  equal but separate objects. No static helpers or static data on the Parameters class.
- Keep related helpers together, and fold near-duplicates into one parameterised helper. Blackjack had three
  "next player to bet / insure / play" loops and three "pass the turn, or if nobody is left do X" blocks; they became
  `firstPlayerFrom(state, from, IntPredicate)` with three one-line wrappers, and `passTurnOrElse(state, next, Runnable)`.
- A calculation that depends only on the parameters (e.g. the winnings on a bet at the payout ratio) is a method on
  the Parameters class, not on the state or a static in the forward model.
- **Fixed rule data goes in the Parameters too**, not a `static final` in the forward model - especially when a
  parameter modifies it. Whist's trump rotation (Hearts, Diamonds, Spades, Clubs) was a constant in the forward model
  while `noTrumpsInRotation` in the Parameters added a no-trumps deal to it; the list became
  `WhistParameters.TRUMP_ROTATION` and the rule `WhistParameters.rotationTrumps(deal)`.
- **Create a new component for a new instance of a thing; do not reset and reuse the old one.** A new trick is
  `new Trick(name, nPlayers, winner)`, not `trick.reset(winner)` - no `reset`/`clear-and-reassign` methods on
  components. It is cleaner, and it lets completed tricks be kept as a history. Because each new component gets a
  new `componentID`, and `Deck`'s equals/hashCode include it, such a component needs **value equality** (see `Trick`),
  or two states in the same position stop being equal. Decks that last the whole game (hands, draw deck) are still
  emptied and refilled.
- **Static helpers that never read the state do not belong on the GameState.** Card values, and scoring a given list
  of cards (Cribbage's pipValue, rank, playPairPoints, showScore), go in a separate static `<P>Utils` class
  (`CribbageUtils`). The state keeps the queries that read its own fields (`getRunningTotal`, `canPlay`). Using the
  state as a store for unrelated statics was flagged by the user as a major code smell.
- A method that only updates the state from the state itself (e.g. Cribbage's `scoreShow`) may live on the
  GameState or the forward model - the user accepts either.
- **No hard-coded numbers that are game rules.** A rule value is a field in the Parameters (tunable where it makes
  sense) or, if it can never vary, a named constant (in the Parameters, or a `<P>Constants` class). Code, identifiers,
  display text (`getString`, `toString`, sequence names, GUI text) and comments refer to it, never to its default
  value. Hare and Tortoise's draw-or-discard card used `HareAndTortoiseParameters.carrotsPerChew`, but was named
  "Draw or discard 10 carrots", `DRAW_OR_DISCARD_10` and `nDrawOrDiscard10`, and `ChewCarrot.toString` said
  "draw 10": all wrong as soon as the parameter changes. Display text that has the parameters to hand builds the
  value in (`"Either draw or discard " + params.carrotsPerChew + " carrots"`); text that does not, leaves it out.
- **No uncalled methods.** Delete a method nothing calls (Hare and Tortoise's `SquareType.isNumber`, written for a
  rule that ended up as `paysRacePosition`). Overrides, and getters on the state for its fields, are exempt; a
  method called only from tests is kept only if it is one of those or is marked "For testing only".

## Heuristics
- An initial implementation needs only a trivial `_getHeuristicScore` (e.g. the terminal result, else a simple
  score-based value). Designing better heuristics is out of scope for the initial phases.
- By convention the heuristic is in [0, 1]: divide a score by the maximum value it can take. Blackjack uses
  chips / `BlackjackParameters.maxChips()` (an upper bound computed from the parameters), throughout the game.

## GUI
- Only a stub `GUIManager` is created with the game. GUI implementation is a separate Stage after the rules are done;
  do not put GUI work into the implementation phases.
- Show a face-up hand of FrenchCards sorted - not a stock, discard pile or trick, where the order means something.
  Sorting is display only, and applies only while the deck is face-up (sorting a partly hidden hand would reveal the
  hidden cards from their positions). Use `handView.setDisplayOrder(FrenchCard.HAND_DISPLAY_ORDER)` (the classic
  order: Spades, Hearts, Diamonds, Clubs; Ace down to Two), or the game's own comparator where its ranks differ
  (Rummy: `RummyUtils.MELD_ORDER`, Aces low). A view that overrides `drawDeck` takes the order from
  `displayIndices(deck)` rather than sorting again.
- Overlapping cards in a hand must be drawn **rightmost on top**, so each card's top-left index stays visible:
  `handView.setRightmostOnTop(true)` (DeckView's default, leftmost on top, is for piles). Whist's hands showed only
  the first card's rank and suit before this was set in `CardHandView`.
- Label a player's area with the agent's name, `game.getPlayers().get(p).toString()` ("Player 0 [" + name + "]"),
  never its class name (`getClass().getSimpleName()`, or `getClass().toString()` split on dots). `AbstractPlayer.toString`
  is the name set by `setName` (the one given in the Frontend or a run's player JSON) and falls back to the class name
  only when there is none. Perudo and Ventlife showed "MCTSPlayer" and "BrowserPlayer" instead of the players' names.
- Rules tabs: Markdown files in `data/rules/<GameType>/`, filled in from the parameters (`gui.RulesPages`) and added
  with `RulesView.addTabs`, written to `references/rules-tab-style.md`. The same files are the rules
  on the web server's page. Never put rules in an HTML `JLabel`: it does not wrap.
- The history panel: with no human player it shows the state's `getHistoryAsText()`; with a human it is built by an
  ACTION_CHOSEN listener in `AbstractGUIManager`, which must use `event.playerID` - in a simultaneous-move game
  `getCurrentPlayer()` is the same for every part of the joint action (Lawn and Order showed one player id for all).
- Layout gotchas found by offscreen PNG renders (Crazy Eights, Agram), all invisible to the compiler:
  - custom views must override getMinimumSize/getMaximumSize as well as getPreferredSize, or GridBagLayout/BoxLayout
    collapse or stretch them;
  - set unused side panels, and the JTabbedPane (UIManager TabbedPane.contentOpaque/opaque/tabsOpaque), non-opaque;
  - derive band heights from the views' preferred sizes: Agram's trick panel was taller than a player area, so a
    fixed `3 * playerAreaHeight` cut it off and overlapped the South hand;
  - render the decision point with the most actions to check the action panel's height: `createActionPanel` lays
    the buttons out as a wrapping grid that scrolls vertically (gui.FlowGridLayout), whatever the `boxLayout` flag;
  - text on the green table background must be light (white), including TitledBorder titles (`setTitleColor`);
  - render the game-over state too: a centre panel that describes "whose turn it is" can be wrong once the game ends;
  - **`JFrame.pack()` is capped at the screen size**, so a GUI taller than the screen is silently cut off - in the
    offscreen renders and in the Frontend alike, and raising `setPreferredSize` changes nothing. Keep the whole window
    within a 1080-pixel screen (info panel 180 + action panel 100 + tab header and insets ~60 + the game area), and
    check the rendered size against the preferred size. Cuckoo's 780-pixel table lost its bottom seat; 660 fits;
  - seats placed round a table (an ellipse) can overlap at the largest player count: render that count, and widen
    the table for it if needed (Cuckoo: 1240 wide for more than 8 players, 980 otherwise).

## Rules analysis checklist (generic)
- Brief vs external source vs reference code: record precedence and every conflict.
- Special cases at setup (e.g. the first card turned up is a special card).
- Resource exhaustion (empty draw pile) and what happens if it can't be replenished.
- Can the game fail to terminate (everyone blocked / passing)? Propose a termination rule.
- Forced vs optional actions (must play if able? can play a drawn card?).
- Social rules with no real decision for an agent (e.g. announcing "last card") - usually out of scope.
- Player-count dependent setup (cards dealt, number of decks).
- Check the brief's player range against the components (cards dealt x players <= deck size). Agram's brief said
  2-8, but its 35-card deck with 6 cards each only allows 5 players.

## Build
- From `claude_game_creator/`: `mvn -q -f ..\pom.xml compile -o`.
- Tests are skipped by default; see AGENTS.md for compiling and running a single test class.
