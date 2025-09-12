package games.powergrid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import games.powergrid.PowerGridParameters.Resource;
import games.powergrid.actions.BidPowerPlant;
import games.powergrid.actions.PassBid;
import games.powergrid.components.PowerGridCard;
import games.powergrid.components.PowerGridGraphBoard;
import games.powergrid.components.PowerGridResourceMarket;

import static core.CoreConstants.VisibilityMode.*;


public class PowerGridForwardModel extends StandardForwardModel {

	@Override
	protected void _setup(AbstractGameState firstState) {
		PowerGridGameState state = (PowerGridGameState)firstState;
		PowerGridParameters params = (PowerGridParameters) state.getGameParameters();
		state.gameMap = PowerGridGraphBoard.northAmerica();;
		state.resourceMarket = new PowerGridResourceMarket();
		state.resourceMarket.setUpMarket(true);//Eventually change this when EU implemeted
		state.initFuelStorage(); 
		state.drawPile = setupDecks(params,state.getNPlayers(), state.getRnd());
		PowerGridCard cardA = state.drawPile.draw(); //test
		PowerGridCard cardB = state.drawPile.draw();//test
		state.currentMarket = new Deck<>("currentMarket", VISIBLE_TO_ALL);
		state.futureMarket = new Deck<>("futureMarket",  VISIBLE_TO_ALL);
		initMarkets(state);
		state.initCityStorageForBoard();//creates a 2d array which keeps track of which cities are bought 
		randomizeTurnOrder(state);
		state.setStartingMoney(params.startingMoney);
		state.setPhase(PowerGridParameters.Phase.AUCTION); // or PLAYER_ORDER if you want that first
		int first = state.getTurnOrder().get(0);
		state.setTurnOwner(first);
		state.initOwnedPlants();
		
		
		System.out.println(state.fuelSummary());
		state.addFuel(2, PowerGridParameters.Resource.URANIUM, 4);
		System.out.println(state.fuelSummary());
		System.out.println(Arrays.deepToString(state.getCitygraph()));
		System.out.println(state.getTurnOrder().toString());
		System.out.println(state.resourceMarket);
		state.resourceMarket.buy(PowerGridParameters.Resource.URANIUM, 1);
		System.out.println(state.resourceMarket);
		state.addPlantToPlayer(0, cardA);  // test
		state.addPlantToPlayer(1, cardB);  // test

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

	    switch (s.getPhase()) {
        case PLAYER_ORDER:
            break;

        case AUCTION:
            if (!s.isAuctionLive()) {
                for (PowerGridCard c : s.getCurrentMarket().getComponents()) {
                    int minOpen = c.getNumber();
                    System.out.println(minOpen);
                    System.out.println(s.getPlayersMoney(me));
                    if (s.getPlayersMoney(me) >= minOpen) {
                        actions.add(new BidPowerPlant(me, c.getNumber(), minOpen));
                    }
                }
                actions.add(new PassBid(me)); // decline to open an auction
            } else {
                int minRaise = Math.max(s.getCurrentBid() + 1, s.getAuctionPlantNumber());
                if (s.getPlayersMoney(me) >= minRaise) {
                    actions.add(new BidPowerPlant(me, s.getAuctionPlantNumber(), minRaise));
                }
                actions.add(new PassBid(me));
            }
            break;

        case RESOURCE_BUY:
            break;

        case BUILD:
            break;

        case BUREAUCRACY:
            break;
    }
    return actions;
}
	@Override
	protected void _afterAction(AbstractGameState gameState, AbstractAction actionTaken) {
	    PowerGridGameState s = (PowerGridGameState) gameState;

	    if (s.getPhase() != PowerGridParameters.Phase.AUCTION) {
	        advanceToNextPlayer(s);
	        return;
	    }

	    if (!s.isAuctionLive()) {

	        advanceAuctionPointerIfNeeded(s);
	        return;
	    }

	    // Live auction path: either hand to next bidder or resolve if closed
	    resolveAuctionIfClosed(s); // if 1 active left: award, clear, advanceToNextPlayer
	    if (s.isAuctionLive()) {
	        handToNextBidder(s);   // still live → move to next active bidder
	    }
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
    
    private void recomputeAuctionOrder(PowerGridGameState s) {
        List<Integer> order = IntStream.range(0, s.getNPlayers())
            .boxed()
            .sorted(
                Comparator.<Integer>comparingInt(s::getCityCount).reversed()
                    .thenComparingInt(s::getHighestPlantNumber).reversed()
                    .thenComparingInt(Integer::intValue) // stable tie-breaker by playerId
            )
            .collect(java.util.stream.Collectors.toList()); // use toList() if on JDK 16+
        s.setTurnOrder(order);
    }

    private void recomputeReverseOrder(PowerGridGameState s) {
        List<Integer> order = new ArrayList<>(s.getTurnOrder());
        Collections.reverse(order);
        s.setTurnOrder(order);
    }
    
    private void randomizeTurnOrder(PowerGridGameState state) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < state.getNPlayers(); i++) {
            order.add(i);
        }
        Collections.shuffle(order, state.getRnd());  // reproducible shuffle
        state.setTurnOrder(order);
    }
    
    // ===== Auction resolution helpers =====
    private void handToNextBidder(PowerGridGameState s) {
        int nextBidder = s.nextActiveBidderAfter(s.getCurrentBidder()); // you implement this in state
        s.setTurnOwner(nextBidder);
    }

    private void resolveAuctionIfClosed(PowerGridGameState s) {
        if (s.getActiveBidders().size() == 1) {
            int winner = s.getCurrentBidder();
            int price  = s.getCurrentBid();
            int plant  = s.getAuctionPlantNumber();
            System.out.printf(
                    "[AuctionClosed] Winner=P%d | Plant=%d | Price=%d | RemainingMoney=%d%n",
                    winner, plant, price, s.getPlayersMoney(winner)
                );
            
            awardPlantToWinner(s, winner, plant, price); // another FM helper
            System.out.println("player money " + s.getPlayersMoney(winner));
            s.clearAuction();
            advanceToNextPlayer(s); // next player decides whether to start the next auction
        }
    }

    private void awardPlantToWinner(PowerGridGameState s, int winner, int plant, int price) {
        s.decreasePlayerMoney(winner, price);
        // remove plant from market, add to player’s plant deck, refill market, etc.
    }
    
    private void advanceAuctionPointerIfNeeded(PowerGridGameState s) {
        advanceToNextPlayer(s);
    }

    private void advanceToNextPlayer(PowerGridGameState s) {
        s.advanceTurn();
        int next = s.getTurnOrder().get(s.getTurnOrderIndex());
        s.setTurnOwner(next);
    }

}



