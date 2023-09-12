package games.monopolydeal.actions.boardmanagement;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.actions.informationcontainer.MoveCardFromTo;
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
public class ModifyBoard extends AbstractAction implements IExtendedSequence {

    // The extended sequence usually keeps record of the player who played this action, to be able to inform the game whose turn it is to make decisions
    final int playerID;
    boolean executed;

    public ModifyBoard(int playerID) { this.playerID = playerID; }

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
        // move card from to
        // Iterate through sets
        //   find wilds
        //      get alternate
        //      MoveFromTo
        List<AbstractAction> availableActions = new ArrayList<>();
        for (PropertySet pSet: MDGS.getPropertySets(playerID)) {
            if(pSet.hasWild && !pSet.hasHouse){
                for (int i=0;i<pSet.getSize();i++) {
                    MonopolyDealCard card = pSet.get(i);
                    if(card.isPropertyWildCard()){
                        SetType sType = card.getAlternateSetType(card);
                        if(sType==SetType.UNDEFINED){
                            for (PropertySet propSet:MDGS.getPropertySets(playerID)) {
                                if(propSet.getSetType() != card.getUseAs() && !availableActions.contains(new MoveCardFromTo(playerID,card,pSet.getSetType(),propSet.getSetType()))){
                                    availableActions.add(new MoveCardFromTo(playerID,card,pSet.getSetType(),propSet.getSetType()));
                                }
                            }
                        }
                        if(!availableActions.contains(new MoveCardFromTo(playerID,card,pSet.getSetType(),sType)))
                            availableActions.add(new MoveCardFromTo(playerID,card,pSet.getSetType(),sType));
                    }
                }
            } else if (pSet.isComplete && pSet.hasHouse && !pSet.hasHotel) { // Moving House
                for (PropertySet propSet: MDGS.getPropertySets(playerID)) {
                    if(propSet!= pSet && propSet.isComplete && !propSet.hasHouse){
                        if(!availableActions.contains(new MoveCardFromTo(playerID,MonopolyDealCard.create(CardType.House),pSet.getSetType(),propSet.getSetType())))
                            availableActions.add(new MoveCardFromTo(playerID,MonopolyDealCard.create(CardType.House),pSet.getSetType(),propSet.getSetType()));
                    }
                }
                if(!availableActions.contains(new MoveCardFromTo(playerID,MonopolyDealCard.create(CardType.House),pSet.getSetType(),SetType.UNDEFINED)))
                    availableActions.add(new MoveCardFromTo(playerID,MonopolyDealCard.create(CardType.House),pSet.getSetType(),SetType.UNDEFINED));
            }else if (pSet.isComplete && pSet.hasHouse && pSet.hasHotel){ // Moving Hotel
                for (PropertySet propSet: MDGS.getPropertySets(playerID)) {
                    if(propSet!= pSet && propSet.isComplete && propSet.hasHouse && !propSet.hasHotel){
                        if(!availableActions.contains(new MoveCardFromTo(playerID,MonopolyDealCard.create(CardType.Hotel),pSet.getSetType(),propSet.getSetType())))
                            availableActions.add(new MoveCardFromTo(playerID,MonopolyDealCard.create(CardType.Hotel),pSet.getSetType(),propSet.getSetType()));
                    }
                }
                if(!availableActions.contains(new MoveCardFromTo(playerID,MonopolyDealCard.create(CardType.Hotel),pSet.getSetType(),SetType.UNDEFINED)))
                    availableActions.add(new MoveCardFromTo(playerID,MonopolyDealCard.create(CardType.Hotel),pSet.getSetType(),SetType.UNDEFINED));
            }
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
        return playerID;
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
        MoveCardFromTo actionTaken = (MoveCardFromTo) action;

        MonopolyDealCard card = actionTaken.card;
        SetType from = actionTaken.from;
        SetType to = actionTaken.to;

        MonopolyDealGameState MDGS = (MonopolyDealGameState) state;
        MDGS.removePropertyFrom(playerID,card,from);
        card.setUseAs(to);
        MDGS.addProperty(playerID,card);
        MDGS.modifyBoard();
        executed = true;

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
    public ModifyBoard copy() {
        // TODO: copy non-final variables appropriately
        ModifyBoard action = new ModifyBoard(playerID);
        action.executed = executed;
        return action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModifyBoard that = (ModifyBoard) o;
        return playerID == that.playerID && executed == that.executed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, executed);
    }

    @Override
    public String toString() {
        return "Modify Board";
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
