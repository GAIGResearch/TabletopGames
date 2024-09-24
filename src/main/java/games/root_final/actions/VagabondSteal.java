package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.Item;
import games.root_final.components.RootBoardNodeWithRootEdges;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VagabondSteal extends AbstractAction implements IExtendedSequence {
    public final int playerID;
    public int targetID;

    public boolean done = false;

    public enum Stage{
        chooseTarget,
        steal,
    }

    public Stage stage = Stage.chooseTarget;

    public VagabondSteal(int playerID){
        this.playerID = playerID;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
            for (Item item: currentState.getSachel()){
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
                    actions.add(new ChooseTargetPlayer(playerID,i));
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
        if (action instanceof ChooseTargetPlayer crp){
            stage = Stage.steal;
            targetID = crp.targetID;
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
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof VagabondSteal vs){
            return playerID == vs.playerID && targetID == vs.targetID && done == vs.done && stage == vs.stage;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("Steal", playerID, targetID, done, stage);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " wants to steal";
    }
}
