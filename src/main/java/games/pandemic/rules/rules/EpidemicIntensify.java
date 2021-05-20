package games.pandemic.rules.rules;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;
import core.rules.Node;
import core.rules.nodetypes.RuleNode;
import games.pandemic.PandemicGameState;

import java.util.Random;

import static games.pandemic.PandemicConstants.infectionDiscardHash;
import static games.pandemic.PandemicConstants.infectionHash;

@SuppressWarnings("unchecked")
public class EpidemicIntensify extends RuleNode {
    Random rnd;

    public EpidemicIntensify(Random rnd) {
        super();
        this.rnd = rnd;
    }

    /**
     * Copy constructor
     * @param epidemicIntensify - Node to be copied
     */
    public EpidemicIntensify(EpidemicIntensify epidemicIntensify) {
        super(epidemicIntensify);
        this.rnd = epidemicIntensify.rnd;
    }

    @Override
    protected boolean run(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState)gs;
        Deck<Card> infectionDiscard = (Deck<Card>) pgs.getComponent(infectionDiscardHash);
        Deck<Card> infectionDeck = (Deck<Card>) pgs.getComponent(infectionHash);
        // 3. shuffle infection discard deck, add back on top of infection deck
        infectionDiscard.shuffle(rnd);
        infectionDeck.add(infectionDiscard);
        infectionDiscard.clear();
        return true;
    }

    @Override
    protected Node _copy() {
        return new EpidemicIntensify(this);
    }
}
