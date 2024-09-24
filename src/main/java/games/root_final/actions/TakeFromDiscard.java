package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.cards.RootCard;

import java.util.Objects;

public class TakeFromDiscard extends AbstractAction {
    public final int playerID;
    public RootCard card;

    public TakeFromDiscard(int playerID, RootCard card){
        this.playerID = playerID;
        this.card = card;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState state = (RootGameState) gs;
        if (state.getCurrentPlayer() == playerID){
            PartialObservableDeck<RootCard> hand = state.getPlayerHand(playerID);
            Deck<RootCard> discard = state.getDiscardPile();
            boolean[] visibility = new boolean[state.getNPlayers()];
            // Fill the array with true values
            for (int i = 0; i < state.getNPlayers(); i++) {
                visibility[i] = true;
            }
            for (int i = 0; i < discard.getSize(); i++){
                if (discard.get(i).equals(card)){
                    hand.add(discard.get(i), visibility);
                    discard.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new TakeFromDiscard(playerID, card);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){ return true;}
        if (obj instanceof TakeFromDiscard td){
            return playerID == td.playerID && card.equals(td.card);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("TakeFromDiscard", playerID, card.hashCode());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " takes " + card.suit.toString() + " card " + card.cardtype.toString() + " from the discard pile";
    }
}
