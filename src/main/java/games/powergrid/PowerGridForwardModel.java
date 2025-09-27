package games.powergrid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;

import core.components.Deck;
import games.powergrid.PowerGridGameState.PowerGridGamePhase;
import core.interfaces.ITreeActionSpace;
import games.powergrid.PowerGridParameters.Resource;
import games.powergrid.actions.AuctionPowerPlant;
import games.powergrid.actions.BuyResource;
import games.powergrid.actions.IncreaseBid;
import games.powergrid.actions.PassAction;
import games.powergrid.actions.PassBid;
import games.powergrid.components.PowerGridCard;

import games.powergrid.components.PowerGridGraphBoard;
import games.powergrid.components.PowerGridResourceMarket;
import utilities.ActionTreeNode;

import static core.CoreConstants.VisibilityMode.*;


public class PowerGridForwardModel extends StandardForwardModel implements ITreeActionSpace {
	boolean passed = false; 
	@Override
	protected void _setup(AbstractGameState firstState) {
		PowerGridGameState state = (PowerGridGameState)firstState;
		PowerGridParameters params = (PowerGridParameters) state.getGameParameters();
		state.gameMap = PowerGridGraphBoard.northAmerica();;
		state.resourceMarket = new PowerGridResourceMarket();
		state.resourceMarket.setUpMarket(true);//TODO Eventually change this when EU implemeted and put in parameters the amount of intial setup
		state.initFuelStorage(); 
		state.setStep(1);
		state.drawPile = setupDecks(params,state.getNPlayers(), state.getRnd());

		state.currentMarket = new Deck<>("currentMarket", VISIBLE_TO_ALL);
		state.futureMarket = new Deck<>("futureMarket",  VISIBLE_TO_ALL);
		initMarkets(state);
		state.initCityStorageForBoard();//creates a 2d array which keeps track of which cities are bought 
		randomizeTurnOrder(state);
		state.setStartingMoney(params.startingMoney);
		state.setGamePhase(PowerGridGameState.PowerGridGamePhase.AUCTION); 
		int first = state.getTurnOrder().get(0);
		state.setTurnOwner(first);
		state.initOwnedPlants();
				
		System.out.println(state.fuelSummary());
		System.out.println(Arrays.deepToString(state.getCitygraph()));
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
	    int me = gameState.getCurrentPlayer();
	    List<AbstractAction> actions = new ArrayList<>();
	    PowerGridGameState.PowerGridGamePhase phase= (PowerGridGameState.PowerGridGamePhase) gameState.getGamePhase();

	    switch (phase) {
	    //this will be deleted 
        case PLAYER_ORDER:
            break;

        case AUCTION:        	
        	//if the auction is not live and the player is still eligible aka in the round order
            if (s.getRoundOrder().contains(me) && s.isAuctionLive() == false) {
                for (PowerGridCard c : s.getCurrentMarket().getComponents()) {
                    int minOpen = c.getNumber();
                    if (s.getPlayersMoney(me) >= minOpen) {
                        actions.add(new AuctionPowerPlant(me, c.getNumber()));
                    }
                }
                actions.add(new PassAction(me)); //player done for the round
            }else{
            	actions.add(new IncreaseBid(me));
            	actions.add(new PassBid(me));

            	}
            break;

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

        case BUILD:
        	System.exit(0);
            break;

        case BUREAUCRACY:
            break; 
    }
	
    return actions;
}
	

	
	// How much the player can legally buy now (storage-only; ignores market availability)
	// Return order: [COAL, GAS, OIL, URANIUM]
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
	            int units = Math.max(g, o);      // robust if data isn't symmetric
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
	    // No auction live: advance normal round turn
	    if (s.isRoundOrderAllPassed()) { 
	    	advancePhase(s); 
	    	return; 
	    }else {	    //else we advance the round 
	    	if (actionTaken instanceof games.powergrid.actions.PassAction) {
	    	s.removeFromRound(me);
	    	endPlayerTurn(s, s.nextPlayerInRound());
	    	}
	    }
	}



	private void onExitPhase(PowerGridGameState state, PowerGridGamePhase phase) {
	    switch (phase) {
	    	case AUCTION ->{System.out.println(state.getRoundOrder());
	    					System.out.println(state.getTurnOrder());}
	        case BUILD -> { /* finalize builds */ }
	        case BUREAUCRACY -> { /* clear round markers */ }
	        default -> {}
	    }
	}

	private void onEnterPhase(PowerGridGameState state, PowerGridGamePhase phase) {
	    switch (phase) {
	        //case PLAYER_ORDER -> recomputePlayerOrder(state);
	        //case AUCTION -> prepareAuction(state);
	        case RESOURCE_BUY ->{
	        	List<Integer> order = state.getRoundOrder();
	        	Collections.reverse(state.getRoundOrder());
	        	state.setTurnOwner(order.get(0));
		        System.out.println(state.getRoundOrder());//delete when shown to work}

	        }
	        case BUILD ->{System.out.println("==== BUILDING PHASE ====");}
	        //case BUILD -> enableBuilding(state);
	        //case BUREAUCRACY -> doBureaucracy(state);
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

	
	
	
	
	
	//TODO might need to be deleted ot fully implemented yet 
	private void endGameNow(AbstractGameState gs, int[] winners) {
	    // Set everyone to LOSE by default (or DRAW if your game uses ties)
	    for (int p = 0; p < gs.getNPlayers(); p++) {
	        gs.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, p);
	    }
	    // Mark winners
	    for (int w : winners) {
	        gs.setPlayerResult(CoreConstants.GameResult.WIN_GAME, w);
	    }
	    // Mark the game as ended
	    gs.setGameStatus(CoreConstants.GameResult.GAME_END);
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
    

    
    private void randomizeTurnOrder(PowerGridGameState state) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < state.getNPlayers(); i++) {
            order.add(i);
        }
        Collections.shuffle(order, state.getRnd());  // reproducible shuffle
        //turnOrder remains constant throughtout the turn 
        state.setTurnOrder(order);
        //will be modified during a specific phase 
        state.setRoundOrder(order);
        //bidOrder matches RoundOrder
        state.resetBidOrder();
    }
    
  
    private void awardPlantToWinner(PowerGridGameState s, int winner, int plantNumber, int price) {
        s.decreasePlayerMoney(winner, price);

        // remove the bought plant from CURRENT and give it to the player
        Deck<PowerGridCard> current = s.getCurrentMarket();
        PowerGridCard bought = null;
        for (PowerGridCard c : current.getComponents()) {
            if (c.getNumber() == plantNumber) { bought = c; break; }
        }
        if (bought == null) {
            System.err.printf("Plant %d not found in current market!%n", plantNumber);
            return;
        }
        current.remove(bought);
        s.addPlantToPlayer(winner, bought);

        System.out.printf(">>> Player %d wins auction for plant %d at price %d%n",
                winner, plantNumber, price);
        s.printOwnedPlants();

        // Proper refill
        rebalanceMarkets(s);
    }
    
    private void rebalanceMarkets(PowerGridGameState s) {
        Deck<PowerGridCard> current = s.getCurrentMarket();
        Deck<PowerGridCard> future  = s.getFutureMarket();
        Deck<PowerGridCard> drawPile    = s.getDrawPile();

        PowerGridCard replacementPlant = drawPile.draw(); 
        current.add(replacementPlant); //we have taken a card from the current market so we replace it here 
        sortMarket(current); //sort the current market so the largest card is last
        PowerGridCard firstFuture = future.peek(0);  
        PowerGridCard lastCurrent = current.peek(current.getSize() - 1);
        //compare the largest current market card to the smallest future market
        if(firstFuture.getNumber() < lastCurrent.getNumber()) { //if the futrue has a smaller card then swap and resort 
        	PowerGridCard A = future.draw();
        	PowerGridCard B = current.pickLast();
        	future.add(B);
        	current.add(A);
        }
        sortMarket(current);
        sortMarket(future);           
        s.printMarkets();
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



