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
 * <p>They should also extend the {@link AbstractAction} class, or any other core actions.</p>
 */
public class ForcedDealAction extends AbstractAction implements IExtendedSequence {
    // The extended sequence usually keeps record of the player who played this action, to be able to inform the game whose turn it is to make decisions
    final int playerID;
    int target;
    MonopolyDealCard take,give;
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
                            if(!availableActions.contains(new ChooseCardFrom(pSet.get(i),pSet.getSetType(),0)))
                                availableActions.add(new ChooseCardFrom(pSet.get(i),pSet.getSetType(),0));
                        }
                    }
                }
                break;
            case GiveCard:
                for (PropertySet pSet: MDGS.getPropertySets(playerID)) {
                    if((!pSet.hasHouse && !pSet.hasHotel)){
                        for(int i=0;i<pSet.getSize();i++){
                            if(!availableActions.contains(new ChooseCardFrom(pSet.get(i),pSet.getSetType(),1)))
                                availableActions.add(new ChooseCardFrom(pSet.get(i),pSet.getSetType(),1));
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
        MDGS.discardCard(MonopolyDealCard.create(CardType.ForcedDeal),playerID);
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
        return playerID == that.playerID && target == that.target && reaction == that.reaction && executed == that.executed && Objects.equals(take, that.take) && Objects.equals(give, that.give) && tFrom == that.tFrom && gFrom == that.gFrom && actionState == that.actionState;
    }
    @Override
    public int hashCode() {
        return Objects.hash(playerID, target, take, give, tFrom, gFrom, actionState, reaction, executed);
    }
    @Override
    public String toString() { return "ForcedDeal action"; }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
