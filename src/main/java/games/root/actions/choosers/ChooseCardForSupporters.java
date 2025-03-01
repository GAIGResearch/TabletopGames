package games.root.actions.choosers;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.cards.RootCard;

import java.util.ArrayList;
import java.util.Objects;

public class ChooseCardForSupporters extends AbstractAction {
    public final int playerID;
    public final int targetPlayerID;
    public final int cardIndex;

    public ChooseCardForSupporters(int playerID, int targetPlayerID, int rootCard){
        this.playerID = playerID;
        this.targetPlayerID = targetPlayerID;
        this.cardIndex = rootCard;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.WoodlandAlliance && playerID != targetPlayerID){
            PartialObservableDeck<RootCard> targetHand = currentState.getPlayerHand(targetPlayerID);
            PartialObservableDeck<RootCard> supporters = currentState.getSupporters();
            boolean[] visibility = new boolean[currentState.getNPlayers()];
            visibility[playerID] = true;
            visibility[targetPlayerID] = true;
            ArrayList<boolean[]> handVisibility = new ArrayList<>();
            for (int i = 0; i <  targetHand.getSize(); i++){
                boolean[] cardVisibility = new boolean[currentState.getNPlayers()];
                cardVisibility[playerID] = true;
                cardVisibility[targetPlayerID] = true;
                handVisibility.add(cardVisibility);
            }
            targetHand.setVisibility(handVisibility);
            supporters.add(targetHand.get(cardIndex),visibility);
            targetHand.remove(cardIndex);
            return true;
        }
        return false;
    }

    @Override
    public ChooseCardForSupporters copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChooseCardForSupporters that = (ChooseCardForSupporters) o;
        return playerID == that.playerID && targetPlayerID == that.targetPlayerID && cardIndex == that.cardIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, targetPlayerID, cardIndex);
    }

    @Override
    public String toString() {
        return "p" + playerID + " takes card from " + targetPlayerID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " takes card from " + gs.getPlayerFaction(targetPlayerID).toString()  +" and adds it to the Supporters Deck";
    }
}
