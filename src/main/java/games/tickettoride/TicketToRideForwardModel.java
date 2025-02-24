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

        if (playerTrainCardDeck.getSize() > 2){
            actions.add(new DrawTrainCards(playerId));
        }
        if (playerDestinationCardDeck.getSize() > 1){
            actions.add(new DrawDestinationTicketCards(playerId));
        }

        //System.out.println(playerId + " in compute action");


        HashMap<Edge, List<Integer>> routesAvailableToBuy = (HashMap<Edge, List<Integer>>) checkRoutesAvailable(gameState); //key of edges, index of which color to buy
        if (!routesAvailableToBuy.isEmpty()) {

            for (Map.Entry<Edge, List<Integer>> currentRoute : routesAvailableToBuy.entrySet()) { //add every route available to list of actions

                Edge currentEdge = currentRoute.getKey();


                Property trainCardsRequiredProp = currentEdge.getProperty(trainCardsRequiredHashKey);
                int trainCardsRequired = ((PropertyInt) trainCardsRequiredProp).value;

                List<Integer> indexsOfColors = routesAvailableToBuy.get(currentEdge);
                Property colorProp = currentEdge.getProperty(colorHashKey);
                String[] colorsOfRoute = ((PropertyStringArray) colorProp).getValues();
                for (int i = 0; i < indexsOfColors.size(); i++){
                    String colorOfRoute = colorsOfRoute[indexsOfColors.get(i)];
//                    System.out.println(colorOfRoute + " is the colors");
//
//                    System.out.println("In for loop for routes available to buy " + currentEdge.getProperties());

                    actions.add(new ClaimRoute(currentEdge,playerId, colorOfRoute, trainCardsRequired, i));
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
                System.out.println("Edge info: ");

                calculateDestinationCardPoints(gs);

                endGame(gs);

            }
        }


        endPlayerTurn(gs);

    }
    //return the edge and which integer of colour they can buy
    HashMap<Edge, List<Integer>> checkRoutesAvailable(AbstractGameState gameState) {
        TicketToRideGameState tg = (TicketToRideGameState) gameState;
        HashMap<Edge, List<Integer>> routesAvailableToBuy = new HashMap<Edge, List<Integer>>();

        Area gameArea = tg.getArea(-1);
        GraphBoardWithEdges world = (GraphBoardWithEdges) gameArea.getComponent(ticketToRideBoardHash);

        HashSet<Edge> boardEdges = (HashSet<Edge>) world.getBoardEdges();

        Collection<BoardNodeWithEdges> boardNodes = (Collection<BoardNodeWithEdges>) world.getBoardNodes();

        System.out.println(boardNodes.size());

        int routeClaimedHashKey = Hash.GetInstance().hash("routeClaimed");
        int nodesHashKey = Hash.GetInstance().hash("nodes");
        //goes through boardEdges list approach to find free edges - potentially faster
        for (Edge edge : boardEdges) {
            HashMap<Integer, Property> allProps = edge.getProperties();
            Property nodeProp = edge.getProperty(nodesHashKey);

            String nodes = Arrays.toString(((PropertyStringArray) nodeProp).getValues());

            System.out.println("Edge has nodes: " +  nodes);

            Property routeClaimedProp = edge.getProperty(routeClaimedHashKey);
            if (routeClaimedProp instanceof PropertyBoolean) {
                boolean routeClaimed = ((PropertyBoolean) routeClaimedProp).value;

                if (routeClaimed) {
                    System.out.println("This route is already claimed.");
                } else {
                    boolean[] routesPlayerCanAfford = checkPlayerCanAffordRoute(tg, edge);

                    boolean atleastOneAfforded = false;
                    List<Integer> colorIndexesAvailable = new ArrayList<>();
                    for (int i = 0; i < routesPlayerCanAfford.length; i++){ //allows the different colour options of 1 route
                        if (routesPlayerCanAfford[i]){
                            System.out.println("Route option is claimable " + i);
                            colorIndexesAvailable.add(i);
                            atleastOneAfforded = true;

                        }
                    }
                    routesAvailableToBuy.put(edge, colorIndexesAvailable);
                }
            }

        }


//        //goes through board node with edges approach to find free edges
//        for ( BoardNodeWithEdges b : boardNodes){
//            Map<Edge, BoardNodeWithEdges> boardNodeMapping = b.getNeighbourEdgeMapping();
//            System.out.println("Current BoardNode: " + b.getComponentName());
//
//
//            for (Map.Entry<Edge, BoardNodeWithEdges> entry : boardNodeMapping.entrySet()) {
//                Edge edge = entry.getKey();
//                BoardNodeWithEdges neighbourNode = entry.getValue();
//
//                System.out.println("Edge between " + b.getComponentName() + " and " + neighbourNode.getComponentName());
//
//                Property routeClaimedProp = edge.getProperty(routeClaimedHashKey);
//
//                HashMap<Integer, Property> allProps = edge.getProperties();
//
//                if (routeClaimedProp instanceof PropertyBoolean) {
//                    boolean routeClaimed = ((PropertyBoolean) routeClaimedProp).value;
//
//                    if (routeClaimed) {
//                        System.out.println("This route is already claimed.");
//
//                    } else {
//                        boolean canAffordRoute = checkPlayerCanAffordRoute(tg, edge);
//                        if (canAffordRoute){
//                            routesAvailableToBuy.add(edge);
//                        }
//                        System.out.println("This route is available.");
//                    }
//                }
//            }
//
//        }

        return routesAvailableToBuy;
    }
    //return the index of color (in edges array) that they can afford. If can't afford any, return -1
    boolean[] checkPlayerCanAffordRoute(AbstractGameState gameState, Edge edge) {
        TicketToRideGameState tg = (TicketToRideGameState) gameState;

        boolean[] listOfAvailable = new boolean[2]; //holds indexes of colors they can afford. if both are false, then none
        listOfAvailable[0] = false;
        listOfAvailable[1] = false;

        Property colorProp = edge.getProperty(colorHash);

        Property claimedByPlayerRoute1Prop = edge.getProperty(claimedByPlayerRoute1Hash);
        Property claimedByPlayerRoute2Prop = edge.getProperty(claimedByPlayerRoute2Hash);

        int claimedByPlayerRoute1 = ((PropertyInt) claimedByPlayerRoute1Prop).value;
        int claimedByPlayerRoute2 = -2;
        if (claimedByPlayerRoute2Prop != null) { //if one track route, this would not exist
            claimedByPlayerRoute2 = ((PropertyInt) claimedByPlayerRoute2Prop).value;
        }

        Area gameArea = tg.getArea(-1);


        Property trainCardsRequiredProp = edge.getProperty(trainCardsRequiredHash);


        if (trainCardsRequiredProp instanceof PropertyInt) {

            int trainCardsRequired = ((PropertyInt) trainCardsRequiredProp).value;
            int playerID = tg.getCurrentPlayer();
            Deck<Card> playerTrainCardHandDeck = (Deck<Card>) tg.getComponentActingPlayer(playerID,playerHandHash);


            System.out.println("In check can afford, needs # of train cards "  + trainCardsRequired );

            Map<String, Integer>  playerTrainCards = this.getTrainCarCardAmounts(playerTrainCardHandDeck);
            int currentAmountOfLocomotivesInHand = playerTrainCards.getOrDefault("Locomotive",0);
            if (colorProp instanceof PropertyStringArray) {
                String[] colorsOfRoute = (((PropertyStringArray) colorProp).getValues());
                System.out.println("colors of route: "  + colorsOfRoute );
                System.out.println("color of route: "  + colorsOfRoute[0] );

                if (colorsOfRoute[0].equals("Gray")){ //gray route claimable by any colour, so check every colour

                    for (String color : playerTrainCards.keySet()) {

                        int colorCount = playerTrainCards.get(color);

                        if ((colorCount + currentAmountOfLocomotivesInHand) >= trainCardsRequired) {
                            System.out.println("Gray route affordable " + color);
                            if (claimedByPlayerRoute1 == -1) {
                                listOfAvailable[0] = true;
                                System.out.println("Gray route route 1 able");
                            }
                            if (claimedByPlayerRoute2Prop != null){
                                if (claimedByPlayerRoute2 == -1) {
                                    listOfAvailable[1] = true;
                                    System.out.println("Gray route route 2 able");
                                }
                            }
                        }
                    }
                } else {

                    for (int i = 0; i < colorsOfRoute.length; i++){ //
                        int numberOfRequiredColor = playerTrainCards.getOrDefault(colorsOfRoute[i], 0);
                        System.out.println("Train Cards Required: " + trainCardsRequired + " for color " + colorsOfRoute[i] + " which is index " + i);
                        System.out.println("Player has Train Cards: " + numberOfRequiredColor);
                        if ((numberOfRequiredColor + currentAmountOfLocomotivesInHand) >= trainCardsRequired) {
                            System.out.println("Player ID " + playerID + " can buy this route of choice " + i);
                            listOfAvailable[i] = true;
                        } else {
                            System.out.println("Player ID " + playerID + " cant buy this route");
                        }
                    }
                }



            }
        }
        return listOfAvailable;
    }

    // Gives map of colours and the amount a player has of it
    Map<String, Integer> getTrainCarCardAmounts(Deck trainCards){

        Map<String, Integer> trainCardCount = new HashMap<>();

        for (Object card : trainCards) {
            String cardColor = card.toString();
            trainCardCount.put(cardColor, trainCardCount.getOrDefault(cardColor, 0) + 1);
        }

        return trainCardCount;
    }

    void calculateDestinationCardPoints(AbstractGameState gameState) {


        TicketToRideGameState tgs = (TicketToRideGameState) gameState;
        TicketToRideParameters tp = (TicketToRideParameters) gameState.getGameParameters();

        int numOfPlayers = tgs.getNPlayers() - 1;

        Area gameArea = tgs.getArea(-1);
        Deck<Card> destinationCardDeck = (Deck<Card>) gameArea.getComponent(TicketToRideConstants.destinationCardDeckHash);


        GraphBoardWithEdges world = (GraphBoardWithEdges) gameArea.getComponent(ticketToRideBoardHash);

        Collection<BoardNodeWithEdges> boardNodes = (Collection<BoardNodeWithEdges>) world.getBoardNodes();
        HashSet<Edge> boardEdges = (HashSet<Edge>) world.getBoardEdges();

        for (int currentPlayer = 0; currentPlayer <= numOfPlayers; currentPlayer++) {
            int scoreToAddOrSubtract = 0;  //based on if players completed or didnt complete destination ticket cards

            Deck<Card> playerDestinationCardHandDeck = (Deck<Card>) tgs.getComponentActingPlayer(currentPlayer,TicketToRideConstants.playerDestinationHandHash);
            Map<String, List<String>> graphForSearch = getSearchGraph(boardEdges, currentPlayer);
            //System.out.println("search graph:  "+ graphForSearch + " for player  " + currentPlayer);

            for (Card currentCard : playerDestinationCardHandDeck) {
                String location1 = String.valueOf(currentCard.getProperty(location1Hash));
                String location2 = String.valueOf(currentCard.getProperty(location2Hash));
                boolean isConnected = checkIfConnectedCity(graphForSearch,location1,location2);
                //System.out.println("IN FOR LOOP for cards "+ location1 + " " + location2 + " isConnected: " + isConnected);
                int pointsOnDestinationCard = ((PropertyInt)(currentCard.getProperty(pointsHash))).value;
                if (isConnected){
                    scoreToAddOrSubtract = scoreToAddOrSubtract + pointsOnDestinationCard;
                    //System.out.println("adding due to connected: " +  pointsOnDestinationCard);
                } else {
                    scoreToAddOrSubtract = scoreToAddOrSubtract - pointsOnDestinationCard; //not connected reduce score
                    //System.out.println("subtracting due to unconnected: " +  -pointsOnDestinationCard);
                }
            }
            System.out.println("total points due to destination ticket cards " + scoreToAddOrSubtract);
            tgs.addScore(currentPlayer, scoreToAddOrSubtract);

        }


        System.out.println(boardEdges.size() + " board edges size");
        System.out.println(" all  board edges" + boardEdges );
        for (Edge edge : boardEdges) {
            System.out.println("FINISHED GAME EDGE: "+ edge.getProperties());
        }



    }

    public Map<String, List<String>> getSearchGraph(HashSet<Edge> edges, int playerId) {
        Map<String, List<String>> searchGraph = new HashMap<>();

        for (Edge currentEdge : edges) {

            boolean routeClaimed = ((PropertyBoolean)(currentEdge.getProperty(routeClaimedHash))).value;
            //System.out.println(routeClaimed +" route claimed in getsearch");

            PropertyInt claimedByRoute1Prop =  (PropertyInt)(currentEdge.getProperty(claimedByPlayerRoute1Hash));
            PropertyInt claimedByRoute2Prop =  (PropertyInt)(currentEdge.getProperty(claimedByPlayerRoute2Hash));


            // some edges have 2 different routes (colors), so either can be null
            int claimedByRoute1 = -1;
            int claimedByRoute2 = -1;

            if (claimedByRoute1Prop != null) {
                claimedByRoute1 = claimedByRoute1Prop.value;
            }

            if (claimedByRoute2Prop != null) {
                claimedByRoute2 = claimedByRoute2Prop.value;
            }


            if ((routeClaimed && claimedByRoute1 == playerId) || (routeClaimed && claimedByRoute2 == playerId))  {

                Property nodeProp = currentEdge.getProperty(nodesHash);
                String[] nodes = ((PropertyStringArray) nodeProp).getValues();

                System.out.println("Edge has nodes om getsearch: " + Arrays.toString(nodes));

                String node1 = nodes[0];
                String node2 = nodes[1];

                searchGraph.putIfAbsent(node1, new ArrayList<>());
                searchGraph.putIfAbsent(node2, new ArrayList<>());
                searchGraph.get(node1).add(node2);
                searchGraph.get(node2).add(node1);
            }
        }

        return searchGraph;
    }

    public boolean checkIfConnectedCity(Map<String, List<String>> searchGraph, String location1, String location2) {
        Set<String> locationsVisited = new HashSet<>();
        return graphSearch(searchGraph, location1, location2, locationsVisited);
    }

    private boolean graphSearch(Map<String, List<String>> searchGraph, String currentLocation, String target, Set<String> locationsVisited) {
        if (currentLocation.equals(target)) {
            return true;
        }
        if (locationsVisited.contains(currentLocation)){
            return false;
        }

        locationsVisited.add(currentLocation);

        //Go through the adjacent locations of current location
        List<String> adjacentLocations = searchGraph.getOrDefault(currentLocation, new ArrayList<>());
        for (String currentAdjacentLocation : adjacentLocations) {
            if (graphSearch(searchGraph, currentAdjacentLocation, target, locationsVisited)) {
                return true;
            }
        }

        return false;
    }
}
