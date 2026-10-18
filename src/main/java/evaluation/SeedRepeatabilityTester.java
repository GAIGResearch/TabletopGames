package evaluation;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.GameType;
import players.IAnyTimePlayer;
import players.PlayerConstants;
import players.PlayerFactory;
import players.mcts.MCTSPlayer;
import utilities.Utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Checks that a game is repeatable: if the game and every agent playing it are given the same seed, then
 * playing it several times gives exactly the same sequence of actions.
 * <p>
 * Each run creates a fresh game and fresh agents (from the supplied factory), seeds them all with the one
 * seed, plays the game to the end, and records the action history. The runs are then compared action by
 * entry by the game's own log of play ({@code getHistoryAsText()}: each action as {@code getString()} shows it
 * in the state it was taken in, plus anything the game logs besides). The log is compared rather than the actions
 * themselves because component IDs come from a counter shared by every game, so the same action in two runs
 * differs in {@code equals()} and often in {@code toString()}.
 * <p>
 * An agent is seeded through its {@code PlayerParameters} (with {@code resetSeedEachGame}, which MCTS uses to
 * re-seed its rollout and opponent policies) and by re-seeding its own {@code Random}. An agent with a time
 * budget can never be repeatable, so such a budget is reported as a warning.
 * <p>
 * The game's {@code redeterminisationSeed} is set to the seed too, so that the random events an agent simulates on
 * copies of the state are repeatable. This needs a positive seed.
 * <p>
 * Runs within one JVM share its identity hash codes, so they cannot see a game that depends on them (for
 * example by iterating over a {@code HashMap} keyed by an enum, whose hash code is its identity hash code).
 * {@link #checkAcrossLaunches} plays each run in a separate JVM to catch those. HotSpot draws identity hash codes
 * from a per-thread sequence that starts the same in every launch, so each launch first uses up a different number
 * of them; otherwise two launches doing the same work would give the same hash codes, and pass by luck.
 * <p>
 * From the command line ({@code key=value} arguments): game, nPlayers (2), seed (42), runs (2),
 * agent (a player JSON file or a {@link PlayerFactory} name; defaults to an {@link MCTSPlayer} with default
 * parameters), budget (overrides the agent's budget, if it has one) and launches (if 2 or more, play that many
 * runs each in a separate JVM instead of runs in this one).
 */
public class SeedRepeatabilityTester {

    /**
     * @param actions          one entry per run: the game's log of play, in order
     * @param firstDivergence  the index of the first log entry that differs between runs, or -1 if none does
     * @param divergingRun     the run that first differs from run 0 (-1 if none does)
     */
    public record Result(boolean repeatable, List<List<String>> actions, int firstDivergence, int divergingRun) {

        @Override
        public String toString() {
            if (repeatable)
                return String.format("Repeatable: %d runs of %d logged actions each", actions.size(), actions.get(0).size());
            List<String> first = actions.get(0);
            List<String> other = actions.get(divergingRun);
            return String.format("NOT repeatable: run %d diverges from run 0 at log entry %d%n  run 0      : %s%n  run %-7d: %s",
                    divergingRun, firstDivergence,
                    firstDivergence < first.size() ? first.get(firstDivergence) : "<game over>",
                    divergingRun,
                    firstDivergence < other.size() ? other.get(firstDivergence) : "<game over>");
        }
    }

    public static void main(String... args) {
        GameType gameType = GameType.valueOf(Utils.getArg(args, "game", "TicTacToe"));
        int nPlayers = Utils.getArg(args, "nPlayers", 2);
        long seed = Utils.getArg(args, "seed", 42L);
        int runs = Utils.getArg(args, "runs", 2);
        String agent = Utils.getArg(args, "agent", "");
        int budget = Utils.getArg(args, "budget", -1);
        int launches = Utils.getArg(args, "launches", 0);
        String logFile = Utils.getArg(args, "logFile", "");
        int hashShift = Utils.getArg(args, "hashShift", 0);

        Supplier<AbstractPlayer> agentFactory = agentFactory(agent, budget);
        if (!logFile.isEmpty()) {
            // one run of a check across launches: play once and write the log for the parent to compare
            for (int i = 0; i < hashShift; i++)
                System.identityHashCode(new Object());
            writeLog(play(gameType, nPlayers, seed, agentFactory).getHistoryAsText(), Path.of(logFile));
            System.exit(0);
        }
        Result result = launches >= 2
                ? checkAcrossLaunches(gameType, nPlayers, seed, agent, budget, launches)
                : check(gameType, nPlayers, seed, agentFactory, runs);
        System.out.println(gameType + " with seed " + seed + " : " + result);
        if (!result.repeatable())
            System.exit(1);
    }

    /**
     * @param agent  a player JSON file or a {@link PlayerFactory} name; empty for an {@link MCTSPlayer} with
     *               default parameters
     * @param budget overrides the agent's budget if positive (and the agent has one)
     */
    public static Supplier<AbstractPlayer> agentFactory(String agent, int budget) {
        return () -> {
            AbstractPlayer player = agent.isEmpty() ? new MCTSPlayer() : PlayerFactory.createPlayer(agent);
            if (budget > 0 && player instanceof IAnyTimePlayer anyTime)
                anyTime.setBudget(budget);
            return player;
        };
    }

    /**
     * Checks repeatability with two runs of a default MCTS agent in every seat.
     */
    public static Result check(GameType gameType, int nPlayers, long seed) {
        return check(gameType, nPlayers, seed, MCTSPlayer::new, 2);
    }

    /**
     * @param agentFactory called once per seat per run, and must return a new agent each time
     *                     (an agent kept between runs would carry its history into the next)
     * @param runs         the number of times to play the game (at least 2)
     */
    public static Result check(GameType gameType, int nPlayers, long seed, Supplier<AbstractPlayer> agentFactory, int runs) {
        if (runs < 2)
            throw new IllegalArgumentException("At least two runs are needed to compare them");
        List<List<String>> actions = new ArrayList<>();
        for (int run = 0; run < runs; run++) {
            // one line per entry, as when a run's log is read back from another launch
            actions.add(play(gameType, nPlayers, seed, agentFactory).getHistoryAsText().stream()
                    .map(SeedRepeatabilityTester::oneLine).toList());
        }
        return compare(actions);
    }

    /**
     * As {@link #check(GameType, int, long, Supplier, int)}, but plays each run in a new JVM (with this one's
     * classpath), so that runs do not share identity hash codes. The agent is given as for {@link #agentFactory}.
     */
    public static Result checkAcrossLaunches(GameType gameType, int nPlayers, long seed, String agent, int budget, int launches) {
        if (launches < 2)
            throw new IllegalArgumentException("At least two launches are needed to compare them");
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        List<List<String>> actions = new ArrayList<>();
        try {
            for (int launch = 0; launch < launches; launch++) {
                Path log = Files.createTempFile("repeatability", ".log");
                try {
                    Process process = new ProcessBuilder(java, "-Djava.awt.headless=true",
                            "-cp", System.getProperty("java.class.path"), SeedRepeatabilityTester.class.getName(),
                            "game=" + gameType.name(), "nPlayers=" + nPlayers, "seed=" + seed,
                            "agent=" + agent, "budget=" + budget, "logFile=" + log, "hashShift=" + launch * 101)
                            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                            .redirectError(ProcessBuilder.Redirect.DISCARD)
                            .start();
                    int exitCode = process.waitFor();
                    if (exitCode != 0)
                        throw new IllegalStateException("Launch " + launch + " of " + gameType + " failed with exit code " + exitCode);
                    actions.add(Files.readAllLines(log, StandardCharsets.UTF_8));
                } finally {
                    Files.deleteIfExists(log);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a launch of " + gameType, e);
        }
        return compare(actions);
    }

    private static Result compare(List<List<String>> actions) {
        for (int run = 1; run < actions.size(); run++) {
            int divergence = firstDivergence(actions.get(0), actions.get(run));
            if (divergence >= 0)
                return new Result(false, actions, divergence, run);
        }
        return new Result(true, actions, -1, -1);
    }

    /**
     * Plays one game to the end, with the game and all agents seeded with the given seed.
     */
    public static AbstractGameState play(GameType gameType, int nPlayers, long seed, Supplier<AbstractPlayer> agentFactory) {
        Game game = gameType.createGameInstance(nPlayers, seed);
        game.getGameState().getGameParameters().setRedeterminisationSeed(seed);
        List<AbstractPlayer> players = new ArrayList<>();
        for (int i = 0; i < nPlayers; i++) {
            AbstractPlayer player = agentFactory.get();
            if (players.stream().anyMatch(p -> p == player))
                throw new IllegalArgumentException("The agent factory must return a new agent each time it is called");
            seed(player, seed);
            players.add(player);
        }
        game.reset(players, seed);
        game.run();
        return game.getGameState();
    }

    /**
     * Seeds an agent as far as the AbstractPlayer API allows: its parameters (which MCTS re-reads at the start
     * of each game when resetSeedEachGame is set) and its own Random.
     */
    public static void seed(AbstractPlayer player, long seed) {
        if (player.getParameters().budgetType == PlayerConstants.BUDGET_TIME)
            System.out.println("Warning: " + player + " has a time budget, so its decisions will not be repeatable");
        player.getParameters().setRandomSeed(seed);
        player.getParameters().resetSeedEachGame = true;
        player.getRnd().setSeed(seed);
    }

    private static String oneLine(String entry) {
        return entry.replace("\r", "\\r").replace("\n", "\\n");
    }

    private static void writeLog(List<String> log, Path file) {
        try {
            Files.write(file, log.stream().map(SeedRepeatabilityTester::oneLine).toList(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static int firstDivergence(List<String> log, List<String> other) {
        int length = Math.min(log.size(), other.size());
        for (int i = 0; i < length; i++) {
            if (!log.get(i).equals(other.get(i)))
                return i;
        }
        return log.size() == other.size() ? -1 : length;
    }
}
