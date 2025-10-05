package games.powergrid.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import games.powergrid.PowerGridForwardModel;
import games.powergrid.PowerGridGameState;
import games.powergrid.components.PowerGridCard;



public class AuctionPowerPlant extends AbstractAction implements IExtendedSequence {

    private final int openerId;
    private final int plantNumber;
    private boolean awaitingDiscard = false;
    private int winnerId = -1;

    private boolean started = false;
    private boolean done     = false;
    private int currentBidder;  

    public AuctionPowerPlant(int playerId, int plantNumber) {
        this.openerId = playerId;
        this.plantNumber = plantNumber;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PowerGridGameState s = (PowerGridGameState) gs;           
        if (plantNumber > s.getPlayersMoney(openerId)) return false;
        s.setAuctionPlantNumber(plantNumber);
        s.resetBidOrder();
        if(s.getStep() == 2 && s.getDiscountCard() == plantNumber) { //if step 2 and first card then it get the discount 
        	s.setCurrentBid(1, openerId);
        	s.setDiscountCard(-1);
        }else {
        	s.setCurrentBid(plantNumber, openerId);
        }

        currentBidder = s.checkNextBid(openerId); //first available bidder
        
        s.setTurnOwner(currentBidder);

        gs.setActionInProgress(this);
        started = true;
        return true;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return currentBidder;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        PowerGridGameState s = (PowerGridGameState) gs;

        if (awaitingDiscard) {
            Deck<PowerGridCard> deck = s.getPlayerPlantDeck(winnerId);
            List<AbstractAction> actions = new ArrayList<>();
            for (int i = 0; i < deck.getSize() - 1; i++) {
                actions.add(new Discard(i));
            }
            return actions;
        }

        List<AbstractAction> actions = new ArrayList<>();
        final int money = s.getPlayersMoney(currentBidder);

        final int highestBidder = s.getCurrentBidder();         
        final boolean isHighest = (currentBidder == highestBidder);
        
        final boolean onlyOneLeft = s.isAuctionLive() && s.checkNextBid(highestBidder) == highestBidder;
        //edge case where the last person in round is the one auctioning
        if (onlyOneLeft) {
            actions.add(new PassBid(currentBidder));
            return actions;
        }

        if (!isHighest && money > s.getCurrentBid()) {
            actions.add(new IncreaseBid(currentBidder));
        }

        actions.add(new PassBid(currentBidder));
        return actions;
    }

    @Override
    public void _afterAction(AbstractGameState gs, AbstractAction action) {
        PowerGridGameState s = (PowerGridGameState) gs;
        if (awaitingDiscard && action instanceof Discard) {
            if (s.getPlayerPlantDeck(winnerId).getSize() <= 3) {
                s.removeFromRound(winnerId);
                if (!s.isRoundOrderAllPassed()) s.setTurnOwner(s.nextPlayerInRound());
                done = true;
            }
            return;
        }

        int lastActor;
        
        if (action instanceof IncreaseBid ib) {
            lastActor = ib.getPlayerId();
        } else if (action instanceof PassBid pb) {
            lastActor = pb.getPlayerId();
        } else {
            lastActor = currentBidder; // fallback
        }

        // Find the next eligible bidder
        int next = s.checkNextBid(lastActor);

        // If only the highest bidder remains, the auction ends
        if (next == s.getCurrentBidder()) {
        	winnerId = s.getCurrentBidder();
        	int price  = s.getCurrentBid();
        	int plant  = s.getAuctionPlantNumber();

        	// pay & take the plant
        	s.decreasePlayerMoney(winnerId, price);
        	PowerGridCard bought = s.removePlantFromMarkets(plant);
        	if (bought == null) throw new IllegalStateException("Plant not in markets");
        	s.addPlantToPlayer(winnerId, bought);

        	// auction is over now draw card and rebalance market
        	s.resetAuction();
        	PowerGridForwardModel.rebalanceMarkets(s);

        	//handles discard if player has more than 3 careds 
        	if (s.getPlayerPlantDeck(winnerId).getSize() > 3) {
        	    awaitingDiscard = true;
        	    currentBidder = winnerId;            
        	    s.setTurnOwner(currentBidder);
        	    return;                              
        	}

        	// otherwise finish
        	s.removeFromRound(winnerId);
        	if (!s.isRoundOrderAllPassed()) s.setTurnOwner(s.nextPlayerInRound());
        	done = true;
        	return;
        }

        // Otherwise continue the auction with the next bidder
        currentBidder = next;
        s.setTurnOwner(currentBidder); 
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return done;
    }

    @Override
    public AuctionPowerPlant copy() {
        AuctionPowerPlant cp = new AuctionPowerPlant(openerId, plantNumber);
        cp.started         = this.started;
        cp.done            = this.done;
        cp.currentBidder   = this.currentBidder;
        cp.awaitingDiscard = this.awaitingDiscard;
        cp.winnerId        = this.winnerId;
        return cp;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "OpenAuction(plant=" + plantNumber + ", open bidder=" + openerId + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AuctionPowerPlant)) return false;
        AuctionPowerPlant other = (AuctionPowerPlant) obj;
        return openerId == other.openerId && plantNumber == other.plantNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(openerId, plantNumber);
    }

}
