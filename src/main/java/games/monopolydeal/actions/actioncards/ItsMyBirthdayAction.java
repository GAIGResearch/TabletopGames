package games.monopolydeal.actions.actioncards;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IExtendedSequence;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.actions.ActionState;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.MonopolyDealCard;

import java.util.ArrayList;
import java.util.Arrays;
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
public class ItsMyBirthdayAction extends AbstractAction implements IExtendedSequence {
    // The extended sequence usually keeps record of the player who played this action, to be able to inform the game whose turn it is to make decisions
    final int playerID;
    int target;
    ActionState actionState;
    boolean[] collectedRent;
    boolean reaction;
    public ItsMyBirthdayAction(int playerID) {
        this.playerID = playerID;
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
                else availableActions.add(new PayRent(target,playerID,2));
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
        MDGS.discardCard(MonopolyDealCard.create(CardType.ItsMyBirthday),playerID);
        MDGS.useAction(1);
        // Set first target
        getNextTarget();
        gs.setActionInProgress(this);
        return true;
    }
    @Override
    public ItsMyBirthdayAction copy() {
        ItsMyBirthdayAction action = new ItsMyBirthdayAction(playerID);
        action.target = target;
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
        ItsMyBirthdayAction that = (ItsMyBirthdayAction) o;
        return playerID == that.playerID && target == that.target && reaction == that.reaction && actionState == that.actionState && Arrays.equals(collectedRent, that.collectedRent);
    }
    @Override
    public int hashCode() {
        int result = Objects.hash(playerID, target, actionState, reaction);
        result = 31 * result + Arrays.hashCode(collectedRent);
        return result;
    }
    @Override
    public String toString() { return "It's my Birthday action"; }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
