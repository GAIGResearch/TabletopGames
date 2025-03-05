package games.tickettoride;

import core.AbstractGameData;
import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.*;
import core.properties.*;

import java.util.*;

import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
import static core.CoreConstants.colorHash;
import static core.CoreConstants.playerHandHash;
import static games.tickettoride.TicketToRideConstants.*;

import games.tickettoride.actions.ClaimRoute;
import games.tickettoride.actions.DrawDestinationTicketCards;
import games.tickettoride.actions.DrawTrainCards;
import utilities.Hash;

/**
 * <p>The forward model contains all the game rules and logic. It is mainly responsible for declaring rules for:</p>
 * <ol>
 *     <li>Game setup</li>
 *     <li>Actions available to players in a given game state</li>
 *     <li>Game events or rules applied after a player's action</li>
 *     <li>Game end</li>
 * </ol>
 */


public class TicketToRideForwardModel extends StandardForwardModel {

    /**
     * Initializes all variables in the given game state. Performs initial game setup according to game rules, e.g.:
     * <ul>
     *     <li>Sets up decks of cards and shuffles them</li>
     *     <li>Gives player cards</li>
     *     <li>Places tokens on boards</li>
     *     <li>...</li>
     * </ul>
     *
     * @param firstState - the state to be modified to the initial game state.
     */
    @Override
    protected void _setup(AbstractGameState firstState) {

        TicketToRideGameState state = (TicketToRideGameState) firstState;
        state._reset();
        TicketToRideParameters tp = (TicketToRideParameters) state.getGameParameters();

        state.tempDeck = new Deck<>("Temp Deck", VISIBLE_TO_ALL);
        state.areas = new HashMap<>();
        state.scores = new int[state.getNPlayers()];
        state.trainCars = new int[state.getNPlayers()];
        AbstractGameData _data = new AbstractGameData();
        _data.load(tp.getDataPath());

        for (int i = 0; i < state.getNPlayers(); i++) {
            state.setTrainCars(i,tp.nInitialTrainCars); //default amount of train cars(35) per player, used to dictate end of game when <= 2
            Area playerArea = new Area(i, "Player Area");
            //Player Train Card hand setup
            Deck<Card> playerTrainCardHand = new Deck<>("Player Train Card Hand", VISIBLE_TO_ALL);
            playerTrainCardHand.setOwnerId(i);
            playerArea.putComponent(playerHandHash, playerTrainCardHand);

            //Destination card hand setup
            Deck<Card> playerDestinationCardHand = new Deck<>("Player Destination Card Hand", VISIBLE_TO_ALL);
            playerDestinationCardHand.setOwnerId(i);
            playerArea.putComponent(playerDestinationHandHash, playerDestinationCardHand);

            //System.out.println("Setup: made deck for player " + i + " has " + playerTrainCardHand);

            state.areas.put(i, playerArea);
        }

        Area gameArea = new Area(-1, "Game Area");
        state.areas.put(-1, gameArea);

        state.world = _data.findGraphBoardWithEdges("locations");
        gameArea.putComponent(ticketToRideBoardHash, state.world);



        // setup train car card deck
        Deck<Card> trainCardDeck = new Deck<>("Train Card Deck", HIDDEN_TO_ALL);
        Deck<Card> trainCardTypes = _data.findDeck("TrainCars");
        for (Card c: trainCardTypes) { //Add x number of each train card type
            for (int i = 0; i < 25; i++) {
                trainCardDeck.add(c);
            }

        }

        Deck<Card> destinationCardDeck = new Deck<>("Destination Card Deck", HIDDEN_TO_ALL);
        Deck<Card> destinationCardTypes = _data.findDeck("DestinationTickets");
        destinationCardDeck = destinationCardTypes;

        trainCardDeck.shuffle(firstState.getRnd());
        destinationCardDeck.shuffle(firstState.getRnd());

        gameArea.putComponent(TicketToRideConstants.trainCardDeckHash, trainCardDeck);
        gameArea.putComponent(TicketToRideConstants.destinationCardDeckHash, destinationCardDeck);

        state.addComponents();


        //draw initial cards
        for (int i = 0; i < state.getNPlayers(); i++) {
            Area playerArea = state.getArea(i);
            Deck<Card> playerTrainCardHandDeck = (Deck<Card>) playerArea.getComponent(playerHandHash);
            Deck<Card> playerDestinationCardHandDeck = (Deck<Card>) playerArea.getComponent(TicketToRideConstants.playerDestinationHandHash);
            for (int j = 0; j < tp.nInitialTrainCards; j++) {
                new DrawCard(trainCardDeck.getComponentID(), playerTrainCardHandDeck.getComponentID()).execute(state);
            }
            for (int j = 0; j < tp.nInitialDestinationCards; j++) {
                new DrawCard(destinationCardDeck.getComponentID(), playerDestinationCardHandDeck.getComponentID()).execute(state);
            }

            System.out.println("Setup: player " + i + " has destination card " + playerDestinationCardHandDeck.peek().getProperty(pointsHash));
            System.out.println("Setup: player " + i + " has " + playerTrainCardHandDeck);
        }

        state.setGamePhase(TicketToRideGameState.TicketToRideGamePhase.NormalGameRound);


        //test graph works
        /*System.out.println("SETUP , TESTING IF BIDIRECTIONAL");*/

        GraphBoardWithEdges world = (GraphBoardWithEdges) gameArea.getComponent(ticketToRideBoardHash);

        HashSet<Edge> boardEdges = (HashSet<Edge>) world.getBoardEdges();

        Collection<BoardNodeWithEdges> boardNodes = (Collection<BoardNodeWithEdges>) world.getBoardNodes();
        int nodesHashKey = Hash.GetInstance().hash("nodes");


        for ( BoardNodeWithEdges b : boardNodes) {
            Map<Edge, BoardNodeWithEdges> boardNodeMapping = b.getNeighbourEdgeMapping();
            /*System.out.println("Current BoardNode: " + b.getComponentName());
*/
            for (Map.Entry<Edge, BoardNodeWithEdges> entry : boardNodeMapping.entrySet()) {
                Edge edge = entry.getKey();
                Property nodeProp = edge.getProperty(nodesHashKey);
                BoardNodeWithEdges neighbourNode = entry.getValue();


//                System.out.println("NODE PROP for current edge: "+ nodeProp);
            }
        }

        gameArea.putComponent(TicketToRideConstants.trainCardDeckDiscardHash, new Deck<>("trainCardDeckDiscard", VISIBLE_TO_ALL));
        state.setFirstPlayer(0);





    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * @return - List of AbstractAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        TicketToRideGameState tg = (TicketToRideGameState) gameState;
        int trainCardsRequiredHashKey = Hash.GetInstance().hash("trainCardsRequired");
        int colorHashKey = Hash.GetInstance().hash("color");

        List<AbstractAction> actions = new ArrayList<>();
        int playerId = tg.getCurrentPlayer();
        Deck<Card> playerTrainCardDeck = (Deck<Card>) tg.getComponent(trainCardDeckHash);
        Deck<Card> playerDestinationCardDeck = (Deck<Card>) tg.getComponent(destinationCardDeckHash);
        Deck<Card> trainCardDiscardDeck = (Deck<Card>) tg.getComponent(trainCardDeckDiscardHash);

        if (playerTrainCardDeck.getSize() < 2){ // shuffle discard into deck
            System.out.println("Shuffle discard into deck before" + " train card size:" + playerTrainCardDeck.getSize() + " discard size: " + trainCardDiscardDeck.getSize());
            trainCardDiscardDeck.shuffle(tg.getRnd());
            playerTrainCardDeck.add(trainCardDiscardDeck);
            trainCardDiscardDeck.clear();


        }

        if (playerTrainCardDeck.getSize() >= 2){ //to do: shuffle discard into deck
            actions.add(new DrawTrainCards(playerId));
        }
        if (playerDestinationCardDeck.getSize() > 1){
            actions.add(new DrawDestinationTicketCards(playerId));
        }

        //System.out.println(playerId + " in compute action");

        List<Edge> allRoutesAvailable = tg.getAvailableRoutes();
        HashMap<Edge, List<Integer>> routesAvailableToBuy = tg.getAffordableRoutes(allRoutesAvailable);  //key of edges, indexs of which color to buy

        if (!routesAvailableToBuy.isEmpty()) {

            for (Map.Entry<Edge, List<Integer>> currentRoute : routesAvailableToBuy.entrySet()) { //add every route available to list of actions

                Edge currentEdge = currentRoute.getKey();

                Property trainCardsRequiredProp = currentEdge.getProperty(trainCardsRequiredHashKey);
                int trainCardsRequired = ((PropertyInt) trainCardsRequiredProp).value;

                List<Integer> indexesOfColors = routesAvailableToBuy.get(currentEdge);
                Property colorProp = currentEdge.getProperty(colorHashKey);
                String[] colorsOfRoute = ((PropertyStringArray) colorProp).getValues();
                System.out.println(colorsOfRoute + " is the colors");
                System.out.println(indexesOfColors  + " indexs of color");
                for (int i = 0; i < indexesOfColors.size(); i++){
                    String colorOfRoute;
                    if (i < colorsOfRoute.length && indexesOfColors.get(i) < colorsOfRoute.length ){ //need to check because grey double routes, colorsOfRoute would be len = 1 but 2 of them could be available
                        colorOfRoute = colorsOfRoute[indexesOfColors.get(i)];
                    } else{
                        colorOfRoute = colorsOfRoute[0];

                    }

                    System.out.println(colorOfRoute + " is the colors");
//
//                    System.out.println("In for loop for routes available to buy " + currentEdge.getProperties());

                    actions.add(new ClaimRoute(currentEdge,playerId, colorOfRoute, trainCardsRequired, indexesOfColors.get(i)));
                }

            }

        }

        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {

        if (currentState.isActionInProgress()) return;
        TicketToRideGameState gs = (TicketToRideGameState) currentState;
        TicketToRideParameters params = (TicketToRideParameters) gs.getGameParameters();

        int playerId = currentState.getCurrentPlayer();
        int currentTrainCars = gs.getTrainCars(playerId);


        TicketToRideGameState.TicketToRideGamePhase gamePhase = (TicketToRideGameState.TicketToRideGamePhase) gs.getGamePhase();
        if (currentTrainCars <= 2 && gamePhase != TicketToRideGameState.TicketToRideGamePhase.FinalRound){ //trigger final round
            gs.setGamePhase(TicketToRideGameState.TicketToRideGamePhase.FinalRound);
            System.out.println("NOW IN FINAL ROUND: player " + playerId + " with train cars: " + currentTrainCars );
        } else if (gamePhase == TicketToRideGameState.TicketToRideGamePhase.FinalRound) { //already in final round
            gs.setCurrentFinalRoundTurn(gs.getCurrentFinalRoundTurn() + 1);
            System.out.println("Final round for player " + gs.getCurrentPlayer());
            if (gs.getCurrentFinalRoundTurn() == gs.getNPlayers()){ //all final turns have been taken
                System.out.println("GAME ENDED");
                int numOfPlayers = gs.getNPlayers() - 1;
                for (int currentPlayer = 0; currentPlayer <= numOfPlayers; currentPlayer++) {
                    int scoreToAddOrSubtract = gs.calculateDestinationCardPoints(currentPlayer);
                    System.out.println("scoreToAddOrSubtract: " + scoreToAddOrSubtract);
                    gs.addScore(currentPlayer,scoreToAddOrSubtract);


                }


                endGame(gs);

            }
        }


        endPlayerTurn(gs);

    }



}
