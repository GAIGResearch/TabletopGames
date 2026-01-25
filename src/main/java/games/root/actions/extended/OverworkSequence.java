package games.root.actions.extended;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import core.interfaces.IExtendedSequence;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.actions.Overwork;
import games.root.actions.choosers.ChooseCard;
import games.root.actions.choosers.ChooseNode;
import games.root.components.cards.RootCard;
import games.root.components.RootBoardNodeWithRootEdges;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OverworkSequence extends AbstractAction implements IExtendedSequence {
    public enum Stage{
        selectLocation,
        selectCard,
        Overwork,
    }

    public final int playerID;

    int locationID;
    boolean done = false;
    Stage stage = Stage.selectLocation;
    int cardIdx = -1, cardId = -1;

    public OverworkSequence(int playerID){
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (gs.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.MarquiseDeCat && currentState.getWood() > 0){
            currentState.increaseActionsPlayed();
            currentState.setActionInProgress(this);
            return true;
        }
        return false;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        RootGameState gs = (RootGameState) state;
        List<AbstractAction> actions = new ArrayList<>();
        switch (stage){
            case selectLocation:
                for (RootBoardNodeWithRootEdges location: gs.getGameMap().getSawmills()){
                    if (gs.locationIsOverworkable(playerID, location.getComponentID())){
                        actions.add(new ChooseNode(playerID, location.getComponentID()));
                    }
                }
                return actions;
            case selectCard:
                RootBoardNodeWithRootEdges selected = gs.getGameMap().getNodeByID(locationID);
                PartialObservableDeck<RootCard> hand = gs.getPlayerHand(playerID);
                for (int i = 0; i < hand.getSize(); i++){
                    if (hand.get(i).suit == selected.getClearingType() || hand.get(i).suit == RootParameters.ClearingTypes.Bird){
                        actions.add(new ChooseCard(playerID, i, hand.get(i).getComponentID()));
                    }
                }
                return actions;
            case Overwork:
                actions.add(new Overwork(playerID, locationID, cardIdx, cardId));
                return actions;
        }
        return null;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof ChooseNode n){
            locationID = n.nodeID;
            stage = Stage.selectCard;
        }else if (action instanceof ChooseCard c){
            cardId = c.cardId;
            cardIdx = c.cardIdx;
            stage = Stage.Overwork;
        } else if (action instanceof Overwork o) {
            done = true;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return done;
    }

    @Override
    public OverworkSequence copy() {
        OverworkSequence copy = new OverworkSequence(playerID);
        copy.stage = stage;
        copy.cardId = cardId;
        copy.cardIdx = cardIdx;
        copy.done = done;
        copy.locationID = locationID;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OverworkSequence that)) return false;
        return playerID == that.playerID && locationID == that.locationID && done == that.done && cardIdx == that.cardIdx && cardId == that.cardId && stage == that.stage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, locationID, done, stage, cardIdx, cardId);
    }

    @Override
    public String toString() {
        return "p" + playerID + " overworks";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " overworks";
    }
}
