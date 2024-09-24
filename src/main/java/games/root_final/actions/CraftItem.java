package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.cards.EyrieRulers;
import games.root_final.cards.RootCard;
import games.root_final.components.Item;

import java.util.Objects;

public class CraftItem extends AbstractAction {
    public final int playerID;
    public final Item.ItemType itemType;
    public RootCard card;

    public CraftItem(int playerID, Item.ItemType itemType, RootCard card){
        this.playerID = playerID;
        this.itemType = itemType;
        this.card = card;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        RootParameters rp = (RootParameters) gs.getGameParameters();
        if (currentState.getCurrentPlayer() == playerID && itemType == card.getCraftableItem()){
            PartialObservableDeck<RootCard> hand = currentState.getPlayerHand(playerID);
            for (int i = 0; i < hand.getSize(); i++){
                if (hand.get(i).equals(card)){
                    hand.remove(i);
                }
            }

            for (Item item: currentState.getCraftableItems()){
                if (item.itemType == itemType){
                    currentState.getPlayerCraftedItems(playerID).add(item);
                    currentState.getCraftableItems().remove(item);
                    if (currentState.getPlayerFaction(playerID) != RootParameters.Factions.EyrieDynasties || currentState.getRuler().ruler == EyrieRulers.CardType.Builder) {
                        currentState.addGameScorePLayer(playerID, rp.itemCraftPoints.get(item.itemType));
                    } else {
                        currentState.addGameScorePLayer(playerID, rp.itemCraftPoints.get(item.itemType));
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new CraftItem(playerID, itemType, card);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof CraftItem ci){
            return playerID == ci.playerID && itemType == ci.itemType && card.equals(ci.card);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("CraftItem", playerID, card.hashCode(), itemType);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " crafts " + itemType.toString();
    }
}
