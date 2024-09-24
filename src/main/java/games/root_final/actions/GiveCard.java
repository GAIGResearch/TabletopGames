package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.root_final.RootGameState;
import games.root_final.cards.RootCard;

import java.util.Objects;

public class GiveCard extends AbstractAction {
    public final int playerID;
    public final int targetID;
    public RootCard card;
    public GiveCard(int playerID, int targetID, RootCard card){
        this.playerID = playerID;
        this.targetID = targetID;
        this.card = card;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID){
            PartialObservableDeck<RootCard> playerHand = currentState.getPlayerHand(playerID);
            PartialObservableDeck<RootCard> targetHand = currentState.getPlayerHand(targetID);
            boolean[] visibility = new boolean[currentState.getNPlayers()];
            visibility[playerID] = true;
            visibility[targetID] = true;
            for (int i = 0; i < playerHand.getSize(); i++){
                if (playerHand.get(i).equals(card)){
                    targetHand.add(playerHand.get(i), visibility);
                    playerHand.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new GiveCard(playerID, targetID, card);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof GiveCard gc){
            return playerID == gc.playerID && targetID == gc.targetID && card.equals(gc.card);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("GiveCard", playerID, targetID, card);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " gives " + gs.getPlayerFaction(targetID).toString() + " " + card.suit.toString() + " card " + card.cardtype.toString();
    }
}
