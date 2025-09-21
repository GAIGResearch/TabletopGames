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
import games.powergrid.PowerGridParameters.Phase;
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
    private List<Integer> bidOrder = new ArrayList<>();
    private int turnOrderIndex = 0;
    private int[] playerMoney;
    private int step;

	
    private int[] cityCountByPlayer;
    private int[][] citySlotsById;          // [cityId][slot] -> playerId or -1
    private PowerGridParameters.Phase currentPhase;
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
        copy.roundOrder = new ArrayList<>(this.roundOrder);
        copy.bidOrder = new ArrayList<>(this.bidOrder);

        // auction sub-state
        copy.auctionPlantNumber = this.auctionPlantNumber;
        copy.currentBid         = this.currentBid;
        copy.currentBidder      = this.currentBidder;
        // current bids (Bid is immutable enough for shallow copy)

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
	
	public void resetRoundOrderNextPhase() {
		this.roundOrder = new ArrayList<>(turnOrder);
	}
	//If a player elects to skip they are removed from the phase 
	public int removeFromRound(int playerId) {
	    int index = roundOrder.indexOf(playerId);   
	    if (index != -1) {                          
	        roundOrder.set(index, -1);             
	    }
	    return index;
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
	
	// Phase helpers


	public PowerGridParameters.Phase getPhase() {
	    return currentPhase;
	}
	
	public void advancePhase() {
		    setPhase(getPhase().next());
	}        // optional: initialize per-phase data
	
	
	
	public void setPhase(PowerGridParameters.Phase phase) {
	    this.currentPhase = phase;
	}
	

	//Auction Helpers
	public boolean isAuctionLive() {
	    return auctionPlantNumber != -1;
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
	
	public Deck<PowerGridCard>[] getOwnedPlantsByPlayer() {
		return ownedPlantsByPlayer; 
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

	public void setAuctionPlantNumber(int plantNumber) {
		this.auctionPlantNumber = plantNumber; 
		
	}
	
	public int getAuctionPlantNumber() {
		return this.auctionPlantNumber; 
	}

	public List<Integer> getBidOrder() {
		return bidOrder;
	}

	public void resetBidOrder() {
		this.bidOrder = new ArrayList<Integer>(roundOrder);
	}
	
	public void passBid(int playerId) {
		int index = bidOrder.indexOf(playerId);   
	    if (index != -1) {                          
	        bidOrder.set(index, -1);             
	    }
	}
	
	public int getCurrentBid() {
		return currentBid; 
	}
	
	public void setCurrentBid(int newBid, int playerId) {
		this.currentBid = newBid;
		this.currentBidder = playerId; 
						
	}
	
	public void resetAuction() {
		this.auctionPlantNumber = -1;
		this.currentBid = 0;
		this.currentBidder  =-1;
	}
	
	public PowerGridResourceMarket getResourceMarket() {
		return this.resourceMarket;
	}
	
	//this might have to be moved 
	public Integer checkNextBid(int playerID) {
	    int startIndex = turnOrder.indexOf(playerID);
	    if (startIndex == -1) {
	    	System.out.println(bidOrder);
	    	System.out.println("Player" + playerID +  "Not in BidOrder");
	        return null; // player not in bidOrder
	    }

	    int size = bidOrder.size();

	    // Scan forward, wrapping around
	    for (int offset = 1; offset < size; offset++) {
	        int nextIndex = (startIndex + offset) % size;
	        int candidate = bidOrder.get(nextIndex);

	        if (candidate != -1) {
	            return candidate;  // first eligible player
	        }
	    }
	    System.out.println("Current player " +  playerID);
	    

	    // If no eligible players found
	    return playerID;
	}
	
	
	
	public boolean isRoundOrderAllPassed() {
	    return roundOrder.stream().allMatch(v -> v == -1);
	}
	
	public int nextPlayerInRound() {
	    for (int player : roundOrder) {
	        if (player != -1) {
	            return player;  // first valid player found
	        }
	    }
	    // If all are -1, return -1 (or throw, depending on your game logic)
	    return -1;
	}


	public void printOwnedPlants() {
	    for (int i = 0; i < ownedPlantsByPlayer.length; i++) {
	        Deck<PowerGridCard> deck = ownedPlantsByPlayer[i];
	        System.out.print("Player " + i + ": ");
	        if (deck == null) {
	            System.out.println("No plants");
	        } else {
	            for (PowerGridCard c : deck.getComponents()) {
	                System.out.print(c.getNumber() + " ");
	            }
	            System.out.println();
	        }
	    }
	}
	public void printMarkets() {
	    Deck<PowerGridCard> current = getCurrentMarket();
	    Deck<PowerGridCard> future  = getFutureMarket();

	    System.out.print("Current Market: ");
	    if (current.getSize() == 0) {
	        System.out.println("[empty]");
	    } else {
	        current.getComponents().forEach(card ->
	            System.out.print(card.getNumber() + " ")
	        );
	        System.out.println();
	    }

	    System.out.print("Future Market: ");
	    if (future.getSize() == 0) {
	        System.out.println("[empty]");
	    } else {
	        future.getComponents().forEach(card ->
	            System.out.print(card.getNumber() + " ")
	        );
	        System.out.println();
	    }
	}



	
	}







