package games.hareandtortoise;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import games.hareandtortoise.actions.ChewLettuce;
import games.hareandtortoise.actions.Move;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static games.hareandtortoise.HareAndTortoiseParameters.BOARD;
import static games.hareandtortoise.HareAndTortoiseParameters.HOME_SQUARE;

/**
 * Rules of Hare and Tortoise, 1978 Ravensburger edition.
 */
public class HareAndTortoiseForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        HareAndTortoiseGameState state = (HareAndTortoiseGameState) firstState;
        HareAndTortoiseParameters params = (HareAndTortoiseParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();

        state.squares = new int[nPlayers];
        state.carrots = new int[nPlayers];
        Arrays.fill(state.carrots, params.startCarrots);
        state.lettuces = new int[nPlayers];
        Arrays.fill(state.lettuces, params.startLettuces);
        state.lettuceToChew = new boolean[nPlayers];
        state.missNextTurn = new boolean[nPlayers];
        state.finishPositions = new int[nPlayers];

        state.hareDeck = new Deck<>("Hare cards", HIDDEN_TO_ALL);
        state.hareDeck.add(params.hareCards());
        state.hareDeck.shuffle(state.getRnd());
        state.nUnseenHareCards = state.hareDeck.getSize();

        state.setFirstPlayer(0);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        HareAndTortoiseGameState state = (HareAndTortoiseGameState) gameState;
        return legalActions(state, state.getCurrentPlayer());
    }

    /**
     * Every action open to the player on their turn.
     */
    static List<AbstractAction> legalActions(HareAndTortoiseGameState state, int player) {
        List<AbstractAction> actions = new ArrayList<>();
        if (state.lettuceToChew[player]) {
            actions.add(new ChewLettuce());
            return actions;
        }
        HareAndTortoiseParameters params = (HareAndTortoiseParameters) state.getGameParameters();
        int from = state.squares[player];
        int carrots = state.carrots[player];
        for (int to = from + 1; to < HOME_SQUARE; to++) {
            if (params.moveCost(to - from) > carrots) break;
            if (canLandOn(state, player, to) && BOARD[to] != SquareType.TORTOISE)
                actions.add(new Move(from, to));
        }
        if (canGoHome(state, player))
            actions.add(new Move(from, HOME_SQUARE));
        return actions;
    }

    /**
     * Whether the player's runner may end a move on the square: it is unoccupied, and not a lettuce square unless
     * the player still holds a lettuce.
     */
    static boolean canLandOn(HareAndTortoiseGameState state, int player, int square) {
        if (state.isOccupied(square)) return false;
        return BOARD[square] != SquareType.LETTUCE || state.lettuces[player] > 0;
    }

    /**
     * Whether the player may move to HOME: no lettuces left, and after paying no more carrots than the limit for the
     * place they would finish in.
     */
    static boolean canGoHome(HareAndTortoiseGameState state, int player) {
        HareAndTortoiseParameters params = (HareAndTortoiseParameters) state.getGameParameters();
        if (state.lettuces[player] > 0) return false;
        int left = state.carrots[player] - params.moveCost(HOME_SQUARE - state.squares[player]);
        return left >= 0 && left <= params.homeCarrotsPerRacePosition * (state.getNPlayersHome() + 1);
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        HareAndTortoiseGameState state = (HareAndTortoiseGameState) currentState;
        if (!state.isNotTerminal()) return;
        int nPlayers = state.getNPlayers();
        if (state.getNPlayersHome() >= nPlayers - 1) {
            endGame(state);
            return;
        }

        int current = state.getCurrentPlayer();
        int next = current;
        do {
            next = (next + 1) % nPlayers;
        } while (state.isHome(next));
        endPlayerTurn(state, next);
        if (next <= current) {
            endRound(state, next);
            if (!state.isNotTerminal()) return;
        }
        startTurn(state, next);
    }

    /**
     * What happens automatically as the player's turn begins: a player with no legal action goes back to START with
     * a fresh supply of carrots, and moves off from there.
     */
    private void startTurn(HareAndTortoiseGameState state, int player) {
        if (legalActions(state, player).isEmpty()) {
            HareAndTortoiseParameters params = (HareAndTortoiseParameters) state.getGameParameters();
            state.squares[player] = 0;
            state.carrots[player] = params.startCarrots;
            state.lettuceToChew[player] = false;
            state.missNextTurn[player] = false;
        }
    }
}
