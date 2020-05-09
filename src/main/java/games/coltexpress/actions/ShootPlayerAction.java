package games.coltexpress.actions;

import core.AbstractGameState;
import core.components.PartialObservableDeck;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Compartment;

public class ShootPlayerAction extends ColtExpressExecuteCardAction{

    private final int targetID;
    private final Compartment playerCompartment;
    private final Compartment targetCompartment;

    private final boolean isDjango;

    public ShootPlayerAction(ColtExpressCard card, PartialObservableDeck<ColtExpressCard> plannedActions,
                             PartialObservableDeck<ColtExpressCard> playerDeck,
                             Compartment playerCompartment,
                             int targetID,
                             Compartment targetCompartment, boolean isDjango) {
        super(card, plannedActions, playerDeck);
        this.targetID = targetID;
        this.playerCompartment = playerCompartment;
        this.targetCompartment = targetCompartment;
        this.isDjango = isDjango;
    }


    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);
        ColtExpressGameState cegs = ((ColtExpressGameState) gs);
        cegs.addBullet(targetID, card.playerID);

        if (isDjango){
            int direction = targetCompartment.id - playerCompartment.id;
            if (direction == 0)
            {
                throw new IllegalArgumentException("when django shoots the player needs to be in a different " +
                        "compartment than its target");
            }
            int movementIndex;
            if (direction > 0){
                movementIndex = targetCompartment.id + 1;
            } else{
                movementIndex = targetCompartment.id - 1;
            }

            if (movementIndex > 0 && movementIndex <= cegs.getNPlayers())
            {
                Compartment movementCompartment = cegs.getTrain().getCompartment(movementIndex);

                // django's shots can move the player
                if (targetCompartment.playersInsideCompartment.contains(targetID)) {
                    targetCompartment.playersInsideCompartment.remove(targetID);
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

    public String toString(){
        return "Player " + card.playerID + " shoots player " + targetID;
    }
}
