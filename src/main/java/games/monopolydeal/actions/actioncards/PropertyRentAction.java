package games.monopolydeal.actions.actioncards;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IExtendedSequence;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.actions.ActionState;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.MonopolyDealCard;
import games.monopolydeal.cards.PropertySet;
import games.monopolydeal.cards.SetType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * <p> PropertyRentAction is a multiple target rent action. It uses this EAS for iterating through the targeted players and uses 'PayRent' EAS for collection of rent.
 * <ol>
 *     <li>Action card : Collect rent of said property from all players</li>
 *     <li>Execution description:
 *     <ul>
 *         <li>Initial 'execute' call : <ul>
 *             <li>The action card is played onto the discard pile</li>
 *             <li>The rent of said property is calculated</li>
 *             <li>A boolean array for keeping track of each player's execution status is setup</li>
 *             <li>The first target is chosen</li>
 *         </ul>
 *         <li>actionState 'GetReaction' : The targeted player has the option of denying the action by using JustSayNo. The action state is forwarded to either 'CollectRent' or 'ReactToReaction'</li>
 *         <li>actionState 'ReactToReaction' : A JustSayNo can be played on top of a JustSayNo to force execution. The opponent can also play a JustSayNo on top of this JustSayNo, so a loop of GetReaction and ReactToReaction is formed until either the action is denied or executed.</li>
 *         <li>actionState 'CollectRent' : A 'PayRent' EAS is called for the execution of the rent, the next target is chosen and action state is switched back to 'GetReaction'.</li>
 *     </ul></li>
 * </ol>
 * </p>
 */
public class PropertyRentAction extends AbstractAction implements IExtendedSequence, IActionCard {

    // The extended sequence usually keeps record of the player who played this action, to be able to inform the game whose turn it is to make decisions
    final int playerID;
    final SetType setType;
    final CardType cardType;
    final int doubleTheRent;

    int target;
    int rent;
    ActionState actionState;
    boolean[] collectedRent;
    boolean reaction;

    public PropertyRentAction(int playerID, SetType setType, CardType cardType, int doubleTheRent) {
        this.playerID = playerID;
        this.setType = setType;
        this.cardType = cardType;
        this.doubleTheRent = doubleTheRent;
        actionState = ActionState.GetReaction;
    }
    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        MonopolyDealGameState MDGS = (MonopolyDealGameState) state;
        List<AbstractAction> availableActions = new ArrayList<>();

        switch (actionState){
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
            case GetReaction:
                if(action instanceof JustSayNoAction) actionState = ActionState.ReactToReaction;
                else actionState = ActionState.CollectRent;
                break;
            case ReactToReaction:
                if(!(action instanceof JustSayNoAction)) {
                    collectedRent[target] = true;
                    getNextTarget();
                }
                actionState = ActionState.GetReaction;
                break;
            case CollectRent:
                collectedRent[target] = true;
                getNextTarget();
                actionState = ActionState.GetReaction;
                break;
        }
    }
    public boolean collectedAllRent() {
        for (boolean b : collectedRent) if (!b) return false;
        return true;
    }
    public void getNextTarget(){
        if(!collectedAllRent()) {
            for (int i = 0; i < collectedRent.length; i++) {
                if (!collectedRent[i]) {
                    target = i;
                    return;
                }
            }
        }
    }
    @Override
    public boolean executionComplete(AbstractGameState state) {
        return collectedAllRent();
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        collectedRent = new boolean[gs.getNPlayers()];
        collectedRent[playerID] = true;
        // Discard card used
        MonopolyDealGameState MDGS = (MonopolyDealGameState) gs;
        MDGS.discardCard(cardType,playerID);
        for(int i=0;i<doubleTheRent;i++) MDGS.discardCard(CardType.DoubleTheRent,playerID);
        MDGS.useAction(1 + doubleTheRent);
        // Calculate rent
        PropertySet pSet = MDGS.getPlayerPropertySet(playerID,setType);
        if(pSet.isComplete){
            rent = setType.rent[setType.setSize - 1];
            if(pSet.hasHouse) rent = rent + 3;
            if(pSet.hasHotel) rent = rent + 4;
        }
        else{
            if(pSet.getPropertySetSize() == 0)
                rent = 0;
            else rent = setType.rent[pSet.getPropertySetSize() - 1];
        }
        // Double the rent
        rent = (int) (rent * Math.pow(2,doubleTheRent));
        // Set first target
        getNextTarget();
        gs.setActionInProgress(this);
        return true;
    }
    @Override
    public PropertyRentAction copy() {
        PropertyRentAction action = new PropertyRentAction(playerID,setType,cardType,doubleTheRent);
        action.target = target;
        action.rent = rent;
        action.actionState = actionState;
        if(collectedRent != null) action.collectedRent = collectedRent.clone();
        else action.collectedRent = null;
        action.reaction = reaction;
        return action;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyRentAction that = (PropertyRentAction) o;
        return playerID == that.playerID && doubleTheRent == that.doubleTheRent && setType == that.setType &&
                cardType == that.cardType && that.target == target && that.rent == rent &&
                actionState == that.actionState && reaction == that.reaction && Arrays.equals(collectedRent, that.collectedRent);
    }
    @Override
    public int hashCode() {
        int result = Objects.hash(playerID, setType, cardType, doubleTheRent, target, rent, actionState, reaction);
        result = 31 * result + Arrays.hashCode(collectedRent);
        return result;
    }
    @Override
    public String toString() {
        if(doubleTheRent == 0)
            return "Collect rent : " + setType;
        else
            return "Collect rent : " + setType + " With " + doubleTheRent + " DTR";
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    public int getTarget(MonopolyDealGameState gs) {
        return -1;  //all players
    }
}
