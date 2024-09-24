package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.root_final.RootGameState;
import games.root_final.cards.RootCard;

import java.util.Objects;

public class DiscardSupporter extends AbstractAction {
    protected final int playerID;
    protected final RootCard card;
    protected final boolean passSubGamePhase;
    public DiscardSupporter(int playerID, RootCard card, boolean passSubGamePhase){
        this.playerID = playerID;
        this.card = card;
        this.passSubGamePhase = passSubGamePhase;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if(playerID == gs.getCurrentPlayer()) {
            Deck<RootCard> discardPile = currentState.getDiscardPile();
            Deck<RootCard> supporters = currentState.getSupporters();
            for (int i = 0; i < supporters.getSize(); i++){
                if(supporters.get(i).equals(card)){
                    discardPile.add(supporters.get(i));
                    supporters.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new DiscardSupporter(playerID,card, passSubGamePhase);
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj instanceof DiscardSupporter){
            DiscardSupporter object = (DiscardSupporter) obj;
            return playerID == object.playerID && object.card.equals(card) && passSubGamePhase == object.passSubGamePhase;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("Discard Supporter",playerID,card, passSubGamePhase);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " discards supporter " + card.toString();
    }
}
