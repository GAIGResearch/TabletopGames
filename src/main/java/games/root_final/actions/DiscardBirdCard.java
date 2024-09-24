package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.cards.RootCard;

import java.util.Objects;

public class DiscardBirdCard extends AbstractAction {
    public final RootCard card;
    public final int playerID;

    public DiscardBirdCard(int playerID, RootCard card){
        this.card = card;
        this.playerID = playerID;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if(currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.MarquiseDeCat){
            if(currentState.getPlayerHand(playerID).contains(card)){
                for(int i = 0; i< currentState.getPlayerHand(playerID).getSize(); i++){
                    if(currentState.getPlayerHand(playerID).get(i).equals(card)){
                        currentState.getDiscardPile().add(card);
                        currentState.getPlayerHand(playerID).remove(card);
                        currentState.decreaseActionsPlayed();
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){return  true;}
        if(obj instanceof DiscardBirdCard){
            DiscardBirdCard other = (DiscardBirdCard) obj;
            return playerID == other.playerID && card.equals(other.card);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, card.toString());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " spends bird card " + card.toString() + " to get an additional action";
    }
}
