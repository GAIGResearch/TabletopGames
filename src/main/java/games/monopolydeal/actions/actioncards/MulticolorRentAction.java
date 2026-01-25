package games.monopolydeal.actions.actioncards;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IExtendedSequence;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.actions.ActionState;
import games.monopolydeal.actions.informationcontainer.ChoosePropertySet;
import games.monopolydeal.actions.informationcontainer.RentOf;
import games.monopolydeal.actions.informationcontainer.TargetPlayer;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.MonopolyDealCard;
import games.monopolydeal.cards.PropertySet;
import games.monopolydeal.cards.SetType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p> MulticolorRentAction is a single target rent action. It uses this EAS for collecting the required information and 'PayRent' EAS for final execution of the Multicolor Rent action card.
 * <ol>
 *     <li>Action card : Collect rent of any property on the player's board from a chosen player</li>
 *     <li>Execution description:
 *     <ul>
 *         <li>Initial 'execute' call : The action card is played onto the discard pile</li>
 *         <li>actionState 'Target' : The targeted player is chosen and action state is forwarded to 'ChoosePropertySet'</li>
 *         <li>actionState 'ChoosePropertySet' : The property set whose rent is to be charged is chosen and action state is forwarded to 'GetReaction'</li>
 *         <li>actionState 'GetReaction' : The targeted player has the option of denying the action by using JustSayNo. The action state is forwarded to either 'CollectRent' or 'ReactToReaction'</li>
 *         <li>actionState 'ReactToReaction' : A JustSayNo can be played on top of a JustSayNo to force execution. The opponent can also play a JustSayNo on top of this JustSayNo, so a loop of GetReaction and ReactToReaction is formed until either the action is denied or executed.</li>
 *         <li>actionState 'CollectRent' : A 'PayRent' EAS is called for the execution of the rent.</li>
 *     </ul></li>
 * </ol>
 * </p>
 */
public class MulticolorRentAction extends AbstractAction implements IExtendedSequence, IActionCard {
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
    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
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
                    if(pSet.getSetType() != SetType.UNDEFINED && pSet.getPropertySetSize()>0){
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
    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        if(actionState == ActionState.GetReaction) return target;
        else return playerID;
    }
    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
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
    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyDealGameState MDGS = (MonopolyDealGameState) gs;
        MDGS.discardCard(CardType.MulticolorRent,playerID);
        for(int i=0;i<doubleTheRent;i++) MDGS.discardCard(CardType.DoubleTheRent,playerID);
        MDGS.useAction(1 + doubleTheRent);
        gs.setActionInProgress(this);
        return true;
    }
    @Override
    public MulticolorRentAction copy() {
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
        return playerID == that.playerID && doubleTheRent == that.doubleTheRent && target == that.target &&
                rent == that.rent && actionState == that.actionState && reaction == that.reaction &&
                executed == that.executed;
    }
    @Override
    public int hashCode() {
        return Objects.hash(playerID, doubleTheRent, target, rent, actionState, reaction, executed);
    }
    @Override
    public String toString() {
        if(doubleTheRent > 0)
            return "Multicolor Rent with " + doubleTheRent + " Double the rent";
        else
            return "Multicolor Rent action";
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    public int getTarget(MonopolyDealGameState gs) {
        return target;
    }
}