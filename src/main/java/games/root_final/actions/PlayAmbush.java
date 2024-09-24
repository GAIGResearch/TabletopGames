package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.cards.RootCard;

import java.util.Objects;

public class PlayAmbush extends AbstractAction {
    public final int playerID;
    public RootCard card;

    public PlayAmbush(int playerID, RootCard card){
        this.playerID = playerID;
        this.card = card;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID){
            PartialObservableDeck<RootCard> hand = currentState.getPlayerHand(playerID);
            for (int i = 0; i < hand.getSize(); i++){
                if (hand.get(i).equals(card)){
                    currentState.getDiscardPile().add(hand.get(i));
                    hand.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new PlayAmbush(playerID, card);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){return true;}
        if(obj instanceof PlayAmbush){
            PlayAmbush other = (PlayAmbush) obj;
            return playerID == other.playerID && card.equals(other.card);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("PlayAmbush", playerID, card.hashCode());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " plays ambush";
    }
}
