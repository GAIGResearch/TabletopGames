package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.cards.RootCard;

import java.util.Objects;

public class Mobilize extends AbstractAction {
    public final int playerID;
    public final int cardIdx, cardId;

    public Mobilize(int playerID, int cardIdx, int cardId){
        this.playerID = playerID;
        this.cardIdx = cardIdx;
        this.cardId = cardId;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState state = (RootGameState) gs;
        if(playerID == state.getCurrentPlayer() && state.getPlayerFaction(playerID) == RootParameters.Factions.WoodlandAlliance){
            Deck<RootCard> hand = state.getPlayerHand(playerID);
            Deck<RootCard> supporters = state.getSupporters();
            RootCard card = hand.pick(cardIdx);
            supporters.add(card);
            state.increaseActionsPlayed();
            //todo discarding supporters
            return true;
        }
        return false;
    }

    @Override
    public Mobilize copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mobilize mobilize = (Mobilize) o;
        return playerID == mobilize.playerID && cardIdx == mobilize.cardIdx && cardId == mobilize.cardId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, cardIdx, cardId);
    }

    @Override
    public String toString() {
        return "p" + playerID + " adds card " + cardIdx + " to supporters";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        RootCard card = (RootCard) gs.getComponentById(cardId);
        return gs.getPlayerFaction(playerID).toString() + " adds " + card.cardType.toString() + " to supporters";
    }
}
