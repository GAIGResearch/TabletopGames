package games.goofspiel;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.goofspiel.actions.Bid;

import java.util.ArrayList;
import java.util.List;

import static core.CoreConstants.VisibilityMode.*;

/**
 * Rules of Goofspiel. All players bid at the same time, so a Bid only puts the card face down, and the round is
 * resolved once every player has bid.
 */
public class GoofspielForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        GoofspielGameState state = (GoofspielGameState) firstState;
        GoofspielParameters params = (GoofspielParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();

        state.hands = new ArrayList<>();
        state.bids = new ArrayList<>();
        state.playedBids = new ArrayList<>();
        state.wonPrizes = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++) {
            Deck<FrenchCard> hand = new Deck<>("Hand " + p, p, VISIBLE_TO_OWNER);
            // in ascending order of value, which GoofspielGameState.returnToHand relies on
            hand.add(params.suitCards(params.handSuit(p)));
            state.hands.add(hand);
            state.bids.add(new Deck<>("Bid " + p, p, VISIBLE_TO_OWNER));
            state.playedBids.add(new Deck<>("Played bids " + p, p, VISIBLE_TO_ALL));
            state.wonPrizes.add(new Deck<>("Won prizes " + p, p, VISIBLE_TO_ALL));
        }

        state.prizeDeck = new Deck<>("Prize deck", HIDDEN_TO_ALL);
        state.prizeDeck.add(params.suitCards(GoofspielParameters.PRIZE_SUIT));
        state.prizeDeck.shuffle(state.getRnd());
        state.prizesOnOffer = new Deck<>("Prizes on offer", VISIBLE_TO_ALL);
        state.discardedPrizes = new Deck<>("Discarded prizes", VISIBLE_TO_ALL);

        // The first prize is turned up before anyone bids
        state.prizesOnOffer.add(state.prizeDeck.draw());
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        return _computeAvailableActions(gameState, gameState.getCurrentPlayer());
    }

    /**
     * The given player's bids: one per card in their hand, or none if they have already bid this round.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState, int activePlayer) {
        GoofspielGameState state = (GoofspielGameState) gameState;
        List<AbstractAction> actions = new ArrayList<>();
        if (state.getBid(activePlayer).getSize() == 0) {
            for (FrenchCard card : state.getHand(activePlayer))
                actions.add(new Bid(activePlayer, card));
        }
        return actions;
    }

    /**
     * Resolves the round once every player has bid, and otherwise passes the turn to the next player still to bid.
     */
    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        GoofspielGameState state = (GoofspielGameState) currentState;
        if (!state.getPlayersStillToBid().isEmpty()) {
            endPlayerTurn(state, nextPlayerToBid(state));
            return;
        }
        resolveBids(state);
        if (state.getHand(0).getSize() == 0) {
            // A tie in the last round leaves the prizes on offer unwon
            state.discardedPrizes.add(state.prizesOnOffer);
            state.prizesOnOffer.clear();
            endGame(state);
        } else {
            state.prizesOnOffer.add(state.prizeDeck.draw());
            endRound(state, 0);
        }
    }

    /**
     * Reveals the bids and gives the prizes on offer to the winning bidder, if there is one.
     */
    void resolveBids(GoofspielGameState state) {
        GoofspielParameters params = (GoofspielParameters) state.getGameParameters();
        int[] bidValues = new int[state.getNPlayers()];
        for (int p = 0; p < state.getNPlayers(); p++) {
            FrenchCard card = state.getBid(p).draw();
            bidValues[p] = params.cardValue(card);
            state.getPlayedBids(p).add(card);
        }
        int winner = bidWinner(bidValues, params.tieRule);
        if (winner >= 0) {
            state.getWonPrizes(winner).add(state.prizesOnOffer);
            state.prizesOnOffer.clear();
        } else if (params.tieRule != GoofspielParameters.TieRule.CARRY_OVER) {
            state.discardedPrizes.add(state.prizesOnOffer);
            state.prizesOnOffer.clear();
        }
        // Otherwise (CARRY_OVER) the prizes stay on offer, and are contested again with the next prize
    }

    /**
     * The winning bidder under the tie rule, or -1 if nobody wins.
     */
    static int bidWinner(int[] bidValues, GoofspielParameters.TieRule tieRule) {
        if (tieRule == GoofspielParameters.TieRule.HIGHEST_UNIQUE) {
            // the highest bid that nobody else made
            int winner = -1;
            for (int p = 0; p < bidValues.length; p++) {
                if (winner >= 0 && bidValues[p] <= bidValues[winner]) continue;
                boolean unique = true;
                for (int q = 0; q < bidValues.length; q++)
                    if (q != p && bidValues[q] == bidValues[p]) unique = false;
                if (unique) winner = p;
            }
            return winner;
        }
        // the highest bid, if only one player made it
        int winner = -1;
        int highest = Integer.MIN_VALUE;
        for (int p = 0; p < bidValues.length; p++) {
            if (bidValues[p] > highest) {
                highest = bidValues[p];
                winner = p;
            } else if (bidValues[p] == highest) {
                winner = -1;
            }
        }
        return winner;
    }

    /**
     * The next player, cycling round from the current one, who has not yet bid this round.
     */
    private int nextPlayerToBid(GoofspielGameState state) {
        int current = state.getCurrentPlayer();
        for (int i = 1; i <= state.getNPlayers(); i++) {
            int p = (current + i) % state.getNPlayers();
            if (state.getBid(p).getSize() == 0)
                return p;
        }
        throw new AssertionError("No player is still to bid");
    }
}
