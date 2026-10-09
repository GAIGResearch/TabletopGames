package games.leducpoker;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.leducpoker.actions.Call;
import games.leducpoker.actions.Fold;
import games.leducpoker.actions.Raise;

import java.util.ArrayList;
import java.util.List;

import static core.CoreConstants.VisibilityMode.*;

public class LeducPokerForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        LeducPokerGameState state = (LeducPokerGameState) firstState;
        LeducPokerParameters params = (LeducPokerParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();

        state.hands = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++)
            state.hands.add(new Deck<>("Hand " + p, p, VISIBLE_TO_OWNER));
        state.drawDeck = new Deck<>("Draw deck", HIDDEN_TO_ALL);
        state.drawDeck.add(params.deckCards());
        state.board = new Deck<>("Board", VISIBLE_TO_ALL);
        state.contributions = new int[nPlayers];
        state.netChips = new int[nPlayers];

        // Player 0 acts first in the first hand (the framework's default first player)
        startHand(state);
    }

    /**
     * Gathers every card into the draw deck, shuffles it, takes the antes, deals one card to each player,
     * and starts the first betting round.
     */
    void startHand(LeducPokerGameState state) {
        LeducPokerParameters params = (LeducPokerParameters) state.getGameParameters();
        for (Deck<FrenchCard> hand : state.hands) {
            state.drawDeck.add(hand);
            hand.clear();
        }
        state.drawDeck.add(state.board);
        state.board.clear();
        state.drawDeck.shuffle(state.getRnd());
        for (int p = 0; p < state.getNPlayers(); p++) {
            state.contributions[p] = params.ante;
            state.hands.get(p).add(state.drawDeck.draw());
        }
        state.raisesThisRound = 0;
        state.actionsThisRound = 0;
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        LeducPokerGameState state = (LeducPokerGameState) gameState;
        List<AbstractAction> actions = new ArrayList<>();
        // a player may fold only when facing a bet
        if (state.amountToCall(state.getCurrentPlayer()) > 0)
            actions.add(new Fold());
        actions.add(new Call());
        if (state.canRaise())
            actions.add(new Raise());
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        LeducPokerGameState state = (LeducPokerGameState) currentState;
        state.actionsThisRound++;
        int player = state.getCurrentPlayer();
        if (actionTaken instanceof Fold) {
            endHand(state, 1 - player);
        } else if (actionTaken instanceof Call && state.actionsThisRound >= 2) {
            // A call ends the betting round unless it is the round's first action (a check)
            if (state.getBettingRound() == 0) {
                state.board.add(state.drawDeck.draw());
                state.raisesThisRound = 0;
                state.actionsThisRound = 0;
                endPlayerTurn(state, state.getFirstPlayer());
            } else {
                FrenchCard board = state.board.peek();
                LeducPokerParameters params = (LeducPokerParameters) state.getGameParameters();
                int winner = LeducPokerUtils.showdownWinner(state.hands.get(0).peek(), state.hands.get(1).peek(),
                        board, params.highCardUsesBoard);
                endHand(state, winner);
            }
        } else {
            endPlayerTurn(state);
        }
    }

    /**
     * Settles the hand won by the given player (-1 for a tie), then deals the next hand or ends the game.
     */
    void endHand(LeducPokerGameState state, int winner) {
        // The winner takes what the loser put in the pot; on a tie no chips change hands
        if (winner >= 0) {
            int loser = 1 - winner;
            state.netChips[winner] += state.contributions[loser];
            state.netChips[loser] -= state.contributions[loser];
        }
        LeducPokerParameters params = (LeducPokerParameters) state.getGameParameters();
        if (state.getRoundCounter() + 1 < params.nHands) {
            // each hand is a round, and the other player acts first in the next one
            endRound(state, 1 - state.getFirstPlayer());
            startHand(state);
        } else {
            endGame(state);
        }
    }
}
