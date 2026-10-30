# JSON-Bag

JSON-Bag represents a game position as a bag of tokens taken from its JSON serialisation, then compares two positions with the Jensen-Shannon distance (JSD). This package holds the tokeniser, the distance, a value function built on prototype bags, and a one-step look-ahead player that uses that value function.

The point of going through JSON is that the same code works for any game, as long as the game can produce a JSON snapshot of its state. Nothing here knows the rules of a particular game.

## Pipeline

```
AbstractGameState  ->  XGameStateContainer  ->  Gson JSON  ->  tokens  ->  Map<String,Integer>
```

The container is the piece you write per game. Everything downstream is game-agnostic.

## Files

| File | What it does |
|---|---|
| `Tokenizer` | Turns an object, a JSON string, or a game state into a token frequency map. Loads prototypes and filter lists from disk. |
| `JensenShannonDistance` | `normalize` and `jsd` over two frequency maps. JSD is symmetric and bounded by 1. |
| `JSONBagHeuristic` | `IStateHeuristic` that scores a state by JSD to one prototype bag per player. |
| `JSONBagOSLAPlayer` | One-step look-ahead: rolls each legal action forward, scores the result with `JSONBagHeuristic`, plays the best. |
| `GameStateContainerFactory` | Maps a `GameType` to the container class that snapshots it. Throws if the game has no container. |
| `TokenizerTest` | Scratch harness, not a JUnit test. It lives in `src/main/java`, reads `test.txt` and `filterTest.json`, and does not run under surefire. |

## Quick start

For a game that already has a container (`CantStop`, `Catan`, `Connect4`, `Dominion` and its two variants, `DotsAndBoxes`, `SeaSaltPaper`, `Uno`, `Wonders7`):

```java
// One prototype bag per player. The bundled folders hold four, one per seat of a 4-player game;
// load as many as your game has players, because evaluateState throws on a mismatch.
// Prototype folders are produced by your own runs and are not checked in; see the experiment section.
List<Map<String, Integer>> prototypes = Tokenizer.loadPrototypes(
        "Experiments/valueFuncTest2/CantStop/prototype100_0_allSegFS_seg4_cum", 4, true);
JSONBagHeuristic heuristic = new JSONBagHeuristic(prototypes);

heuristic.resetCurrentBag(gs);                        // start the running bag at the current state
double value = heuristic.evaluateState(gs, playerId); // 0 when the player has lost, 1 when they have won
```

To play with it:

```java
JSONBagOSLAPlayer player = new JSONBagOSLAPlayer(prototypes, List.of());  // empty filter = keep every token
player.setForwardModel(game.getForwardModel());  // not needed inside Game, which sets it for you
```

`JSONBagOSLAPlayer` accumulates tokens as the game runs, so call `initializePlayer(gs)` before it acts, which `Game` also does for you. Its `_getAction` rolls each candidate through `getForwardModel()`, so a player built outside a `Game` throws `NullPointerException` until the forward model is set. For a game with simultaneous turns, set `player.setSimultaneousMode(true)` before the game starts; that makes each candidate roll to the end of the round rather than a single action.

## Adding a new game

### 1. Write the container

Create `games.<game>.<Game>GameStateContainer` next to the state class. It extends `AbstractGameStateContainer` and copies the fields you want in the token bag. CantStop's is the smallest complete example:

```java
public class CantStopGameStateContainer extends AbstractGameStateContainer {

    protected boolean[] completedColumns;
    protected int[][] playerMarkerPositions;
    protected List<Dice> dice;

    public CantStopGameStateContainer(CantStopGameState gs) {
        super(gs);
        completedColumns = gs.completedColumns;
        playerMarkerPositions = gs.playerMarkerPositions;
        dice = gs.dice;
    }
}
```

What to include: decks, hands, counters, board occupancy, scores, and phase flags that describe the position.

What to leave out: the game parameters, the action history, timers and the extended-sequence stack. The base class already excludes those, and repeating them adds tokens that say nothing about the position and cost time to serialise. `CatanGameStateContainer` shows both sides: it copies scores, resources, tokens, dev cards and the trade offer, while exchange rates, `developmentCardPlayed` and `nTradesThisTurn` are commented out.

Any field of the container is tokenised, so the container is also your control over what the bag can see. If a field is needed by the game but not by the bag, leave it out.

### 2. Register it in the factory

```java
// in GameStateContainerFactory's static block
containerFactories.put(GameType.Foo, gs -> new FooGameStateContainer((FooGameState) gs));
```

Lookup is by exact `GameType`, so a game with variants needs one line per variant. Dominion does this for `DominionSizeDistortion` and `DominionImprovements`, both pointing at `DominionGameStateContainer`.

`GameStateContainerFactory.hasContainer(gameType)` answers whether a game is wired up.

### 3. Make sure the container serialises

`Tokenizer` serialises the container with Gson. A field that Gson cannot write fails at runtime, and the most common failure is a name collision inside a component:

```
java.lang.IllegalArgumentException: class games.uno.cards.UnoCard declares multiple JSON fields named type
```

`Component` already declares `type`, so a subclass field of the same name cannot be serialised as well. The fork renames the subclass field instead of the parent: `UnoCard.type` became `unoCardType`, `DominionCard.type` became `cardType`, `Wonder7Card.type` became `buildingType`, `Dice.type` became `diceType`. Rename the subclass field and everything that reads it; do not add a Gson exclusion, because the parent's `type` is what tells the tokeniser whether a node is a card, deck or token.

A quick check before going further:

```java
AbstractGameStateContainer container = GameStateContainerFactory.createContainer(gs);
System.out.println(new Gson().toJson(container));
```

### 4. Look at the tokens

Tokenise a fixed position and read a few entries:

```java
Map<String, Integer> bag = Tokenizer.tokenize(gs);
bag.entrySet().stream()
   .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
   .limit(20)
   .forEach(System.out::println);
```

The keys carry the path to the value. From real states:

```
.completedColumns[0].false                       -> 1      indexed position
.completedColumns.false                          -> 13     the same value without the index (13 slots, one per track)
.cardsIncludedInGame.COPPER                      -> 32     from the numeric frequency rule
.cardsIncludedInGame.COPPER.32                   -> 1      the same field as a plain value
.playerDrawPiles.components.cardType.COPPER      -> 7
.currentCard.unoCardType.Skip                    -> 1
```

With `mode=both`, each atomic value is emitted twice, once with array indices and once without, so expect roughly two tokens per value. For scale, a fresh 2-player game gives 133 tokens for CantStop, 384 for Dominion and 945 for Uno.

Check that you see the entities you meant to include, that the counts are stable across two runs of the same game, and that noisy fields stay out. Key names come from the game's own state, so two games with different names produce different bags even when the positions are equivalent.

### 5. Add a test

Put it in `src/test/java/players/jsonBagPlayers/`, next to `JSONBagOSLAPlayerTest`. Assert on tokens you expect, not on the whole bag, so small changes to the container do not break the test for no reason:

```java
Map<String, Integer> bag = Tokenizer.tokenize(state);   // a fresh 2-player CantStop game
assertEquals(Integer.valueOf(1), bag.get(".completedColumns[0].false"));
assertEquals(Integer.valueOf(13), bag.get(".completedColumns.false"));
```

Run it with:

```bash
mvn test -Dmaven.test.skip=false -Dtest=JSONBagTokenizerTest
```

The pom sets `maven.test.skip=true`, so the flag is required.

## Prototypes and filters

A prototype is a bag of tokens produced by one player's trajectories. `Tokenizer.loadPrototypes(dir, n, full)` reads `prototype0.json` to `prototype<n-1>.json`, or `full_prototype<i>.json` when `full` is true; both are plain `Map<String, Integer>` documents.

`JSONBagHeuristic` needs exactly one prototype per player, because `evaluateState` returns the entry for the player it is asked about:

```java
if (gs.getNPlayers() != prototypes.size()) throw new RuntimeException("PLAYER COUNT AND PROTOTYPE COUNT MISMATCHED: ...");
```

A filter list is applied as a whitelist by the heuristic: `Tokenizer.tokenize(gs, filterSet, true)`. An empty list means no filtering. `Tokenizer.loadStringList(path)` reads a JSON array of token strings, which is the format `features-top500.json` uses in the experiment folders.

## Tokeniser options

The full signature is `Tokenizer.tokenize(obj, prefix, ordered, mode, numSuffixFrequency, filterPlayer, binning, pairXY, filterSet, whitelist)`.

| Parameter | Effect |
|---|---|
| `prefix` | Prepended to every token produced for this node. |
| `ordered` | Include array positions (`[0].`, `[1].`). Turn it off when the order of a list carries no meaning. |
| `mode` | `both` emits ordered and unordered tokens, `ordered` and `unordered` narrow that, `char` throws `NotImplementedException`. |
| `numSuffixFrequency` | For a positive integer value, add the key with that integer as its count, so `"gold": 30` contributes `gold` 30 times. Integers only; decimals are ignored. |
| `filterPlayer` | Skip any object with a `player` field greater than 0. Useful when only the acting player's view matters. |
| `binning` | Bucket `x` and `y` values into multiples of 2. |
| `pairXY` | Combine `x` and `y` into one token (`.x.3.y.5`) instead of two. |
| `filterSet`, `whitelist` | Keep only the listed tokens, or drop them. |

The convenience overloads use `mode=both`, `numSuffixFrequency=true`, no binning, no pairing and no filter. They differ in one respect: `tokenize(Object)` and `tokenize(AbstractGameState)` pass `ordered=true`, which is the path the heuristic and the player take, while `tokenize(String json)` passes `ordered=false` and so emits only the unindexed form at the top level.

## Running the experiments

`core.TAG` dispatches to the driver, which lives in `evaluation/jsonBag/`:

```bash
java -jar target/TAG.jar JSONBagValueFunctionExperiments game=DotsAndBoxes n=1 nPlayers=2 \
     experimentRootPath=Experiments/valueFuncTest2/DotsAndBoxes
```

Arguments: `game` (`GameType` name, default `Dominion`), `n` (games per seed, default 100), `nPlayers` (default 4), `topK` (default 500), `includeNone` (default false), `experimentRootPath` (default `Experiments/valueFuncTest2/<game>`).

The root holds one folder per prototype set and one per feature selection; the driver walks every subfolder and uses each as both roles:

```
<root>/<prototypesA>/full_prototype0.json ... full_prototype<nPlayers-1>.json
<root>/<featuresB>/features-top500.json
<root>/results/jsonbag_pair_statistics.csv
```

Folders whose name contains `singleFS` are skipped, and a folder with no `features-top<K>.json` is skipped unless `includeNone` is set, which adds a run with no filter. Each combination runs once with the JSON-Bag player in each seat, the other seats filled by `OSLAPlayer` (`SimultaneousOSLAPlayer` and `setSimultaneousMode(true)` for Wonders7). Results go to a new CSV under `results/`; an existing file is never overwritten, the driver adds `_1`, `_2` and so on.

The prototype and feature folders are generated by your own runs and are not in the repository. Keep them under `Experiments/valueFuncTest2/<game>` on disk, which is where the driver looks by default, or pass `experimentRootPath`.

## Limitations worth knowing

- `JSONBagOSLAPlayer._getAction` shuffles the legal actions with the global `Collections.shuffle(actions)` before scoring, so a seeded run can still pick different actions between ties. Seed the shuffle through `rnd` if you need reproducible runs.
- A bag that shares no tokens with any prototype is a degenerate input; `subtractNormalized` throws `IllegalStateException` with the distances rather than returning `NaN`.
- The container is a hand-written snapshot. When the game state gains a field you want tokenised, add it to the container as well, or the bag silently ignores it.
- The tokeniser walks whatever Gson produces, so a field with an unstable iteration order (a `HashMap` keyed by objects without a stable `hashCode`) shows up as noisy tokens. Prefer lists and `LinkedHashMap`/enum-keyed maps in containers.

