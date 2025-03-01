package games.descent2e.actions.attack;

import core.AbstractGameState;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.abilities.HeroAbilities;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.DescentDice;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.Objects;

public class TarhaAbilityReroll extends DescentAction {

    // Widow Tarha Hero Ability
    int dice;
    public TarhaAbilityReroll(int dice) {
        super(Triggers.ROLL_OWN_DICE);
        this.dice = dice;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        int rerollFace = HeroAbilities.tarha(dgs, dgs.getAttackDicePool().getDice(dice));
        if (rerollFace != -1) {
            dgs.getAttackDicePool().getDice(dice).setFace(rerollFace);
        }
        dgs.getActingFigure().addActionTaken(toString());
        return true;
    }

    @Override
    public TarhaAbilityReroll copy() {
        return new TarhaAbilityReroll(dice);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        // We can only use this if it interrupts an Attack action
        if (dgs.isActionInProgress()) {
            IExtendedSequence action = dgs.currentActionInProgress();
            // Ranged Attacks are instances of Melee Attacks, so both types are covered
            if (action instanceof MeleeAttack) {
                Figure f = dgs.getActingFigure();
                if (f instanceof Hero) return ((Hero) f).getAbility().equals(HeroAbilities.HeroAbility.RerollOnce) && !((Hero) f).hasUsedHeroAbility() && !f.hasRerolled();
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TarhaAbilityReroll other) {
            return other.dice == dice;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dice);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        DescentDice dice = ((DescentGameState) gameState).getAttackDicePool().getDice(this.dice);
        return "Hero Ability: Reroll " + dice.getColour() + " dice (Face: " + dice.getFace() +", Range: " + dice.getRange() + ", Damage: " + dice.getDamage() + ", Surge: " + dice.getSurge() + ")";
    }

    public String toString() {
        return "REROLL_DICE_" + dice;
    }
}