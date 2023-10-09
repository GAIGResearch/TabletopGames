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
 * <p>The extended actions framework supports 2 use-cases: <ol>
 *     <li>A sequence of decisions required to complete an action (e.g. play a card in a game area - which card? - which area?).
 *     This avoids very large action spaces in favour of more decisions throughout the game (alternative: all unit actions
 *     with parameters supplied at initialization, all combinations of parameters computed beforehand).</li>
 *     <li>A sequence of actions triggered by specific decisions (e.g. play a card which forces another player to discard a card - other player: which card to discard?)</li>
 * </ol></p>
 * <p>Extended actions should implement the {@link IExtendedSequence} interface and appropriate methods, as detailed below.</p>
 * <p>They should also extend the {@link AbstractAction} class, or any other core actions. As such, all guidelines in {@link } apply here as well.</p>
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
                if(MDGS.isBoardEmpty(target)) availableActions.add(new DoNothing());
                else availableActions.add(new PayRent(target,playerID,rent));
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
        MDGS.discardCard(MonopolyDealCard.create(CardType.MulticolorRent),playerID);
        for(int i=0;i<doubleTheRent;i++) MDGS.discardCard(MonopolyDealCard.create(CardType.DoubleTheRent),playerID);
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
        return playerID == that.playerID && doubleTheRent == that.doubleTheRent && target == that.target && rent == that.rent && reaction == that.reaction && executed == that.executed && actionState == that.actionState;
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
}