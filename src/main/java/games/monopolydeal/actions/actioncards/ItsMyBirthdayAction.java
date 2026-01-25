package games.monopolydeal.actions.actioncards;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IExtendedSequence;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.actions.ActionState;
import games.monopolydeal.cards.CardType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * <p> ItsMyBirthDay is a multiple target rent action. It uses this EAS for iterating through the targeted players and 'PayRent' EAS for collection of rent.
 * <ol>
 *     <li>Action card : Collect 2M from all players</li>
 *     <li>Execution description:
 *     <ul>
 *         <li>Initial 'execute' call : The action card is played onto the discard pile, a boolean array for keeping track of each players execution status is setup and the first target is chosen.</li>
 *         <li>actionState 'GetReaction' : The targeted player has the option of denying the action by using JustSayNo. The action state is forwarded to either 'CollectRent' or 'ReactToReaction'</li>
 *         <li>actionState 'ReactToReaction' : A JustSayNo can be played on top of a JustSayNo to force execution. The opponent can also play a JustSayNo on top of this JustSayNo, so a loop of GetReaction and ReactToReaction is formed until either the action is denied or executed.</li>
 *         <li>actionState 'CollectRent' : A 'PayRent' EAS is called for the execution of the rent, the next target is chosen and action state is switched back to 'GetReaction'.</li>
 *     </ul></li>
 * </ol>
 * </p>
 */
public class ItsMyBirthdayAction extends AbstractAction implements IExtendedSequence, IActionCard {
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

        switch (actionState) {
            case GetReaction:
                availableActions.add(new DoNothing());
                if (MDGS.CheckForJustSayNo(target)) availableActions.add(new JustSayNoAction());
                break;
            case ReactToReaction:
                availableActions.add(new DoNothing());
                if (MDGS.CheckForJustSayNo(playerID)) availableActions.add(new JustSayNoAction());
                break;
            case CollectRent:
                if (MDGS.isBoardEmpty(target)) availableActions.add(new DoNothing());
                else availableActions.add(new PayRent(target, playerID, 2));
                break;
        }
        return availableActions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        if (actionState == ActionState.GetReaction) return target;
        else return playerID;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        switch (actionState) {
            case GetReaction:
                if (action instanceof JustSayNoAction) actionState = ActionState.ReactToReaction;
                else actionState = ActionState.CollectRent;
                break;
            case ReactToReaction:
                if (!(action instanceof JustSayNoAction)) {
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

    public void getNextTarget() {
        if (!collectedAllRent()) {
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
        MDGS.discardCard(CardType.ItsMyBirthday, playerID);
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
        if (collectedRent != null) action.collectedRent = collectedRent.clone();
        else action.collectedRent = null;
        action.reaction = reaction;

        return action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItsMyBirthdayAction that = (ItsMyBirthdayAction) o;
        return playerID == that.playerID &&
                target == that.target && actionState == that.actionState &&
                reaction == that.reaction && Arrays.equals(collectedRent, that.collectedRent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, target, actionState, reaction) + 31 * Arrays.hashCode(collectedRent);
    }

    @Override
    public String toString() {
        return "It's my Birthday action";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    public int getTarget(MonopolyDealGameState gs) {
        return -1;  //all players
    }
}
