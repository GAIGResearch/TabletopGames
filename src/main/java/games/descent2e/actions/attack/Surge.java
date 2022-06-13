package games.descent2e.actions.attack;

import games.descent2e.DescentGameState;
import games.descent2e.components.Figure;

import java.util.Objects;
import java.util.function.BiConsumer;

// These should be immutable - do not add internal state
public enum Surge {
    RANGE_PLUS_1(1, (a, s) -> a.addRange(1)),
    PIERCE_2(1, (a, s) -> a.addPierce(2)),
    STUN(1, (a, s) -> a.setStunning(true)), // TODO: This doesn't yet have any actual effect
    RUNIC_KNOWLEDGE(1, (a, s) -> {
        int fatigue = s.getActingFigure().getAttribute(Figure.Attribute.Fatigue).getValue();
        s.getActingFigure().setAttribute(Figure.Attribute.Fatigue, fatigue + 1);
        a.addDamage(2);
    });

    private final BiConsumer<MeleeAttack, DescentGameState> lambda;
    private final int surgesUsed;

    Surge(int surges, BiConsumer<MeleeAttack, DescentGameState> lambda) {
        this.lambda = lambda;
        surgesUsed = surges;
    }

    public void apply(MeleeAttack attack, DescentGameState state) {
        if (surgesUsed > attack.surgesToSpend) {
            throw new AssertionError(String.format("%s: Requires %d surges and we only have %d to spend.", toString(), surgesUsed, attack.surgesToSpend));
        }
        // TODO: Record which Surges have been used to avoid re-use!
        attack.surgesToSpend -= surgesUsed;
        lambda.accept(attack, state);
    }

}
