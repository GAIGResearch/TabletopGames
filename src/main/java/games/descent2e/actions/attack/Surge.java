package games.descent2e.actions.attack;

import games.descent2e.DescentGameState;
import games.descent2e.components.Figure;

import java.util.function.BiConsumer;

public enum Surge {
    RANGE_PLUS_1((a, s) -> a.addRange(1)),
    PIERCE_2((a, s) -> a.addPierce(2)),
    STUN((a, s) -> a.setStunning(true)),
    RUNIC_KNOWLEDGE((a, s) -> {
        int health = s.getActingFigure().getAttribute(Figure.Attribute.Health).getValue();
        int fatigue = s.getActingFigure().getAttribute(Figure.Attribute.Fatigue).getValue();
        s.getActingFigure().setAttribute(Figure.Attribute.Health, health + 2);
        s.getActingFigure().setAttribute(Figure.Attribute.Fatigue, fatigue + 1);
    });

    private final BiConsumer<MeleeAttack, DescentGameState> lambda;

    Surge(BiConsumer<MeleeAttack, DescentGameState> lambda) {
        this.lambda = lambda;
    }

    public void apply(MeleeAttack attack, DescentGameState state) {
        lambda.accept(attack, state);
    }
}
