## Start here

A more detailed, up-to-date summary of the codebase structure is maintained at
**https://deepwiki.com/GAIGResearch/TabletopGames**. Consult it first (e.g. via WebFetch) before
doing any detailed analysis of the source — it is faster than reading the code directly and gives a
fuller architectural picture than the overview below.

## What this is

TAG (Tabletop Games Framework) is a Java benchmark for board/card game AI research. It provides a
common API so that ~40 games and a range of AI agents can be run against each other, tuned, and
analysed through a shared infrastructure. This fork (`Advisers` branch) adds **advisers** (players
that suggest moves to other players) and an **LLM** module that generates/uses heuristics via
langchain4j.

## Build, test, run

Maven project, **Java 21**.

```bash
mvn package                 # builds target/TAG.jar (jar-with-dependencies, main class core.TAG)
```

Tests are **skipped by default** (`maven.test.skip=true` in pom.xml). To run them you must override:

```bash
mvn test -Dmaven.test.skip=false                       # all tests (JUnit 4 + Mockito)
mvn test -Dmaven.test.skip=false -Dtest=CustomDiceTest  # single test class
mvn test -Dmaven.test.skip=false -Dtest=CustomDiceTest#methodName  # single method
```

Running the framework — `core.TAG` dispatches on its first argument to a sub-module:

```bash
java -jar target/TAG.jar <EntryPoint> [args...]
# EntryPoints: RunGames, ParameterSearch, FrontEnd, FrontEndSimple,
#              ExpertIteration, OneStepDeviations, SkillLadder
java -jar target/TAG.jar RunGames --help    # each entry point has its own --help
```

From an IDE, run `core.TAG`, or run `gui.Frontend` directly for the GUI. Most entry points are
configured via JSON config files (see `--help` / `RunArg`) or command-line `key=value` args parsed
by `evaluation/RunArg.java`. A `Dockerfile` builds and packages the same jar with the `data/`
directory.

## Core architecture

A game is the combination of four cooperating pieces, all in `core`:

- **`AbstractGameState`** — all mutable game data. Implement `_copy(playerId)` (redeterminises hidden
  info for that player's perspective), `_getAllComponents()`, `getGameScore()`, `_getHeuristicScore()`,
  `_equals()`. State is the single source of truth; the forward model never holds game data.
- **`AbstractForwardModel`** — the rules engine. Implement `_setup()`, `_next(state, action)`,
  `_computeAvailableActions(state)`, `endPlayerTurn(state)`. Most games extend
  **`StandardForwardModel`** (use `StandardForwardModelWithTurnOrder` only for legacy turn-order games).
- **`AbstractParameters`** — tunable game parameters (implements `ITunableParameters` so games can be
  optimised/searched).
- **`AbstractGUIManager`** subclass — Swing rendering (optional but expected).

`core.Game` is the orchestrator: it owns the state, forward model, and the list of `AbstractPlayer`s,
drives the turn loop, broadcasts `Event`s to `IGameListener`s, and tracks per-tick statistics.

**Actions** (`core.actions`) implement `AbstractAction`; multi-step decisions use
`IExtendedSequence` to keep an action "in progress" across several player decisions.

**Registering a game**: every game lives in `games.<name>` (e.g. `games.catan`) and is wired into the
central enum **`games.GameType`**, which maps the game to its state/forward-model/parameters/GUI
classes plus metadata (min/max players, categories). This enum is the canonical list of games — add
new games here.

## Players (agents)

`core.AbstractPlayer` defines an agent via `_getAction(state, possibleActions)` and `copy()`. Notable
implementations under `players/`:

- `simple` — `RandomPlayer`, `OSLAPlayer` (one-step look-ahead), `FirstActionPlayer`.
- `mcts` (full MCTS, heavily configurable), `basicMCTS`, `rhea` (rolling-horizon EA), `rmhc`.
- `heuristics` — `IStateHeuristic` / `IActionHeuristic` implementations (linear, logistic, decision-tree,
  learned-value, etc.) used to bias the search agents.
- `decorators` (implement `IPlayerDecorator`) — wrap a player to **filter the action list** before it
  decides (e.g. forbidding action classes, epsilon-randomness).
- `observers` — **the Advisers feature** (`Advisers` branch). `GameAdviser` is an `IGameListener`
  composed of an `AbstractPlayer` (decides what is "best") plus an `IAdviceFilter` (decides *which*
  players are advised and *when* to intervene), writing advice to a file.

`players.PlayerFactory.createPlayer(jsonFileOrName)` constructs agents from JSON descriptors — this is
how tournaments and the GUI instantiate configurable players (see `json/players/`).

## Evaluation & analytics

`evaluation/` is the experiment-running side:

- `RunGames` — run tournaments of agents over one or more games (round-robin, one-vs-all, etc.).
- `tournaments/` — `RoundRobinTournament`, `SkillLadder`/`SkillGrid` (vary budget), result analyses.
- `optimisation/` — `ParameterSearch` (NTBEA-based tuning of tunable parameters), `OneStepDeviations`.
- `metrics/`, `listeners/`, `loggers/`, `summarisers/` — `IGameListener` + `Event` pipeline that
  records game data to files/stats for analysis. Listener configs live in `metrics/*.json`.
- `features/`, `heuristics/` — state/action feature extraction feeding learned heuristics and
  `ExpertIteration`.

## LLM module

`llm/` integrates langchain4j (OpenAI, Anthropic, Gemini, Mistral backends; see pom.xml). `JavaCoder`
asks an LLM to write Java heuristic code; `GamePromptGenerator` builds game-state prompts;
`StringHeuristic` compiles LLM-produced heuristic strings (uses `javaparser`). Requires provider API
keys in the environment.

## Config & data locations

- `json/` — player definitions, search spaces, listener and learner configs consumed at runtime.
- `metrics/` — metric/listener JSON configs; `metrics/out/` is the default results directory.
- `data/` — game assets (images, card/board definitions) loaded by some games; shipped into the
  Docker image.

## Unit testing

Tests live under `src/test/java/` mirroring the main source tree (e.g. game tests in
`src/test/java/games/<name>/`, core tests in `src/test/java/core/`). The framework is **JUnit 4**
with **Mockito 5**; there is no JUnit 5 in the project.

**Any test written during development should be committed to the repository** unless it is purely
throwaway scaffolding (e.g. a one-off render harness or a `main` used for manual inspection or to check a rendered image). If a
test helped verify a bug fix or a new feature, it belongs permanently in the test suite.

### Naming

- Class: `<Subject>Test` or `Test<Subject>` (both styles exist; prefer suffix form for new work).
- Methods: descriptive camelCase, no mandatory `test` prefix — `availableActionsExcludeBlockedMoves()`
  is clearer than `testAvailableActionsExcludeBlockedMoves()`.

### Structure

```java
public class XIIScriptaForwardModelTest {

    BGGameState state;
    BGForwardModel fm;

    @Before
    public void setup() {
        Game game = GameType.XIIScripta.createGameInstance(2);
        game.reset(List.of(new RandomPlayer(), new RandomPlayer()));
        state = (BGGameState) game.getGameState();
        fm = (BGForwardModel) game.getForwardModel();
    }

    @Test
    public void bearOffOnlyWhenAllPiecesHome() { ... }
}
```

- Use `@Before` (not `@BeforeEach`) for shared setup.
- Use standard JUnit 4 assertions (`assertEquals`, `assertTrue`, etc.) — no external assertion
  libraries are used in this project.
- No shared base test class; each test class is self-contained.

### Setting up game state

Two patterns are in use — pick the one that fits:

1. **Factory + reset** (preferred for integration-level tests):
   ```java
   Game game = GameType.CantStop.createGameInstance(3, 34, params);
   game.reset(players);
   BGGameState state = (BGGameState) game.getGameState();
   ```

2. **Direct instantiation** (preferred for tightly scoped unit tests):
   ```java
   DominionGameState state = new DominionGameState(new DominionFGParameters(), 2);
   new DominionForwardModel().setup(state);
   ```

### Setting up game state from a saved JSON file

For games that support JSON serialisation, load a saved state rather than simulating to reach a
scenario — simulation can land on awkward mid-tick states (e.g. null dice rolls). Edit the JSON
fields to vary conditions rather than forcing values via reflection:

```java
Game game = GameType.XIIScripta.createGameInstance(2);
game.reset(List.of(new RandomPlayer(), new RandomPlayer()));
BGGameState state = (BGGameState) game.getGameState();
BGStateJSON.loadFromJSON(state, JSONUtils.loadJSONFile("P0_Tick340.json"));
// Edit e.g. "piecesBorneOff" : [4, 7] in the JSON to test bear-off logic
```

For GUI/board-view verification, paint the view to a `BufferedImage` and write a PNG:

```java
MyBoardView view = new MyBoardView((MyForwardModel) game.getForwardModel());
view.update(state);
view.setSize(view.getPreferredSize());
BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
view.paint(img.getGraphics());
ImageIO.write(img, "png", new File("render.png"));
```

Remove any throwaway render harness files and output PNGs before committing.

### Running tests

**Never run the full suite during development** — it includes slow integration tests. Always target
a specific class or method.

```bash
# 1. Copy the dependency jars once (into target/dependency; repeat after mvn clean or a pom.xml change)
mvn dependency:copy-dependencies

# 2. Compile tests
mvn test-compile -Dmaven.test.skip=false

# 3. Run a single test class (quote the classpath: java expands the * itself)
java -cp "target/classes;target/test-classes;target/dependency/*" \
     org.junit.runner.JUnitCore games.cantstop.TestCantStop
```

`java` may not be on PATH; use the full JDK path if needed
(e.g. `C:/Users/<you>/.jdks/ms-21.0.10/bin/java`). Alternatively, run directly from the IDE
by right-clicking the test class or method.

## Python

`core.PyTAG` + `players/python` expose a Python-facing API (PyTAG) for RL/gym-style use.
