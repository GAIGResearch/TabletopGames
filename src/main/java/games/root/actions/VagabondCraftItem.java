package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.cards.RootCard;
import games.root.components.Item;

import java.util.Objects;

public class VagabondCraftItem extends AbstractAction {
    public final int playerID;
    public final Item.ItemType itemType;
    public final int cardIdx, cardId;

    public VagabondCraftItem(int playerID, Item.ItemType itemType, int cardIdx, int cardId){
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
            RootCard card = hand.pick(cardIdx);

            if (itemType == card.getCraftableItem()) {
                for (Item item: currentState.getCraftableItems()){
                    if (item.itemType == itemType){
                        switch (itemType){
                            case bag:
                                currentState.getBags().add(item);
                                break;
                            case coin:
                                currentState.getCoins().add(item);
                                break;
                            case tea:
                                currentState.getTeas().add(item);
                                break;
                            default:
                                currentState.getSatchel().add(item);
                                break;
                        }
                        currentState.addGameScorePlayer(playerID, rp.itemCraftPoints.get(item.itemType));
                        currentState.getCraftableItems().remove(item);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public VagabondCraftItem copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VagabondCraftItem that = (VagabondCraftItem) o;
        return playerID == that.playerID && cardIdx == that.cardIdx && cardId == that.cardId && itemType == that.itemType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, itemType, cardIdx, cardId);
    }

    @Override
    public String toString() {
        return "p" + playerID + " crafts " + itemType.toString();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " crafts " + itemType.toString();
    }
}
