package games.descent2e.actions.herofeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.DicePool;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.List;
import java.util.Objects;

public class HealAllInRange extends DescentAction {

    // Avric Albright Heroic Feat
    String heroName = "Avric Albright";
    List<Hero> heroesInRange;
    int range;
    public HealAllInRange(List<Hero> heroesInRange, int range) {
        super(Triggers.ACTION_POINT_SPEND);
        this.range = range;
        this.heroesInRange = heroesInRange;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Hero f = (Hero) dgs.getActingFigure();

        // Health recovery: roll 2 red dice
        DicePool.revive.roll(dgs.getRandom());
        for (Hero hero : heroesInRange) {
            hero.incrementAttribute(Figure.Attribute.Health, DicePool.revive.getDamage());
            if (hero.isDefeated())
                hero.setDefeated(dgs, false);
        }
        f.setFeatAvailable(false);
        f.getNActionsExecuted().increment();
        return true;
    }

    @Override
    public HealAllInRange copy() {
        return new HealAllInRange(heroesInRange, range);
    }

    boolean canHealHeroes() {
        // Check all heroes in range
        // If at least one of them is not at full HP, we can heal them
        for(Hero hero : heroesInRange) {
            if(hero.getAttributeValue(Figure.Attribute.Health) < hero.getAttributeMax(Figure.Attribute.Health)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Hero f = (Hero) dgs.getActingFigure();
        boolean canHealHeroes = canHealHeroes();
        return  f.getName().contains(heroName) && f.isFeatAvailable() && !f.getNActionsExecuted().isMaximum() && canHealHeroes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof HealAllInRange) {
            HealAllInRange other = (HealAllInRange) obj;
            return other.heroesInRange.equals(heroesInRange) && other.range == range;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(heroesInRange, range);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Heroic Feat: Heal all Heroes in " + range + " spaces for 2 Red Power Dice";
    }
}
