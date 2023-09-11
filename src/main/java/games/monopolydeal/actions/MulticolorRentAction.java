package games.monopolydeal.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.MonopolyDealCard;
import games.monopolydeal.cards.PropertySet;
import games.monopolydeal.cards.SetType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>The extended actions framework supports 2 use-cases: <ol>
 *     <li>A sequence of decisions required to complete an action (e.g. play a card in a game area - which card? - which area?).
 *     This avoids very large action spaces in favour of more decisions throughout the game (alternative: all unit actions
 *     with parameters supplied at initialization, all combinations of parameters computed beforehand).</li>
 *     <li>A sequence of actions triggered by specific decisions (e.g. play a card which forces another player to discard a card - other player: which card to discard?)</li>
 * </ol></p>
 * <p>Extended actions should implement the {@link IExtendedSequence} interface and appropriate methods, as detailed below.</p>
 * <p>They should also extend the {@link AbstractAction} class, or any other core actions. As such, all guidelines in {@link MonopolyDealAction} apply here as well.</p>
 */
public class MulticolorRentAction extends AbstractAction implements IExtendedSequence {

    // The extended sequence usually keeps record of the player who played this action, to be able to inform the game whose turn it is to make decisions
    final int playerID;
    final int doubleTheRent;
    int target;
    int rent;
    ActionState actionState;
    boolean reaction = false;
    boolean executed = false;
    public MulticolorRentAction(int playerID, int doubleTheRent) {
        this.playerID = playerID;
        this.doubleTheRent = doubleTheRent;
        actionState = ActionState.Target;
    }

    /**
     * Forward Model delegates to this from {@link core.StandardForwardModel#computeAvailableActions(AbstractGameState)}
     * if this Extended Sequence is currently active.
     *
     * @param state The current game state
     * @return the list of possible actions for the {@link AbstractGameState#getCurrentPlayer()}.
     * These may be instances of this same class, with more choices between different values for a not-yet filled in parameter.
     */
    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        // TODO populate this list with available actions
        MonopolyDealGameState MDGS = (MonopolyDealGameState) state;
        List<AbstractAction> availableActions = new ArrayList<>();

        switch (actionState){
            case Target:
                for(int i=0;i<MDGS.getNPlayers();i++){
                    if(playerID!=i)
                        availableActions.add(new TargetPlayer(i));
                }
                break;
            case ChoosePropertySet:
                for (PropertySet pSet: MDGS.getPropertySets(playerID)) {
                    if(pSet.getSetType() != SetType.UNDEFINED){
                        availableActions.add(new RentOf(pSet));
                    }
                }
                break;
            case GetReaction:
                availableActions.add(new DoNothing());
                if(MDGS.CheckForJustSayNo(target)) availableActions.add(new JustSayNoAction());
                break;
            case ReactToReaction:
                availableActions.add(new DoNothing());
                if(MDGS.CheckForJustSayNo(playerID)) availableActions.add(new JustSayNoAction());
                break;
            case CollectRent:
                if(MDGS.isBoardEmpty(target)) {
                    availableActions.add(new DoNothing());
                } else {
                    availableActions.add(new PayRent(target,playerID,rent));
                }
                break;
        }
        return availableActions;
    }

    /**
     * TurnOrder delegates to this from {@link core.turnorders.TurnOrder#getCurrentPlayer(AbstractGameState)}
     * if this Extended Sequence is currently active.
     *
     * @param state The current game state
     * @return The player ID whose move it is.
     */
    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        if(actionState == ActionState.GetReaction) return target;
        else return playerID;
    }

    /**
     * <p>This is called by ForwardModel whenever an action is about to be taken. It enables the IExtendedSequence
     * to maintain local state in whichever way is most suitable.</p>
     *
     * <p>After this call, the state of IExtendedSequence should be correct ahead of the next decision to be made.
     * In some cases, there is no need to implement anything in this method - if for example you can tell if all
     * actions are complete from the state directly, then that can be implemented purely in {@link #executionComplete(AbstractGameState)}</p>
     *
     * @param state The current game state
     * @param action The action about to be taken (so the game state has not yet been updated with it)
     */
    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        // TODO: Process the action that was taken.
        switch (actionState){
            case Target:
                target = ((TargetPlayer) action).target;
                actionState = ActionState.ChoosePropertySet;
                break;
            case ChoosePropertySet:
                rent = (int) ((((RentOf) action).rent) * Math.pow(2,doubleTheRent));
                actionState = ActionState.GetReaction;
                break;
            case GetReaction:
                if(action instanceof JustSayNoAction) actionState = ActionState.ReactToReaction;
                else actionState = ActionState.CollectRent;
                break;
            case  ReactToReaction:
                if(action instanceof JustSayNoAction) actionState = ActionState.GetReaction;
                else executed = true;
                break;
            case CollectRent:
                executed = true;
                break;
        }
    }
    /**
     * @param state The current game state
     * @return True if this extended sequence has now completed and there is nothing left to do.
     */
    @Override
    public boolean executionComplete(AbstractGameState state) {
        // TODO is execution of this sequence of actions complete?
        return executed;
    }

    /**
     * <p>Executes this action, applying its effect to the given game state. Can access any component IDs stored
     * through the {@link AbstractGameState#getComponentById(int)} method.</p>
     * <p>In extended sequences, this function makes a call to the
     * {@link AbstractGameState#setActionInProgress(IExtendedSequence)} method with the argument <code>`this`</code>
     * to indicate that this action has multiple steps and is now in progress. This call could be wrapped in an <code>`if`</code>
     * statement if sometimes the action simply executes an effect in one step, or all parameters have values associated.</p>
     * @param gs - game state which should be modified by this action.
     * @return - true if successfully executed, false otherwise.
     */
    @Override
    public boolean execute(AbstractGameState gs) {
        // TODO: Some functionality applied which changes the given game state.
        MonopolyDealGameState MDGS = (MonopolyDealGameState) gs;
        MDGS.discardCard(MonopolyDealCard.create(CardType.MulticolorRent),playerID);
        for(int i=0;i<doubleTheRent;i++) MDGS.discardCard(MonopolyDealCard.create(CardType.DoubleTheRent),playerID);
        MDGS.useAction(1 + doubleTheRent);
        gs.setActionInProgress(this);
        return true;
    }

    /**
     * @return Make sure to return an exact <b>deep</b> copy of the object, including all of its variables.
     * Make sure the return type is this class (e.g. GTAction) and NOT the super class AbstractAction.
     * <p>If all variables in this class are final or effectively final (which they should be),
     * then you can just return <code>`this`</code>.</p>
     */
    @Override
    public MulticolorRentAction copy() {
        // TODO: copy non-final variables appropriately
        MulticolorRentAction action = new MulticolorRentAction(playerID,doubleTheRent);
        action.target = target;
        action.rent = rent;
        action.actionState = actionState;
        action.reaction = reaction;
        action.executed = executed;
        return action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MulticolorRentAction that = (MulticolorRentAction) o;
        return playerID == that.playerID && doubleTheRent == that.doubleTheRent && target == that.target && rent == that.rent && reaction == that.reaction && executed == that.executed && actionState == that.actionState;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, doubleTheRent, target, rent, actionState, reaction, executed);
    }

    @Override
    public String toString() {
        // TODO: Replace with appropriate string, including any action parameters
        if(doubleTheRent > 0)
            return "Multicolor Rent with " + doubleTheRent + " Double the rent";
        else
            return "Multicolor Rent action";
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
}
