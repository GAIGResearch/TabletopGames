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

public class CollectMoneyAction extends DrawCard {

    private final int availableLoot;
    private final int loot;

    public CollectMoneyAction(int plannedActions, int playerDeck,
                              int loot, int availableLoot) {
        super(plannedActions, playerDeck);

        this.loot = loot;
        this.availableLoot = availableLoot;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        super.execute(gameState);
        if (loot == -1) {
            return false;
        }

        Deck<Loot> availableLootDeck = (Deck<Loot>) gameState.getComponentById(availableLoot);
        for (Loot available : availableLootDeck.getComponents()){
            if (available.getComponentID() == loot) {
                ColtExpressCard card = (ColtExpressCard) getCard(gameState);
                ((ColtExpressGameState) gameState).addLoot(card.playerID, available);
                return true;
            }
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
        if (loot == -1)
            return "Attempt to collect loot but no loot is available";
        return "Collect loot";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Deck<Loot> availableLootDeck = (Deck<Loot>) gameState.getComponentById(availableLoot);
        ColtExpressTypes.LootType lt = null;
        for (Loot available : availableLootDeck.getComponents()){
            if (available.getComponentID() == loot) {
                lt = available.getLootType();
            }
        }
        if (loot == -1 || lt == null) {
            return "Collect loot (none)";
        } else {
            return "Collect loot (" + lt + " " + loot + ")";
        }
    }

    @Override
    public AbstractAction copy() {
        return new CollectMoneyAction(deckFrom, deckTo, loot, availableLoot);
    }
}
