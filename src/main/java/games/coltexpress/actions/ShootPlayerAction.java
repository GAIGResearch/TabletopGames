package games.coltexpress.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Compartment;

import java.util.Objects;

public class ShootPlayerAction extends DrawCard {

    private final int targetID;
    private final int playerCompartment;
    private final int targetCompartment;

    private final boolean isDjango;

    public ShootPlayerAction(int plannedActions, int playerDeck, int cardIdx,
                             int playerCompartment, int targetCompartment, int targetID, boolean isDjango) {
        super(plannedActions, playerDeck, cardIdx);
        this.targetID = targetID;
        this.playerCompartment = playerCompartment;
        this.targetCompartment = targetCompartment;
        this.isDjango = isDjango;
    }


    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);
        if (targetID == -1)
            return false;

        Compartment player = (Compartment) gs.getComponentById(playerCompartment);
        Compartment target = (Compartment) gs.getComponentById(targetCompartment);
        ColtExpressCard card = (ColtExpressCard) getCard(gs);

        int direction = target.getCompartmentID() - player.getCompartmentID();

        ColtExpressGameState cegs = ((ColtExpressGameState) gs);
        cegs.addBullet(targetID, card.playerID);

        if (isDjango){
            if (direction == 0)
            {
                throw new IllegalArgumentException("when shooting the player needs to be in a different " +
                        "compartment than its target");
            }

            int movementIndex;
            if (direction > 0){
                movementIndex = target.getCompartmentID() + 1;
            } else{
                movementIndex = target.getCompartmentID() - 1;
            }

            if (movementIndex > 0 && movementIndex <= cegs.getNPlayers())
            {
                Compartment movementCompartment = cegs.getTrainCompartments().get(movementIndex);

                // django's shots can move the player
                if (target.playersInsideCompartment.contains(targetID)) {
                    target.playersInsideCompartment.remove(targetID);

                    if (movementCompartment.containsMarshal) {
                        cegs.addNeutralBullet(targetID);
                        movementCompartment.playersOnTopOfCompartment.add(targetID);
                    } else
                        movementCompartment.playersInsideCompartment.add(targetID);
                }
            }
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShootPlayerAction)) return false;
        if (!super.equals(o)) return false;
        ShootPlayerAction that = (ShootPlayerAction) o;
        return targetID == that.targetID &&
                playerCompartment == that.playerCompartment &&
                targetCompartment == that.targetCompartment &&
                isDjango == that.isDjango;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), targetID, playerCompartment, targetCompartment, isDjango);
    }

    public String toString(){
        if (targetID != -1)
            return "Shoot player " + targetID;
        return "Player attempts to shoot but has not target available";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        if (targetID == -1) return "Shoot nobody";

        Compartment target = (Compartment) gameState.getComponentById(targetCompartment);
        String character = ((ColtExpressGameState)gameState).getPlayerCharacters().get(targetID).name();
        return "Shoot " + character + (isDjango?" to c=" + target.getCompartmentID(): "");
    }

    @Override
    public AbstractAction copy() {
        return new ShootPlayerAction(deckFrom, deckTo, fromIndex, playerCompartment, targetCompartment, targetID, isDjango);
    }
}
