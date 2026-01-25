package games.root.actions.extended;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.actions.TakeFromDiscard;
import games.root.components.cards.RootCard;
import games.root.components.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VagabondDayLabour extends AbstractAction implements IExtendedSequence {
    public final int playerID;
    boolean done;

    public VagabondDayLabour(int playerID){
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
        Deck<RootCard> discard = gs.getDiscardPile();
        for (int i = 0; i < discard.getSize(); i++){
            actions.add(new TakeFromDiscard(playerID, i, discard.get(i).getComponentID()));
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof TakeFromDiscard){
            done = true;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return done;
    }

    @Override
    public VagabondDayLabour copy() {
        VagabondDayLabour copy = new VagabondDayLabour(playerID);
        copy.done = done;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VagabondDayLabour that)) return false;
        return playerID == that.playerID && done == that.done;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, done);
    }

    @Override
    public String toString() {
        return "p" + playerID + " performs day labour";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " performs day labour";
    }
}
