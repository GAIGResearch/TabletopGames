package games.coltexpress.actions;

import core.AbstractGameState;
import core.components.PartialObservableDeck;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Loot;

import java.util.LinkedList;
import java.util.Random;

public class CollectMoneyAction extends ColtExpressExecuteCardAction {

    private final PartialObservableDeck<Loot> availableLoot;
    private final Loot.LootType lootType;

    public CollectMoneyAction(ColtExpressCard card, PartialObservableDeck<ColtExpressCard> plannedActions,
                              PartialObservableDeck<ColtExpressCard> playerDeck, Loot.LootType lootType,
                              PartialObservableDeck<Loot> availableLoot) {
        super(card, plannedActions, playerDeck);
        this.lootType = lootType;
        this.availableLoot = availableLoot;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        super.execute(gameState);
        if (lootType == null)
            return true;

        LinkedList<Loot> lootOfCorrectType = new LinkedList<>();
        for (Loot loot : availableLoot.getComponents()){
            if (loot.getLootType() == lootType)
                lootOfCorrectType.add(loot);
        }

        if (lootOfCorrectType.size() == 0){
            System.out.println();
        }
        ((ColtExpressGameState) gameState).addLoot(card.playerID,
                lootOfCorrectType.get(new Random().nextInt(lootOfCorrectType.size())));

        return true;
    }

    @Override
    public boolean equals(Object obj) {
        throw new UnsupportedOperationException();
        //return false;
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    public String toString(){
        if (lootType == null)
            return "Attempt to collect loot but no loot is available";
        return "Collect random loot of type " + lootType;
    }
}
