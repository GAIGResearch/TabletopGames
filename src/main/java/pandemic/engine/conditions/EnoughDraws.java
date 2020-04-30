package pandemic.engine.conditions;

import core.GameState;
import pandemic.PandemicGameState;
import pandemic.engine.Node;

public class EnoughDraws extends ConditionNode {
    int cards_to_draw;

    public EnoughDraws(int cards_to_draw, Node yes, Node no) {
        super(yes, no);
        this.cards_to_draw = cards_to_draw;
    }

    @Override
    public boolean test(GameState gs) {
        return ((PandemicGameState)gs).getNCardsDrawn() == cards_to_draw;
    }
}
