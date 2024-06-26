package games.pandemic.rules.rules;

import core.AbstractGameStateWithTurnOrder;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import core.rules.Node;
import core.rules.nodetypes.RuleNode;
import games.pandemic.PandemicGameState;
import games.pandemic.actions.InfectCity;

import static games.pandemic.PandemicConstants.*;

public class EpidemicInfect extends RuleNode {
    int n_cubes_epidemic;
    int max_cubes_per_city;

    public EpidemicInfect(int max_cubes_per_city, int n_cubes_epidemic) {
        super();
        this.n_cubes_epidemic = n_cubes_epidemic;
        this.max_cubes_per_city = max_cubes_per_city;
    }

    /**
     * Copy constructor
     * @param epidemicInfect - Node to be copied
     */
    public EpidemicInfect(EpidemicInfect epidemicInfect) {
        super(epidemicInfect);
        this.n_cubes_epidemic = epidemicInfect.n_cubes_epidemic;
        this.max_cubes_per_city = epidemicInfect.max_cubes_per_city;
    }

    @Override
    protected boolean run(AbstractGameStateWithTurnOrder gs) {
        // 1. infection counter idx ++
        PandemicGameState pgs = (PandemicGameState)gs;
        ((Counter)pgs.getComponent(infectionRateHash)).increment(1);

        Deck<Card> infectionDiscard = (Deck<Card>) pgs.getComponent(infectionDiscardHash);
        Deck<Card> infectionDeck = (Deck<Card>) pgs.getComponent(infectionHash);
        if (infectionDeck.getSize() <= 0) return false;

        // 2. N cubes on bottom card in infection deck, then add this card on top of infection discard
        return new InfectCity(infectionDeck.getComponentID(), infectionDiscard.getComponentID(),
                infectionDeck.getSize()-1, max_cubes_per_city, n_cubes_epidemic).execute(gs);
    }

    protected Node _copy() {
        return new EpidemicInfect(this);
    }
}
