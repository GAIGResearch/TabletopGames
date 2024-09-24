package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import core.interfaces.IExtendedSequence;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.cards.RootCard;
import games.root_final.components.RootBoardNodeWithRootEdges;

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
    public RootCard card;
    public int locationID;
    public boolean done = false;
    public Stage stage = Stage.selectLocation;

    public OverworkSequence(int playerID){
        this.playerID = playerID;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (gs.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.MarquiseDeCat && currentState.getWood() > 0){
            currentState.increaseActionsPlayed();
            currentState.setActionInProgress(this);
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
                        actions.add(new ChooseCard(playerID,hand.get(i)));
                    }
                }
                return actions;
            case Overwork:
                actions.add(new Overwork(playerID, locationID, card));
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
            card = c.card;
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
        copy.card = card;
        copy.done = done;
        copy.locationID = locationID;
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof OverworkSequence os){
            if (card == null || os.card == null){
                return playerID == os.playerID && locationID == os.locationID && done== os.done && stage == os.stage && card == os.card;
            }
            return playerID == os.playerID && locationID == os.locationID && done== os.done && card.equals(os.card) && stage == os.stage;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("OverworkSequence", playerID, done, locationID, stage);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " overworks";
    }
}
