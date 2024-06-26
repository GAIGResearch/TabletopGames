package games.pandemic.rules.conditions;

import core.AbstractGameState;
import core.rules.Node;
import core.rules.nodetypes.ConditionNode;
import games.pandemic.PandemicGameState;

public class EnoughDraws extends ConditionNode {
    int cards_to_draw;

    public EnoughDraws(int cards_to_draw) {
        super();
        this.cards_to_draw = cards_to_draw;
    }

    /**
     * Copy constructor
     * @param enoughDraws - Node to be copied
     */
    public EnoughDraws(EnoughDraws enoughDraws) {
        super(enoughDraws);
        this.cards_to_draw = enoughDraws.cards_to_draw;
    }

    @Override
    public boolean test(AbstractGameState gs) {
        return ((PandemicGameState)gs).getNCardsDrawn() >= cards_to_draw;
    }

    @Override
    protected Node _copy() {
        return new EnoughDraws(this);
    }
}
