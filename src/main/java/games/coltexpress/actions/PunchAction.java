package games.coltexpress.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.Deck;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressTypes;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;


public class PunchAction  extends DrawCard {

    private final int opponentID;
    private final int sourceCompartment;
    private final int targetCompartment;
    private final ColtExpressTypes.LootType loot;
    private final int availableLoot;
    private final boolean playerIsCheyenne;

    public PunchAction(int plannedActions, int playerDeck, int cardIdx,
                       int opponentID, int sourceCompartment, int targetCompartment, ColtExpressTypes.LootType loot,
                       int availableLoot, boolean playerIsCheyenne) {
        super(plannedActions, playerDeck, cardIdx);
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
        Deck<Loot> targetLootArea;
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
        if (loot != null){
            for (Loot l : availableLootDeck.getComponents()){
                if (l.getLootType() == loot)
                    potentialLoot.add(l);
            }

            if (!potentialLoot.isEmpty()){
                Loot chosenLoot = potentialLoot.get(gameState.getRnd().nextInt(potentialLoot.size()));
                if (playerIsCheyenne && loot == ColtExpressTypes.LootType.Purse)
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
        if (!(o instanceof PunchAction)) return false;
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
    public String getString(AbstractGameState gameState) {
        if (opponentID == -1) return "Punch nobody";

        String character = ((ColtExpressGameState)gameState).getPlayerCharacters().get(opponentID).name();
        Compartment target = (Compartment) gameState.getComponentById(targetCompartment);
        int tIdx = target.getCompartmentID();

        String drop = "nothing";
        if (loot != null) {
            drop = loot.name();
        }

        return "Punch " + character + " to c=" + tIdx + " dropping " + drop;
    }

    @Override
    public String toString(){
        if (opponentID == -1)
            return "Attempt to punch player, but no player is available.";
        if (loot == null)
            return "Punch player " + opponentID + " without him dropping any loot.";
        if (playerIsCheyenne)
            return "Punch player " + opponentID + " and (maybe) steal him a random Purse";
        return "Punch player " + opponentID + " and let him drop " + loot;
    }

    @Override
    public AbstractAction copy() {
        return new PunchAction(deckFrom, deckTo, fromIndex, opponentID, sourceCompartment, targetCompartment, loot, availableLoot, playerIsCheyenne);
    }
}
