package games.monopolydeal.actions.actioncards;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IExtendedSequence;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.actions.ActionState;
import games.monopolydeal.actions.informationcontainer.ChoosePropertySet;
import games.monopolydeal.actions.informationcontainer.TargetPlayer;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.PropertySet;
import games.monopolydeal.cards.SetType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p> DealBreakerAction is a single target action. It uses this EAS for collecting the required information and execution of the Deal Breaker action card.
 * <ol>
 *     <li>Action card : Steal a complete property set</li>
 *     <li>Execution description:
 *     <ul>
 *         <li>Initial 'execute' call : The action card is played onto the discard pile</li>
 *         <li>actionState 'Target' : The targeted player is chosen and action state is forwarded to 'ChoosePropertySet'</li>
 *         <li>actionState 'ChoosePropertySet' : Which complete property set of said target is to be stolen is chosen and action state is forwarded to 'GetReaction'</li>
 *         <li>actionState 'GetReaction' : The targeted player has the option of denying the action by using JustSayNo. The action is either executed or action state is forwarded to 'ReactToReaction'</li>
 *         <li>actionState 'ReactToReaction' : A JustSayNo can be played on top of a JustSayNo to force execution. The opponent can also play a JustSayNo on top of this JustSayNo, so a loop of GetReaction and ReactToReaction is formed until either the action is denied or executed.</li>
 *     </ul></li>
 * </ol>
 * </p>
 */
public class DealBreakerAction extends AbstractAction implements IExtendedSequence {

    // The extended sequence usually keeps record of the player who played this action, to be able to inform the game whose turn it is to make decisions
    final int playerID;
    int target;
    SetType setType;
    ActionState actionState;
    boolean reaction = false;
    boolean executed = false;
    public DealBreakerAction(int playerID) {
        this.playerID = playerID;
        target = playerID;
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
                        if(MDGS.playerDealBreaker(i))
                            availableActions.add(new TargetPlayer(i));
                }
                break;
            case ChoosePropertySet:
                for (PropertySet pSet: MDGS.getPropertySets(target)) {
                    if(pSet.isComplete){
                        if(!availableActions.contains(new ChoosePropertySet(pSet.getSetType())))
                            availableActions.add(new ChoosePropertySet(pSet.getSetType()));
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
                setType = ((ChoosePropertySet) action).setType;
                actionState = ActionState.GetReaction;
                break;
            case GetReaction:
                if(action instanceof JustSayNoAction) actionState = ActionState.ReactToReaction;
                else executeAction(state);
                break;
            case  ReactToReaction:
                if(action instanceof JustSayNoAction) actionState = ActionState.GetReaction;
                else executed = true;
                break;
        }
    }
    protected void executeAction(AbstractGameState state){
        MonopolyDealGameState MDGS = (MonopolyDealGameState) state;
        MDGS.movePropertySetFromTo(setType,target,playerID);
        executed = true;
    }
    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyDealGameState MDGS = (MonopolyDealGameState) gs;
        MDGS.discardCard(CardType.DealBreaker,playerID);
        MDGS.useAction(1);
        gs.setActionInProgress(this);
        return true;
    }
    @Override
    public DealBreakerAction copy() {
        DealBreakerAction action = new DealBreakerAction(playerID);
        action.target = target;
        action.setType = setType;
        action.actionState = actionState;
        action.reaction = reaction;
        action.executed = executed;
        return action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DealBreakerAction that = (DealBreakerAction) o;
        return playerID == that.playerID && target == that.target && reaction == that.reaction && executed == that.executed && setType == that.setType && actionState == that.actionState;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, target, setType, actionState, reaction, executed);
    }

    @Override
    public String toString() {
        return "DealBreaker action";
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}