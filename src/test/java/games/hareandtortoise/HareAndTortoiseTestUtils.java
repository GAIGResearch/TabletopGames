package games.hareandtortoise;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.hareandtortoise.actions.Move;
import games.hareandtortoise.components.HareCard;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static games.hareandtortoise.HareAndTortoiseParameters.HOME_SQUARE;

/**
 * Helpers to build and arrange Hare and Tortoise states. The arrange helpers write the package-private state fields
 * directly; the hare cards are only ever reordered, so all 12 are kept.
 */
final class HareAndTortoiseTestUtils {

    private HareAndTortoiseTestUtils() {
    }

    /**
     * A real game from the factory with default parameters, reset with random players, for tests driven by fm.next.
     */
    static Game newGame(int nPlayers, long seed) {
        return newGame(nPlayers, seed, new HareAndTortoiseParameters());
    }

    /**
     * As newGame(nPlayers, seed), with the given parameters (never null). The seed overrides any seed on params.
     */
    static Game newGame(int nPlayers, long seed, HareAndTortoiseParameters params) {
        Game game = GameType.HareAndTortoise.createGameInstance(nPlayers, seed, params);
        List<AbstractPlayer> players = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++)
            players.add(new RandomPlayer(new Random(seed + p)));
        game.reset(players);
        return game;
    }

    static HareAndTortoiseGameState stateOf(Game game) {
        return (HareAndTortoiseGameState) game.getGameState();
    }

    static HareAndTortoiseForwardModel fmOf(Game game) {
        return (HareAndTortoiseForwardModel) game.getForwardModel();
    }

    /**
     * Asserts the action is available to the current player, then plays it with fm.next.
     */
    static void play(HareAndTortoiseForwardModel fm, HareAndTortoiseGameState state, AbstractAction action) {
        org.junit.Assert.assertTrue(action + " is not available to player " + state.getCurrentPlayer(),
                fm.computeAvailableActions(state).contains(action));
        fm.next(state, action);
    }

    /**
     * Puts the player's runner on the square (0 START, 1-63 the board) with the given carrots and lettuces, still in
     * the race, with no lettuce to chew and no missed turn.
     */
    static void place(HareAndTortoiseGameState state, int player, int square, int carrots, int lettuces) {
        state.squares[player] = square;
        state.carrots[player] = carrots;
        state.lettuces[player] = lettuces;
        state.lettuceToChew[player] = false;
        state.missNextTurn[player] = false;
        state.finishPositions[player] = 0;
    }

    /**
     * Puts the player's runner HOME, having finished in the given place (1 for the first home).
     */
    static void putHome(HareAndTortoiseGameState state, int player, int finishPosition) {
        state.squares[player] = HOME_SQUARE;
        state.finishPositions[player] = finishPosition;
        state.lettuces[player] = 0;
        state.lettuceToChew[player] = false;
        state.missNextTurn[player] = false;
    }

    /**
     * The set of Move(from, to) for each of the given target squares.
     */
    static Set<AbstractAction> moves(int from, int... tos) {
        Set<AbstractAction> set = new HashSet<>();
        for (int to : tos)
            set.add(new Move(from, to));
        return set;
    }

    /**
     * The current player's available actions, as a set.
     */
    static Set<AbstractAction> actions(HareAndTortoiseForwardModel fm, HareAndTortoiseGameState state) {
        return new HashSet<>(fm.computeAvailableActions(state));
    }

    /**
     * Reorders the existing 12 hare cards so that the given types are at the top (index 0 first) in that order; the
     * other cards follow in their current order. Conserves the cards: fails if a type is not available.
     */
    static void stackHareDeck(HareAndTortoiseGameState state, HareCard.Type... top) {
        List<HareCard> rest = new ArrayList<>(state.hareDeck.getComponents());
        List<HareCard> ordered = new ArrayList<>();
        for (HareCard.Type type : top) {
            HareCard found = null;
            for (HareCard c : rest)
                if (c.type == type) {
                    found = c;
                    break;
                }
            org.junit.Assert.assertNotNull("no " + type + " card left to stack", found);
            rest.remove(found);
            ordered.add(found);
        }
        ordered.addAll(rest);
        state.hareDeck.clear();
        for (HareCard c : ordered)
            state.hareDeck.addToBottom(c);
    }

    /**
     * The types of the hare cards, top (index 0) first.
     */
    static List<HareCard.Type> hareTypes(HareAndTortoiseGameState state) {
        List<HareCard.Type> types = new ArrayList<>();
        for (HareCard c : state.hareDeck.getComponents())
            types.add(c.type);
        return types;
    }

    /**
     * The list with its first element moved to the end the given number of times: the pile after that many draws,
     * each card going back at the bottom.
     */
    static <T> List<T> topToBottom(List<T> list, int times) {
        List<T> result = new ArrayList<>(list);
        for (int i = 0; i < times; i++)
            result.add(result.remove(0));
        return result;
    }
}
