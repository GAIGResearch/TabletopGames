package games.descent2e.actions.searchcards;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.BoardNode;
import core.components.Deck;
import core.components.GridBoard;
import core.interfaces.IExtendedSequence;
import core.properties.PropertyInt;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;

import static core.CoreConstants.playersHash;
import static utilities.Utils.getNeighbourhood;

public class UseStaminaPotion extends DescentAction implements IExtendedSequence {
    int toRestoreID;

    public UseStaminaPotion(int toRestoreID) {
        super(Triggers.ANYTIME);
        this.toRestoreID = toRestoreID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Use stamina potion on " + gameState.getComponentById(toRestoreID).getComponentName();
    }

    @Override
    public String toString() {
        return "Use stamina potion";
    }

    @Override
    public boolean execute(DescentGameState gs) {
        // Heal hero
        Hero hero = (Hero) gs.getComponentById(toRestoreID);
        hero.setAttributeToMin(Figure.Attribute.Fatigue);
        hero.addActionTaken(toString());
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {

        // Data definition
        List<AbstractAction> actions = new ArrayList<>();
        DescentGameState dgs = (DescentGameState) state;

        // Add Hero to Actions
        Hero executer = (Hero) dgs.getActingFigure();
        actions.add(new UseHealthPotion(executer.getComponentID()));

        // Get Neighbours
        if (executer.getOwnerId() == ((DescentGameState) state).getActingFigure().getOwnerId()) {
            Vector2D loc = executer.getPosition();
            GridBoard board = dgs.getMasterBoard();
            List<Vector2D> neighbours = getNeighbourhood(loc.getX(), loc.getY(), board.getWidth(), board.getHeight(), true);
            for (Vector2D n : neighbours) {
                BoardNode bn = board.getElement(n.getX(), n.getY());
                if (bn != null) {
                    PropertyInt figureAtNode = ((PropertyInt) bn.getProperty(playersHash));
                    if (figureAtNode != null && figureAtNode.value != -1) {
                        Figure f = (Figure) dgs.getComponentById(figureAtNode.value);
                        if (f instanceof Hero) {
                            actions.add(new UseHealthPotion(f.getComponentID()));
                        }
                    }
                }
            }
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return state.getCurrentPlayer();
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {

    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return toRestoreID != -1;
    }

    @Override
    public UseStaminaPotion copy() {
        return new UseStaminaPotion(toRestoreID);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Deck<DescentCard> heroEquipment = ((Hero) dgs.getActingFigure()).getOtherEquipment();
        return heroEquipment.stream()
                .anyMatch(a -> a.getComponentName().equals("Stamina Potion"));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UseStaminaPotion other)) return false;
        return toRestoreID == other.toRestoreID;
    }

    @Override
    public int hashCode() {
        return toRestoreID - 9867;
    }
}
