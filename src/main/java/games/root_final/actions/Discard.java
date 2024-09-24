package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.root_final.RootGameState;
import games.root_final.cards.RootCard;

import java.util.Objects;

public class Discard extends AbstractAction {
    protected final int playerID;
    protected final RootCard card;
    protected final boolean passSubGamePhase;
    public Discard(int playerID, RootCard card, boolean passSubGamePhase){
        this.playerID = playerID;
        this.card = card;
        this.passSubGamePhase = passSubGamePhase;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if(playerID == gs.getCurrentPlayer()) {
            Deck<RootCard> discardPile = currentState.getDiscardPile();
            Deck<RootCard> playerHand = currentState.getPlayerHand(playerID);
            for (int i = 0; i < playerHand.getSize(); i++){
                if(playerHand.get(i).equals(card)){
                    discardPile.add(playerHand.get(i));
                    playerHand.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new Discard(playerID,card, passSubGamePhase);
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj instanceof Discard){
            Discard object = (Discard) obj;
            return playerID == object.playerID && object.card.equals(card) && passSubGamePhase == object.passSubGamePhase;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("Discard",playerID,card, passSubGamePhase);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " discards " + card.suit.toString() + " card " + card.toString();
    }
}
