package games.coltexpress.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Random;
import java.util.Set;


public class PunchAction  extends DrawCard {

    private final int opponentID;
    private final int sourceCompartment;
    private final int targetCompartment;
    private final int loot;
    private final int availableLoot;
    private final boolean playerIsCheyenne;

    public PunchAction(int plannedActions, int playerDeck,
                       int opponentID, int sourceCompartment, int targetCompartment, int loot,
                       int availableLoot, boolean playerIsCheyenne) {
        super(plannedActions, playerDeck);
        this.opponentID = opponentID;
        this.sourceCompartment = sourceCompartment;
        this.targetCompartment = targetCompartment;
        this.loot = loot;

        this.availableLoot = availableLoot;
        this.playerIsCheyenne = playerIsCheyenne;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        super.execute(gameState);
        if (opponentID == -1)
            return false;    //no player to punch

        Compartment source = (Compartment) gameState.getComponentById(sourceCompartment);
        Compartment target = (Compartment) gameState.getComponentById(targetCompartment);
        ColtExpressCard card = (ColtExpressCard) getCard(gameState);
        Deck<Loot> availableLootDeck = (Deck<Loot>) gameState.getComponentById(availableLoot);

        //move player
        Set<Integer> sourceArea;
        Set<Integer> targetArea;
        PartialObservableDeck<Loot> targetLootArea;
        if (source.playersOnTopOfCompartment.contains(opponentID)){
            sourceArea = source.playersOnTopOfCompartment;
            targetLootArea = source.lootOnTop;
            targetArea = target.playersOnTopOfCompartment;
        } else {
            sourceArea = source.playersInsideCompartment;
            targetLootArea = source.lootInside;
            if (target.containsMarshal)
            {
                targetArea = target.playersOnTopOfCompartment;
                ((ColtExpressGameState) gameState).addNeutralBullet(opponentID);
            } else
                targetArea = target.playersInsideCompartment;

        }
        sourceArea.remove(opponentID);
        targetArea.add(opponentID);

        //drop loot
        LinkedList<Loot> potentialLoot = new LinkedList<>();
        if (loot != -1){
            for (Loot l : availableLootDeck.getComponents()){
                if (l.getComponentID() == loot)
                    potentialLoot.add(l);
            }

            if (potentialLoot.size() > 0){
                Loot chosenLoot = potentialLoot.get(new Random().nextInt(potentialLoot.size()));
                if (playerIsCheyenne)
                    ((ColtExpressGameState) gameState).addLoot(card.playerID, chosenLoot);
                else
                    targetLootArea.add(chosenLoot);
                availableLootDeck.remove(chosenLoot);
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PunchAction that = (PunchAction) o;
        return opponentID == that.opponentID &&
                sourceCompartment == that.sourceCompartment &&
                targetCompartment == that.targetCompartment &&
                loot == that.loot &&
                availableLoot == that.availableLoot &&
                playerIsCheyenne == that.playerIsCheyenne;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), opponentID, sourceCompartment, targetCompartment, loot, availableLoot, playerIsCheyenne);
    }

    @Override
    public String toString(){
        if (opponentID == -1)
            return "Attempt to punch player, but no player is available.";
        if (loot == -1)
            return "Punch player " + opponentID + " without him dropping any loot.";
        if (playerIsCheyenne)
            return "Punch player " + opponentID + " and (maybe) steal him a random Purse";
        return "Punch player " + opponentID + " and let him drop " + loot;
    }

    @Override
    public AbstractAction copy() {
        return new PunchAction(deckFrom, deckTo, opponentID, sourceCompartment, targetCompartment, loot, availableLoot, playerIsCheyenne);
    }
}
