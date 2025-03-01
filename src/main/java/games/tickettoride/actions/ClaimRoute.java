package games.tickettoride.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Component;
import core.components.Deck;
import core.components.Edge;
import core.properties.*;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.tickettoride.TicketToRideGameState;
import games.tickettoride.TicketToRideParameters;
import utilities.Hash;

import java.util.HashMap;
import java.util.Map;

import static core.CoreConstants.colorHash;
import static core.CoreConstants.playerHandHash;
import static games.tickettoride.TicketToRideConstants.*;

/**
 * <p>Actions are unit things players can do in the game (e.g. play a card, move a pawn, roll dice, attack etc.).</p>
 * <p>Actions in the game can (and should, if applicable) extend one of the other existing actions, in package {@link core.actions}.
 * Or, a game may simply reuse one of the existing core actions.</p>
 * <p>Actions may have parameters, so as not to duplicate actions for the same type of functionality,
 * e.g. playing card of different types (see {@link games.sushigo.actions.ChooseCard} action from SushiGo as an example).
 * Include these parameters in the class constructor.</p>
 * <p>They need to extend at a minimum the {@link AbstractAction} super class and implement the {@link AbstractAction#execute(AbstractGameState)} method.
 * This is where the main functionality of the action should be inserted, which modifies the given game state appropriately (e.g. if the action is to play a card,
 * then the card will be moved from the player's hand to the discard pile, and the card's effect will be applied).</p>
 * <p>They also need to include {@link Object#equals(Object)} and {@link Object#hashCode()} methods.</p>
 * <p>They <b>MUST NOT</b> keep references to game components. Instead, store the {@link Component#getComponentID()}
 * in variables for any components that must be referenced in the action. Then, in the execute() function,
 * use the {@link AbstractGameState#getComponentById(int)} function to retrieve the actual reference to the component,
 * given your componentID.</p>
 */
public class ClaimRoute extends AbstractAction {


    public final Edge edge;
    public final int playerID;
    public final String colorOfRoute;
    public final int costOfRoute;
    public final int indexOfColor;



    public ClaimRoute(Edge edge, int playerID, String colorOfRoute, int costOfRoute, int indexOfColor) {
        this.edge = edge;
        this.playerID = playerID;
        this.colorOfRoute = colorOfRoute;
        this.costOfRoute = costOfRoute;
        this.indexOfColor = indexOfColor;

    }
    /**
     * Executes this action, applying its effect to the given game state. Can access any component IDs stored
     * through the {@link AbstractGameState#getComponentById(int)} method.
     * @param gs - game state which should be modified by this action.
     * @return - true if successfully executed, false otherwise.
     */
    @Override
    public boolean execute(AbstractGameState gs) {
        TicketToRideGameState tgs = (TicketToRideGameState) gs;
        TicketToRideParameters tp = (TicketToRideParameters) gs.getGameParameters();

        int amountToRemove = costOfRoute; //may also change because they can supplement with locomotive
        int amountOfLocomotiveToRemove = 0;

        String colorOfRouteToRemove = colorOfRoute; // could change incase of gray route

        System.out.println("PLAYER " + playerID + " CLAIMING ROUTE" );
        System.out.println("route properties before : " + edge.getProperties());
        Deck<Card> playerTrainCardHandDeck = (Deck<Card>) tgs.getComponentActingPlayer(playerID,playerHandHash);
        Deck<Card> trainCardDiscardDeck = (Deck<Card>) tgs.getComponent(trainCardDeckDiscardHash);

        System.out.println("player hand before " + playerTrainCardHandDeck);

        Map<String, Integer>  playerTrainCards = this.getTrainCarCardAmounts(playerTrainCardHandDeck);

        int currentAmountOfLocomotivesInHand = playerTrainCards.getOrDefault("Locomotive",0);
        //when its gray colour route, any colour can be used
        if (colorOfRoute.equals("Gray")) {
            // Find the first color that has enough cards (excluding wildcards for now)
            for (String color : playerTrainCards.keySet()) {
                if (playerTrainCards.get(color)  >= costOfRoute) { //correct amount
                    colorOfRouteToRemove = color;  // Use this color to claim the route
                    System.out.println("in gray route claiming route, removing " + colorOfRouteToRemove);
                    break;
                } else if ((playerTrainCards.get(color) + currentAmountOfLocomotivesInHand >= costOfRoute) && !color.equals("Locomotive")) { //has enough locomotive to finish route
                    colorOfRouteToRemove = color;  // Use this color to claim the route
                    int currentColorAmount = playerTrainCards.get(color);
                    amountOfLocomotiveToRemove = costOfRoute - currentColorAmount;
                    amountToRemove = currentColorAmount;
                    System.out.println("in gray route claiming route with locomotive, removing # of " + amountToRemove + colorOfRouteToRemove + " with # of locomotives: " + amountOfLocomotiveToRemove);
                    break;
                }
            }
            removeTrainCarCards(playerTrainCardHandDeck, amountToRemove, colorOfRouteToRemove, amountOfLocomotiveToRemove,trainCardDiscardDeck);
        } else if (playerTrainCards.getOrDefault(colorOfRoute,0)  >= costOfRoute){ //color of route in hand has correct amount
            System.out.println("color of route in hand has correct amount");
            removeTrainCarCards(playerTrainCardHandDeck, costOfRoute, colorOfRoute, amountOfLocomotiveToRemove,trainCardDiscardDeck);

        } else if (playerTrainCards.getOrDefault(colorOfRoute,0) +  currentAmountOfLocomotivesInHand >= costOfRoute){ //use locomotive to supplement
            int currentColorAmount = playerTrainCards.getOrDefault(colorOfRouteToRemove,0);
            amountOfLocomotiveToRemove = costOfRoute - currentColorAmount;
            amountToRemove = currentColorAmount;
            System.out.println("Using Locomotive to claim route");
            removeTrainCarCards(playerTrainCardHandDeck, amountToRemove, colorOfRouteToRemove, amountOfLocomotiveToRemove, trainCardDiscardDeck);
        }

        {

        }



        tgs.deductTrainCars(playerID, costOfRoute);



        //Needed to differentiate which route they used (if theres 2 diff colours on same route)
        if (indexOfColor == 0){
            Property claimedByPlayerProp = new PropertyInt("claimedByPlayerRoute1", (Integer) playerID);
            edge.setProperty(claimedByPlayerProp);
        } else if (indexOfColor == 1) {
            Property claimedByPlayerProp = new PropertyInt("claimedByPlayerRoute2", (Integer) playerID);
            edge.setProperty(claimedByPlayerProp);
        }

        Property claimedByPlayerRoute1Prop = edge.getProperty(claimedByPlayerRoute1Hash);
        Property claimedByPlayerRoute2Prop = edge.getProperty(claimedByPlayerRoute2Hash);

        Property routeClaimedProp = new PropertyBoolean("routeClaimed", (Boolean) true);

        int claimedByPlayerRoute1 = ((PropertyInt) claimedByPlayerRoute1Prop).value;
        System.out.println("route1 claimed: " + claimedByPlayerRoute1);
        int claimedByPlayerRoute2 = -2;
        if (claimedByPlayerRoute2Prop != null){
            claimedByPlayerRoute2  = ((PropertyInt) claimedByPlayerRoute2Prop).value;
        }
        System.out.println("route2 claimed: " + claimedByPlayerRoute2);


        //note to self: gray ones not setting properly cuz its always editing route1
        if (tgs.getNPlayers() >= 4){ //in 4+ players, double routes open up
            System.out.println(tgs.getNPlayers() + " more than 4 players");
            if (claimedByPlayerRoute1 != -1 && claimedByPlayerRoute2 != -1) { //if both routes taken and double route enabled, its now completely closed off
                edge.setProperty(routeClaimedProp);
                System.out.println(" double route claimed more than 4 players");
            } else {
                System.out.println(" double route not claimed more than 4 players");
            }
        } else{
            System.out.println("less than 4 players");
            edge.setProperty(routeClaimedProp);
        }


        System.out.println(tgs.getGameScore(playerID) + " points before");
        tgs.addScore(playerID, tp.getPointsPerRoute(costOfRoute));
        System.out.println("Claim route of color: " + colorOfRoute + " and size of " + costOfRoute);
        System.out.println(tgs.getGameScore(playerID) + " points after");
        System.out.println("player hand after " + playerTrainCardHandDeck);

        System.out.println("route properties after : " + edge.getProperties());

        return true;
    }


    Map<String, Integer> getTrainCarCardAmounts(Deck<Card> trainCards){

        Map<String, Integer> trainCardCount = new HashMap<>();

        for (Object card : trainCards) {
            String cardColor = card.toString();
            trainCardCount.put(cardColor, trainCardCount.getOrDefault(cardColor, 0) + 1);
        }


        return trainCardCount;
    }

    void removeTrainCarCards(Deck<Card> playerTrainCardHandDeck ,int amountToRemove, String colorOfRouteToRemove, int amountOfLocomotivesToRemove, Deck<Card> trainCardDiscardDeck){

        for (int i = 0; i < playerTrainCardHandDeck.getSize() && amountToRemove > 0; i ++) { //remove colour cards
            Card currentCard = playerTrainCardHandDeck.get(i);
            if (currentCard.toString().equals(colorOfRouteToRemove)){
                trainCardDiscardDeck.add(currentCard);
                playerTrainCardHandDeck.remove(i);

                i--;
                amountToRemove--;
            }
        }

        for (int i = 0; i < playerTrainCardHandDeck.getSize() && amountOfLocomotivesToRemove > 0; i++) { //remove locomotives
            Card currentCard = playerTrainCardHandDeck.get(i);
            if (currentCard.toString().equals("Locomotive")) {
                trainCardDiscardDeck.add(currentCard);
                playerTrainCardHandDeck.remove(i);
                i--;
                amountOfLocomotivesToRemove--;
            }
        }


    }
    /**
     * @return Make sure to return an exact <b>deep</b> copy of the object, including all of its variables.
     * Make sure the return type is this class (e.g. GTAction) and NOT the super class AbstractAction.
     * <p>If all variables in this class are final or effectively final (which they should be),
     * then you can just return <code>`this`</code>.</p>
     */
    @Override
    public ClaimRoute copy() {
        // TODO: copy non-final variables appropriately
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        // TODO: compare all other variables in the class
        return obj instanceof ClaimRoute;
    }

    @Override
    public int hashCode() {
        // TODO: return the hash of all other variables in the class
        return 0;
    }

    @Override
    public String toString() {
        Property nodeProp = edge.getProperty(nodesHash);
        String[] nodes = ((PropertyStringArray) nodeProp).getValues();
        return "Claimed route between " + nodes[0] + " and " + nodes[1] + " of color: " + colorOfRoute + " and size of " + costOfRoute ;
    }

    /**
     * @param gameState - game state provided for context.
     * @return A more descriptive alternative to the toString action, after access to the game state to e.g.
     * retrieve components for which only the ID is stored on the action object, and include the name of those components.
     * Optional.
     */
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }


    /**
     * This next one is optional.
     *
     *  May optionally be implemented if Actions are not fully visible
     *  The only impact this has is in the GUI, to avoid this giving too much information to the human player.
     *
     *  An example is in Resistance or Sushi Go, in which all cards are technically revealed simultaneously,
     *  but the game engine asks for the moves sequentially. In this case, the action should be able to
     *  output something like "Player N plays card", without saying what the card is.
     * @param gameState - game state to be used to generate the string.
     * @param playerId - player to whom the action should be represented.
     * @return
     */
   // @Override
   // public String getString(AbstractGameState gameState, int playerId);
}
