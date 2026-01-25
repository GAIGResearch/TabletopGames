package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import evaluation.metrics.Event;
import games.root.RootGameState;
import games.root.components.cards.RootCard;

import java.util.Objects;

public class TakeRandomCard extends AbstractAction {
    public final int playerID;
    public final int targetPlayerID;

    public TakeRandomCard(int playerID, int targetPlayerID){
        this.playerID = playerID;
        this.targetPlayerID = targetPlayerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (playerID == gs.getCurrentPlayer()){
            PartialObservableDeck<RootCard> targetHand = currentState.getPlayerHand(targetPlayerID);
            PartialObservableDeck<RootCard> hand = currentState.getPlayerHand(playerID);
            boolean[] visibility = new boolean[currentState.getNPlayers()];
            visibility[playerID] = true;
            visibility[targetPlayerID] = true;
            if (targetHand.getSize() > 0){
                int random = currentState.getRnd().nextInt(targetHand.getSize());
                hand.add(targetHand.get(random), visibility);
                currentState.logEvent(Event.GameEvent.GAME_EVENT, currentState.getPlayerFaction(playerID).toString() + " takes " + targetHand.get(random).suit.toString() + " card " + targetHand.get(random).cardType.toString() + " from " + currentState.getPlayerFaction(targetPlayerID).toString());
                targetHand.remove(random);
                return true;
            }
        }
        return false;
    }

    @Override
    public TakeRandomCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof TakeRandomCard trc){
            return playerID == trc.playerID && targetPlayerID == trc.targetPlayerID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("TakeRandomCard", playerID, targetPlayerID);
    }

    @Override
    public String toString() {
        return "p" + playerID + " takes a random card from p" + targetPlayerID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " takes a random card from " + gs.getPlayerFaction(targetPlayerID).toString() ;
    }
}
