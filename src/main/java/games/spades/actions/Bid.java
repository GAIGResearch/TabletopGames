package games.spades.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.spades.SpadesGameState;

import java.util.Objects;

/**
 * Action representing a player's bid in Spades.
 * A bid is the number of tricks the player expects to win.
 */
public class Bid extends AbstractAction {
    
    public final int bidAmount;
    public final boolean blind; // true for Blind Nil
    
    public Bid(int bidAmount) {
        this(bidAmount, false);
    }

    public Bid(int bidAmount, boolean blind) {
        this.bidAmount = bidAmount;
        this.blind = blind;
    }
    
    @Override
    public boolean execute(AbstractGameState gameState) {
        SpadesGameState state = (SpadesGameState) gameState;
        
        if (state.getGamePhase() != SpadesGameState.Phase.BIDDING) {
            throw new AssertionError("Bid action called outside of bidding phase");
        }
        int playerId = state.getCurrentPlayer();
        
        state.setPlayerBid(playerId, bidAmount);
        if (bidAmount == 0) {
            state.playerBlindNil[playerId] = blind;
        } else {
            state.playerBlindNil[playerId] = false;
        }
        
        return true;
    }
    
    @Override
    public AbstractAction copy() {
        return this;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bid bid)) return false;
        return bidAmount == bid.bidAmount && blind == bid.blind;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(bidAmount, blind);
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
        return "Bids " + (bidAmount == 0 ? (blind ? "Blind Nil" : "Nil") : bidAmount);
    }
} 