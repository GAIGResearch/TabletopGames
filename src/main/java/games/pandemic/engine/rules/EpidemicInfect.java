package games.pandemic.engine.rules;

import core.AbstractGameState;
import core.actions.AddCardToDeck;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import games.pandemic.PandemicGameState;
import games.pandemic.actions.InfectCity;

import static games.pandemic.Constants.*;

@SuppressWarnings("unchecked")
public class EpidemicInfect extends RuleNode {
    int n_cubes_epidemic;
    int max_cubes_per_city;

    public EpidemicInfect(int max_cubes_per_city, int n_cubes_epidemic) {
        super();
        this.n_cubes_epidemic = n_cubes_epidemic;
        this.max_cubes_per_city = max_cubes_per_city;
    }

    @Override
    protected boolean run(AbstractGameState gs) {
        // 1. infection counter idx ++
        PandemicGameState pgs = (PandemicGameState)gs;
        ((Counter)pgs.getComponent(infectionRateHash)).increment(1);

        Deck<Card> infectionDiscard = (Deck<Card>) pgs.getComponent(infectionDiscardHash);
        Deck<Card> infectionDeck = (Deck<Card>) pgs.getComponent(infectionHash);
        // 2. 3 cubes on bottom card in infection deck, then add this card on top of infection discard
        Card c = infectionDeck.pickLast();
        new InfectCity(max_cubes_per_city, c, n_cubes_epidemic).execute(gs);
        new AddCardToDeck(c, infectionDiscard).execute(gs);
        return true;
    }
}
