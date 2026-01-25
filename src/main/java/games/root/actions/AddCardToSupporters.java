package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.cards.RootCard;

import java.util.Objects;

public class AddCardToSupporters extends AbstractAction {
    public final int playerID;
    public final int cardIdx;
    public final int cardId;

    public AddCardToSupporters(int playerID, int cardIdx, int cardId){
        this.playerID = playerID;
        this.cardIdx = cardIdx;
        this.cardId = cardId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID)!= RootParameters.Factions.WoodlandAlliance){
            PartialObservableDeck<RootCard> hand = currentState.getPlayerHand(playerID);
            RootCard card = hand.pick(cardIdx);
            boolean[] visibility = new boolean[currentState.getNPlayers()];
            visibility[playerID] = true;
            visibility[currentState.getFactionPlayerID(RootParameters.Factions.WoodlandAlliance)] = true;
            currentState.getSupporters().add(card, visibility);
            return true;

        }
        return false;
    }

    @Override
    public AddCardToSupporters copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddCardToSupporters that = (AddCardToSupporters) o;
        return playerID == that.playerID && cardIdx == that.cardIdx && cardId == that.cardId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, cardIdx, cardId);
    }

    @Override
    public String toString() {
        return "p" + playerID + " adds card " + cardIdx + " to the Supporters deck";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        RootCard card = (RootCard) gs.getComponentById(cardId);
        return gs.getPlayerFaction(playerID).toString() + " adds " + card.suit.toString() + " " + card.cardType.toString() + " to the Supporters deck";
    }
}
