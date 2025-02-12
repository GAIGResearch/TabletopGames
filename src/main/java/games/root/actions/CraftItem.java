package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.cards.EyrieRulers;
import games.root.components.cards.RootCard;
import games.root.components.Item;

import java.util.Objects;

public class CraftItem extends AbstractAction {
    public final int playerID;
    public final Item.ItemType itemType;
    public final int cardIdx, cardId;

    public CraftItem(int playerID, Item.ItemType itemType, int cardIdx, int cardId){
        this.playerID = playerID;
        this.itemType = itemType;
        this.cardIdx = cardIdx;
        this.cardId = cardId;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        RootParameters rp = (RootParameters) gs.getGameParameters();
        if (currentState.getCurrentPlayer() == playerID){
            PartialObservableDeck<RootCard> hand = currentState.getPlayerHand(playerID);
            RootCard card = hand.get(cardIdx);

            if (itemType == card.getCraftableItem()) {
                hand.remove(card);

                for (Item item : currentState.getCraftableItems()) {
                    if (item.itemType == itemType) {
                        currentState.getPlayerCraftedItems(playerID).add(item);
                        currentState.getCraftableItems().remove(item);
                        if (currentState.getPlayerFaction(playerID) != RootParameters.Factions.EyrieDynasties || currentState.getRuler().ruler == EyrieRulers.CardType.Builder) {
                            currentState.addGameScorePlayer(playerID, rp.itemCraftPoints.get(item.itemType));
                        } else {
                            currentState.addGameScorePlayer(playerID, rp.itemCraftPoints.get(item.itemType));
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public CraftItem copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CraftItem craftItem = (CraftItem) o;
        return playerID == craftItem.playerID && cardIdx == craftItem.cardIdx && cardId == craftItem.cardId && itemType == craftItem.itemType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, itemType, cardIdx, cardId);
    }

    @Override
    public String toString() {
        return "p" + playerID + " crafts " + itemType.name();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " crafts " + itemType.toString();
    }
}
