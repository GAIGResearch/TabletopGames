package games.descent2e.actions.attack;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.actions.AttributeTest;
import games.descent2e.actions.herofeats.DoubleMoveAttack;
import games.descent2e.actions.herofeats.HeroicFeatExtraMovement;
import org.apache.hadoop.shaded.org.apache.kerby.kerberos.kerb.crypto.util.Des;

import javax.management.Attribute;
import java.util.Objects;

public class EndCurrentPhase extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        IExtendedSequence action = Objects.requireNonNull(gs.currentActionInProgress());
        if (action instanceof MeleeAttack) {
            ((MeleeAttack) action).setSkip(true);
        }
        if (action instanceof AttributeTest) {
            ((AttributeTest) action).setSkip(true);
        }

        ((DescentGameState) gs).getActingFigure().addActionTaken(toString());
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EndCurrentPhase;
    }

    @Override
    public int hashCode() {
        return 490404;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "End current decision phase";
    }
}
