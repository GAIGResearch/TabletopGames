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

public class UseHealthPotion extends DescentAction implements IExtendedSequence {
    int toHealID;

    public UseHealthPotion(int toHealID) {
        super(Triggers.ACTION_POINT_SPEND);
        this.toHealID = toHealID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Use health potion on " + gameState.getComponentById(toHealID).getComponentName();
    }

    @Override
    public String toString() {
        return "Use health potion";
    }

    @Override
    public boolean execute(DescentGameState gs) {
        // Heal hero
        Hero hero = (Hero) gs.getComponentById(toHealID);
        hero.setAttributeToMax(Figure.Attribute.Health);
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
        return toHealID != -1;
    }

    @Override
    public UseHealthPotion copy() {
        return new UseHealthPotion(toHealID);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Deck<DescentCard> heroEquipment = ((Hero) dgs.getActingFigure()).getOtherEquipment();
        return heroEquipment.stream()
                .anyMatch(a -> a.getComponentName().equals("Health Potion"));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UseHealthPotion other)) return false;
        return toHealID == other.toHealID;
    }

    @Override
    public int hashCode() {
        return toHealID - 789793 + 31;
    }
}
