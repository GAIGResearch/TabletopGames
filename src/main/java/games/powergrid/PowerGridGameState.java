package games.powergrid;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.interfaces.IGamePhase;
import games.GameType;
import games.powergrid.PowerGridParameters.Resource;
import games.powergrid.components.PowerGridCard;
import games.powergrid.components.PowerGridGraphBoard;
import games.powergrid.components.PowerGridResourceMarket;
import games.powergrid.components.PowerGridCard.PlantInput;

import java.util.*;

import static core.CoreConstants.VisibilityMode.*;


public class PowerGridGameState extends AbstractGameState {

  
    protected PowerGridGraphBoard gameMap;
    protected PowerGridResourceMarket resourceMarket; 
    protected HashMap<Resource,Integer> resourceDiscardPile; 
    protected EnumMap<PowerGridParameters.Resource, Integer>[] fuelByPlayer;
    protected Deck<PowerGridCard> drawPile;
    protected Deck<PowerGridCard> currentMarket;
    protected Deck<PowerGridCard> futureMarket;
    
    
    private List<Integer> turnOrder = new ArrayList<>();
    private List<Integer> roundOrder = new ArrayList<>();
    private List<Integer> bidOrder = new ArrayList<>();
    private Set<Integer> plantsRan = new HashSet<>();
    
    
    private int turnOrderIndex = 0;
    private int[] playerMoney;
    private int[] poweredCities; 
    private int step;

	
    private int[] cityCountByPlayer;
    private int[][] citySlotsById;          // [cityId][slot] -> playerId or -1 if empty
    private Deck<PowerGridCard>[] ownedPlantsByPlayer;
    
    public enum PowerGridGamePhase implements IGamePhase{
    	PLAYER_ORDER,
    	AUCTION,
    	RESOURCE_BUY,
    	BUILD,
    	BUREAUCRACY;
    	public PowerGridGamePhase next() {
	        return switch (this) {
	            case PLAYER_ORDER -> AUCTION;
	            case AUCTION -> RESOURCE_BUY;
	            case RESOURCE_BUY -> BUILD;
	            case BUILD -> BUREAUCRACY;
	            case BUREAUCRACY -> PLAYER_ORDER; 
	        };
	    }
    }

    // Track which plant is currently being auctioned (-1 means none)
    private int auctionPlantNumber = -1;

    // Track the current highest bid 
    private int currentBid = 0;
    // Tracks the current highest bidder
    private int currentBidder = -1;
	private Set<Integer> activeRegions;
	private Set<Integer> invalidCities; 
	private Set<Integer> validCities; 
  

    

    public PowerGridGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        this.cityCountByPlayer = new int[nPlayers];
        this.playerMoney = new int[nPlayers];
        this.poweredCities = new int[nPlayers];
    }

    // ---------- Required TAG overrides ----------

    @Override
    protected GameType _getGameType() {
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

        copy.gamePhase = gamePhase; 

        copy.playerMoney       = (this.playerMoney == null) ? null : this.playerMoney.clone();
        copy.cityCountByPlayer = (this.cityCountByPlayer == null) ? null : this.cityCountByPlayer.clone();
        copy.poweredCities       = (this.poweredCities  == null) ? null : this.poweredCities .clone();

        if (this.citySlotsById != null) {
            copy.citySlotsById = new int[this.citySlotsById.length][];
            for (int i = 0; i < this.citySlotsById.length; i++) {
                copy.citySlotsById[i] = (this.citySlotsById[i] == null) ? null : this.citySlotsById[i].clone();
            }
        }

        copy.turnOrder = new ArrayList<>(this.turnOrder);
        copy.turnOrderIndex = this.turnOrderIndex;
        copy.roundOrder = new ArrayList<>(this.roundOrder);
        copy.bidOrder = new ArrayList<>(this.bidOrder);
        

        copy.auctionPlantNumber = this.auctionPlantNumber;
        copy.currentBid         = this.currentBid;
        copy.currentBidder      = this.currentBidder;
        
        copy.step = this.step;

        if (this.activeRegions != null) {
            copy.activeRegions = new HashSet<>(this.activeRegions);
        } else {
            copy.activeRegions = null;
        }
        
        if (this.validCities != null) {
            copy.validCities = new HashSet<>(this.validCities);
        } else {
            copy.validCities = null;
        }
        
        if (this.invalidCities != null) {
            copy.invalidCities = new HashSet<>(this.invalidCities);
        } else {
            copy.invalidCities = null;
        }
        
        if (this.plantsRan != null) {
            copy.plantsRan = new HashSet<>(this.plantsRan);
        } else {
            copy.plantsRan = null;
        }


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
        
        if (this.ownedPlantsByPlayer != null) {
            @SuppressWarnings("unchecked")
            Deck<PowerGridCard>[] opCopy = (Deck<PowerGridCard>[]) new Deck<?>[this.ownedPlantsByPlayer.length];
            for (int p = 0; p < this.ownedPlantsByPlayer.length; p++) {
                Deck<PowerGridCard> src = this.ownedPlantsByPlayer[p];
                opCopy[p] = (src == null) ? null : src.copy();  
            }
            copy.ownedPlantsByPlayer = opCopy;
        }

        return copy;
    }




    public PowerGridGraphBoard getGameMap() { return gameMap; }
    public PowerGridResourceMarket getResourceMarket() {return resourceMarket;}
    public Deck<PowerGridCard> getDrawPile() { return drawPile; }
    public Deck<PowerGridCard> getCurrentMarket() { return currentMarket; }
    public Deck<PowerGridCard> getFutureMarket() { return futureMarket; }
    
    public void setGameMap(PowerGridGraphBoard gameMap) {this.gameMap = gameMap;}
    public void setResourceMarket(PowerGridResourceMarket resourceMarket) {this.resourceMarket = resourceMarket;}
    public void setDrawPile(Deck<PowerGridCard> drawPile) {this.drawPile = drawPile;}
    public void setCurrentMarket(Deck<PowerGridCard> currentMarket) {this.currentMarket = currentMarket;}
    public void setFutureMarket(Deck<PowerGridCard> futureMarket) {this.futureMarket = futureMarket;}

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
	

	
	public int getCityCountByPlayer(int playerId) {
	    if (playerId < 0 || playerId >= cityCountByPlayer.length) {
	        throw new IllegalArgumentException("Invalid playerId: " + playerId);
	    }
	    return cityCountByPlayer[playerId];
	}
	
	public int[] getCityCountByPlayer() {	    
	    return cityCountByPlayer;
	}
	public int getMaxCitiesOwned() {
		return Arrays.stream(cityCountByPlayer).max().getAsInt();
	}
	
	public boolean stepTwoTrigger() {
		int trigger = (getNPlayers() > 5) ? 6 : 7; //TODO make this dynamic with the parameters 
		return Arrays.stream(cityCountByPlayer).anyMatch(v -> v > trigger);		
	}

	
	public int [][] getCitySlotsById() {
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
		Deck<PowerGridCard> deck = getPlayerPlantDeck(playerId);
		int highest = -1;
		for (PowerGridCard card : deck) {
		    int value = card.getNumber(); 
		    if (value > highest) {
		        highest = value;
		    }
		}
		return highest;
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
	

	public boolean isAuctionLive() {
	    return auctionPlantNumber != -1;
	}


	public Deck<PowerGridCard> getPlayerPlantDeck(int playerId) {
	    return ownedPlantsByPlayer[playerId];
	}

	public PowerGridCard addPlantToPlayer(int playerId, PowerGridCard newCard) {
	    if (newCard == null) throw new IllegalArgumentException("newCard is null");

	    Deck<PowerGridCard> deck = ownedPlantsByPlayer[playerId];

	    PowerGridCard removed = null;
	    if (deck.getSize() >= 3) {
	        // remove the lowest-numbered existing plant (keep the new one)
	        java.util.List<PowerGridCard> comps = deck.getComponents();
	        int idxLowest = 0;
	        for (int i = 1; i < comps.size(); i++) {
	            if (comps.get(i).getNumber() < comps.get(idxLowest).getNumber()) {
	                idxLowest = i;
	            }
	        }
	        removed = comps.remove(idxLowest);
	    }

	    deck.add(newCard);
	    deck.getComponents().sort(java.util.Comparator.comparingInt(PowerGridCard::getNumber));
	    return removed; // return what was replaced (or null if none)
	}
	
	public Deck<PowerGridCard> getOwnedPlantsByPlayer(int playerId) {
		return ownedPlantsByPlayer[playerId]; 
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
	

	//TODO this might have to be moved 
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

	public Set<Integer> getDisabledRegions() {
	    // assume regions are numbered 1 through 7
	    Set<Integer> all = new HashSet<>(Arrays.asList(1,2,3,4,5,6,7));

	    if (activeRegions == null) {
	        // if never set, treat all as disabled
	        return all;
	    }

	    all.removeAll(activeRegions);
	    return all;
	}
	
	public void setActiveRegions(Set<Integer> activeRegions){
		this.activeRegions = activeRegions; 
	}
	public Set<Integer>  getActiveRegions(){
		return this.activeRegions; 
	}

	public Set<Integer> getInvalidCities() {
		return invalidCities;
	}

	public void setInvalidCities(Set<Integer> invalidCities) {
		this.invalidCities = invalidCities;
	}

	public Set<Integer> getValidCities() {
		return validCities;
	}

	public void setValidCities(Set<Integer> validCities) {
		this.validCities = validCities;
	}


	public void incrementCityCount(int playerId) {
	    if (playerId < 0 || playerId >= cityCountByPlayer.length)
	        throw new IllegalArgumentException("Invalid playerId " + playerId);
	    cityCountByPlayer[playerId]++;
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
	public void generateResourceDiscardPile(int [] startValues) {
		HashMap<Resource, Integer> resourceMap = new HashMap<>();
        resourceMap.put(Resource.COAL, startValues[0]);
        resourceMap.put(Resource.GAS, startValues[1]);
        resourceMap.put(Resource.OIL, startValues[2]);
        resourceMap.put(Resource.URANIUM, startValues[3]);
        this.resourceDiscardPile = resourceMap;
	}
	
	public boolean playerOwnsPlant(int playerId, int plantId) {
	    Deck<PowerGridCard> playerHand = this.ownedPlantsByPlayer[playerId];
	    for (PowerGridCard card : playerHand) {
	        if (card.getNumber() == plantId) {
	            return true;
	        }
	    }
	    return false;
	}
	public void markPlantRun(int plantId) {
        plantsRan.add(plantId);
    }

    public boolean hasPlantRun(int plantId) {
        return plantsRan.contains(plantId);
    }

    public void resetPlantsRan() {
        plantsRan.clear();
    }
    
    public int numberOfPoweredCities(int playerId) {
        return this.poweredCities[playerId];
    }

    public void clearPoweredCities() {
        Arrays.fill(this.poweredCities, 0);
    }

    public void addPoweredCities(int playerId, int numberOfCities) {
    	int totalCities = poweredCities[playerId] + numberOfCities;
        int maxCities = cityCountByPlayer[playerId];
        poweredCities[playerId] = Math.min(totalCities, maxCities);
    }

    public List<Integer> getComputedTurnOrder() {
        int n = getNPlayers();
        List<Integer> order = new ArrayList<>(n);
        for (int p = 0; p < n; p++) order.add(p);

        order.sort(
            Comparator.<Integer>comparingInt(p -> cityCountByPlayer[p]).reversed()
                .thenComparingInt(p -> highestPlantNumber(ownedPlantsByPlayer[p])).reversed()
                .thenComparingInt(p -> p)
        );
        return java.util.Collections.unmodifiableList(order);
    }

    private static int highestPlantNumber(Deck<PowerGridCard> deck) {
        int max = -1;
        if (deck != null) {
            for (PowerGridCard c : deck.getComponents()) {
                if (c != null) max = Math.max(max, c.getNumber());
            }
        }
        return max;  // -1 if no plants owned
    }
    

    
}







