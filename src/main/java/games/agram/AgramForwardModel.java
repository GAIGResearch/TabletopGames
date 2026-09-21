package games.agram;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.tricktaking.KnownVoids;
import games.tricktaking.PlayCard;
import games.tricktaking.PlayRule;
import games.tricktaking.Trick;

import java.util.ArrayList;
import java.util.List;

import static core.CoreConstants.VisibilityMode.*;

/**
 * <p>The forward model contains all the game rules and logic for Agram.</p>
 */
public class AgramForwardModel extends StandardForwardModel {

    /**
     * The 35-card Agram deck: Ace and 10 down to 3 in each suit, without the Ace of Spades.
     */
    public static Deck<FrenchCard> generateDeck() {
        Deck<FrenchCard> deck = FrenchCard.generateDeck("DrawDeck", HIDDEN_TO_ALL);
        deck.removeAll(deck.getComponents().stream().filter(c -> switch (c.type) {
            case Jack, Queen, King -> true;
            case Ace -> c.suite == FrenchCard.Suite.Spades;
            case Number -> c.number < 3;
        }).toList());
        return deck;
    }

    @Override
    protected void _setup(AbstractGameState firstState) {
        AgramGameState state = (AgramGameState) firstState;
        AgramParameters params = (AgramParameters) state.getGameParameters();

        state.drawDeck = generateDeck();
        if (params.nCardsPerPlayer * state.getNPlayers() > state.drawDeck.getSize())
            throw new IllegalArgumentException("Not enough cards to deal " + params.nCardsPerPlayer + " cards to "
                    + state.getNPlayers() + " players");
        state.discardPile = new Deck<>("DiscardPile", VISIBLE_TO_ALL);
        state.playerHands = new ArrayList<>();
        for (int p = 0; p < state.getNPlayers(); p++)
            state.playerHands.add(new Deck<>("Hand " + p, p, VISIBLE_TO_OWNER));
        state.knownVoids = new KnownVoids(state.getNPlayers());
        state.dealsWon = new int[state.getNPlayers()];

        // the dealer is the last player, so player 0 (the next player after the dealer) leads the first trick
        state.currentTrick = new Trick("CurrentTrick", state.getNPlayers(), 0);
        deal(state, 0);
        state.setFirstPlayer(0);
    }

    /**
     * Gather all the cards into the draw deck, shuffle, deal a fresh hand to each player and clear the known voids.
     * The given player leads the first trick.
     */
    private void deal(AgramGameState state, int leader) {
        AgramParameters params = (AgramParameters) state.getGameParameters();
        for (Deck<FrenchCard> hand : state.playerHands) {
            state.drawDeck.add(hand);
            hand.clear();
        }
        state.drawDeck.add(state.discardPile);
        state.discardPile.clear();
        state.drawDeck.add(state.currentTrick);
        state.currentTrick = new Trick("CurrentTrick", state.getNPlayers(), leader);
        state.drawDeck.shuffle(state.getRnd());
        for (int p = 0; p < state.getNPlayers(); p++) {
            for (int i = 0; i < params.nCardsPerPlayer; i++)
                state.playerHands.get(p).add(state.drawDeck.draw());
        }
        state.knownVoids.clear();
    }

    /**
     * One PlayCard per card in hand, restricted to the suit led if the player holds any of it.
     * There is no obligation to play a higher card.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        AgramGameState state = (AgramGameState) gameState;
        List<FrenchCard> hand = state.getPlayerHand(state.getCurrentPlayer()).getComponents();
        List<AbstractAction> actions = new ArrayList<>();
        for (FrenchCard card : PlayRule.FOLLOW_SUIT.legalPlays(hand, state.currentTrick))
            actions.add(new PlayCard(card));
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        AgramGameState state = (AgramGameState) currentState;
        if (!state.currentTrick.isComplete()) {
            endPlayerTurn(state);
            return;
        }
        int winner = state.currentTrick.winner(null);  // Agram has no trumps
        state.discardPile.add(state.currentTrick);
        state.currentTrick = new Trick("CurrentTrick", state.getNPlayers(), winner);
        if (state.playerHands.stream().anyMatch(h -> h.getSize() > 0)) {
            endPlayerTurn(state, winner);
            return;
        }
        // the winner of the last trick wins the deal
        state.dealsWon[winner]++;
        AgramParameters params = (AgramParameters) state.getGameParameters();
        if (state.getRoundCounter() + 1 >= params.nDeals) {
            endGame(state);
        } else {
            // the winner deals the next deal, so the player after them leads
            int leader = (winner + 1) % state.getNPlayers();
            endRound(state, leader);
            deal(state, leader);
        }
    }
}
