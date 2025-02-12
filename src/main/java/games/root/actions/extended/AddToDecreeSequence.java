package games.root.actions.extended;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.actions.AddToDecree;
import games.root.actions.Pass;
import games.root.actions.choosers.ChooseCard;
import games.root.components.cards.RootCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AddToDecreeSequence extends AbstractAction implements IExtendedSequence {
    public final int playerID;

    public enum Stage{
        chooseCard,
        addToDecree
    }

    int cardsAdded = 0;
    boolean done = false;
    Stage stage = Stage.chooseCard;
    int cardId = -1, cardIdx = -1;

    public AddToDecreeSequence(int playerID){
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.EyrieDynasties){
            currentState.increaseSubGamePhase();
            currentState.setActionInProgress(this);
            return true;
        }
        return false;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        RootGameState gs = (RootGameState) state;
        List<AbstractAction> actions = new ArrayList<>();
        if (stage == Stage.chooseCard){
            Deck<RootCard> hand = gs.getPlayerHand(playerID);
            for (int i = 0; i < hand.getSize(); i++){
                actions.add(new ChooseCard(playerID, i, hand.get(i).getComponentID()));
            }
            if (cardsAdded == 1){
                actions.add(new Pass(playerID, " does not add second card"));
            }
            if (actions.isEmpty()){
                actions.add(new Pass(playerID, " both drawpile and player hand are empty"));
            }
            return actions;

        } else if (stage == Stage.addToDecree) {
            for (int i = 0; i < 4; i++){
                actions.add(new AddToDecree(playerID, i, cardIdx, cardId, false));
            }
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
        if (stage == Stage.chooseCard){
            if (action instanceof ChooseCard cc){
                cardId = cc.cardId;
                cardIdx = cc.cardIdx;
                stage = Stage.addToDecree;
            } else if (action instanceof Pass) {
                done = true;
            }
        } else if (stage == Stage.addToDecree) {
            cardsAdded++;
            if (cardsAdded == 2){
                done = true;
            }else{
                stage = Stage.chooseCard;
            }
        }

    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return done;
    }

    @Override
    public AddToDecreeSequence copy() {
        AddToDecreeSequence copy = new AddToDecreeSequence(playerID);
        copy.cardId = cardId;
        copy.cardIdx = cardIdx;
        copy.cardsAdded = cardsAdded;
        copy.done = done;
        copy.stage = stage;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AddToDecreeSequence that)) return false;
        return playerID == that.playerID && cardsAdded == that.cardsAdded && done == that.done && cardId == that.cardId && cardIdx == that.cardIdx && stage == that.stage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, cardsAdded, done, stage, cardId, cardIdx);
    }

    @Override
    public String toString() {
        return "p" + playerID + " adds to decree";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " adds to decree";
    }

}
