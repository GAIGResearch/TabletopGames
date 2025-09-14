package games.powergrid;

import java.util.HashMap;
import java.util.List;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IGamePhase;
import games.GameType;
import games.powergrid.components.PowerGridCard;
import games.powergrid.components.PowerGridCity;
import games.powergrid.components.PowerGridGraphBoard;
import games.powergrid.components.PowerGridResourceMarket;
import java.util.*;
import java.util.stream.Collectors;
import static core.CoreConstants.VisibilityMode.*;

/**
 * Power Grid Game State (TAG-friendly)
 * Keep rules out of here; this is just data + cheap helpers.
 */




import java.util.ArrayList;
import java.util.List;

/**
 * Minimal Power Grid Game State for early testing.
 * Only includes fields used by your current ForwardModel:
 * - gameMap
 * - drawPile
 * - currentMarket
 * - futureMarket
 */
public class PowerGridGameState extends AbstractGameState {

    // Keep these public for now to match your ForwardModel's direct field access.
    public PowerGridGraphBoard gameMap;
    public PowerGridResourceMarket resourceMarket; 
    public EnumMap<PowerGridParameters.Resource, Integer>[] fuelByPlayer;
    public Deck<PowerGridCard> drawPile;
    public Deck<PowerGridCard> currentMarket;
    public Deck<PowerGridCard> futureMarket;
    private List<Integer> turnOrder = new ArrayList<>();
    private List<Integer> roundOrder = new ArrayList<>();
    private int turnOrderIndex = 0;
    private int[] playerMoney;
    private int step;
	private Map<Integer, Bid> currentBids = new HashMap<>();
    private int[] cityCountByPlayer;
    private int[][] citySlotsById;          // [cityId][slot] -> playerId or -1
    private PowerGridParameters.Phase currentPhase;
    private List<Integer> activeBidders = new ArrayList<>();
    private Deck<PowerGridCard>[] ownedPlantsByPlayer;
    
    

 // Track which plant is currently being auctioned (-1 means none)
    private int auctionPlantNumber = -1;

    // Track the current highest bid and who holds it
    private int currentBid = 0;
    private int currentBidder = -1;
  

    

    public PowerGridGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        this.cityCountByPlayer = new int[nPlayers];
        this.playerMoney = new int[nPlayers];
    }

    // ---------- Required TAG overrides ----------

    @Override
    protected GameType _getGameType() {
        // Ensure GameType.PowerGrid exists in your enum; if not, add it or return a placeholder.
        return GameType.PowerGrid;
    }

    @Override
    protected List<Component> _getAllComponents() {
        ArrayList<Component> all = new ArrayList<>();
        if (gameMap != null) all.add(gameMap);
        if (resourceMarket != null) all.add(resourceMarket);
        if (drawPile != null) all.add(drawPile);
        if (currentMarket != null) all.add(currentMarket);
        if (futureMarket != null) all.add(futureMarket);
        return all;
    }

    @Override
    protected PowerGridGameState _copy(int playerId) {
        PowerGridGameState copy = new PowerGridGameState(gameParameters, getNPlayers());

        // existing component copies...
        copy.gameMap        = (this.gameMap == null) ? null : this.gameMap.copy();
        copy.drawPile       = (this.drawPile == null) ? null : this.drawPile.copy();
        copy.currentMarket  = (this.currentMarket == null) ? null : this.currentMarket.copy();
        copy.futureMarket   = (this.futureMarket == null) ? null : this.futureMarket.copy();
        copy.resourceMarket = (this.resourceMarket == null) ? null : this.resourceMarket.copy();

        // === NEW: copy all scalar/array/list fields you read later ===
        // phase
        copy.currentPhase = this.currentPhase;

        // money & cities
        copy.playerMoney       = (this.playerMoney == null) ? null : this.playerMoney.clone();
        copy.cityCountByPlayer = (this.cityCountByPlayer == null) ? null : this.cityCountByPlayer.clone();

        // city slots deep copy
        if (this.citySlotsById != null) {
            copy.citySlotsById = new int[this.citySlotsById.length][];
            for (int i = 0; i < this.citySlotsById.length; i++) {
                copy.citySlotsById[i] = (this.citySlotsById[i] == null) ? null : this.citySlotsById[i].clone();
            }
        }

        // turn order
        copy.turnOrder = new ArrayList<>(this.turnOrder);
        copy.turnOrderIndex = this.turnOrderIndex;

        // auction sub-state
        copy.auctionPlantNumber = this.auctionPlantNumber;
        copy.currentBid         = this.currentBid;
        copy.currentBidder      = this.currentBidder;
        copy.activeBidders = new ArrayList<>(this.activeBidders);
        // current bids (Bid is immutable enough for shallow copy)
        copy.currentBids = new HashMap<>(this.currentBids);

        // fuel (you already do this)
        if (fuelByPlayer != null) {
            @SuppressWarnings("unchecked")
            EnumMap<PowerGridParameters.Resource, Integer>[] fbCopy =
                    new EnumMap[fuelByPlayer.length];
            for (int p = 0; p < fuelByPlayer.length; p++) {
                fbCopy[p] = new EnumMap<>(PowerGridParameters.Resource.class);
                fbCopy[p].putAll(fuelByPlayer[p]);
            }
            copy.fuelByPlayer = fbCopy;
        }
        
     // owned plants by player (deep copy each Deck)
        if (this.ownedPlantsByPlayer != null) {
            @SuppressWarnings("unchecked")
            Deck<PowerGridCard>[] opCopy = (Deck<PowerGridCard>[]) new Deck<?>[this.ownedPlantsByPlayer.length];
            for (int p = 0; p < this.ownedPlantsByPlayer.length; p++) {
                Deck<PowerGridCard> src = this.ownedPlantsByPlayer[p];
                opCopy[p] = (src == null) ? null : src.copy();  // TAG Deck.copy() does a safe component copy
            }
            copy.ownedPlantsByPlayer = opCopy;
        }

        return copy;
    }


    // ---------- (Optional) convenience getters ----------

    public PowerGridGraphBoard getGameMap() { return gameMap; }
    public Deck<PowerGridCard> getDrawPile() { return drawPile; }
    public Deck<PowerGridCard> getCurrentMarket() { return currentMarket; }
    public Deck<PowerGridCard> getFutureMarket() { return futureMarket; }

	@Override
	protected double _getHeuristicScore(int playerId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getGameScore(int playerId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected boolean _equals(Object o) {
		// TODO Auto-generated method stub
		return false;
		
	}
	
	@SuppressWarnings("unchecked")
	public void initFuelStorage() {
	    int nPlayers = getNPlayers();
	    fuelByPlayer = new EnumMap[nPlayers];
	    for (int p = 0; p < nPlayers; p++) {
	        fuelByPlayer[p] = new EnumMap<>(PowerGridParameters.Resource.class);
	        for (PowerGridParameters.Resource r : PowerGridParameters.Resource.values())
	            fuelByPlayer[p].put(r, 0);
	    }
	}
	@SuppressWarnings("unchecked")
	public void initOwnedPlants() {
	    int n = getNPlayers();
	    ownedPlantsByPlayer = (Deck<PowerGridCard>[]) new Deck<?>[n]; 
	    for (int p = 0; p < n; p++) {
	        ownedPlantsByPlayer[p] = new Deck<>("OwnedPlants_P" + p, 0, VISIBLE_TO_ALL);
	    }
	}


	// helpers
	public int getFuel(int playerId, PowerGridParameters.Resource r) {
	    return fuelByPlayer[playerId].get(r);
	}
	public void addFuel(int playerId, PowerGridParameters.Resource r, int amount) {
	    fuelByPlayer[playerId].merge(r, amount, Integer::sum);
	}
	public void removeFuel(int playerId, PowerGridParameters.Resource r, int amount) {
	    int have = getFuel(playerId, r);
	    if (amount > have) throw new IllegalArgumentException("Player lacks " + r);
	    fuelByPlayer[playerId].put(r, have - amount);
	}
	
	public List<Integer> getTurnOrder() {
		return Collections.unmodifiableList(turnOrder); 
		}
	public int getTurnOrderIndex() {
		return turnOrderIndex; 
		}
	void setTurnOrder(List<Integer> newOrder) {
		turnOrder = new ArrayList<>(newOrder);
		turnOrderIndex = 0; 
		}
	
	public List<Integer> getRoundOrder() {
		return roundOrder;
	}

	public void setRoundOrder(List<Integer> roundOrder) {
		this.roundOrder = new ArrayList<>(roundOrder);
	}
	
	void advanceTurn() {
		turnOrderIndex = (turnOrderIndex + 1) % turnOrder.size(); 
		}
	
	public int getCityCount(int playerId) {
	    return cityCountByPlayer[playerId];
	}
	
	public int [][] getCitygraph() {
		return citySlotsById;
	
	}
	
	public void claimCitySlot(int playerId, int cityId, int slotIndex) {
	    if (citySlotsById[cityId][slotIndex] != -1)
	        throw new IllegalStateException("Slot already occupied");
	    citySlotsById[cityId][slotIndex] = playerId;
	    // Count *cities*, not houses: increment only on first presence in that city
	    cityCountByPlayer[playerId]++;
	}
	
	public void initCityStorageForBoard() {
        if (gameMap == null) throw new IllegalStateException("Board not set");
        int maxCityId = gameMap.maxCityId();
        citySlotsById = new int[maxCityId + 1][3];
        for (int id = 0; id <= maxCityId; id++) Arrays.fill(citySlotsById[id], -1);
    }
	
	public int getHighestPlantNumber(int playerId) {
	    // TODO: return the highest-numbered plant owned by playerId.
	    // If you haven't modeled ownership yet, return 0 as a safe default.
	    return 0;
	}
	
	public String fuelSummary() {
	    StringBuilder sb = new StringBuilder();
	    for (int p = 0; p < fuelByPlayer.length; p++) {
	        sb.append("P").append(p).append(": ");
	        for (PowerGridParameters.Resource r : PowerGridParameters.Resource.values()) {
	            sb.append(r).append("=").append(fuelByPlayer[p].get(r)).append(" ");
	        }
	        sb.append("\n");
	    }
	    return sb.toString();
	}
	
	/*Money Helper Methods*/
	public void setStartingMoney(int starting_money) {
		for (int i = 0; i < nPlayers; i++) {
	        playerMoney[i] = starting_money;
	    }
	}
	public int getPlayersMoney(int playerId) {
		return playerMoney[playerId];
	}
	
	public int increasePlayerMoney(int playerId, int amount) {
		playerMoney[playerId] += amount;
		return playerMoney[playerId];
	}
	
	public int decreasePlayerMoney(int playerId, int amount) {
		playerMoney[playerId] -= amount;
		return playerMoney[playerId];
	}
	
	// Bid helpers

	public static class Bid {
	    public final int plantNumber;
	    public final int amount;
	    public Bid(int plantNumber, int amount) {
	        this.plantNumber = plantNumber;
	        this.amount = amount;
	    }
	}

	public void recordBid(int playerId, int plantNumber, int amount) {
	    currentBids.put(playerId, new Bid(plantNumber, amount));
	}

	public Map<Integer, Bid> getCurrentBids() {
	    return Collections.unmodifiableMap(currentBids);
	}

	public void clearBids() {
	    currentBids.clear();
	}
	
	public PowerGridParameters.Phase getPhase() {
	    return currentPhase;
	}

	public void setPhase(PowerGridParameters.Phase phase) {
	    this.currentPhase = phase;
	}
	
	
	//Auction Helpers
	public boolean isAuctionLive() {
	    return auctionPlantNumber != -1;
	}

	public int getAuctionPlantNumber() { return auctionPlantNumber; }
	public void setAuctionPlantNumber(int number) { auctionPlantNumber = number; }

	public int getCurrentBid() { return currentBid; }
	public void setCurrentBid(int amount, int bidder) {
	    currentBid = amount;
	    currentBidder = bidder;
	}
	public int getCurrentBidder() { return currentBidder; }

	public void clearAuction() {
	    auctionPlantNumber = -1;
	    currentBid = 0;
	    currentBidder = -1;
	}
	
	public void startAuction(List<Integer> cycle) {
	    activeBidders.clear();
	    activeBidders.addAll(cycle);
	}

	public List<Integer> getActiveBidders() { return activeBidders; }

	public void passBid(int playerId) {
	    activeBidders.remove(playerId);
	}

	public boolean isStillInAuction(int playerId) {
	    return activeBidders.contains(playerId);
	}
	public void passOnAuction(int pid) {
	    activeBidders.removeIf(p -> p == pid);

	}

	// Helper for cycling bidders
	public int nextActiveBidderAfter(int pid) {
	    if (activeBidders.isEmpty()) return -1;
	    int idx = activeBidders.indexOf(pid);
	    if (idx < 0) {
	        // If pid isn't in list (e.g., currentBidder is the high bidder, not acting),
	        // start from the beginning.
	        return activeBidders.get(0);
	    }
	    return activeBidders.get((idx + 1) % activeBidders.size());
	}
	
	// --- Ops ---
	public Deck<PowerGridCard> getPlayerPlantDeck(int playerId) {
	    return ownedPlantsByPlayer[playerId];
	}

	public void addPlantToPlayer(int playerId, PowerGridCard card) {
	    Deck<PowerGridCard> d = ownedPlantsByPlayer[playerId];
	    if (d.getSize() >= 3) throw new IllegalStateException("Must replace when already at 3 plants.");
	    d.add(card);
	    d.getComponents().sort(Comparator.comparingInt(PowerGridCard::getNumber));
	}

	public void replacePlant(int playerId, int indexToSell, PowerGridCard newCard) {
	    Deck<PowerGridCard> d = ownedPlantsByPlayer[playerId];
	    if (d.getSize() != 3) throw new IllegalStateException("Replace only valid when at 3 plants.");
	    d.getComponents().set(indexToSell, newCard);
	    d.getComponents().sort(Comparator.comparingInt(PowerGridCard::getNumber));
	}

	public int getStep() {
		return step;
	}

	public void setStep(int step) {
		this.step = step;
	}





}
