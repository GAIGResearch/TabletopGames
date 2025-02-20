package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.cards.RootCard;
import games.root.components.RootBoardNodeWithRootEdges;

import java.util.Objects;

public class Overwork extends AbstractAction {
    public final int playerID;
    public final int locationID;
    public final int cardIdx, cardId;

    public Overwork(int playerID, int locationID, int cardIdx, int cardId){
        this.playerID = playerID;
        this.locationID = locationID;
        this.cardIdx = cardIdx;
        this.cardId = cardId;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.MarquiseDeCat && currentState.getWood()>0){
            RootBoardNodeWithRootEdges location = currentState.getGameMap().getNodeByID(locationID);
            PartialObservableDeck<RootCard> hand = currentState.getPlayerHand(playerID);
            RootCard card = hand.pick(cardIdx);
            location.addToken(RootParameters.TokenType.Wood);
            currentState.removeWood();
            currentState.getDiscardPile().add(card);
            return true;
        }
        return false;
    }

    @Override
    public Overwork copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Overwork overwork = (Overwork) o;
        return playerID == overwork.playerID && locationID == overwork.locationID && cardIdx == overwork.cardIdx && cardId == overwork.cardId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, locationID, cardIdx, cardId);
    }

    @Override
    public String toString() {
        return "p" + playerID + " overworks and places a wood token on location " + locationID + " by spending card " + cardIdx;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        RootCard card = (RootCard) gs.getComponentById(cardId);
        return gs.getPlayerFaction(playerID).toString() + " overworks and places a wood token on " + gs.getGameMap().getNodeByID(locationID).identifier + " by spending " + card.suit.toString() + " card " + card.cardType.toString() ;
    }
}
