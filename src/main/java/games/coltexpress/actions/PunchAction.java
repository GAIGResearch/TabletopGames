package games.coltexpress.actions;

import core.AbstractGameState;
import core.components.PartialObservableDeck;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;

import java.util.LinkedList;
import java.util.Random;
import java.util.Set;


public class PunchAction  extends ColtExpressExecuteCardAction{

    private final int opponentID;
    private final Compartment sourceCompartment;
    private final Compartment targetCompartment;
    private final Loot.LootType lootType;
    private final PartialObservableDeck<Loot> availableLoot;
    private final boolean playerIsCheyenne;

    public PunchAction(ColtExpressCard card, PartialObservableDeck<ColtExpressCard> plannedActions,
                       PartialObservableDeck<ColtExpressCard> playerDeck,
                       Integer opponentID,
                       Compartment sourceCompartment,
                       Compartment targetCompartment,
                       Loot.LootType lootType,
                       PartialObservableDeck<Loot> availableLoot,
                       boolean playerIsCheyenne) {
        super(card, plannedActions, playerDeck);
        this.opponentID = opponentID;
        this.sourceCompartment = sourceCompartment;
        this.targetCompartment = targetCompartment;
        this.lootType = lootType;
        this.availableLoot = availableLoot;
        this.playerIsCheyenne = playerIsCheyenne;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        super.execute(gameState);
        if (opponentID == -1)
            return true;    //no player to punch

        //move player
        Set<Integer> sourceArea;
        Set<Integer> targetArea;
        PartialObservableDeck<Loot> targetLootArea;
        if (sourceCompartment.playersOnTopOfCompartment.contains(opponentID)){
            sourceArea = sourceCompartment.playersOnTopOfCompartment;
            targetLootArea = sourceCompartment.lootOnTop;
            targetArea = targetCompartment.playersOnTopOfCompartment;
        } else {
            sourceArea = sourceCompartment.playersInsideCompartment;
            targetLootArea = sourceCompartment.lootInside;
            if (targetCompartment.containsMarshal)
            {
                targetArea = targetCompartment.playersOnTopOfCompartment;
                ((ColtExpressGameState) gameState).addNeutralBullet(opponentID);
            } else
                targetArea = targetCompartment.playersInsideCompartment;
        }
        sourceArea.remove(opponentID);
        targetArea.add(opponentID);

        //drop loot
        LinkedList<Loot> potentialLoot = new LinkedList<>();
        if (lootType != null){
            for (Loot loot : availableLoot.getComponents()){
                if (loot.getLootType() == lootType)
                    potentialLoot.add(loot);
            }

            if (potentialLoot.size() > 0){
                Loot chosenLoot = potentialLoot.get(new Random().nextInt(potentialLoot.size()));
                if (playerIsCheyenne)
                    ((ColtExpressGameState) gameState).addLoot(card.playerID, chosenLoot);
                else
                    targetLootArea.add(chosenLoot);
                availableLoot.remove(chosenLoot);
            }
        }
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

    @Override
    public String toString(){
        if (opponentID == -1)
            return "Attempt to punch player, but no player is available.";
        if (lootType == null)
            return "Punch player " + opponentID + " without him dropping any loot.";
        if (playerIsCheyenne && lootType == Loot.LootType.Purse)
            return "Punch player " + opponentID + " and steal him a random Purse";
        return "Punch player " + opponentID + " and let him drop " + lootType;
    }
}
