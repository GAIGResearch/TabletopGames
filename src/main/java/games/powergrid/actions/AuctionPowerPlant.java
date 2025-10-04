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

    // Sequence-local state (prevents recursion & drives bidding)
    private boolean started = false;
    private boolean done     = false;
    private int currentBidder;   // who should act NOW (managed by the sequence)

    public AuctionPowerPlant(int playerId, int plantNumber) {
        this.openerId = playerId;
        this.plantNumber = plantNumber;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PowerGridGameState s = (PowerGridGameState) gs;
        if (started) return true;                     // idempotent

        if (plantNumber > s.getPlayersMoney(openerId)) return false;

        s.setAuctionPlantNumber(plantNumber);
        s.resetBidOrder();
        s.setCurrentBid(plantNumber, openerId);

        // Choose the first bidder after the opener
        currentBidder = s.checkNextBid(openerId);

        // Optional: keep state.turnOwner in sync (nice for logs/UI)
        s.setTurnOwner(currentBidder);

        gs.setActionInProgress(this);
        started = true;
        System.out.printf("Player %d opens auction on plant %d%n", openerId, plantNumber);
        return true;
    }

    // IMPORTANT: do NOT call state.getCurrentPlayer() here
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
            for (int i = 0; i < deck.getSize(); i++) {
                actions.add(new Discard(i));   
            }
            return actions;
        }

        List<AbstractAction> actions = new ArrayList<>();
        int money = s.getPlayersMoney(currentBidder);

        if (money > s.getCurrentBid()) {
            actions.add(new IncreaseBid(currentBidder));
        }
        actions.add(new PassBid(currentBidder));   // always allow pass
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

        	// auction is over now
        	s.resetAuction();
        	PowerGridForwardModel.rebalanceMarkets(s);

        	// need a discard?
        	if (s.getPlayerPlantDeck(winnerId).getSize() > 3) {
        	    awaitingDiscard = true;
        	    currentBidder = winnerId;            // winner chooses
        	    s.setTurnOwner(currentBidder);
        	    return;                              // keep sequence alive
        	}

        	// otherwise finish
        	s.removeFromRound(winnerId);
        	if (!s.isRoundOrderAllPassed()) s.setTurnOwner(s.nextPlayerInRound());
        	done = true;
        	return;
        }

        // Otherwise continue the auction with the next bidder
        currentBidder = next;
        s.setTurnOwner(currentBidder); // optional, but keeps UI/logs consistent
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return done;
    }

    @Override
    public AuctionPowerPlant copy() {
        AuctionPowerPlant cp = new AuctionPowerPlant(openerId, plantNumber);
        cp.started = this.started;
        cp.done = this.done;
        cp.currentBidder = this.currentBidder;
        cp.awaitingDiscard = this.awaitingDiscard;   
        cp.winnerId = this.winnerId;                
        return cp;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "OpenAuction(plant=" + plantNumber + ", opener=" + openerId + ")";
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
    
	private void awardPlantToWinner(PowerGridGameState s, int winner, int plantNumber, int price) {
	    // Preconditions protect you if callers ever change
	    if (price < 0) throw new IllegalArgumentException("Negative price");
	    if (s.getPlayersMoney(winner) < price)
	        throw new IllegalStateException("Insufficient funds");

	    s.decreasePlayerMoney(winner, price);

	    PowerGridCard bought = s.removePlantFromMarkets(plantNumber);
	    if (bought == null)
	        throw new IllegalStateException("Plant " + plantNumber + " not in markets");
	    if(s.getPlayerPlantDeck(winner).getSize() > 3) {
	    	//add actions to remove cards 
	    }
	    s.addPlantToPlayer(winner, bought);
	    
	    // TODO: enforce 3-plant cap → trigger discard sub-sequence if >3
	    // e.g., gs.setActionInProgress(new DiscardDownToThreePlants(winner));

	    PowerGridForwardModel.rebalanceMarkets(s);

	    // Optional: structured log
	    // gs.logEvent(new LogEvent("AUCTION_WIN", winner, plantNumber, price));
	}
}
