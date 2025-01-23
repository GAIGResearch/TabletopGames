package games.tickettoride.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Component;
import core.components.Deck;
import core.components.Edge;
import core.properties.Property;
import core.properties.PropertyBoolean;
import core.properties.PropertyColor;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.tickettoride.TicketToRideGameState;
import games.tickettoride.TicketToRideParameters;
import utilities.Hash;

import static core.CoreConstants.colorHash;
import static core.CoreConstants.playerHandHash;

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



    public ClaimRoute(Edge edge, int playerID, String colorOfRoute, int costOfRoute) {
        this.edge = edge;
        this.playerID = playerID;
        this.colorOfRoute = colorOfRoute;
        this.costOfRoute = costOfRoute;

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
        int amountToRemove = costOfRoute;

        System.out.println("PLAYER " + playerID + " CLAIMING ROUTE" );

        Deck<Card> playerTrainCardHandDeck = (Deck<Card>) tgs.getComponentActingPlayer(playerID,playerHandHash);

        System.out.println("player hand before " + playerTrainCardHandDeck);

        for(int i = 0; i < playerTrainCardHandDeck.getSize() && amountToRemove > 0; i ++){
            Card currentCard = playerTrainCardHandDeck.get(i);
            if (currentCard.toString().equals(colorOfRoute)){
                playerTrainCardHandDeck.remove(i);
                i--;
                amountToRemove--;
            }
        }
        System.out.println("player train cars before " + tgs.getTrainCars(playerID));
        tgs.deductTrainCars(playerID, costOfRoute);
        System.out.println("player train cars after  " + tgs.getTrainCars(playerID));
        Property routeClaimedProp = new PropertyBoolean("routeClaimed", (Boolean) true);
        edge.setProperty(routeClaimedProp );


        System.out.println(tgs.getGameScore(playerID) + " points before");
        tgs.addScore(playerID, tp.getPointsPerRoute(costOfRoute));
        System.out.println("Claim route of color: " + colorOfRoute + " and size of " + costOfRoute);
        System.out.println(tgs.getGameScore(playerID) + " points after");
        System.out.println("player hand after " + playerTrainCardHandDeck);

        return true;
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
        // TODO: Replace with appropriate string, including any action parameters
        return "My action name";
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
