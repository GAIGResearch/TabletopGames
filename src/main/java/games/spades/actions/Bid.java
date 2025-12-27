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
    
    public final int playerId;
    public final int bidAmount;
    public final boolean blind; // true for Blind Nil
    
    public Bid(int playerId, int bidAmount) {
        this(playerId, bidAmount, false);
    }

    public Bid(int playerId, int bidAmount, boolean blind) {
        this.playerId = playerId;
        this.bidAmount = bidAmount;
        this.blind = blind;
    }
    
    @Override
    public boolean execute(AbstractGameState gameState) {
        SpadesGameState state = (SpadesGameState) gameState;
        
        if (state.getCurrentPlayer() != playerId) {
            throw new AssertionError("Player " + playerId + " tried to bid out of turn");
        }
        
        if (state.getSpadesGamePhase() != SpadesGameState.Phase.BIDDING) {
            throw new AssertionError("Bid action called outside of bidding phase");
        }
        
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
        if (!(o instanceof Bid)) return false;
        Bid bid = (Bid) o;
        return playerId == bid.playerId && bidAmount == bid.bidAmount && blind == bid.blind;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(playerId, bidAmount, blind);
    }
    
    @Override
    public void printToConsole() {
        System.out.println(toString());
    }
    
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
    
    @Override
    public String toString() {
        return "Player " + playerId + " bids " + (bidAmount == 0 ? (blind ? "Blind Nil" : "Nil") : bidAmount);
    }
} 