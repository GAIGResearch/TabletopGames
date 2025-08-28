package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import core.components.Component;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;

import java.util.Objects;

import static games.descent2e.DescentHelper.checkAdjacent;

public class PrayerOfPeace extends DescentAction {

    int cardID = -1;

    public PrayerOfPeace(int cardID) {
        super(Triggers.ACTION_POINT_SPEND);
        this.cardID = cardID;
    }

    @Override
    public boolean execute(DescentGameState dgs) {

        Figure f = dgs.getActingFigure();
        f.addBonus(DescentTypes.SkillBonus.PrayerOfPeace);
        f.getNActionsExecuted().increment();
        f.getAttribute(Figure.Attribute.Fatigue).increment(2);

        Component card = dgs.getComponentById(cardID);
        if (card != null)
        {
            f.exhaustCard((DescentCard) card);
        }

        f.addActionTaken(toString());

        return true;
    }

    @Override
    public DescentAction copy() {
        return new PrayerOfPeace(cardID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PrayerOfPeace that)) return false;
        return cardID == that.cardID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Prayer of Peace: Prevent adjacent Monsters from attacking this turn";
    }

    @Override
    public String toString() {
        return "Prayer of Peace " + " (" + cardID + ")";
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {

        Figure f = dgs.getActingFigure();
        if (f.getNActionsExecuted().isMaximum()) return false;

        // Can't use if already exhausted the card this turn
        if (f.isExhausted((DescentCard) dgs.getComponentById(cardID))) return false;

        // This costs 2 Fatigue to use
        return f.getAttribute(Figure.Attribute.Fatigue).getValue() + 2 <= f.getAttributeMax(Figure.Attribute.Fatigue);
    }

    public static boolean canAttackPrayer(DescentGameState dgs, Figure f) {
        if (f instanceof Monster)
        {
            for (Hero hero : dgs.getHeroes())
            {
                if (hero.hasBonus(DescentTypes.SkillBonus.PrayerOfPeace) && checkAdjacent(dgs, f, hero))
                {
                    // If any adjacent Hero has the Prayer of Peace bonus, Monsters cannot attack
                    return false;
                }
            }
        }
        return true;
    }

    public static void removePrayerBonus(Hero hero)
    {
        hero.removeBonus(DescentTypes.SkillBonus.PrayerOfPeace);
    }
}
