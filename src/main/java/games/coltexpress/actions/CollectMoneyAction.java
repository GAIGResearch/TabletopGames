package games.coltexpress.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.Deck;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressTypes;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Loot;

import java.util.Objects;

import static core.CoreConstants.VisibilityMode;

public class CollectMoneyAction extends DrawCard {

    private final int availableLoot;
    private final ColtExpressTypes.LootType loot;

    public CollectMoneyAction(int plannedActions, int playerDeck, int cardIdx,
                              ColtExpressTypes.LootType loot, int availableLoot) {
        super(plannedActions, playerDeck, cardIdx);

        this.loot = loot;
        this.availableLoot = availableLoot;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        super.execute(gameState);
        if (loot == null) {
            return false;
        }

        // Find all loot of type
        Deck<Loot> possible = new Deck<>("tmp", VisibilityMode.HIDDEN_TO_ALL);
        Deck<Loot> availableLootDeck = (Deck<Loot>) gameState.getComponentById(availableLoot);
        for (Loot available : availableLootDeck.getComponents()){
            if (available.getLootType() == loot) {
                possible.add(available);
            }
        }

        // Choose random loot of type to collect
        if (possible.getSize() > 0) {
            Loot available = possible.pick(gameState.getRnd());
            ColtExpressCard card = (ColtExpressCard) getCard(gameState);
            ((ColtExpressGameState) gameState).addLoot(card.playerID, available);
            availableLootDeck.remove(available);
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CollectMoneyAction)) return false;
        if (!super.equals(o)) return false;
        CollectMoneyAction that = (CollectMoneyAction) o;
        return availableLoot == that.availableLoot &&
                loot == that.loot;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), availableLoot, loot);
    }

    public String toString(){
        if (loot == null)
            return "Attempt to collect loot but no loot is available";
        return "Collect " + loot.name();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        if (availableLoot == -1) return "Collect loot (none)";
        if (loot == null) {
            return "Collect loot (none)";
        } else {
            return "Collect loot (" + loot + ")";
        }
    }

    @Override
    public AbstractAction copy() {
        return new CollectMoneyAction(deckFrom, deckTo, fromIndex, loot, availableLoot);
    }
}
