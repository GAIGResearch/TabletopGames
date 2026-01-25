package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.cards.RootCard;

import java.util.Objects;

public class Train extends AbstractAction {
    public final int playerID;
    public final int cardIdx, cardId;

    public Train(int playerID, int cardIdx, int cardId){
        this.playerID = playerID;
        this.cardIdx = cardIdx;
        this.cardId = cardId;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState state = (RootGameState) gs;
        if(state.getCurrentPlayer() == playerID && state.getPlayerFaction(playerID) == RootParameters.Factions.WoodlandAlliance){
            Deck<RootCard> hand = state.getPlayerHand(playerID);
            Deck<RootCard> discardPile = state.getDiscardPile();
            state.increaseActionsPlayed();
            RootCard card = hand.pick(cardIdx);
            discardPile.add(card);
            try {
                state.addOfficer();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            return true;
        }
        return false;
    }

    @Override
    public Train copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Train train = (Train) o;
        return playerID == train.playerID && cardIdx == train.cardIdx && cardId == train.cardId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, cardIdx, cardId);
    }

    @Override
    public String toString() {
        return "p" + playerID + " discards card " + cardIdx + " to train an officer";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        RootCard card = (RootCard) gs.getComponentById(cardId);
        return gs.getPlayerFaction(playerID).toString()  + " discards " + card.cardType.toString() + " to train an officer";
    }
}
