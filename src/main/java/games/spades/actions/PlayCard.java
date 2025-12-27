package games.spades.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import core.interfaces.IPrintable;
import games.spades.SpadesGameState;
import utilities.Pair;

import java.util.AbstractMap;
import java.util.Objects;

/**
 * Action representing playing a card in Spades.
 */
public class PlayCard extends AbstractAction implements IPrintable {
    
    public final FrenchCard card;
    
    public PlayCard(FrenchCard card) {
        this.card = card;
    }
    
    @Override
    public boolean execute(AbstractGameState gameState) {
        SpadesGameState state = (SpadesGameState) gameState;
        int playerId = state.getCurrentPlayer();

        if (state.getSpadesGamePhase() != SpadesGameState.Phase.PLAYING) {
            throw new AssertionError("PlayCard action called outside of playing phase");
        }
        
        // Get player's hand
        Deck<FrenchCard> playerHand = state.getPlayerHands().get(playerId);
        
        // Remove card from player's hand
        if (!playerHand.getComponents().remove(card)) {
            throw new AssertionError("Card not found in player's hand: " + card.toString());
        }
        
        // Add card to current trick
        state.getCurrentTrick().add(new Pair<>(playerId, card));
        
        return true;
    }
    
    @Override
    public AbstractAction copy() {
        return this; // immutable
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayCard playCard)) return false;
        return Objects.equals(card, playCard.card);
    }
    
    @Override
    public int hashCode() {
        return card.hashCode() + 2798;
    }
    
    @Override
    public void printToConsole() {
        System.out.println(this);
    }
    
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
    
    @Override
    public String toString() {
        return "Plays " + card.toString();
    }
} 