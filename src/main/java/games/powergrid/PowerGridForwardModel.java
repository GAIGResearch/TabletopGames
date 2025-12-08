package games.powergrid;

import java.util.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


import core.AbstractGameState;
import core.CoreConstants;
import core.CoreConstants.GameResult;
import core.StandardForwardModel;
import core.actions.AbstractAction;

import core.components.Deck;
import games.powergrid.PowerGridGameState.PowerGridGamePhase;
import core.interfaces.ITreeActionSpace;
import games.powergrid.PowerGridParameters.Resource;
import games.powergrid.actions.AuctionPowerPlant;
import games.powergrid.actions.BuildGenerator;
import games.powergrid.actions.BuyResource;
import games.powergrid.actions.PassAction;
import games.powergrid.actions.RunPowerPlant;
import games.powergrid.components.PowerGridCard;
import games.powergrid.components.PowerGridCard.Type;
import games.powergrid.components.PowerGridCity;

import java.util.HashSet;

import games.powergrid.components.PowerGridGraphBoard;
import games.powergrid.components.PowerGridResourceMarket;
import utilities.ActionTreeNode;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;

import static core.CoreConstants.GameResult.LOSE_GAME;
import static core.CoreConstants.GameResult.WIN_GAME;
import static core.CoreConstants.VisibilityMode.*;


public class PowerGridForwardModel extends StandardForwardModel implements ITreeActionSpace {
	
	boolean discountFirstCard = true; 
	@Override
	protected void _setup(AbstractGameState firstState) {
		PowerGridGameState state = (PowerGridGameState)firstState;
		PowerGridParameters params = (PowerGridParameters) state.getGameParameters();
		state.setActiveRegions(RegionPicker.randomContiguousSetDFS(state));		
		state.setGameMap( PowerGridGraphBoard.northAmerica().penalizeRegions(state.getActiveRegions()));//creates a board where the cost to go to invalid region is 100000
		state.setInvalidCities(state.gameMap.invalidCities(state.getActiveRegions()));//creates a set of invalid cities based on the current legal board 
		state.setValidCities(state.gameMap.validCities(state.getActiveRegions()));//creates a set of valid cities based on the current legal board 
		state.setOneHotRegions(state.getActiveRegions(), 7);
		state.setResourceMarket(new PowerGridResourceMarket());
		state.resourceMarket.setUpMarket(params.startinResources);//TODO Eventually change this when EU implemented and put in parameters the amount of initial setup
		state.initFuelStorage(); 
		state.setIncome(new int [] {0, 0, 0, 0, 0, 0});
		state.setPlayerMoney(new int [] {0,0,0,0,0,0} );
		state.setCityCountByPlayer(new int [] {0,0,0,0,0,0} );
		state.setPoweredCities(new int [] {0,0,0,0,0,0} );
		state.setRewardGiven(new ArrayList<>(Arrays.asList(false,false,false,false,false,false)));
		state.setStep(1);
		state.setDrawPile(setupDecks(params,state.getNPlayers(), state.getRnd()));
		state.setCurrentMarket(new Deck<>("currentMarket", VISIBLE_TO_ALL));
		state.setFutureMarket(new Deck<>("futureMarket",  VISIBLE_TO_ALL));
		initMarkets(state);
		state.initCityStorageForBoard();//creates a 2d array which keeps track of which cities are bought 
		state.setStartingMoney(params.startingMoney);
		buildTurnOrder(state);
		state.setGamePhase(PowerGridGamePhase.AUCTION); 
		int first_player = state.getTurnOrder().get(0);
		state.setTurnOwner(first_player);
		state.initOwnedPlants();	
		List<Integer> ord = new ArrayList<>(state.getTurnOrder());
        state.setRoundOrder(ord);
        state.resetAuction();
        
	}
	

	@Override
	protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
		PowerGridGameState s = (PowerGridGameState) gameState;
		PowerGridParameters params = (PowerGridParameters) s.getGameParameters();
	    int me = gameState.getTurnOwner();
	    List<AbstractAction> actions = new ArrayList<>();
	    PowerGridGameState.PowerGridGamePhase phase= (PowerGridGameState.PowerGridGamePhase) gameState.getGamePhase();

	    switch (phase) {


	    case AUCTION: {
	        // Player can start an auction only if they're still in the round and no auction is live
	        final boolean canStartAuction = s.getRoundOrder().contains(me) && !s.isAuctionLive();
	        final int money = s.getPlayersMoney(me);

	        if (canStartAuction) {
	            // Current market
	            for (PowerGridCard card : s.getCurrentMarket().getComponents()) {
	                int minBid = card.getNumber(); // plant cost is minimum opening bid
	                if (money >= minBid) actions.add(new AuctionPowerPlant(minBid));          
	            }

	            // Future market only in Step 3
	            if (s.getStep() == 3) {
	                for (PowerGridCard card : s.getFutureMarket().getComponents()) {
	                	if(card.type == Type.STEP3)continue;
	                    int minBid = card.getNumber();
	                    if (money >= minBid) actions.add(new AuctionPowerPlant(minBid));
	                    
	                }
	            }
	        } 
	        	actions.add(new PassAction());
	        }

	        break;
	    

        case RESOURCE_BUY:
        	EnumMap<PowerGridParameters.Resource, Integer> buyCapacity = playerBuyCapacity(s, me);
    		for (Map.Entry<Resource, Integer> e : buyCapacity.entrySet()) {
    		    Resource r = e.getKey();
    		    int amt = e.getValue(); 
    		    if(amt == 0 )continue;    
    		    for(int i = 1; i <= amt; i++) {
    		    	if(s.getResourceMarket().costToBuy(r, i) > s.getPlayersMoney(me)) {
    		    		continue;
    		    	}
    		    	actions.add(new BuyResource(r,i));
    		    }
    		    
    		}
    		actions.add(new PassAction());  		        	
        	break;

        case BUILD: {
            final Set<Integer> citiesInPlay = s.gameMap.validCities(s.getActiveRegions());
            final Set<Integer> playerValid = new HashSet<>();
            final Set<Integer> playerOwned = new HashSet<>();
            final int[][] citySlotsById = s.getCitySlotsById();
            final int step = s.getStep();
            for (int cityId : citiesInPlay) {
                boolean alreadyHere = Arrays.stream(citySlotsById[cityId])
                                            .anyMatch(slot -> slot == me);
                if (alreadyHere) {
                    playerOwned.add(cityId);                 
                    continue;
                }
                // city has an open slot allowed by the current step?
                if (step >= 1 && citySlotsById[cityId][0] == -1) playerValid.add(cityId);
                else if (step >= 2 && citySlotsById[cityId][1] == -1) playerValid.add(cityId);
                else if (step >= 3 && citySlotsById[cityId][2] == -1) playerValid.add(cityId);
            }

            Map<Integer, Integer> cityCost = citiesToBuildIn(playerValid, playerOwned, s, params.citySlotPrices);
            for (Map.Entry<Integer, Integer> e : cityCost.entrySet()) {
                int cityId = e.getKey();
                int totalCost = e.getValue();
                if (totalCost <= s.getPlayersMoney(me)) {
                    actions.add(new BuildGenerator(cityId, totalCost));
                }
            }
            actions.add(new PassAction());
            break;
        }

        case BUREAUCRACY:{
            Deck<PowerGridCard> playerHand = s.getPlayerPlantDeck(me);
            for (PowerGridCard card : playerHand) { //generates all possible resource spending combos 
                if(s.hasPlantRun(card.getNumber()))continue; //cards already been ran cant run a second time 
            	List<EnumMap<Resource, Integer>> reqs = card.generatePossibleCombos();
            	for (EnumMap<Resource, Integer> combo : reqs) {
            	    boolean feasible = true;
            	    for (Map.Entry<Resource, Integer> entry : combo.entrySet()) {
            	        Resource resource = entry.getKey();
            	        int amount = entry.getValue();
            	        if (s.getFuel(me, resource) < amount) {
            	            feasible = false;
            	            break;
            	        }
            	    }
            	    if (feasible) {
            	        actions.add(new RunPowerPlant(card.getNumber(),combo));
            	    }
            	}
            }
            actions.add(new PassAction());
            break;
        }
		default:
			break;
    }
	
    return actions;
}
	
	
	@Override
	protected void _afterAction(AbstractGameState gs, AbstractAction actionTaken) {
	    PowerGridGameState s = (PowerGridGameState) gs;
	    int me = gs.getCurrentPlayer();
	    if (s.isRoundOrderAllPassed()) {
	        advancePhase(s);
	        return;
	    }
	    if (actionTaken instanceof PassAction) {
	        s.removeFromRound(me);
	        if (s.isRoundOrderAllPassed()) {
	            advancePhase(s);
	        } else {
	            endPlayerTurn(s, s.nextPlayerInRound());
	        }
	    }
	}

	

	/**
	 * Handles all preprocessing logic when the game enters a new phase.
	 * This includes updating turn order, setting discount cards,
	 * reversing player order for certain phases, and applying step-change rules.
	 * <p>
	 * Called by advancePhase whenever the GamePhase changes.
	 *
	 * @param state the current game state
	 * @param phase the newly entered phase
	 */
	private void onEnterPhase(PowerGridGameState state, PowerGridGamePhase phase) {
	    switch (phase) {
	        case PLAYER_ORDER -> {
	            buildTurnOrder(state);
	            advancePhase(state);
	            state.clearPoweredCities();
	    		state.setRewardGiven(new ArrayList<>(Arrays.asList(false,false,false,false,false,false)));
	            endRound(state, state.getTurnOwner());
	        }

	        case AUCTION -> {
	            buildTurnOrder(state);
	            // always sets the discount card to the first but the auction action checks if it's phase 2
	            PowerGridCard firstCard = state.currentMarket.peek(0);
	            if (firstCard != null) {
	                state.setDiscountCard(firstCard.getNumber());
	            }
	        }

	        case RESOURCE_BUY -> {
	            List<Integer> order = state.getRoundOrder();
	            Collections.reverse(state.getRoundOrder());
	            state.setTurnOwner(order.get(0));
	        }

	        case BUILD -> {
	            List<Integer> order = state.getRoundOrder();
	            Collections.reverse(state.getRoundOrder());
	            state.setTurnOwner(order.get(0));
	        }

	        case BUREAUCRACY -> {
	            state.resetPlantsRan();
	            if (state.getStep() == 1 && state.stepTwoTrigger()) { // no plants have been run yet 
	                state.setStep(2);
	                state.getCurrentMarket().draw(); // remove lowest card 
	                rebalanceMarkets(state);
	            }
	        }

	        default -> {}
	    }
	}





	/**
	 * Handles all post processing logic when the game exits a phase.
	 * This includes awarding income, handling the post turn market, and checking the end game condition.
	 * <p>
	 * Called by advancePhase whenever the GamePhase changes.
	 *
	 * @param state the current game state
	 * @param phase the newly entered phase
	 */
	private void onExitPhase(PowerGridGameState state, PowerGridGamePhase phase) {
	    switch (phase) {
	    	case AUCTION ->{}
	    	case RESOURCE_BUY->{}
	        case BUILD -> {}
	        
	        case BUREAUCRACY -> { 
	        					PowerGridParameters params = (PowerGridParameters) state.getGameParameters();
	        					if(state.getMaxCitiesOwned() >= params.citiesToTriggerEnd[state.getNPlayers()-1]) {
	        						endGame(state);
	        					    return; 
	        					}
	        					awardIncome(state);
	        					state.getResourceMarket().refill(params, state.getStep(), state.getNPlayers(), false);
	        					if(state.getStep()!=3) {
	        						PowerGridCard largestCard = state.getFutureMarket().pickLast();
	        						Deck<PowerGridCard> drawPile = state.getDrawPile(); 
	        						drawPile.addToBottom(largestCard);
	        						
	        					}else {
	        						state.getCurrentMarket().pick(0);	        						
	        					}
	        					rebalanceMarkets(state);       	
	        }
	        default -> {}
	    }
	}


	
	

	/**
	 * Advances the game to the next phase, performing all required transition logic.
	 * <p>
	 * This includes:
	 * <ul>
	 *   <li>Resetting round turn order</li>
	 *   <li>Ending the current player's turn</li>
	 *   <li>Running phase-exit cleanup</li>
	 *   <li>Updating the state to the next phase</li>
	 *   <li>Running phase-entry initialization</li>
	 * </ul>
	 *
	 * @param gameState the current game state
	 */
	public void advancePhase(AbstractGameState gameState) {

		PowerGridGameState s = (PowerGridGameState) gameState;
	    s.resetRoundOrderNextPhase(); //rest the round turn order based on the global turn order
	    endPlayerTurn(s, gameState.getCurrentPlayer()); //
	    PowerGridGameState.PowerGridGamePhase current = (PowerGridGameState.PowerGridGamePhase) gameState.getGamePhase(); //get the current phase from the state 
	    onExitPhase(s, current); //clean up actions for leaving a phase 
	    if (s.getGameStatus() == GameResult.GAME_END) return;
	    PowerGridGameState.PowerGridGamePhase next = current.next(); // get the next phase by calling next on the phase class in parameters 
	    gameState.setGamePhase(next); //set the phase in state to next 
	    onEnterPhase(s, next); //perfroms ations that need to be taken prior to the next phase  
	}
	
	
	/**
	 * Determines and records the winner at game end using Power Grid’s standard scoring:
	 * <ol>
	 *   <li>Highest number of powered cities this round wins;</li>
	 *   <li>Ties are broken by most money on hand.</li>
	 * </ol>
	 *
	 * <p>Exactly one winner is selected. In the rare case of a perfect tie on both
	 * powered cities and money, the implementation resolves the tie by iteration order
	 * (i.e., the later player in the loop that matches the best metrics becomes winner).
	 *
	 * <p>All players are first marked {@code LOSE_GAME}; the chosen winner is then marked
	 * {@code WIN_GAME}.
	 *
	 * @param gs the current {@link core.AbstractGameState}; must be a {@link games.powergrid.PowerGridGameState}
	 */
	
    @Override
	protected void endGame(AbstractGameState gs) {
        PowerGridGameState state = (PowerGridGameState) gs;
        int winner = 0;
        int highestCities = -1;
        int highestMoney = -1;

        for (int i = 0; i < state.getNPlayers(); i++) {
            int poweredCities = state.getPoweredCities(i);
            int money = state.getPlayersMoney(i);

            if (poweredCities > highestCities ||(poweredCities == highestCities && money > highestMoney)) {               
                winner = i;
                highestCities = poweredCities;
                highestMoney = money;
            }
        }
        for (int i = 0; i < state.getNPlayers(); i++) {
            gs.setPlayerResult(LOSE_GAME, i);
        }
        gs.setPlayerResult(WIN_GAME, winner);
		state.setGameStatus(CoreConstants.GameResult.GAME_END);
		
    }

    /**
     * Awards money to each player based on how many cities they powered this round.
     * Uses the INCOME_TRACK table to determine payout and records the income.
     *
     * @param s the current game state
     */
	private static void awardIncome(PowerGridGameState s) {
	    for (int p = 0; p < s.getNPlayers(); p++) {    	
	        int powered = Math.min(s.getPoweredCities(p), PowerGridParameters.INCOME_TRACK.length - 1);
	        s.increasePlayerMoney(p, PowerGridParameters.INCOME_TRACK[powered]);
	        s.setPlayerIncome(p,PowerGridParameters.INCOME_TRACK[powered]);
	    }
	}
	//TODO STILL BUG HERE SOMETIMES DOESNT move cards to top row still works logic wise tho 
	/**
	 * Computes how much of each resource type a player can legally purchase
	 * based on the storage capacity of their owned power plants, ignoring
	 * market availability.
	 *
	 * <p>Storage capacity is calculated as twice the input requirement of
	 * each plant. Hybrid Gas/Oil plants contribute to a shared pool. Current
	 * player fuel holdings are subtracted, and Gas/Oil overflow can use the
	 * hybrid pool.</p>
	 *
	 * @param s        current game state
	 * @param playerId ID of the player to evaluate
	 * @return an {@link EnumMap} mapping each {@link PowerGridParameters.Resource}
	 *         (COAL, GAS, OIL, URANIUM) to the maximum additional units that
	 *         player can buy; values are non-negative
	 */
	private EnumMap<PowerGridParameters.Resource, Integer>playerBuyCapacity(PowerGridGameState s, int playerId) {
	    var R = PowerGridParameters.Resource.class;
	    EnumMap<PowerGridParameters.Resource, Integer> canBuy = new EnumMap<>(R);
	    // init zeros
	    for (PowerGridParameters.Resource r : PowerGridParameters.Resource.values())
	        canBuy.put(r, 0);

	    Deck<PowerGridCard> hand = s.getOwnedPlantsByPlayer(playerId);
	    if (hand == null) return canBuy;

	    // Sum capacities: dedicated + hybrid pool
	    int coalCap = 0, gasCap = 0, oilCap = 0, urCap = 0, hybridCap = 0;

	    for (PowerGridCard card : hand.getComponents()) {
	        PowerGridCard.PlantInput in = card.getInput();
	        int g = in.get(PowerGridParameters.Resource.GAS);
	        int o = in.get(PowerGridParameters.Resource.OIL);

	        // hybrid if it has BOTH GAS and OIL (and nothing else)
	        var req = in.asMap();
	        boolean hybrid = (g > 0 && o > 0 && req.size() == 2);
	        // If you added card.isHybridGasOil(), use: boolean hybrid = card.isHybridGasOil();
	        if (hybrid) {
	            int units = Math.max(g, o);      
	            hybridCap += 2 * units;          // shared pool for GAS+OIL
	        } else {
	            coalCap += 2 * in.get(PowerGridParameters.Resource.COAL);
	            urCap   += 2 * in.get(PowerGridParameters.Resource.URANIUM);
	            if (g > 0 && o == 0) gasCap += 2 * g;  // dedicated GAS-only
	            if (o > 0 && g == 0) oilCap += 2 * o;  // dedicated OIL-only
	        }
	    }

	    //  Subtract what the player already stores
	    int cHave = s.getFuel(playerId, PowerGridParameters.Resource.COAL);
	    int gHave = s.getFuel(playerId, PowerGridParameters.Resource.GAS);
	    int oHave = s.getFuel(playerId, PowerGridParameters.Resource.OIL);
	    int uHave = s.getFuel(playerId, PowerGridParameters.Resource.URANIUM);

	    int coalFree = Math.max(0, coalCap - cHave);
	    int urFree   = Math.max(0, urCap   - uHave);

	    //  Dedicated first, overflow into hybrid pool (shared for GAS/OIL)
	    int gasDedicatedUsed = Math.min(gHave, gasCap);
	    int oilDedicatedUsed = Math.min(oHave, oilCap);
	    int gasOverflow = Math.max(0, gHave - gasCap);
	    int oilOverflow = Math.max(0, oHave - oilCap);
	    int hybridUsed  = Math.min(hybridCap, gasOverflow + oilOverflow);
	    int hybridFree  = Math.max(0, hybridCap - hybridUsed);

	    int gasFree = Math.max(0, gasCap - gasDedicatedUsed) + hybridFree;
	    int oilFree = Math.max(0, oilCap - oilDedicatedUsed) + hybridFree;
	    
	    //limits the max purchase to match the action tree for RL
	    int coalret = Math.min(coalFree, 9);
	    int urret   = Math.min(urFree, 6);
	    int gasret = Math.min(gasFree, 9);
	    int oilret = Math.min(oilFree, 9);
	    canBuy.put(PowerGridParameters.Resource.COAL,    coalret);
	    canBuy.put(PowerGridParameters.Resource.GAS,     gasret);
	    canBuy.put(PowerGridParameters.Resource.OIL,     oilret);
	    canBuy.put(PowerGridParameters.Resource.URANIUM, urret);
	    return canBuy;
	}
	
	
	/**
	 * Computes the total cost to build in each candidate city for the current player,
	 * combining the **network connection cost** from any already–owned city with the
	 * **cheapest available house-slot price** permitted by the current game step.
	 * <p>
	 * For each {@code cityId} in {@code playerValidCities}, this method:
	 * <ol>
	 *   <li>Calls {@link PowerGridGraphBoard#shortestPathCosts(Set, Set)} to get the
	 *       minimum connection cost from any {@code playerOwnedCities} source
	 *       (or {@code 0} if the player owns no cities — the “first city is free” rule).</li>
	 *   <li>Looks at {@code s.getCitySlotsById()[cityId]} and, scanning slots from index {@code 0}
	 *       up to (but not including) {@code step = s.getStep()}, picks the first empty slot
	 *       (value {@code -1}) and adds its price from {@code citySlotPrices[slotIndex]}.</li>
	 * </ol>
	 *
	 *
	 * @param playerValidCities cities the player is allowed to build in (e.g., inside active regions and not already built)
	 * @param playerOwnedCities cities already owned by the player; used as sources for connection cost
	 * @param s                 game state providing the map, slots, and current step
	 * @param citySlotPrices    price by slot index (0-based); index 0 is Step 1 price, 1 is Step 2, 2 is Step 3
	 * @return a map {@code cityId -> totalCost} (connection cost + slot price). The returned map is modifiable
	 */
	public Map<Integer, Integer> citiesToBuildIn(Set<Integer> playerValidCities,Set<Integer> playerOwnedCities,PowerGridGameState s, int[] citySlotPrices) {
				PowerGridGraphBoard gameMap = s.getGameMap();
				int[][] citySlotsById = s.getCitySlotsById(); 
				int step = s.getStep();		
				Map<Integer,Integer> costMap = new HashMap<>(gameMap.shortestPathCosts(playerValidCities, playerOwnedCities));
				for (Map.Entry<Integer, Integer> entry : costMap.entrySet()) {
				    int cityId = entry.getKey();
				    int[] citySlot = citySlotsById[cityId];
				    for(int i = 0;i < step; i++){
				    	if(citySlot[i] == -1){
				    		int total = entry.getValue() + citySlotPrices[i];
				            entry.setValue(total);			    		
				    		break;
				    	}
				    }
				}
				return costMap;
				
			}

	


	
	/**
	 * Sets up the initial Power Grid deck according to the game parameters and number of players.
	 * <p>
	 * This includes:
	 * <ul>
	 *   <li>Removing the Step 3 card and placing it at the bottom of the draw pile</li>
	 *   <li>Separating out plants with number <= 15 into a temporary pile</li>
	 *   <li>Shuffling both piles with the provided RNG</li>
	 *   <li>Removing cards based on the number of players (2–4)</li>
	 *   <li>Recombining the piles so that tempPile is on top of drawPile</li>
	 * </ul>
	 *
	 * @param params   Power Grid game parameters (defines which plants are included)
	 * @param nPlayers Number of players in the game (2–4) if (5-6) players no cards removed
	 * @param rnd      Random number generator from the game state, for reproducible shuffles
	 * @return         The fully prepared draw pile, ready for the start of the game
	 */
    public static Deck<PowerGridCard> setupDecks(PowerGridParameters params,int nPlayers, Random rnd) {
        Deck<PowerGridCard> drawPile = new Deck<>("Draw", HIDDEN_TO_ALL);
        Deck<PowerGridCard> tempPile = new Deck<>("Temp", HIDDEN_TO_ALL);

        // Add all cards
        for (PowerGridCard card : params.plantsIncludedInGame) {
            drawPile.add(card);
        }
        drawPile.add(PowerGridCard.step3());

        // Separate step 3 + low-number plants
        PowerGridCard step3Card = null;
        List<PowerGridCard> snap = new ArrayList<>(drawPile.getComponents());
        for (PowerGridCard c : snap) {
            if (c.type == PowerGridCard.Type.STEP3) {
                step3Card = c;
                drawPile.remove(c);
            } else if (c.type == PowerGridCard.Type.PLANT && c.getNumber() <= 15) {
                drawPile.remove(c);
                tempPile.addToBottom(c);
            }
        }

        // Shuffle piles
        tempPile.shuffle(rnd);
        drawPile.shuffle(rnd);

        // Remove cards based on player count
        if (nPlayers == 2) {
            tempPile.draw();
            for (int k = 0; k < 5; k++) drawPile.draw();
        } else if (nPlayers == 3) {
            tempPile.draw();
            tempPile.draw();
            for (int k = 0; k < 6; k++) drawPile.draw();
        } else if (nPlayers == 4) {
            tempPile.draw();
            for (int k = 0; k < 3; k++) drawPile.draw();
        }

        List<PowerGridCard> buf = new ArrayList<>();
        while (tempPile.getSize() > 0) {
            buf.add(tempPile.draw());
        }
        for (int i = buf.size() - 1; i >= 0; i--) {
            drawPile.add(buf.get(i));
        }

        // Add step 3 card to bottom
        if (step3Card != null) {
            drawPile.addToBottom(step3Card);
        }

        return drawPile;
    }
    
    
    /**
     * Initializes the power plant markets at game start by drawing the first 8 cards,
     * sorting them, and placing the lowest 4 into the current market and the next 4 into the future market.
     *
     * @param state the current game state
     */
    private void initMarkets(PowerGridGameState state) {
        // Draw 8
        List<PowerGridCard> firstEight = new ArrayList<>(8);
        for (int i = 0; i < 8 && state.drawPile.getSize() > 0; i++) {
            firstEight.add(state.drawPile.draw());
        }
        //sort ascending by plant number
        firstEight.sort(Comparator.comparingInt(PowerGridCard::getNumber));

        for (int i = 0; i < firstEight.size(); i++) {
            if (i < 4) state.currentMarket.add(firstEight.get(i)); 
            else       state.futureMarket.add(firstEight.get(i));  
        }
        // split 4/4 into current/future
        sortMarket(state.currentMarket);
        sortMarket(state.futureMarket);
    }
    
    //helper to sort the cards in the market 
    private static void sortMarket(Deck<PowerGridCard> market) {
        market.getComponents().sort(Comparator.comparingInt(PowerGridCard::getNumber));
    }
    
    /**
     * Builds and assigns the player turn order.
     * Random on the first round, otherwise based on computed order (plants owned → highest plant → player ID).
     *
     * @param state the current game state
     */
    private void buildTurnOrder(PowerGridGameState state) {
        List<Integer> order = new ArrayList<>();
        if(state.getTurnCounter() == 0) { 
	        for (int i = 0; i < state.getNPlayers(); i++) {
	            order.add(i);
	        }
	        Collections.shuffle(order, state.getRnd());
        }else {
        	order = state.getComputedTurnOrder();
        }

        state.setTurnOrder(order);
        state.setRoundOrder(order);
        state.resetBidOrder();
    }
    


    
    /**
     * Rebalances the power plant markets after a purchase.
     * <p>
     * Draws a replacement plant from the draw pile into the current market,
     * ensures the current market is sorted, and swaps cards between the
     * current and future markets if ordering rules are violated
     * (i.e., the largest current market card is greater than the smallest
     * future market card). Both markets are re-sorted afterwards.
     * </p>
     *
     * @param s the current {@link PowerGridGameState} containing markets to rebalance
     */
    
    public static void rebalanceMarkets(PowerGridGameState s) {
        Deck<PowerGridCard> current  = s.getCurrentMarket();
        Deck<PowerGridCard> future   = s.getFutureMarket();
        Deck<PowerGridCard> drawPile = s.getDrawPile();

        if (current == null || future == null || drawPile == null) {
            System.err.println("Market rebalance aborted: a deck was null.");
            return;
        }

        PowerGridCard drawn = drawPile.draw();
        if (drawn != null && s.getDiscountCard() != -1 && drawn.getNumber() < s.getDiscountCard()) {
            s.setDiscountCard(-1);
        }

        if (drawn != null) {
            if (drawn.type == Type.STEP3) {
                s.setStep(3);
                sortMarket(current);
                if (current.getSize() > 0) {
                    current.draw(); 
                }
            } else {
                int targetCurrent = (s.getStep() == 3) ? 3 : 4;
                if (current.getSize() < targetCurrent) {
                    current.add(drawn);
                } else {
                    future.add(drawn);
                }
            }
        }

        sortMarket(current);
        sortMarket(future);

        PowerGridCard largestCurrent = current.getSize() > 0 ? current.peek(current.getSize() - 1) : null;
        PowerGridCard smallestFuture = future.getSize() > 0 ? future.peek(0) : null;

        if (largestCurrent != null && smallestFuture != null
                && smallestFuture.getNumber() < largestCurrent.getNumber()) {
            
            PowerGridCard a = future.draw();      
            PowerGridCard b = current.pickLast(); 
            future.add(b);
            current.add(a);
            sortMarket(current);
            sortMarket(future);
        }

        //s.printMarkets();
    }



    

    /**
     * Utility for selecting a random contiguous set of map regions used to build the map.
     * <p>
     * Uses a single depth-first search (DFS) over
     * {@link PowerGridGraphBoard#REGION_ADJ_NA} to collect {@code k} regions,
     * where {@code k = min(nPlayers, 5)}. The RNG is taken from
     * {@link AbstractGameState#getRnd()} so results are reproducible under a fixed seed.
     * </p>
     * <implNote>
     * Assumes the region-adjacency graph is such that from any start node
     * a connected set of size {@code k} is reachable.
     * </implNote>
     */
    public final class RegionPicker {
        private static final List<Integer> ALL_REGIONS =
                new ArrayList<>(PowerGridGraphBoard.REGION_ADJ_NA.keySet());

        /** Returns a random contiguous set of size k via a single DFS. */
        public static Set<Integer> randomContiguousSetDFS(AbstractGameState gs) {
            int k = Math.min(gs.getNPlayers(), 5); //if its less than 5 players we just use the number of players else its 5 regions
            Random rnd =  gs.getRnd();
            int start = ALL_REGIONS.get(rnd.nextInt(ALL_REGIONS.size()));
            return dfsGrow(start, k, rnd);
        }

        private static Set<Integer> dfsGrow(int start, int k, Random rnd) {
            Set<Integer> chosen = new LinkedHashSet<>(k);
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(start);

            while (!stack.isEmpty() && chosen.size() < k) {
                int u = stack.pop();
                if (!chosen.add(u)) continue;

                // Randomize neighbor order to avoid bias
                List<Integer> nbrs = new ArrayList<>(PowerGridGraphBoard.REGION_ADJ_NA.get(u));
                Collections.shuffle(nbrs, rnd);
                for (int v : nbrs) {
                    if (chosen.size() >= k) break;
                    if (!chosen.contains(v)) stack.push(v);
                }
            }
            return chosen; 
        }
    }
    
    
    /**
     * Initializes the full hierarchical {@link ActionTreeNode} structure for the Power Grid game.
     * <p>
     * This method defines the static action-space layout used by both Java and Python agents 
     * in PyTAG integration. Each major game phase (e.g., AUCTION, RESOURCE_BUY, BUILD, BUREAUCRACY)
     * is represented as a child branch under the root node. Individual legal actions (such as
     * "increase_bid", "build_city_5", or "run_13_COAL2OIL1") are added as named leaves to ensure
     * the action mask remains consistent across all game states.
     * </p>
     *
     * <p>
     * The resulting tree provides a deterministic, phase-based structure that mirrors the 
     * game's possible action types:
     * <ul>
     *   <li><b>AUCTION</b> – Includes sub-actions for bidding, passing, discarding, and 
     *       opening new auctions (one per available power plant).</li>
     *   <li><b>RESOURCE_BUY</b> – Contains fixed "buy_RESOURCE_AMOUNT" options for each fuel type.</li>
     *   <li><b>BUILD</b> – Includes one leaf per city ("build_city_ID") for expansion actions.</li>
     *   <li><b>BUREAUCRACY</b> – Includes all valid combinations of plant operation mixes
     *       ("run_PLANT_MIXKEY") for the production phase.</li>
     *   <li><b>Pass Round</b> – Always available at the root level as a fallback/pass action.</li>
     * </ul>
     * </p>
     *
     * @param gs the current {@link AbstractGameState}, expected to be an instance of {@link PowerGridGameState}
     * @return the fully constructed root {@link ActionTreeNode} containing all static phase and action leaves
     */
    
    @Override
    public ActionTreeNode initActionTree(AbstractGameState gs) {
        PowerGridGameState s = (PowerGridGameState) gs;
        PowerGridGraphBoard map = s.getGameMap();
        PowerGridParameters params = (PowerGridParameters) s.getGameParameters();

        ActionTreeNode root = new ActionTreeNode(0, "root");

        //Phase NODES
        ActionTreeNode auction       = root.addChild(0, "AUCTION");
        ActionTreeNode startAuction  = auction.addChild(0, "start_auction");
        ActionTreeNode buyResource   = root.addChild(0, "RESOURCE_BUY");
        ActionTreeNode build         = root.addChild(0, "BUILD");
        ActionTreeNode runPhase      = root.addChild(0, "BUREAUCRACY");

        //AUCTION 
        auction.addChild(0, "increase_bid");
        auction.addChild(0, "pass_bid");
        auction.addChild(0, "discard_0");
        auction.addChild(0, "discard_1");
        auction.addChild(0, "discard_2");

        // Open auction leaves one per plant under start_auction
        for (PowerGridCard card : params.plantsIncludedInGame) {
            startAuction.addChild(0, "auction_plant_" + card.getNumber());
        }

        //BUREAUCRACY 
        for (PowerGridCard card : params.plantsIncludedInGame) {
            int plantNumber = card.getNumber();
            if (card.getInput().hasMultipleTypes()) {
                for (EnumMap<PowerGridParameters.Resource, Integer> mix : card.generatePossibleCombos()) {
                    runPhase.addChild(0, "run_" + plantNumber + "_" + mixKey(mix));
                }
            } else {
                runPhase.addChild(0, "run_" + plantNumber + "_" + mixKey(card.getInput().asMap()));
            }
        }

        //RESOURCE_BUY
        for (int i = 1; i <= 9; i++) {
            buyResource.addChild(0, "buy_COAL_" + i);
            buyResource.addChild(0, "buy_GAS_"  + i);
            buyResource.addChild(0, "buy_OIL_"  + i);
            if (i <= 6) buyResource.addChild(0, "buy_URANIUM_" + i);
        }

        //BUILD 
        for (PowerGridCity city : map.cities()) {
            build.addChild(0, "build_city_" + city.getComponentID());
        }

        // Always include pass to keep a stable slot (at root)
        root.addChild(0, "Pass Round");

        return root;
    }

    // helper method to deterministically encode a mix of rosource -> amount in the Enum Map  like {COAL=2, OIL=1} -> "COAL2OIL1"
    private String mixKey(EnumMap<PowerGridParameters.Resource, Integer> mix) {
        StringBuilder sb = new StringBuilder();
        var keys = new java.util.ArrayList<>(mix.keySet());
        keys.sort(java.util.Comparator.comparing(Enum::name));
        for (var k : keys) {
            Integer v = mix.get(k);
            int amount = (v == null) ? 0 : v;
            if (amount > 0) sb.append(k.name()).append(amount);
        }
        return sb.toString();
    }
    
    /**
     * Updates the {@link ActionTreeNode} structure with the currently available actions for the active phase.
     * <p>
     * This method resets the existing tree and rebinds valid {@link AbstractAction} instances to their 
     * corresponding named leaves (e.g., "increase_bid", "buy_COAL_3", "build_city_12") based on the 
     * current {@link PowerGridGamePhase}. Each phase (AUCTION, RESOURCE_BUY, BUILD, BUREAUCRACY) 
     * activates only the relevant action nodes, while the "Pass Round" node is always maintained.
     * </p>
     *
     * @param root       the root {@link ActionTreeNode} previously initialized by {@code initActionTree}
     * @param gameState  the current {@link AbstractGameState}, expected to be a {@link PowerGridGameState}
     * @return the updated {@link ActionTreeNode} with phase-appropriate actions bound to their nodes
     */

    @Override
    public ActionTreeNode updateActionTree(ActionTreeNode root, AbstractGameState gameState) {
        root.resetTree();

        PowerGridGameState s = (PowerGridGameState) gameState;
        var phase = (PowerGridGameState.PowerGridGamePhase) s.getGamePhase();
        java.util.List<AbstractAction> acts = computeAvailableActions(gameState);

        // Phase nodes
        ActionTreeNode auction     = root.findChildrenByName("AUCTION");
        ActionTreeNode start       = (auction != null) ? auction.findChildrenByName("start_auction") : null;
        ActionTreeNode buy         = root.findChildrenByName("RESOURCE_BUY");
        ActionTreeNode build       = root.findChildrenByName("BUILD");
        ActionTreeNode run         = root.findChildrenByName("BUREAUCRACY");
        ActionTreeNode passLeaf    = root.findChildrenByName("Pass Round");

        switch (phase) {
            case PLAYER_ORDER -> {
                for (var a : acts) {
                    if (a instanceof PassAction && passLeaf != null) passLeaf.setAction(a);
                }
            }

            case AUCTION -> {
                for (var a : acts) {
                    if (a instanceof games.powergrid.actions.AuctionPowerPlant app) {
                        var leaf = (start != null) ? start.findChildrenByName("auction_plant_" + app.getPlantNumber()) : null;
                        if (leaf != null) leaf.setAction(a);
                    } else if (a instanceof games.powergrid.actions.IncreaseBid) {
                        var leaf = (auction != null) ? auction.findChildrenByName("increase_bid") : null;
                        if (leaf != null) leaf.setAction(a);
                    } else if (a instanceof games.powergrid.actions.PassBid) {
                        var leaf = (auction != null) ? auction.findChildrenByName("pass_bid") : null;
                        if (leaf != null) leaf.setAction(a);
                    } else if (a instanceof games.powergrid.actions.Discard d) {
                        var leaf = (auction != null) ? auction.findChildrenByName("discard_" + d.getIndex()) : null;
                        if (leaf != null) leaf.setAction(a);
                    } else if (a instanceof PassAction) {
                        if (passLeaf != null) passLeaf.setAction(a);
                    }
                }
            }

            case RESOURCE_BUY -> {
                for (var a : acts) {
                    if (a instanceof games.powergrid.actions.BuyResource br) {
                        String key = "buy_" + br.getResource().name() + "_" + br.getAmount();
                        var leaf = (buy != null) ? buy.findChildrenByName(key) : null;
                        if (leaf != null) leaf.setAction(a);
                    } else if (a instanceof PassAction) {
                        if (passLeaf != null) passLeaf.setAction(a);
                    }
                }
            }

            case BUILD -> {
                for (var a : acts) {
                    if (a instanceof games.powergrid.actions.BuildGenerator bg) {
                        String key = "build_city_" + bg.getCityId();
                        var leaf = (build != null) ? build.findChildrenByName(key) : null;
                        if (leaf != null) leaf.setAction(a);
                    } else if (a instanceof PassAction) {
                        if (passLeaf != null) passLeaf.setAction(a);
                    }
                }
            }

            case BUREAUCRACY -> {
                for (var a : acts) {
                    if (a instanceof games.powergrid.actions.RunPowerPlant rp) {
                        String key = "run_" + rp.getPlantId() + "_" + mixKey(rp.getSpend());
                        var leaf = (run != null) ? run.findChildrenByName(key) : null;
                        if (leaf != null) leaf.setAction(a);
                    } else if (a instanceof PassAction) {
                        if (passLeaf != null) passLeaf.setAction(a);
                    }
                }
            }

            default -> {
                for (var a : acts) {
                    if (a instanceof PassAction && passLeaf != null) passLeaf.setAction(a);
                }
            }
        }

        return root;
    }


        
        
        
    private static void dbgAuction(PowerGridGameState s) {
        int pid = s.getCurrentPlayer();
        System.out.println(" -- AUCTION DEBUG --");
        System.out.println(" pid=" + pid +
                " money=" + s.getPlayersMoney(pid) +
                " step=" + s.getStep() +
                " auctionLive=" + s.isAuctionLive() +
                " auctionPlant=" + s.getAuctionPlantNumber() +
                " currentBid=" + s.getCurrentBid() +
                " currentBidder=" + s.getCurrentBidder() +
                " roundPassed(pid)=" + s.getRoundOrder());  // or your equivalent
        System.out.print(" market:");
        for (var c : s.getCurrentMarket()) System.out.print(" " + c.getNumber());
        System.out.println();
    }
    
    @Override public int hashCode() { return 0; }
}




