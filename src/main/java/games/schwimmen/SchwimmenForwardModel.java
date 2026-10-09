package games.schwimmen;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.schwimmen.actions.ChooseHand;
import games.schwimmen.actions.Close;
import games.schwimmen.actions.CloseDecision;
import games.schwimmen.actions.ExchangeAll;
import games.schwimmen.actions.ExchangeOne;
import games.schwimmen.actions.Pass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static core.CoreConstants.VisibilityMode.*;

public class SchwimmenForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        SchwimmenGameState state = (SchwimmenGameState) firstState;
        SchwimmenParameters params = (SchwimmenParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();

        state.playerHands = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++)
            state.playerHands.add(new PartialObservableDeck<>("Hand " + p, p, nPlayers, VISIBLE_TO_OWNER));
        state.extraHand = new Deck<>("ExtraHand", HIDDEN_TO_ALL);
        state.table = new Deck<>("Table", VISIBLE_TO_ALL);
        state.discardPile = new Deck<>("DiscardPile", VISIBLE_TO_ALL);
        // the 32-card piquet pack: Seven to Ace in each suit
        state.drawDeck = FrenchCard.generateDeck("DrawDeck", HIDDEN_TO_ALL);
        state.drawDeck.getComponents().removeIf(c -> c.number < 7);
        state.chips = new int[nPlayers];
        Arrays.fill(state.chips, params.startingChips);

        // the last player deals, so player 0 on the dealer's left will play first
        state.dealer = nPlayers - 1;
        deal(state);
        // the dealer's choice between their hand and the extra hand comes before normal play
        state.setFirstPlayer(state.dealer);
        endDealIfSpecialHand(state);
    }

    /**
     * Shuffles the draw deck and deals a hand to each player still in, one card at a time starting on the dealer's
     * left, then the extra hand.
     */
    void deal(SchwimmenGameState state) {
        SchwimmenParameters params = (SchwimmenParameters) state.getGameParameters();
        state.drawDeck.shuffle(state.getRnd());
        for (int i = 0; i < params.handSize; i++) {
            int p = state.dealer;
            do {
                p = nextInGame(state, p);
                state.playerHands.get(p).add(state.drawDeck.draw());
            } while (p != state.dealer);
        }
        for (int i = 0; i < params.handSize; i++)
            state.extraHand.add(state.drawDeck.draw());
        state.consecutivePasses = 0;
        state.closer = -1;
    }

    /**
     * The next player still in the game after the given one, clockwise.
     */
    int nextInGame(SchwimmenGameState state, int player) {
        int p = player;
        do {
            p = (p + 1) % state.getNPlayers();
        } while (!state.isInGame(p));
        return p;
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        SchwimmenGameState state = (SchwimmenGameState) gameState;
        List<AbstractAction> actions = new ArrayList<>();
        if (state.isDealerChoicePending()) {
            actions.add(new ChooseHand(false));
            actions.add(new ChooseHand(true));
            return actions;
        }
        for (FrenchCard handCard : state.getPlayerHand(state.getCurrentPlayer()).getComponents())
            for (FrenchCard tableCard : state.table.getComponents())
                actions.add(new ExchangeOne(handCard, tableCard));
        actions.add(new ExchangeAll());
        actions.add(new Pass());
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        SchwimmenGameState state = (SchwimmenGameState) currentState;
        if (actionTaken instanceof ChooseHand) {
            // the dealer's choice is not a turn, so the turn counter is left alone
            state.setTurnOwner(nextInGame(state, state.getCurrentPlayer()));
            endDealIfSpecialHand(state);
            return;
        }

        if (actionTaken instanceof Close close) {
            if (close.close) {
                state.closer = state.getCurrentPlayer();
                state.consecutivePasses = 0;
            }
            finishTurn(state);
            return;
        }

        // an exchange or a pass
        if (actionTaken instanceof Pass)
            state.consecutivePasses++;
        else {
            state.consecutivePasses = 0;
            if (endDealIfSpecialHand(state))
                return;
        }
        if (state.closer == -1)
            state.setActionInProgress(new CloseDecision(state.getCurrentPlayer()));
        else
            finishTurn(state);
    }

    /**
     * Ends the current player's turn, once any close decision is made.
     */
    void finishTurn(SchwimmenGameState state) {
        SchwimmenParameters params = (SchwimmenParameters) state.getGameParameters();
        int nInGame = state.getNPlayersInGame();
        int next = nextInGame(state, state.getCurrentPlayer());
        // after a full round of passes the table is replaced, or the deal ends if the draw deck cannot replace it
        if (state.consecutivePasses == nInGame) {
            if (state.drawDeck.getSize() < params.handSize) {
                endDeal(state);
                return;
            }
            replaceTable(state);
        }
        // after a close the deal ends when play would come back to the closer
        if (next == state.closer) {
            endDeal(state);
            return;
        }
        endPlayerTurn(state, next);
        // the safeguard
        if (state.getTurnCounter() >= nInGame * params.maxCircuitsPerDeal)
            endDeal(state);
    }

    /**
     * The table cards go to the discard pile, and new ones are dealt face up from the draw deck.
     */
    void replaceTable(SchwimmenGameState state) {
        SchwimmenParameters params = (SchwimmenParameters) state.getGameParameters();
        state.discardPile.add(state.table);
        state.table.clear();
        // the top cards of the draw deck, in order
        for (int i = 0; i < params.handSize; i++)
            state.table.addToBottom(state.drawDeck.draw());
        state.consecutivePasses = 0;
    }

    /**
     * Ends the deal at once if any player's hand is Schnauz or Feuer (the extra hand does not count). Returns true if
     * the deal ended.
     */
    boolean endDealIfSpecialHand(SchwimmenGameState state) {
        SchwimmenParameters params = (SchwimmenParameters) state.getGameParameters();
        for (int p = 0; p < state.getNPlayers(); p++) {
            List<FrenchCard> hand = state.getPlayerHand(p).getComponents();
            if (SchwimmenUtils.isSchnauz(hand, params) || SchwimmenUtils.isFeuer(hand)) {
                endDeal(state);
                return true;
            }
        }
        return false;
    }

    /**
     * The players who lose a chip for the deal just ended (the chips game), in ascending order.
     */
    List<Integer> dealLosers(SchwimmenGameState state) {
        SchwimmenParameters params = (SchwimmenParameters) state.getGameParameters();
        List<Integer> inGame = new ArrayList<>();
        for (int p = 0; p < state.getNPlayers(); p++)
            if (state.isInGame(p))
                inGame.add(p);

        // Feuer costs every other player a chip
        for (int p : inGame) {
            if (SchwimmenUtils.isFeuer(state.getPlayerHand(p).getComponents())) {
                List<Integer> losers = new ArrayList<>(inGame);
                losers.remove(Integer.valueOf(p));
                return losers;
            }
        }

        // otherwise the worst hand loses: the lowest value, then the lowest tiebreak; equal worst hands all lose
        double worstValue = Double.MAX_VALUE, worstTiebreak = Double.MAX_VALUE;
        for (int p : inGame) {
            List<FrenchCard> hand = state.getPlayerHand(p).getComponents();
            double value = SchwimmenUtils.handValue(hand, params);
            double tiebreak = SchwimmenUtils.tiebreak(hand, params);
            if (value < worstValue || value == worstValue && tiebreak < worstTiebreak) {
                worstValue = value;
                worstTiebreak = tiebreak;
            }
        }
        List<Integer> losers = new ArrayList<>();
        for (int p : inGame) {
            List<FrenchCard> hand = state.getPlayerHand(p).getComponents();
            if (SchwimmenUtils.handValue(hand, params) == worstValue && SchwimmenUtils.tiebreak(hand, params) == worstTiebreak)
                losers.add(p);
        }
        return losers;
    }

    /**
     * Ends the deal: the single-deal game ends with it, and in the chips game the next deal starts unless at most one
     * player is still in.
     */
    void endDeal(SchwimmenGameState state) {
        SchwimmenParameters params = (SchwimmenParameters) state.getGameParameters();
        if (!params.livesGame) {
            endGame(state);
            return;
        }

        List<Integer> droppedOut = new ArrayList<>();
        for (int p : dealLosers(state)) {
            if (state.chips[p] > 0)
                state.chips[p]--;
            else
                droppedOut.add(p);  // a swimming player who loses again
        }
        // players who drop out score -1 minus the number still in, so players who went out later rank higher
        int stillIn = state.getNPlayersInGame() - droppedOut.size();
        for (int p : droppedOut)
            state.chips[p] = -1 - stillIn;
        if (stillIn <= 1) {
            endGame(state);
            return;
        }

        int newDealer = nextInGame(state, state.dealer);
        endRound(state, newDealer);
        if (state.isNotTerminal())  // not the last of maxDeals
            startDeal(state, newDealer);
    }

    /**
     * Gathers all the cards into the draw deck and deals again, with the dealer's choice first.
     */
    void startDeal(SchwimmenGameState state, int newDealer) {
        for (Deck<FrenchCard> hand : state.playerHands) {
            state.drawDeck.add(hand);
            hand.clear();
        }
        for (Deck<FrenchCard> deck : List.of(state.extraHand, state.table, state.discardPile)) {
            state.drawDeck.add(deck);
            deck.clear();
        }
        state.dealer = newDealer;
        deal(state);
        state.setFirstPlayer(newDealer);
        endDealIfSpecialHand(state);
    }
}
