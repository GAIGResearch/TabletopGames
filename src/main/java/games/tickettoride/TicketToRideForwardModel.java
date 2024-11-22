package games.tickettoride;

import core.AbstractGameData;
import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.*;
import core.properties.Property;
import core.properties.PropertyBoolean;
import core.properties.PropertyInt;
import core.properties.PropertyString;

import java.util.*;

import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
import static core.CoreConstants.playerHandHash;
import static games.tickettoride.TicketToRideConstants.*;

import games.tickettoride.actions.ClaimRoute;
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
        AbstractGameData _data = new AbstractGameData();
        _data.load(tp.getDataPath());

        for (int i = 0; i < state.getNPlayers(); i++) {
            Area playerArea = new Area(i, "Player Area");
            //Player Train Card hand setup
            Deck<Card> playerTrainCardHand = new Deck<>("Player Train Card Hand", VISIBLE_TO_ALL);
            playerTrainCardHand.setOwnerId(i);
            playerArea.putComponent(playerHandHash, playerTrainCardHand);

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


        trainCardDeck.shuffle(firstState.getRnd());
        gameArea.putComponent(TicketToRideConstants.trainCardDeckHash, trainCardDeck);

        state.addComponents();


        //draw initial cards
        for (int i = 0; i < state.getNPlayers(); i++) {
            Area playerArea = state.getArea(i);
            Deck<Card> playerTrainCardHandDeck = (Deck<Card>) playerArea.getComponent(playerHandHash);
            for (int j = 0; j < tp.nInitialTrainCards; j++) {
                new DrawCard(trainCardDeck.getComponentID(), playerTrainCardHandDeck.getComponentID()).execute(state);
            }

            //System.out.println("Setup: player " + i + " has " + playerTrainCardHandDeck);
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
        //System.out.println(playerId + " in compute action");
        actions.add(new DrawTrainCards(playerId));

        List<Edge> routesAvailableToBuy = checkRoutesAvailable(gameState);
        if (routesAvailableToBuy.size() > 1) {
            Property trainCardsRequiredProp = routesAvailableToBuy.get(0).getProperty(trainCardsRequiredHashKey);
            Property colorProp = routesAvailableToBuy.get(0).getProperty(colorHashKey);
            int trainCardsRequired = ((PropertyInt) trainCardsRequiredProp).value;
            String colorOfRoute = ((PropertyString) colorProp).value;
            actions.add(new ClaimRoute(routesAvailableToBuy.get(0),playerId, colorOfRoute, trainCardsRequired));
        }



        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {

        if (currentState.isActionInProgress()) return;
        TicketToRideGameState gs = (TicketToRideGameState) currentState;
        TicketToRideParameters params = (TicketToRideParameters) gs.getGameParameters();

        endPlayerTurn(gs);



    }

    List<Edge> checkRoutesAvailable(AbstractGameState gameState) {
        TicketToRideGameState tg = (TicketToRideGameState) gameState;
        List<Edge> routesAvailableToBuy = new ArrayList<>();

        Area gameArea = tg.getArea(-1);
        GraphBoardWithEdges world = (GraphBoardWithEdges) gameArea.getComponent(ticketToRideBoardHash);

        Collection<BoardNodeWithEdges> boardNodes = (Collection<BoardNodeWithEdges>) world.getBoardNodes();

        System.out.println(boardNodes.size());

        int routeClaimedHashKey = Hash.GetInstance().hash("routeClaimed");

        for ( BoardNodeWithEdges b : boardNodes){
            Map<Edge, BoardNodeWithEdges> boardNodeMapping = b.getNeighbourEdgeMapping();
            System.out.println("Current BoardNode: " + b.getComponentName());


            for (Map.Entry<Edge, BoardNodeWithEdges> entry : boardNodeMapping.entrySet()) {
                Edge edge = entry.getKey();
                BoardNodeWithEdges neighbourNode = entry.getValue();

                System.out.println("Edge between " + b.getComponentName() + " and " + neighbourNode.getComponentName());

                Property routeClaimedProp = edge.getProperty(routeClaimedHashKey);

                HashMap<Integer, Property> allProps = edge.getProperties();

                if (routeClaimedProp instanceof PropertyBoolean) {
                    boolean routeClaimed = ((PropertyBoolean) routeClaimedProp).value;

                    if (routeClaimed) {
                        System.out.println("This route is already claimed.");

                    } else {
                        boolean canAffordRoute = checkPlayerCanAffordRoute(tg, edge);
                        if (canAffordRoute){
                            routesAvailableToBuy.add(edge);
                        }
                        System.out.println("This route is available.");
                    }
                }
            }

        }

        return routesAvailableToBuy;

    }

    boolean checkPlayerCanAffordRoute(AbstractGameState gameState, Edge edge) {
        TicketToRideGameState tg = (TicketToRideGameState) gameState;
        int trainCardsRequiredHashKey = Hash.GetInstance().hash("trainCardsRequired");
        int colorHashKey = Hash.GetInstance().hash("color");
        Area gameArea = tg.getArea(-1);


        Property trainCardsRequiredProp = edge.getProperty(trainCardsRequiredHashKey);
        Property colorProp = edge.getProperty(colorHashKey);
        if (trainCardsRequiredProp instanceof PropertyInt) {
            int trainCardsRequired = ((PropertyInt) trainCardsRequiredProp).value;
            int playerID = tg.getCurrentPlayer();
            Deck<Card> playerTrainCardHandDeck = (Deck<Card>) tg.getComponentActingPlayer(playerID,playerHandHash);

            Map<String, Integer>  playerTrainCards = this.getTrainCarAmounts(playerTrainCardHandDeck);
            if (colorProp instanceof PropertyString) {
                String colorOfRoute = ((PropertyString) colorProp).value;
                int numberOfRequiredColor = playerTrainCards.getOrDefault(colorOfRoute, 0);
                System.out.println("Train Cards Required: " + trainCardsRequired);
                System.out.println("Player has Train Cards: " + numberOfRequiredColor);
                if (numberOfRequiredColor >= trainCardsRequired) {
                    System.out.println("Player ID " + playerID + " can buy this route");
                    return true;
                } else {
                    System.out.println("Player ID " + playerID + " cant buy this route");
                    return false;
                }
            }
        }
        return  false;
    }

    Map<String, Integer> getTrainCarAmounts(Deck trainCards){

        Map<String, Integer> trainCardCount = new HashMap<>();

        for (Object card : trainCards) {
            String cardColor = card.toString();
            trainCardCount.put(cardColor, trainCardCount.getOrDefault(cardColor, 0) + 1);
        }

        return trainCardCount;
    }


}
