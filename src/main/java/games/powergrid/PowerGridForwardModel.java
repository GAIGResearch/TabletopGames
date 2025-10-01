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
import java.util.stream.IntStream;

import com.google.iam.v1.AuditConfigDelta.Action;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;

import core.components.Deck;
import games.powergrid.PowerGridGameState.PowerGridGamePhase;
import core.interfaces.ITreeActionSpace;
import games.powergrid.PowerGridParameters.Resource;
import games.powergrid.PowerGridParameters.Step;
import games.powergrid.actions.AuctionPowerPlant;
import games.powergrid.actions.BuildGenerator;
import games.powergrid.actions.BuyResource;
import games.powergrid.actions.IncreaseBid;
import games.powergrid.actions.PassAction;
import games.powergrid.actions.PassBid;
import games.powergrid.actions.RunPowerPlant;
import games.powergrid.components.PowerGridCard;
import games.powergrid.components.PowerGridCard.Type;

import java.util.HashSet;

import games.powergrid.components.PowerGridGraphBoard;
import games.powergrid.components.PowerGridResourceMarket;
import utilities.ActionTreeNode;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;

import static core.CoreConstants.GameResult.DRAW_GAME;
import static core.CoreConstants.GameResult.GAME_END;
import static core.CoreConstants.GameResult.LOSE_GAME;
import static core.CoreConstants.GameResult.WIN_GAME;
import static core.CoreConstants.VisibilityMode.*;
import static games.root.RootParameters.VictoryCondition.Score;


public class PowerGridForwardModel extends StandardForwardModel implements ITreeActionSpace {
	@Override
	protected void _setup(AbstractGameState firstState) {
		PowerGridGameState state = (PowerGridGameState)firstState;
		PowerGridParameters params = (PowerGridParameters) state.getGameParameters();
		state.setActiveRegions(RegionPicker.randomContiguousSetDFS(state));		
		state.setGameMap( PowerGridGraphBoard.northAmerica().penalizeRegions(state.getActiveRegions(),10000));//creates a board where the cost to go to invalid region is 100000
		state.setInvalidCities(state.gameMap.invalidCities(state.getActiveRegions()));//creates a set of invalid cities based on the current legal board 
		state.setValidCities(state.gameMap.validCities(state.getActiveRegions()));//creates a set of valid cities based on the current legal board 
		state.setResourceMarket(new PowerGridResourceMarket());
		state.resourceMarket.setUpMarket(params.startinResources);//TODO Eventually change this when EU implemeted and put in parameters the amount of intial setup
		state.initFuelStorage(); 
		state.setStep(1);
		state.setDrawPile(setupDecks(params,state.getNPlayers(), state.getRnd()));
		state.setCurrentMarket(new Deck<>("currentMarket", VISIBLE_TO_ALL));
		state.setFutureMarket(new Deck<>("futureMarket",  VISIBLE_TO_ALL));
		initMarkets(state);
		state.initCityStorageForBoard();//creates a 2d array which keeps track of which cities are bought 
		buildTurnOrder(state);
		state.setStartingMoney(params.startingMoney);
		state.setGamePhase(PowerGridGameState.PowerGridGamePhase.AUCTION); 
		int first_player = state.getTurnOrder().get(0);
		state.setTurnOwner(first_player);
		state.initOwnedPlants();
				
		System.out.println(state.fuelSummary());
		//System.out.println(Arrays.deepToString(state.getCitySlotsById()));
		System.out.println(state.getTurnOrder().toString());
		System.out.println(state.resourceMarket);


		for (PowerGridCard c : state.currentMarket.getComponents()) {
		    System.out.println(" - " + c);
		}

		System.out.println("Future Market:");
		for (PowerGridCard c : state.futureMarket.getComponents()) {
		    System.out.println(" - " + c);
		}
		
		System.out.println("Remaining Cards:");
		for (PowerGridCard c : state.drawPile.getComponents()) {
		    System.out.println(" - " + c);
		}
		
	}
	

	@Override
	protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
		PowerGridGameState s = (PowerGridGameState) gameState;
		PowerGridParameters params = (PowerGridParameters) s.getGameParameters();
	    int me = gameState.getCurrentPlayer();
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
	                if (money >= minBid) {
	                    actions.add(new AuctionPowerPlant(me, minBid));
	                }
	            }

	            // Future market only in Step 3
	            if (s.getStep() == 3) {
	                for (PowerGridCard card : s.getFutureMarket().getComponents()) {
	                	if(card.type == Type.STEP3)continue;
	                    int minBid = card.getNumber();
	                    if (money >= minBid) {
	                        actions.add(new AuctionPowerPlant(me, minBid));
	                    }
	                }
	            }

	            // Player may also pass (done for the round)
	            actions.add(new PassAction(me));
	        } else {
	            // Auction is live (or player not eligible to start) → bidding choices
	            if (money > s.getCurrentBid()) {
	                actions.add(new IncreaseBid(me));
	            }
	            actions.add(new PassBid(me));
	        }

	        break;
	    }

        case RESOURCE_BUY:
        	
        	//get the players current power plant cards
        	System.out.println(me);
        	EnumMap<PowerGridParameters.Resource, Integer> buyCapacity = playerBuyCapacity(s, me);
        		System.out.println(buyCapacity); // {COAL=..., GAS=..., OIL=..., URANIUM=...}
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
    		actions.add(new PassAction(me));  		
    		System.out.println(actions);
        	
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
            actions.add(new PassAction(me));
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
            actions.add(new PassAction(me));
            break;
        }
		default:
			break;
    }
	
    return actions;
}
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
	



	
	
	@Override
	protected void _afterAction(AbstractGameState gs, AbstractAction actionTaken) {
	    PowerGridGameState s = (PowerGridGameState) gs;
	    int me = gs.getCurrentPlayer();

	    if (s.isAuctionLive() && gs.getGamePhase() == PowerGridGamePhase.AUCTION) {
	        int next = s.checkNextBid(me);  //get the next valid bidder 

	        if (next == me) { //if no one else is bidding then I have won the auction
	            awardPlantToWinner(s, me, s.getAuctionPlantNumber(), s.getCurrentBid());
	            s.resetAuction();
	            s.removeFromRound(me);

	            if (s.isRoundOrderAllPassed()) { //You are the last player and you win
	            	advancePhase(s);
	            	return; }
	            endPlayerTurn(s, s.nextPlayerInRound());
	        } else {
	            s.setTurnOwner(next);  // pass bidding turn to next bidder
	        }
	        return;
	    }  
	    // No auction live advance normal round turn
	    if (s.isRoundOrderAllPassed()) { 
	    	advancePhase(s); 
	    	return; 
	    }else {	    //else we advance the round 
	    	if (actionTaken instanceof PassAction) {
		    	s.removeFromRound(me);
		    	endPlayerTurn(s, s.nextPlayerInRound());
	    	}
	    }
	}





	private void onEnterPhase(PowerGridGameState state, PowerGridGamePhase phase) {
	    switch (phase) {
	        case PLAYER_ORDER -> {buildTurnOrder(state);
	        						advancePhase(state);
	        						endRound(state, state.getTurnOwner());
	        }
	        //case AUCTION -> prepareAuction(state);
	        case RESOURCE_BUY ->{List<Integer> order = state.getRoundOrder();
					        	Collections.reverse(state.getRoundOrder());
					        	state.setTurnOwner(order.get(0));
						        System.out.println(state.getRoundOrder());//delete when shown to work

	        }
	        case BUILD ->{List<Integer> order = state.getRoundOrder();
			        	Collections.reverse(state.getRoundOrder());
			        	state.setTurnOwner(order.get(0));
				        System.out.println(state.getRoundOrder());//delete when shown to work
	        	}
	        case BUREAUCRACY ->{state.resetPlantsRan();;
	        					if(state.getStep() == 1 && state.stepTwoTrigger()){//no plants have been run yet 
	        						state.setStep(2);
	        						state.getCurrentMarket().draw(); //remove lowest card 
	        						rebalanceMarkets(state);	        						
	        					}
	        	
	        			
	        }
	        default -> {}
	    }
	}



	private void onExitPhase(PowerGridGameState state, PowerGridGamePhase phase) {
	    switch (phase) {
	    	case AUCTION ->{System.out.println(state.getRoundOrder());
	    					System.out.println(state.getTurnOrder());}
	    	case RESOURCE_BUY->{}
	        case BUILD -> { PowerGridParameters params = (PowerGridParameters) state.getGameParameters();
	        				if(state.getMaxCitiesOwned() >= params.citiesToTriggerEnd[state.getNPlayers()-1]) {
	        					state.setGameStatus(CoreConstants.GameResult.GAME_END); }
	        }
	        case BUREAUCRACY -> { 
	        					if(state.getGameStatus() == CoreConstants.GameResult.GAME_END) {
	        						endGame(state);
	        					}
	        					awardIncome(state);
	        					PowerGridParameters params = (PowerGridParameters) state.getGameParameters();
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


	
	


	public void advancePhase(AbstractGameState gameState) {
		PowerGridGameState s = (PowerGridGameState) gameState;
	    System.out.println("Everyone has went");
	    s.resetRoundOrderNextPhase(); //rest the round turn order based on the global turn order
	    endPlayerTurn(s, s.getCurrentPlayer()); //
	    PowerGridGameState.PowerGridGamePhase current = (PowerGridGameState.PowerGridGamePhase) gameState.getGamePhase(); //get the current phase from the state 
	    onExitPhase(s, current); //clean up actions for leaving a phase 
	    PowerGridGameState.PowerGridGamePhase next = current.next(); // get the next phase by calling next on the phase class in parameters 
	    gameState.setGamePhase(next); //set the phase in state to next 
	    onEnterPhase(s, next); //perfroms ations that need to be taken prior to the next phase 
	    
	   
	}
	
	private static void awardIncome(PowerGridGameState s) {
	    for (int p = 0; p < s.getNPlayers(); p++) {    	
	        int powered = Math.min(s.numberOfPoweredCities(p), PowerGridParameters.INCOME_TRACK.length - 1);
	        System.out.println("Powered Cities" + powered + "by $" + PowerGridParameters.INCOME_TRACK[powered]);
	        s.increasePlayerMoney(p, PowerGridParameters.INCOME_TRACK[powered]);
	    }
	}

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

	    // 1) Sum capacities: dedicated + hybrid pool
	    int coalCap = 0, gasCap = 0, oilCap = 0, urCap = 0, hybridCap = 0;

	    // use getComponents() unless your Deck implements Iterable<PowerGridCard>
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

	    // 2) Subtract what the player already stores
	    int cHave = s.getFuel(playerId, PowerGridParameters.Resource.COAL);
	    int gHave = s.getFuel(playerId, PowerGridParameters.Resource.GAS);
	    int oHave = s.getFuel(playerId, PowerGridParameters.Resource.OIL);
	    int uHave = s.getFuel(playerId, PowerGridParameters.Resource.URANIUM);

	    int coalFree = Math.max(0, coalCap - cHave);
	    int urFree   = Math.max(0, urCap   - uHave);

	    // 3) Dedicated first, overflow into hybrid pool (shared for GAS/OIL)
	    int gasDedicatedUsed = Math.min(gHave, gasCap);
	    int oilDedicatedUsed = Math.min(oHave, oilCap);
	    int gasOverflow = Math.max(0, gHave - gasCap);
	    int oilOverflow = Math.max(0, oHave - oilCap);
	    int hybridUsed  = Math.min(hybridCap, gasOverflow + oilOverflow);
	    int hybridFree  = Math.max(0, hybridCap - hybridUsed);

	    int gasFree = Math.max(0, gasCap - gasDedicatedUsed) + hybridFree;
	    int oilFree = Math.max(0, oilCap - oilDedicatedUsed) + hybridFree;

	    canBuy.put(PowerGridParameters.Resource.COAL,    coalFree);
	    canBuy.put(PowerGridParameters.Resource.GAS,     gasFree);
	    canBuy.put(PowerGridParameters.Resource.OIL,     oilFree);
	    canBuy.put(PowerGridParameters.Resource.URANIUM, urFree);
	    return canBuy;
	}
	
	
	
	
	
    @Override
    protected void endGame(AbstractGameState gs) {
        PowerGridGameState state = (PowerGridGameState) gs;
        int winner = 0;
        int highestCities = -1;
        int highestMoney = -1;

        for (int i = 0; i < state.getNPlayers(); i++) {
            int poweredCities = state.numberOfPoweredCities(i);
            int money = state.getPlayersMoney(i);  // Assuming you have this method

            if (poweredCities > highestCities ||
                (poweredCities == highestCities && money > highestMoney)) {
                
                winner = i;
                highestCities = poweredCities;
                highestMoney = money;
            }
        }

        // Mark all players as lost first
        for (int i = 0; i < state.getNPlayers(); i++) {
            gs.setPlayerResult(LOSE_GAME, i);
        }

        // Then mark the winner
        gs.setPlayerResult(WIN_GAME, winner);
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

        // Recombine: tempPile on top of drawPile
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
    
    private void initMarkets(PowerGridGameState state) {
        // Draw 8, sort ascending by plant number, split 4/4 into current/future
        List<PowerGridCard> firstEight = new ArrayList<>(8);
        for (int i = 0; i < 8 && state.drawPile.getSize() > 0; i++) {
            firstEight.add(state.drawPile.draw());
        }
        firstEight.sort(Comparator.comparingInt(PowerGridCard::getNumber));

        for (int i = 0; i < firstEight.size(); i++) {
            if (i < 4) state.currentMarket.add(firstEight.get(i)); // cheapest 4
            else       state.futureMarket.add(firstEight.get(i));  // next 4
        }
        sortMarket(state.currentMarket);
        sortMarket(state.futureMarket);
    }
    
    private void sortMarket(Deck<PowerGridCard> market) {
        market.getComponents().sort(Comparator.comparingInt(PowerGridCard::getNumber));
    }
    

    //TODO this will be changed to both handle a randomize on the intial set up and handle in Phase1 basing it on #of generators if tied then highest plant number
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
	        // reproducible shuffle
        //turnOrder remains constant throughtout the round 
        state.setTurnOrder(order);
        //will be modified during a specific phase 
        state.setRoundOrder(order);
        //bidOrder matches RoundOrder
        state.resetBidOrder();
    }
    
    /**
     * Awards a power plant to the winning player after an auction.
     * <p>
     * This method deducts the purchase price from the winner, removes the
     * specified plant from the current market, and adds it to the player's
     * owned plants. If the plant is not found in the current market, the
     * method logs an error and returns without changes. After a successful
     * transfer, the power plant markets are rebalanced.
     * </p>
     *
     * @param s           the current {@link PowerGridGameState}
     * @param winner      ID of the player who won the auction
     * @param plantNumber number of the plant that was purchased
     * @param price       final price paid for the plant
     */
    private void awardPlantToWinner(PowerGridGameState s, int winner, int plantNumber, int price) {
        s.decreasePlayerMoney(winner, price);

        Deck<PowerGridCard> current = s.getCurrentMarket();
        Deck<PowerGridCard> future  = s.getFutureMarket();

        PowerGridCard bought = null;
        Deck<PowerGridCard> source = null;

        // Try CURRENT market first
        for (PowerGridCard c : current.getComponents()) {
            if (c.getNumber() == plantNumber) {
                bought = c;
                source = current;
                break;
            }
        }
        // If not found, try FUTURE market (needed in Step 3 where auctions can start from future)
        if (bought == null) {
            for (PowerGridCard c : future.getComponents()) {
                if (c.getNumber() == plantNumber) {
                    bought = c;
                    source = future;
                    break;
                }
            }
        }

        if (bought == null) {
            System.err.printf("Plant %d not found in current or future market!%n", plantNumber);
            return;
        }

        source.remove(bought);
        s.addPlantToPlayer(winner, bought);

        System.out.printf(">>> Player %d wins auction for plant %d at price %d (from %s market)%n",
                winner, plantNumber, price, (source == current ? "current" : "future"));
        s.printOwnedPlants();
        rebalanceMarkets(s);
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
    
    private void rebalanceMarkets(PowerGridGameState s) {
        Deck<PowerGridCard> current  = s.getCurrentMarket();
        Deck<PowerGridCard> future   = s.getFutureMarket();
        Deck<PowerGridCard> drawPile = s.getDrawPile();

        if (current == null || future == null || drawPile == null) {
            System.err.println("Market rebalance aborted: a deck was null.");
            return;
        }

        PowerGridCard drawn = drawPile.draw();

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

        s.printMarkets();
    }



    

    /**
     * Utility for selecting a random contiguous set of map regions.
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
            return chosen; // guaranteed to have size k by your assumption
        }
    }
    
    
    
    
    //PYTAG ACTION TREES 
	@Override
	public ActionTreeNode initActionTree(AbstractGameState gameState) {
		ActionTreeNode root = new ActionTreeNode(0, "root");

        // AUCTION branches
        root.addChild(0, "open_auction");   // leaves = AuctionPowerPlant(plantNo)
        root.addChild(0, "pass_round");     // leaf = PassAction
        root.addChild(0, "bid");            // leaves = IncreaseBid OR PlaceBid(amount)
        root.addChild(0, "pass_bid");       // leaf = PassBid
		return null;
	}


	@Override
	public ActionTreeNode updateActionTree(ActionTreeNode root, AbstractGameState gameState) {
		// TODO Auto-generated method stub
		return null;
	}
}



