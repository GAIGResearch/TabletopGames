package games.monopolydeal.actions.actioncards;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IExtendedSequence;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.actions.ActionState;
import games.monopolydeal.actions.informationcontainer.ChooseCardFrom;
import games.monopolydeal.actions.informationcontainer.TargetPlayer;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.PropertySet;
import games.monopolydeal.cards.SetType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p> ForcedDealAction is a single target action. It uses this EAS for collecting the required information and execution of the Forced Deal action card.
 * <ol>
 *     <li>Action card : Swap any property with another player</li>
 *     <li>Execution description:
 *     <ul>
 *         <li>Initial 'execute' call : The action card is played onto the discard pile</li>
 *         <li>actionState 'Target' : The targeted player is chosen and action state is forwarded to 'TakeCard'</li>
 *         <li>actionState 'TakeCard' : Which property of said target is to be stolen is chosen and action state is forwarded to 'GiveCard'</li>
 *         <li>actionState 'GiveCard' : Which property of self is to be given is chosen and action state is forwarded to 'GetReaction'</li>
 *         <li>actionState 'GetReaction' : The targeted player has the option of denying the set by using JustSayNo. The action is either executed or action state is forwarded to 'ReactToReaction'</li>
 *         <li>actionState 'ReactToReaction' : A JustSayNo can be played on top of a JustSayNo to force execution. The opponent can also play a JustSayNo on top of this JustSayNo, so a loop of GetReaction and ReactToReaction is formed until either the action is denied or executed.</li>
 *     </ul></li>
 * </ol>
 * </p>
 */
public class ForcedDealAction extends AbstractAction implements IExtendedSequence, IActionCard {
    // The extended sequence usually keeps record of the player who played this action, to be able to inform the game whose turn it is to make decisions
    final int playerID;

    int target;
    CardType take,give;
    SetType tFrom,gFrom;
    ActionState actionState;
    boolean reaction = false;
    boolean executed = false;

    public ForcedDealAction(int playerID) {
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
                        if(MDGS.checkForFreeProperty(i))
                            availableActions.add(new TargetPlayer(i));
                }
                break;
            case TakeCard:
                for (PropertySet pSet: MDGS.getPropertySets(target)) {
                    if(!pSet.isComplete){
                        for(int i=0;i<pSet.getSize();i++){
                            if(!availableActions.contains(new ChooseCardFrom(pSet.get(i).cardType(),pSet.getSetType(),0)))
                                availableActions.add(new ChooseCardFrom(pSet.get(i).cardType(),pSet.getSetType(),0));
                        }
                    }
                }
                break;
            case GiveCard:
                for (PropertySet pSet: MDGS.getPropertySets(playerID)) {
                    if((!pSet.hasHouse && !pSet.hasHotel)){
                        for(int i=0;i<pSet.getSize();i++){
                            if(!availableActions.contains(new ChooseCardFrom(pSet.get(i).cardType(),pSet.getSetType(),1)))
                                availableActions.add(new ChooseCardFrom(pSet.get(i).cardType(),pSet.getSetType(),1));
                        }
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
                actionState = ActionState.TakeCard;
                break;
            case TakeCard:
                take = ((ChooseCardFrom) action).take;
                tFrom = ((ChooseCardFrom) action).from;
                actionState = ActionState.GiveCard;
                break;
            case GiveCard:
                give = ((ChooseCardFrom) action).take;
                gFrom = ((ChooseCardFrom) action).from;
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
        MDGS.removePropertyFrom(target, take, tFrom);
        MDGS.removePropertyFrom(playerID, give, gFrom);
        MDGS.addProperty(playerID, take);
        MDGS.addProperty(target, give);
        executed = true;
    }
    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyDealGameState MDGS = (MonopolyDealGameState) gs;
        MDGS.discardCard(CardType.ForcedDeal,playerID);
        MDGS.useAction(1);
        gs.setActionInProgress(this);
        return true;
    }
    @Override
    public ForcedDealAction copy() {
        ForcedDealAction action = new ForcedDealAction(playerID);
        action.target = target;
        action.reaction = reaction;
        action.executed = executed;
        action.take = take;
        action.give = give;
        action.tFrom = tFrom;
        action.gFrom = gFrom;
        action.actionState = actionState;
        return action;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForcedDealAction that = (ForcedDealAction) o;
        return playerID == that.playerID &&
                target == that.target &&
                reaction == that.reaction &&
                executed == that.executed &&
                actionState == that.actionState &&
                Objects.equals(take, that.take) &&
                Objects.equals(give, that.give) &&
                Objects.equals(tFrom, that.tFrom) &&
                Objects.equals(gFrom, that.gFrom);
    }
    @Override
    public int hashCode() {
        return Objects.hash(playerID, target, reaction, executed, actionState, take, give, tFrom, gFrom);
    }
    @Override
    public String toString() { return "ForcedDeal action"; }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    public int getTarget(MonopolyDealGameState gs) {
        return target;
    }
}
