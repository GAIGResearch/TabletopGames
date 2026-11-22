---
name: tag-game-comment-review
description: Final tidy-up of the comments in a newly implemented TAG game once all its stages and the code clean-up (tag-game-cleanup) are done - remove the scaffolding written between phases (plan/log references, phase labels, history), move logic out of Javadoc into the methods, and cut comments down to what the code does not already say.
---

# TAG new game - comment review

Writing comments freely *during* implementation is encouraged: they record intent, decisions and the phase in
which something arrived, and that helps between phases. Once the game is finished much of that is scaffolding.
This skill turns it into the comments a reader of the finished code needs. It is about comments only: the code has
already been checked against house style by `tag-game-cleanup`, which runs first.

## The four checks that matter most - apply them to EVERY comment
Most of what the user corrects is one of these. A grep for scaffolding finds almost none of it (Klaverjassen's
first review grepped, fixed a TODO and a few long Javadocs, and missed the rest): **read every Javadoc and every
comment in the scope, one by one, and decide for each whether it survives these four checks.**

1. **Does it say how the result is worked out?** Then it is logic, and it goes *into the method*, next to the code it
   explains - or is deleted if the code already says it. This covers a case-by-case account ("the nat: if the trump
   team has fewer points..."), a table of values that the switch below it spells out (card points, rank orders), how a
   player id is derived ("the player on the dealer's left" on `getTrumpChooser`), the formula of a heuristic, the
   conditions under which something is recorded, and the same in test helpers and oracles. The Javadoc keeps at most
   one line saying *what* the method does or returns, often nothing. Overrides (`_getHeuristicScore`,
   `_computeAvailableActions`, `getMaxActionSpace`) usually need no Javadoc at all: their comment goes in the body.
2. **Does it repeat something already said** - by the method or class name, by the field list in the class Javadoc,
   or by the contract of a framework/shared class? Delete it. A getter needs no Javadoc when the field list describes
   the field, *including* the value while unset ("or null before it is chosen" belongs once, in the field list, not
   again on `getTrumpSuit`). "The player who dealt this hand" on `getDealer` is the name. The same holds for classes:
   a class whose name says what it is needs no Javadoc - `ChooseTrump` ("The current player chooses the trump suit"),
   a forward model ("contains all the game rules and logic"), a `<P>Utils` class, "Game state for <Game>".
3. **Is it about something else?** A field entry says what the field holds - not who sets it, when, or why
   ("trumpSuit - ... or null while the player on the dealer's left chooses it": who chooses is `getTrumpChooser`'s
   business; the user cut it to "or null before it is chosen"). A class Javadoc says what the class is - not the house
   rules every class follows ("Data-only: all logic lives in the forward model", "This is a view only - it never
   changes the state"), and not what happens elsewhere ("They then lead the first trick" on `ChooseTrump`: the
   forward model does that). An action's Javadoc says what it does - not where it is used or when it is offered
   ("This is how a runner stays on a carrot square, and the choice offered by the hare card" on `ChewCarrot`; "whose
   runner has just reached a lettuce square" on `ChewLettuce`). Nor does a comment state what the code leaves unchanged: "Each card belongs to its
   printed suit" on `KlaverjassenCardOrder` is true whether or not the class exists, so it says nothing about it -
   describe what the code *does*, not what it doesn't touch.
4. **Is it scaffolding?** Plan/log/test-notes references, phase labels, "decision D4", TODOs, template boilerplate.

Then apply the detailed rules below (most are special cases of these four).

## Scope and order of work
Scope: `src/main/java/games/<name>/` (including `actions/` and `gui/`) and `src/test/java/games/<name>/` - **tests and
the GUI get the same review as the rules code**, including test helpers and oracles - plus the game's rules pages in
`data/rules/<GameType>/` and any lines for the game in `src/test/java/games/fmtester/`. Nothing else, except that a shared package the game helped build or changed (e.g.
`games.tricktaking`) is in scope too, and a change to a shared class is carried through every game that uses it (grep
for its callers) - report which games that touched.

Read first: `references/framework-notes.md` (house style), `AGENTS.md` (the repository root), and the
before/after pairs in `Comment Examples.txt` (the user's own corrections).

Order: (1) every file in scope, top to bottom, with the four checks and the detailed rules;
(2) verify. Edit Java with the Edit tool (see
`references/windows-editing.md`).

## 1. Comment rules in detail (learnt from the user's amendments)
Delete, or cut back to what is left once these are removed:
1. **References to the plan or log.** `<Game>_plan.txt`, `<Game>_log.txt` and `<Game>_test_notes.md` are deleted after implementation - no
   "See claude_game_creator/..._plan.txt", no pointers into the log, no "as decided in review", no "(decision D4)".
2. **Phase labels and history.** No "Phase A (...), Phase B (...) implemented", no "Phase D:" prefixes on test-class
   Javadoc, no "(Phase B finding)". Comments describe the code as it is, not the order it was written in. Keep the
   *content* of a phase-labelled sentence if it describes the behaviour; drop the label.
3. **Restating what a called framework method or shared class already documents.** e.g. after
   `DeterminisationUtilities.reshuffle(...)`, "The visibility modes of the decks determine what is hidden from playerId"
   or "keeping hand sizes" is that utility's contract, not this game's. Keep the line that says what *this* game
   hides. Likewise a field list entry "currentTrick - the trick in progress; its leader leads (or led) it" repeats
   `Trick.getLeader`.
4. **Restating house rules that apply to every class.** e.g. "The card is identified by its value, not its position in
   the hand" on PlayCard, "must be equal by value because the action stack is hashed" on a sequence, "Data-only: all
   initialisation and rule logic lives in the forward model" on a GameState, "This is a view only - it holds no rules
   and never changes the state" on a GUIManager. The guidelines say it once; the class does not need to justify
   following them.
5. **Justifying a statement by pointing at itself.** e.g. "players ranked by cards in hand - consistent with the
   blocked-game results" when that ranking *is* the blocked-game result. Cut the trailing clause.
6. **Describing what is absent instead of the effect.** "no tiebreak function, so players on the same number of cards
   share a position" -> "players on the same number of cards share a position". Say what happens.
7. **Stage 1 stub leftovers.** TODOs that point at the plan, copied `gametemplate` boilerplate Javadoc ("This class
   should hold a series of variables..."), empty stub comments.
8. **Synonyms for things the code names.** Use the field's name: "draw deck" for `drawDeck` - in
   comments and GUI text. (Identifiers are renamed to match by `tag-game-cleanup`.)
9. **Parameters referred to vaguely.** "(see dealerHitsSoft17)", "the doubleDown option" ->
   "BlackjackParameters.dealerHitsSoft17".
10. **Implementation notes, or how the result is worked out, on Javadoc** (check 1 above) - public or private, class
   or method, main or test code. Move them into the method body, next to the line they explain ("FrenchCard numbers
   Aces as 14, so Aces are high" beside the `number` comparison in `Trick.beats`). This includes a case-by-case
   account of how the return value or effect is calculated: the scoring cases of `scoreDeal`, the actions offered at
   each stage by `_computeAvailableActions`, how the number in `getMaxActionSpace` is reached, the share formula of
   `_getHeuristicScore`, a class Javadoc listing the rank orders that `rank()` implements, the branches of a test
   oracle. Split it up and put each part as a comment on the branch it describes. The Javadoc keeps at most a
   one-line statement of what the method does, and often nothing.
11. **True but unrelated statements.** A method Javadoc says what the method does; "A total over 21 is bust" does not
   belong on `handValue`.
12. **Elliptical or compressed sentences.** Rewrite directly, in the order things happen. "X: Y" constructions that
   join two different things, and noun-phrase shorthand ("a new hand, straight after the given one, takes the pair's
   second card") are the usual culprits. "Once both have, the starter is turned up" -> "Then the starter is turned up".
   So is a fronted "for each X," - "tricksTaken - for each player, the tricks they have won this deal" -> "the tricks
   each player has won this deal"; when two neighbouring fields differ in what they are kept for (player vs team),
   make that the word the reader notices.
13. **Describing the obvious case.** Give only what is special: "Ace 1, court cards 10", not "Ace 1, number cards
   their number, court cards 10". Name the general purpose rather than listing each use ("for scoring", not "towards
   fifteens and the running total").
14. **A summary where the exact rule is short.** "In even numbered rounds it is player 0; in odd rounds player 1",
   not "it alternates each round, starting with player 0" - the exact rule is what the code does and is no longer.
15. **Timeless present for something that happens later.** Use the future tense for a later event: "the four
   discarded cards that will be scored by the dealer", "the non-dealer of the next one and will act first" - not
   "which the dealer scores", "and acts first in it", which read as if they happen now.
16. **Justifying an override by what the default would do.** "The default implementation would call two partners
   sharing first place a draw" on `endGame`: the Javadoc says what this method does, and the difference shows.
17. **Anything but what the field holds, on a field entry** (check 3 above). "trumpCard - the dealer's turned-up last
   card ... It is public knowledge that the dealer holds it until they play it" - the second sentence is about
   redeterminisation, and belongs (if anywhere) in `redeterminise`. Nor does an entry describe another field
   ("kitty - ... After the deal its top card is the up-card" when `upCard` has its own entry), or who sets it
   ("or null while the player on the dealer's left chooses it" -> "or null before it is chosen").
18. **Javadoc that repeats the method name or the field list** (check 2 above). No comment at all on a method whose
   name already says what it does: "Records the choice of trumps" on `setTrumps`, "The tricks won by the team this
   deal" on `getTeamTricks`, "Records a pass" on `countPass`, "The player who dealt this hand" on `getDealer`. A
   getter whose field is in the class's field list needs nothing - the unset value and units included, since the
   field list gives them. Keep a Javadoc only for what neither the name nor the field list says: a side effect
   ("Also starts the first trick" on `setTrumpSuit`), or a non-obvious meaning ("the player's *team*" on
   `getGameScore`).
19. **Style Review** Check the file "Comment Examples.txt" in the tag-game-comment-review skill directory for example of orginal comments, and how these were rewritten.
Keep:
- the *why* behind a game-specific choice that the code cannot show (e.g. "the dealer is the last player, so player 0
  still plays first"; the source for a rule, e.g. Pagat) - on the Javadoc when it is part of the contract, inside the
  method when it is about the implementation ("FrenchCard numbers Aces as 14, so needs special-casing");
- a Javadoc on a class or public method only where its name does not already say what it is or does (check 2, rule
  18), saying it in rule terms - e.g. the game state's teams and field list, a Parameters class's rules source;
- "For testing only" on test hooks (required by the test guidelines);
- in tests: rule arithmetic and the arranged position (test guideline F1/11), and arrangement-guard explanations. A
  test class Javadoc says what is tested and the conventions of its arranged positions (e.g. "Hearts are trumps
  throughout") - not a restatement of the rules under test, which is check 1 again.
Style: plain sentences, no marker prefixes, no changelog tone ("now", "new", "changed to"), no first person.
The rules text in `data/rules/<GameType>/` is in scope too: rules 5, 6, 8 and 12-15 apply to it as to comments (see
`references/rules-tab-style.md`, "Writing").
Shorter is better when nothing is lost - most edits are truncations or moves into the method.

## 2. Verify
Moving comments rarely breaks anything, but a marker may have asked for a code change:
- `mvn -q -o test-compile "-Dmaven.test.skip=false"` from the repo root (quote dotted `-D` properties in PowerShell);
- run every test class in `src/test/java/games/<name>/` (AGENTS.md "Running tests" - single classes, never the suite);
- if a shared class changed, also run the test classes of every other game that uses it, and their fmtester methods;
- run the game's fmtester methods only:
  `mvn -q -o test "-Dmaven.test.skip=false" "-Dtest=ForwardModelTestsWithRandom#test<Game>,ForwardModelTestsWithMCTS#test<Game>" -Dsurefire.failIfNoSpecifiedTests=false`.
- finally `grep -rn "JG\|Phase [A-Z]\|_plan\|_log" ` over the scope should return nothing. This grep checks only the
  scaffolding; it says nothing about the four checks, which need the read-through.

## Constraints
- Do not edit plan/design docs the user may be editing - suggest text to paste instead.
- Do not commit unless asked.
