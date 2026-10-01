package games.hareandtortoise;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import games.hareandtortoise.actions.ChewCarrot;
import games.hareandtortoise.actions.ChewLettuce;
import games.hareandtortoise.actions.DrawOrDiscardCarrots;
import games.hareandtortoise.actions.Move;
import games.hareandtortoise.components.HareCard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntPredicate;

import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static games.hareandtortoise.HareAndTortoiseParameters.BOARD;
import static games.hareandtortoise.HareAndTortoiseParameters.HOME_SQUARE;
import static games.hareandtortoise.HareAndTortoiseUtils.firstSquare;
import static games.hareandtortoise.HareAndTortoiseUtils.previousTortoise;

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

    static List<AbstractAction> legalActions(HareAndTortoiseGameState state, int player) {
        List<AbstractAction> actions = new ArrayList<>();
        // a turn after reaching a lettuce square is spent chewing it
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
        int tortoise = previousTortoise(from);
        if (tortoise > 0 && !state.isOccupied(tortoise))
            actions.add(new Move(from, tortoise));
        if (BOARD[from] == SquareType.CARROT) {
            actions.add(new ChewCarrot(true));
            if (carrots >= params.carrotsPerChew)
                actions.add(new ChewCarrot(false));
        }
        return actions;
    }

    static boolean canLandOn(HareAndTortoiseGameState state, int player, int square) {
        if (state.isOccupied(square)) return false;
        return BOARD[square] != SquareType.LETTUCE || state.lettuces[player] > 0;
    }

    static boolean canGoHome(HareAndTortoiseGameState state, int player) {
        HareAndTortoiseParameters params = (HareAndTortoiseParameters) state.getGameParameters();
        if (state.lettuces[player] > 0) return false;
        // the carrots left after paying are limited by the place the player will finish in
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
        if (actionTaken instanceof Move move && move.to > move.from && move.to != HOME_SQUARE
                && BOARD[move.to] == SquareType.HARE) {
            HareCard.Type card = jugTheHare(state, current, move);
            if (state.isActionInProgress())
                return;
            if (card == HareCard.Type.ANOTHER_TURN) {
                endPlayerTurn(state, current);
                startTurn(state, current);
                return;
            }
        }
        int next = current;
        while (true) {
            next = (next + 1) % nPlayers;
            if (state.isHome(next)) continue;
            if (state.missNextTurn[next]) {
                state.missNextTurn[next] = false;
                continue;
            }
            break;
        }
        endPlayerTurn(state, next);
        if (next <= current) {
            endRound(state, next);
            if (!state.isNotTerminal()) return;
        }
        startTurn(state, next);
    }

    /**
     * Draws the top hare card, carries it out and puts it at the bottom of the hareDeck. Returns the card's type.
     */
    HareCard.Type jugTheHare(HareAndTortoiseGameState state, int player, Move move) {
        HareAndTortoiseParameters params = (HareAndTortoiseParameters) state.getGameParameters();
        HareCard card = state.hareDeck.draw();
        state.hareDeck.addToBottom(card);
        if (state.nUnseenHareCards > 0)
            state.nUnseenHareCards--;

        int square = state.squares[player];
        IntPredicate freeCarrot = s -> BOARD[s] == SquareType.CARROT && !state.isOccupied(s);
        switch (card.type) {
            case FALL_BACK_ONE_POSITION -> {
                int behind = 0;
                for (int p = 0; p < state.getNPlayers(); p++)
                    if (p != player && !state.isHome(p) && state.squares[p] < square)
                        behind = Math.max(behind, state.squares[p]);
                if (behind > 0)
                    moveByCard(state, player, firstSquare(behind - 1, -1, s -> canLandOn(state, player, s)));
            }
            case LAST_TURN_FREE -> state.addCarrots(player, params.moveCost(move.to - move.from));
            case DRAW_OR_DISCARD_10 -> state.setActionInProgress(new DrawOrDiscardCarrots(player));
            case LEAP_AHEAD_ONE_POSITION -> {
                int ahead = HOME_SQUARE;
                for (int p = 0; p < state.getNPlayers(); p++)
                    if (p != player && !state.isHome(p) && state.squares[p] > square)
                        ahead = Math.min(ahead, state.squares[p]);
                moveByCard(state, player, firstSquare(ahead + 1, 1,
                        s -> canLandOn(state, player, s) && BOARD[s] != SquareType.TORTOISE));
            }
            case NEXT_CARROT_SQUARE -> moveByCard(state, player, firstSquare(square + 1, 1, freeCarrot));
            case PREVIOUS_CARROT_SQUARE -> moveByCard(state, player, firstSquare(square - 1, -1, freeCarrot));
            case MISS_A_TURN -> state.missNextTurn[player] = true;
            case CHEW_A_LETTUCE -> {
                if (state.lettuces[player] > 0)
                    state.chewLettuce(player);
            }
            case ANOTHER_TURN -> {
                // the turn stays with the player: see _afterAction
            }
        }
        return card.type;
    }

    /**
     * Moves the runner free of charge, as a hare card directs. Square 0 means the card found no square, and the
     * runner stays where it is.
     */
    private static void moveByCard(HareAndTortoiseGameState state, int player, int square) {
        if (square == 0) return;
        state.moveRunner(player, square, 0);
        // a runner moved onto a lettuce square chews it next turn, as after a paid move
        if (BOARD[square] == SquareType.LETTUCE)
            state.setLettuceToChew(player, true);
    }

    /**
     * What happens automatically as the player's turn begins.
     */
    private void startTurn(HareAndTortoiseGameState state, int player) {
        HareAndTortoiseParameters params = (HareAndTortoiseParameters) state.getGameParameters();
        // a number square pays when the runner's position in the race matches it as the turn begins
        int position = state.getRacePosition(player);
        if (state.getSquareType(player).paysRacePosition(position))
            state.addCarrots(player, params.carrotsPerRacePosition * position);
        // a stuck player goes back to START with fresh carrots, keeping their lettuces, and moves off from there
        if (legalActions(state, player).isEmpty()) {
            state.squares[player] = 0;
            state.carrots[player] = params.startCarrots;
            state.lettuceToChew[player] = false;
            state.missNextTurn[player] = false;
        }
    }
}
