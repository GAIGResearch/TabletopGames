package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.cards.RootCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AddToDecreeSequence extends AbstractAction implements IExtendedSequence {
    public final int playerID;
    public enum Stage{
        chooseCard,
        addToDecree
    }
    public int cardsAdded = 0;
    public RootCard card;
    public boolean done = false;
    public Stage stage = Stage.chooseCard;

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
                actions.add(new ChooseCard(playerID, hand.get(i)));
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
                actions.add(new AddToDecree(playerID, i, card, false));
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
                card = cc.card;
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
        if (card == null){
            copy.card = card;
        }else {
            copy.card = (RootCard) card.copy();
        }
        copy.cardsAdded = cardsAdded;
        copy.done = done;
        copy.stage = stage;
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){return true;}
        if (obj instanceof AddToDecreeSequence ads){
            if (card == null || ads.card == null){
                return playerID == ads.playerID;
            }
            return playerID == ads.playerID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (card != null) return Objects.hash("AddToDecreeSequence", playerID);
        return Objects.hash("AddToDecreeSequence", playerID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " adds to decree";
    }


}
