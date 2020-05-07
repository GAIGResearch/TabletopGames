package games.pandemic.engine.conditions;

import core.AbstractGameState;
import games.pandemic.PandemicGameState;

public class EnoughDraws extends ConditionNode {
    int cards_to_draw;

    public EnoughDraws(int cards_to_draw) {
        super();
        this.cards_to_draw = cards_to_draw;
    }

    @Override
    public boolean test(AbstractGameState gs) {
        return ((PandemicGameState)gs).getNCardsDrawn() >= cards_to_draw;
    }
}
