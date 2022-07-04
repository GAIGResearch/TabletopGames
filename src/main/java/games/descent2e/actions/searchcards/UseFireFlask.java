package games.descent2e.actions.searchcards;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.attack.RangedAttack;
import games.descent2e.components.DicePool;
import games.descent2e.components.Hero;

import java.util.Collections;

public class UseFireFlask extends DescentAction {
    int heroID;
    int enemyID;


    public UseFireFlask() {
        super(Triggers.ACTION_POINT_SPEND);
    }

    public UseFireFlask(int heroID, int enemyID) {
        super(Triggers.ACTION_POINT_SPEND);
        this.heroID = heroID;
        this.enemyID = enemyID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return null;
    }

    @Override
    public boolean execute(DescentGameState gs) {
        gs.setAttackDicePool(DicePool.constructDicePool("BlUE", "YELLOW"));
        RangedAttack attack = new RangedAttack(heroID, enemyID);
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
        Deck<Card> heroEquipment = ((Hero) dgs.getActingFigure()).getOtherEquipment();
        return heroEquipment.stream()
                .anyMatch(a -> a.getComponentName().equals("Fire Flask"));
    }
}
