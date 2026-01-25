package games.descent2e.actions.searchcards;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.*;
import core.interfaces.IExtendedSequence;
import core.properties.PropertyInt;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static core.CoreConstants.playersHash;
import static utilities.Utils.getNeighbourhood;

public class UseCurseDoll extends DescentAction implements IExtendedSequence {

    int toCureID;
    DescentTypes.DescentCondition conditionToCure;
    boolean complete;

    public UseCurseDoll() {
        super(Triggers.ACTION_POINT_SPEND);
        this.toCureID = -1;
        this.conditionToCure = null;
    }

    public UseCurseDoll(int toCureID, DescentTypes.DescentCondition conditionToCure) {
        super(Triggers.ACTION_POINT_SPEND);
        this.toCureID = toCureID;
        this.conditionToCure = conditionToCure;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return toCureID != 1 && conditionToCure != null? "Curing " + conditionToCure + " from " + toCureID : "Use Curse Doll";
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {

        // Data Def
        List<AbstractAction> actions = new ArrayList<>();
        DescentGameState dgs = (DescentGameState) state;

        //Player Actions
        Hero playerHero = (Hero) dgs.getActingFigure();
        if (!playerHero.getConditions().isEmpty()) {
            for (DescentTypes.DescentCondition condition : playerHero.getConditions()) {
                actions.add(new UseCurseDoll(playerHero.getComponentID(), condition));
            }
        }

        //Neighbour Actions
        if (playerHero.getOwnerId() == ((DescentGameState) state).getActingFigure().getOwnerId()) {
            Vector2D loc = playerHero.getPosition();
            GridBoard board = dgs.getMasterBoard();
            List<Vector2D> neighbours = getNeighbourhood(loc.getX(), loc.getY(), board.getWidth(), board.getHeight(), true);
            for (Vector2D n : neighbours) {
                BoardNode bn = board.getElement(n.getX(), n.getY());
                if (bn != null) {
                    PropertyInt figureAtNode = ((PropertyInt) bn.getProperty(playersHash));
                    if (figureAtNode != null && figureAtNode.value != -1) {
                        Figure f = (Figure) dgs.getComponentById(figureAtNode.value);
                        if (f instanceof Hero && !f.getConditions().isEmpty()) {
                            for (DescentTypes.DescentCondition condition : f.getConditions()) {
                                actions.add(new UseCurseDoll(f.getComponentID(), condition));
                            }
                        }
                    }
                }
            }
        }

        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        DescentGameState dgs = (DescentGameState) state;
        return dgs.getActingFigure().getOwnerId();
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        complete = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return complete;
    }

    @Override
    public boolean execute(DescentGameState gs) {
        if (toCureID != -1 && conditionToCure != null) {
            Hero hero = (Hero) gs.getComponentById(toCureID);
            hero.getConditions().remove(conditionToCure);

            // Remove Card
            Deck<DescentCard> heroEquipment = ((Hero) gs.getActingFigure()).getOtherEquipment();
            DescentCard card = heroEquipment.stream()
                    .filter(a -> a.getComponentName().equals("Curse Doll"))
                    .findAny().orElseThrow(() -> new AssertionError("Card not found: Curse Doll"));
            heroEquipment.remove(card);

            hero.getNActionsExecuted().increment();

            hero.addActionTaken(toString());
        }
        else {
            gs.setActionInProgress(this);
        }
        return true;
    }

    @Override
    public UseCurseDoll copy() {
        UseCurseDoll retValue = new UseCurseDoll(toCureID, conditionToCure);
        retValue.complete = complete;
        return retValue;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        if (dgs.getActingFigure().getNActionsExecuted().isMaximum()) return false;

        Deck<DescentCard> heroEquipment = ((Hero) dgs.getActingFigure()).getOtherEquipment();
        return heroEquipment.stream()
                .anyMatch(a -> a.getComponentName().equals("Curse Doll"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UseCurseDoll that)) return false;
        return toCureID == that.toCureID &&
                conditionToCure == that.conditionToCure &&
                complete == that.complete;
    }

    @Override
    public int hashCode() {
        return Objects.hash(toCureID, conditionToCure, complete);
    }
}
