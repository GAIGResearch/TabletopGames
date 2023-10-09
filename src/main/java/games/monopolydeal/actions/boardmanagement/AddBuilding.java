package games.monopolydeal.actions.boardmanagement;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.cards.MonopolyDealCard;
import games.monopolydeal.cards.SetType;

import java.util.Objects;

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
public class AddBuilding extends AbstractAction {
    final int player;
    final MonopolyDealCard card;
    final SetType setType;
    public AddBuilding(MonopolyDealCard card, int playerId, SetType setType) {
        this.card = card;
        this.setType = setType;
        player = playerId;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyDealGameState state = (MonopolyDealGameState) gs;
        state.removeCardFromHand(player, card);
        // For debugging
        if(setType!=SetType.UNDEFINED){
            int i=0;
        }
        state.addPropertyToSet(player,card,setType);
        state.useAction(1);
        return true;
    }
    @Override
    public AddBuilding copy() {
        return new AddBuilding(card,player,setType);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddBuilding that = (AddBuilding) o;
        return player == that.player && Objects.equals(card, that.card) && setType == that.setType;
    }
    @Override
    public int hashCode() {
        return Objects.hash(player, card, setType);
    }
    @Override
    public String toString() {
        return "Add " + card.toString() + " to " + setType;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
