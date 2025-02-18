package games.descent2e.actions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.abilities.HeroAbilities;
import games.descent2e.components.Figure;

import games.descent2e.components.Hero;

public class EndFigureTurn extends DescentAction{

    public EndFigureTurn() {
        super(Triggers.ACTION_POINT_SPEND);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
    @Override
    public String toString() { return "End Turn"; }

    @Override
    public boolean execute(DescentGameState dgs) {

        Figure f = dgs.getActingFigure();
        f.getNActionsExecuted().setToMax();

        endOfTurn(dgs, f);

        return true;
    }

    // Performs all the End Of Turn cleanup for a given Figure
    // This ensures that the same events happen regardless of whether the EndTurn action is taken
    // Or the game forces a figure to end their turn because they can't do anything else
    public void endOfTurn (DescentGameState dgs, Figure f)
    {
        f.addActionTaken("End Turn");
        //System.out.println("End turn for " + f.getName() + " (" + f.getComponentID() + ") - [" + f.getPosition() + "]");
        //collision(dgs);
        // Removes all attribute tests taken this turn from the list, so we can check them again next turn
        f.clearAttributeTest();

        // If we are Immobilized, remove that condition now
        if(f.hasCondition(DescentTypes.DescentCondition.Immobilize)) { f.removeCondition(DescentTypes.DescentCondition.Immobilize); }

        if (f instanceof Hero)
        {
            // If the Hero has rested, restore their Fatigue
            if (((Hero) f).hasRested()) {
                f.getAttribute(Figure.Attribute.Fatigue).setValue(0);
                ((Hero) f).setRested(false);
            }

            // Syndrael's Hero Ability
            // If Syndrael has not moved this turn, recover 2 Fatigue
            if (((Hero) f).getAbility().equals(HeroAbilities.HeroAbility.HealFatigueOnWait))
                HeroAbilities.syndrael(dgs);

            // Some Heroes can only use their Hero Ability once per turn
            // If they have used their Hero Ability this turn, refresh it
            ((Hero) f).setUsedHeroAbility(false);
        }

        f.setHasMoved(false);
        f.setHasAttacked(false);
        f.setRemovedConditionThisTurn(false);
        f.setUsedExtraAction(false);


    }

    @Override
    public DescentAction copy() {
        return this;
    }

    @Override
    public int hashCode() {
        return 111404;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        return true;
    }
}
