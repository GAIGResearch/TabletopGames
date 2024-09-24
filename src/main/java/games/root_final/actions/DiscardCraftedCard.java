package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.root_final.RootGameState;
import games.root_final.cards.RootCard;
import games.root_final.components.RootBoardNodeWithRootEdges;

import java.util.Objects;

public class DiscardCraftedCard extends AbstractAction {
    protected final int playerID;
    protected final RootCard card;
    public DiscardCraftedCard(int playerID, RootCard card){
        this.playerID = playerID;
        this.card = card;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if(playerID == gs.getCurrentPlayer()) {
            Deck<RootCard> discardPile = currentState.getDiscardPile();
            Deck<RootCard> playerCraftedCard = currentState.getPlayerCraftedCards(playerID);
            for (int i = 0; i < playerCraftedCard.getSize(); i++){
                if(playerCraftedCard.get(i).equals(card)){
                    discardPile.add(playerCraftedCard.get(i));
                    playerCraftedCard.remove(i);
                    if (card.cardtype == RootCard.CardType.RoyalClaim){
                        //add score per clearing ruled
                        for (RootBoardNodeWithRootEdges node: currentState.getGameMap().getNonForrestBoardNodes()){
                            if (node.rulerID == playerID){
                                currentState.addGameScorePLayer(playerID,1);
                            }
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new DiscardCraftedCard(playerID,card);
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj instanceof DiscardCraftedCard){
            DiscardCraftedCard object = (DiscardCraftedCard) obj;
            return playerID == object.playerID && object.card.equals(card);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("DiscardCraftedCard",playerID,card);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " discards crafted" + card.suit.toString() + " card " + card.toString();
    }
}
