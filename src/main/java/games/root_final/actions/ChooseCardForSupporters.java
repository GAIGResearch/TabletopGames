package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.cards.RootCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChooseCardForSupporters extends AbstractAction {
    public final int playerID;
    public final int targetPlayerID;
    public int cardIndex;
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
    public AbstractAction copy() {
        return new ChooseCardForSupporters(playerID, targetPlayerID, cardIndex);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof ChooseCardForSupporters){
            ChooseCardForSupporters other = (ChooseCardForSupporters) obj;
            return playerID == other.playerID && targetPlayerID == other.targetPlayerID && cardIndex == other.cardIndex;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("ChooseCardForSupporters", playerID, targetPlayerID, cardIndex);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + "takes card from " + gs.getPlayerFaction(targetPlayerID).toString()  +" and adds it to the Supporters Deck";
    }
}
