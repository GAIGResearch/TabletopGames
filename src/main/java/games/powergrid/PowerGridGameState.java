package games.powergrid;

import core.AbstractGameState;
import core.AbstractParameters;
import core.AbstractPlayer;
import core.CoreConstants;
import core.components.Component;
import core.components.Deck;
import core.interfaces.IGamePhase;
import games.GameType;
import games.powergrid.PowerGridParameters.Resource;
import games.powergrid.components.PowerGridCard;
import games.powergrid.components.PowerGridGraphBoard;
import games.powergrid.components.PowerGridResourceMarket;

import java.util.*;

import static core.CoreConstants.VisibilityMode.*;

//Module Created by Stephen Hornish with the Support of Johns Hopkins University for independent Study Project Fall 2025 

public class PowerGridGameState extends AbstractGameState {

  
    protected PowerGridGraphBoard gameMap;
    protected PowerGridResourceMarket resourceMarket; 
    protected HashMap<Resource,Integer> resourceDiscardPile; 
    protected EnumMap<PowerGridParameters.Resource, Integer>[] fuelByPlayer;
    protected Deck<PowerGridCard> drawPile;
    protected Deck<PowerGridCard> currentMarket;
    protected Deck<PowerGridCard> futureMarket;
    
    private List<Boolean> RewardGiven = new ArrayList<>();
    
    public List<Boolean> getRewardGiven() {
		return RewardGiven;
	}

    public void setRewardGiven(int playerId) {
        RewardGiven.set(playerId, true);
    }

    
	public void setRewardGiven(List<Boolean> rewardGiven) {
		RewardGiven = rewardGiven;
	}

	private List<Integer> turnOrder = new ArrayList<>();
    private List<Integer> roundOrder = new ArrayList<>();
    private List<Integer> bidOrder = new ArrayList<>();
    private Set<Integer> plantsRan = new HashSet<>();
    
  

	private int turnOrderIndex = 0;
	private int[] income; 
    private int[] playerMoney;
    private int discountCard; 
    private int[] poweredCities; 
    private int step;

	
    private int[] cityCountByPlayer;
    public int[] getPlayerMoney() {
		return playerMoney;
	}


	public void setPlayerMoney(int[] playerMoney) {
		this.playerMoney = playerMoney;
	}


	public void setPoweredCities(int[] poweredCities) {
		this.poweredCities = poweredCities;
	}


	public void setCityCountByPlayer(int[] cityCountByPlayer) {
		this.cityCountByPlayer = cityCountByPlayer;
	}

	private double[] oneHotRegion; 
    private int[][] citySlotsById;          
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
    }


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

        // Components 
        copy.gameMap        = (this.gameMap == null) ? null : this.gameMap.copy();
        copy.drawPile       = (this.drawPile == null) ? null : this.drawPile.copy();        
        copy.currentMarket  = (this.currentMarket == null) ? null : this.currentMarket.copy();
        copy.futureMarket   = (this.futureMarket == null) ? null : this.futureMarket.copy();
        copy.resourceMarket = (this.resourceMarket == null) ? null : this.resourceMarket.copy();

        // Phase
        copy.gamePhase = this.gamePhase;

        // Primitive arrays
        copy.playerMoney       = (this.playerMoney == null) ? null : this.playerMoney.clone();
        copy.income = (this.income == null) ? null: this.income.clone(); 
        copy.cityCountByPlayer = (this.cityCountByPlayer == null) ? null : this.cityCountByPlayer.clone();
        copy.oneHotRegion = (this.oneHotRegion  == null) ? null : this.oneHotRegion.clone();
        copy.poweredCities     = (this.poweredCities == null) ? null : this.poweredCities.clone();

        // 2D array
        if (this.citySlotsById != null) {
            copy.citySlotsById = new int[this.citySlotsById.length][];
            for (int i = 0; i < this.citySlotsById.length; i++) {
                copy.citySlotsById[i] = (this.citySlotsById[i] == null) ? null : this.citySlotsById[i].clone();
            }
        }

        // Lists/sets
        copy.RewardGiven = new ArrayList<>(this.RewardGiven);
        copy.turnOrder       = new ArrayList<>(this.turnOrder);
        copy.turnOrderIndex  = this.turnOrderIndex;
        copy.roundOrder      = new ArrayList<>(this.roundOrder);
        copy.bidOrder        = new ArrayList<>(this.bidOrder);
        copy.plantsRan       = (this.plantsRan == null) ? null : new HashSet<>(this.plantsRan);

        // Auction/bid state
        copy.auctionPlantNumber = this.auctionPlantNumber;
        copy.currentBid         = this.currentBid;
        copy.currentBidder      = this.currentBidder;

        // Step / discounts
        copy.step         = this.step;
        copy.discountCard = this.discountCard; 

        // Regions & city sets
        copy.activeRegions = (this.activeRegions == null) ? null : new HashSet<>(this.activeRegions);
        copy.validCities   = (this.validCities == null) ? null : new HashSet<>(this.validCities);
        copy.invalidCities = (this.invalidCities == null) ? null : new HashSet<>(this.invalidCities);

        // Resource discard (state-level) 
        copy.resourceDiscardPile = (this.resourceDiscardPile == null) ? null : new HashMap<>(this.resourceDiscardPile);

        // Fuel by player
        if (this.fuelByPlayer != null) {
            @SuppressWarnings("unchecked")
            EnumMap<PowerGridParameters.Resource, Integer>[] fbCopy =
                    new EnumMap[this.fuelByPlayer.length];
            for (int p = 0; p < this.fuelByPlayer.length; p++) {
                fbCopy[p] = new EnumMap<>(PowerGridParameters.Resource.class);
                fbCopy[p].putAll(this.fuelByPlayer[p]);
            }
            copy.fuelByPlayer = fbCopy;
        }

        // Owned plants by player
        if (this.ownedPlantsByPlayer != null) {
            @SuppressWarnings("unchecked")
            Deck<PowerGridCard>[] opCopy = (Deck<PowerGridCard>[]) new Deck<?>[this.ownedPlantsByPlayer.length];
            for (int p = 0; p < this.ownedPlantsByPlayer.length; p++) {
                Deck<PowerGridCard> src = this.ownedPlantsByPlayer[p];
                opCopy[p] = (src == null) ? null : src.copy(); // consider player-aware copy if needed
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
	    return new PowerGridHeuristic().evaluateState(this, playerId);
	}


	@Override
	protected boolean _equals(Object o) {
	    if (this == o) return true;
	    if (!(o instanceof PowerGridGameState other)) return false;

	    // Phase (enum compare is fine)
	    if (this.gamePhase != other.gamePhase) return false;

	    // Components / markets
	    if (!Objects.equals(gameMap, other.gameMap)) return false;
	    if (!Objects.equals(resourceMarket, other.resourceMarket)) return false;
	    if (!Objects.equals(resourceDiscardPile, other.resourceDiscardPile)) return false;
	    if (!Objects.equals(drawPile, other.drawPile)) return false;
	    if (!Objects.equals(currentMarket, other.currentMarket)) return false;
	    if (!Objects.equals(futureMarket, other.futureMarket)) return false;

	    // Orders, sets, and lists
	    if (!Objects.equals(turnOrder, other.turnOrder)) return false;
	    if (!Objects.equals(roundOrder, other.roundOrder)) return false;
	    if (!Objects.equals(bidOrder, other.bidOrder)) return false;
	    if (!Objects.equals(plantsRan, other.plantsRan)) return false;
	    if (!Objects.equals(RewardGiven, other.RewardGiven)) return false;
	    if (!Objects.equals(income, other.income)) return false;

	    // Scalars
	    if (turnOrderIndex != other.turnOrderIndex) return false;
	    if (discountCard != other.discountCard) return false;
	    if (step != other.step) return false;
	    if (auctionPlantNumber != other.auctionPlantNumber) return false;
	    if (currentBid != other.currentBid) return false;
	    if (currentBidder != other.currentBidder) return false;

	    // Arrays
	    if (!Arrays.equals(playerMoney, other.playerMoney)) return false;
	    if (!Arrays.equals(poweredCities, other.poweredCities)) return false;
	    if (!Arrays.equals(cityCountByPlayer, other.cityCountByPlayer)) return false;
	    if (!Arrays.equals(oneHotRegion, other.oneHotRegion)) return false;
	    if (!Arrays.deepEquals(citySlotsById, other.citySlotsById)) return false;
	    if (!Arrays.equals(fuelByPlayer, other.fuelByPlayer)) return false;
	    if (!Arrays.equals(ownedPlantsByPlayer, other.ownedPlantsByPlayer)) return false;

	    // Regions / city sets
	    if (!Objects.equals(activeRegions, other.activeRegions)) return false;
	    if (!Objects.equals(validCities, other.validCities)) return false;
	    if (!Objects.equals(invalidCities, other.invalidCities)) return false;

	    return true;
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
	
	  public int getIncome(int playerId) {
			return income[playerId];
		}
	  
	  public int [] getIncome(){
		  return income; 
	  }


		public void setIncome(int [] income) {
			this.income = income;
		}
		
		public void setPlayerIncome(int playerId, int money) {
		    income[playerId] = money;  
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
		PowerGridParameters params = (PowerGridParameters) this.getGameParameters();
		int trigger = params.step2Trigger[this.getNPlayers()-1];
		return Arrays.stream(cityCountByPlayer).anyMatch(v -> v >= trigger);		
	}

	
	public int [][] getCitySlotsById() {
		return citySlotsById;
	
	}
	
	
	public void claimCitySlot(int playerId, int cityId, int slotIndex) {
	    if (citySlotsById[cityId][slotIndex] != -1)
	        throw new IllegalStateException("Slot already occupied");
	    citySlotsById[cityId][slotIndex] = playerId;
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

	public void addPlantToPlayer(int playerId, PowerGridCard newCard) {
	    if (newCard == null) throw new IllegalArgumentException("newCard is null");
	    Deck<PowerGridCard> deck = ownedPlantsByPlayer[playerId];
	    deck.addToBottom(newCard);
	}
	
	public Deck<PowerGridCard> getOwnedPlantsByPlayer(int playerId) {
		return ownedPlantsByPlayer[playerId]; 
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
	

	public Integer checkNextBid(int playerID) {
	    int startIndex = turnOrder.indexOf(playerID);
	    if (startIndex == -1) {
	        return null;
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
	    // If all are -1, return -1 
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
    
    public int getPoweredCities(int playerId) {
        return this.poweredCities[playerId];
    }
    
    public int [] getPoweredCities() {
    	return this.poweredCities; 
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
        	        .thenComparing(
        	            Comparator.comparingInt((Integer p) -> highestPlantNumber(ownedPlantsByPlayer[p])).reversed()
        	        )
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
        return max;  
    }

	public int getCurrentBidder() {
		return this.currentBidder;
	}
    
	public PowerGridCard removePlantFromMarkets(int plantNumber) {
	    PowerGridCard c = removeFromDeck(getCurrentMarket(), plantNumber);
	    if (c != null) return c;
	    return removeFromDeck(getFutureMarket(), plantNumber);
	}

	private PowerGridCard removeFromDeck(Deck<PowerGridCard> deck, int number) {
	    for (PowerGridCard c : deck.getComponents()) {
	        if (c.getNumber() == number) {
	            deck.remove(c);
	            return c;
	        }
	    }
	    return null;
	}
	public boolean removePlantFromPlayer(int playerId, int plantNumber) {
	    Deck<PowerGridCard> deck = getOwnedPlantsByPlayer(playerId);
	    for (PowerGridCard c : deck.getComponents()) {
	        if (c.getNumber() == plantNumber) { deck.remove(c); return true; }
	    }
	    return false;
	}


	public int getDiscountCard() {
		return discountCard;
	}


	public void setDiscountCard(int discoutCard) {
		this.discountCard = discoutCard;
	}
	
	public boolean futureMarketContainsAuction(int cardNumber) {
	    // First, check current market
	    if (currentMarket != null) {
	        for (PowerGridCard card : currentMarket.getComponents()) {
	            if (card.getNumber() == cardNumber) {
	                return false;
	            }
	        }
	    }

	    // Then, check future market
	    if (futureMarket != null) {
	        for (PowerGridCard card : futureMarket.getComponents()) {
	            if (card.getNumber() == cardNumber) {
	                return true;
	            }
	        }
	    }

	    // If not found in either, return null
	    return false;
	}
    public int getPlayerCapacity(int playerId) {
    	int capacity = 0; 
    	for (PowerGridCard card : this.getPlayerPlantDeck(playerId)) {
    		capacity += card.getCapacity(); 
    	}
    	return capacity; 
    }
    
    public int [] getPlayerCapacity() {
    	int[] playersCapacity = new int[this.getNPlayers()];
    	for(int i = 0; i < this.getNPlayers(); i++) {
    		playersCapacity[i] = getPlayerCapacity(i);
    	}
    	return playersCapacity;
    }
    
    public int totalMoney() {
    	return Arrays.stream(playerMoney).sum();  	
    }
    
    public void setOneHotRegions(Set<Integer> activeRegions, int numRegions) {
        double[] out = new double[numRegions];
        for (int i = 1; i <= numRegions; i++) {
            out[i - 1] = activeRegions.contains(i) ? 1 : 0;
        }
        this.oneHotRegion = out;
    }
    
    public double[] getOneHotRegions() {
    	return this.oneHotRegion;
    }
    
    /**
     * Computes the game score for the given player, used for intermediate
     * reward shaping and final outcome scoring. This is what PyTAG uses so this does not 
     * necessarily reflect the "Score" of a game. 
     *
     * <p>After the Bureaucracy phase, the score is calculated based on a 
     * a weighted combination of:
     * <ul>
     *   <li>Normalized city count (progress toward game-end city target)</li>
     *   <li>Normalized income (based on powered cities)</li>
     * </ul>
     *
     * <p>At game end:
     * <ul>
     *   <li>Winners receive +4.0</li>
     *   <li>All non-winners receive −2.0</li>
     * </ul>
     *
     * @param playerId the player whose score is being calculated
     * @return a reward value reflecting current progress or final result
     */

    @Override
    public double getGameScore(int playerId) {
        final PowerGridGamePhase phase = (PowerGridGamePhase) getGamePhase();
        
        final double wCities = 0.8;
        final double wIncome = 1.2;
        
 
       //if the player wins at the end they get a reward else they get penalized 
        if (this.getGameStatus() == CoreConstants.GameResult.GAME_END) {
            CoreConstants.GameResult[] res = getPlayerResults();
            CoreConstants.GameResult r = (res != null && playerId < res.length) ? res[playerId] : null;
            if (r == CoreConstants.GameResult.WIN_GAME) {
                return 4.0;
            } else {
                return -2.0;  
            }
        }

        double base = 0.0;
        if (phase == PowerGridGamePhase.BUREAUCRACY) {      
            int cityTarget = 0;
            try {
                PowerGridParameters params = (PowerGridParameters) getGameParameters();
                if (params != null && params.citiesToTriggerEnd != null &&
                    params.citiesToTriggerEnd.length >= getNPlayers()) {
                    cityTarget = params.citiesToTriggerEnd[getNPlayers() - 1];
                }
            } catch (Exception ignored) {}
            if (cityTarget <= 0) cityTarget = Math.max(1, getMaxCitiesOwned());

            int cities = getCityCountByPlayer(playerId);

            double normCities = clamp01((double) cities / cityTarget);
            double normIncome = clamp01((double) poweredCities[playerId]/20);
        
            base = (wCities * normCities + wIncome * normIncome); 
        }
        
        
        return base;
    }


    private static double clamp01(double x) {
        return x < 0 ? 0 : (x > 1 ? 1 : x);
    }


    @Override
    public int hashCode() {
        int result = 1;

        // Components / markets
        result = 31 * result + Objects.hashCode(gameMap);
        result = 31 * result + Objects.hashCode(resourceMarket);
        result = 31 * result + Objects.hashCode(resourceDiscardPile);
        result = 31 * result + Objects.hashCode(drawPile);
        result = 31 * result + Objects.hashCode(currentMarket);
        result = 31 * result + Objects.hashCode(futureMarket);

        // Orders & sets
        result = 31 * result + Objects.hashCode(turnOrder);
        result = 31 * result + Objects.hashCode(roundOrder);
        result = 31 * result + Objects.hashCode(bidOrder);
        result = 31 * result + Objects.hashCode(plantsRan);

        // Scalars
        result = 31 * result + Integer.hashCode(turnOrderIndex);
        result = 31 * result + Integer.hashCode(discountCard);
        result = 31 * result + Integer.hashCode(step);
        result = 31 * result + Integer.hashCode(auctionPlantNumber);
        result = 31 * result + Integer.hashCode(currentBid);
        result = 31 * result + Integer.hashCode(currentBidder);

        // Arrays
        result = 31 * result + Arrays.hashCode(playerMoney);
        result = 31 * result + Arrays.hashCode(poweredCities);
        result = 31 * result + Arrays.hashCode(cityCountByPlayer);
        result = 31 * result + Arrays.deepHashCode(citySlotsById);

        // Arrays of maps / decks
        result = 31 * result + Arrays.hashCode(fuelByPlayer);
        result = 31 * result + Arrays.hashCode(ownedPlantsByPlayer);

        // Regions / city sets
        result = 31 * result + Objects.hashCode(activeRegions);
        result = 31 * result + Objects.hashCode(validCities);
        result = 31 * result + Objects.hashCode(invalidCities);

        return result;
    }


}





