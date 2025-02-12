package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.root.RootGameState;
import games.root.components.cards.RootCard;
import games.root.components.RootBoardNodeWithRootEdges;

import java.util.Objects;

public class DiscardCraftedCard extends AbstractAction {
    public final int playerID;
    public final int cardIdx, cardId;

    public DiscardCraftedCard(int playerID, int cardIdx, int cardId){
        this.playerID = playerID;
        this.cardIdx = cardIdx;
        this.cardId = cardId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if(playerID == gs.getCurrentPlayer()) {
            Deck<RootCard> discardPile = currentState.getDiscardPile();
            Deck<RootCard> playerCraftedCard = currentState.getPlayerCraftedCards(playerID);
            RootCard card = playerCraftedCard.pick(cardIdx);
            discardPile.add(card);
            if (card.cardType == RootCard.CardType.RoyalClaim){
                //add score per clearing ruled
                for (RootBoardNodeWithRootEdges node: currentState.getGameMap().getNonForrestBoardNodes()){
                    if (node.rulerID == playerID){
                        currentState.addGameScorePlayer(playerID,1);
                    }
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public DiscardCraftedCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscardCraftedCard that = (DiscardCraftedCard) o;
        return playerID == that.playerID && cardIdx == that.cardIdx && cardId == that.cardId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, cardIdx, cardId);
    }

    @Override
    public String toString() {
        return "p" + playerID + " discards crafted card " + cardIdx;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        RootCard card = (RootCard) gs.getComponentById(cardId);
        return gs.getPlayerFaction(playerID).toString() + " discards crafted" + card.suit.toString() + " card " + card;
    }
}
