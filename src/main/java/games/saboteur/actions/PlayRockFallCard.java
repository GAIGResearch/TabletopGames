package games.saboteur.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.Deck;
import games.saboteur.SaboteurGameState;
import games.saboteur.components.ActionCard;
import games.saboteur.components.SaboteurCard;


public class PlayRockFallCard extends SetGridValueAction
{
    private final ActionCard rockFallCard;
    public PlayRockFallCard(int gridBoard, int x, int y, ActionCard rockFallCard) {
        super(gridBoard, x, y, null);
        this.rockFallCard = rockFallCard;
    }


    public boolean execute(AbstractGameState gs) {
        SaboteurGameState sgs = (SaboteurGameState) gs;
        sgs.gridBoard.setElement(getX(), getY(), null);

        Deck<SaboteurCard> currentDeck = sgs.playerDecks.get(sgs.getCurrentPlayer());
        currentDeck.remove(rockFallCard);

        System.out.println(this);
        System.out.println(sgs.gridBoard.toString());
        return false;
    }
    @Override
    public AbstractAction copy() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    public String toString() {
        return "RockFall at (" + super.getX() + ", " + super.getY() + ")";
    }
}

