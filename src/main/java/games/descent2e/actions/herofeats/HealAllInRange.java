package games.descent2e.actions.herofeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.DicePool;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HealAllInRange extends DescentAction {

    int range;
    int healthRecovered = 0;
    int heroesHealed = 0;
    public HealAllInRange(int range) {
        super(Triggers.ACTION_POINT_SPEND);
        this.range = range;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();

        // Health recovery: roll 2 red dice
        DicePool.revive.roll(dgs.getRnd());
        healthRecovered = DicePool.revive.getDamage();
        List<Hero> heroesInRange = HeroesInRange(dgs);
        if (heroesInRange != null) {
            heroesHealed = heroesInRange.size();
            for (Hero hero : heroesInRange) {
                hero.incrementAttribute(Figure.Attribute.Health, healthRecovered);
                if (hero.isDefeated())
                    hero.setDefeated(dgs, false);
            }
            if (dgs.getActingFigure() instanceof Hero) {
                ((Hero) dgs.getActingFigure()).setFeatAvailable(false);
            }
            f.getNActionsExecuted().increment();
        }

        f.addActionTaken(getString(dgs));

        return true;
    }

    @Override
    public HealAllInRange copy() {
        HealAllInRange healAllInRange = new HealAllInRange(range);
        healAllInRange.healthRecovered = healthRecovered;
        healAllInRange.heroesHealed = heroesHealed;
        return healAllInRange;
    }

    boolean canHealHeroes(DescentGameState dgs) {
        // Check all heroes in range
        // If at least one of them is not at full HP, we can heal them
        List<Hero> heroesInRange = HeroesInRange(dgs);
        if (heroesInRange == null) return false;
        for(Hero hero : heroesInRange) {
            if(hero.getAttributeValue(Figure.Attribute.Health) < hero.getAttributeMax(Figure.Attribute.Health)) {
                return true;
            }
        }
        return false;
    }

    private List<Hero> HeroesInRange(DescentGameState dgs)
    {
        Figure f = dgs.getActingFigure();
        List<Hero> heroesInRange = new ArrayList<>();
        for(Hero hero : dgs.getHeroes())
        {
            if (DescentHelper.inRange(f.getPosition(), hero.getPosition(), range)) {
            heroesInRange.add(hero);
            }
        }
        if (heroesInRange.isEmpty())
            return null;
        else
            return heroesInRange;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        if (f instanceof Hero && !((Hero) f).isFeatAvailable()) return false;
        return !f.getNActionsExecuted().isMaximum() && canHealHeroes(dgs);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HealAllInRange
                && ((HealAllInRange) o).range == range
                && ((HealAllInRange) o).healthRecovered == healthRecovered
                && ((HealAllInRange) o).heroesHealed == heroesHealed
                && super.equals(o);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        String retVal = "Heroic Feat: Heal all Heroes in " + range + " spaces for 2 Red Power Dice";
        if (healthRecovered > 0) {
            retVal += " (+" + healthRecovered + " Health for " + heroesHealed + " Heroes healed)";
        }
        return retVal;
    }

    @Override
    public String toString() {
        return "Heroic Feat: Group Heal";
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), range, healthRecovered, heroesHealed);
    }
}
