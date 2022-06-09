package games.descent2e.actions.attack;

import games.descent2e.DescentGameState;
import games.descent2e.components.Figure;

import java.util.function.BiConsumer;

public enum Surge {
    RANGE_PLUS_1(1, (a, s) -> a.addRange(1)),
    PIERCE_2(1, (a, s) -> a.addPierce(2)),
    STUN(1, (a, s) -> a.setStunning(true)), // TODO: This doesn't yet have any actual effect
    RUNIC_KNOWLEDGE(1, (a, s) -> {
        int health = s.getActingFigure().getAttribute(Figure.Attribute.Health).getValue();
        int fatigue = s.getActingFigure().getAttribute(Figure.Attribute.Fatigue).getValue();
        s.getActingFigure().setAttribute(Figure.Attribute.Health, health + 2);
        s.getActingFigure().setAttribute(Figure.Attribute.Fatigue, fatigue + 1);
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
        attack.surgesToSpend -= surgesUsed;
        lambda.accept(attack, state);
    }
}
