package games.descent2e.actions.searchcards;

import core.AbstractGameState;
import core.components.Deck;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.attack.RangedAttack;
import games.descent2e.components.DescentCard;
import games.descent2e.components.DicePool;
import games.descent2e.components.Hero;


public class UseFireFlask extends DescentAction {
    final int enemyID;

    public UseFireFlask(int enemyID) {
        super(Triggers.ACTION_POINT_SPEND);
        this.enemyID = enemyID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Use fire flask on " + gameState.getComponentById(enemyID).getComponentName();
    }

    @Override
    public String toString() {
        return "Use fire flask";
    }

    @Override
    public boolean execute(DescentGameState gs) {
        gs.setAttackDicePool(DicePool.constructDicePool("BlUE", "YELLOW"));
        RangedAttack attack = new RangedAttack(gs.getActingFigure().getComponentID(), enemyID);
        gs.getActingFigure().addActionTaken(toString());
        attack.execute(gs);
        //TODO Blast Surge
        return true;
    }

    @Override
    public DescentAction copy() {
        return this;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        if (dgs.getActingFigure().getNActionsExecuted().isMaximum()) return false;

        Deck<DescentCard> heroEquipment = ((Hero) dgs.getActingFigure()).getOtherEquipment();
        return heroEquipment.stream()
                .anyMatch(a -> a.getComponentName().equals("Fire Flask"));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof UseFireFlask && ((UseFireFlask) obj).enemyID == enemyID;
    }

    @Override
    public int hashCode() {
        return enemyID - 97984;
    }
}
