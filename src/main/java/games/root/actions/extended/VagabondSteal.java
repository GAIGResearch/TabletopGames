package games.root.actions.extended;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.actions.TakeRandomCard;
import games.root.actions.choosers.ChooseNumber;
import games.root.components.Item;
import games.root.components.RootBoardNodeWithRootEdges;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VagabondSteal extends AbstractAction implements IExtendedSequence {
    public final int playerID;

    int targetID;
    boolean done = false;

    public enum Stage{
        chooseTarget,
        steal,
    }
    Stage stage = Stage.chooseTarget;

    public VagabondSteal(int playerID){
        this.playerID = playerID;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
            for (Item item: currentState.getSatchel()){
                if (item.itemType == Item.ItemType.torch && !item.damaged && item.refreshed){
                    item.refreshed = false;
                    currentState.increaseActionsPlayed();
                    currentState.setActionInProgress(this);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        RootGameState gs = (RootGameState) state;
        List<AbstractAction> actions = new ArrayList<>();
        if (stage == Stage.chooseTarget){
            RootBoardNodeWithRootEdges clearing = gs.getGameMap().getVagabondClearing();
            for (int i = 0; i < gs.getNPlayers(); i++){
                if (i != playerID && clearing.isAttackable(gs.getPlayerFaction(i)) && gs.getPlayerHand(i).getSize() > 0){
                    actions.add(new ChooseNumber(playerID,i));
                }
            }
        } else if (stage == Stage.steal) {
            actions.add(new TakeRandomCard(playerID, targetID));
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof ChooseNumber crp){
            stage = Stage.steal;
            targetID = crp.number;
        } else if (action instanceof TakeRandomCard){
            done = true;
        }

    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return done;
    }

    @Override
    public VagabondSteal copy() {
        VagabondSteal copy = new VagabondSteal(playerID);
        copy.done = done;
        copy.targetID = targetID;
        copy.stage = stage;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VagabondSteal that)) return false;
        return playerID == that.playerID && targetID == that.targetID && done == that.done && stage == that.stage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, targetID, done, stage);
    }

    @Override
    public String toString() {
        return "p" + playerID + " wants to steal";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " wants to steal";
    }
}
