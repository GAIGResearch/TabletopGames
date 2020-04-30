package pandemic.engine.rules;

import actions.AddCardToDeck;
import components.Card;
import core.GameState;
import pandemic.actions.InfectCity;

public class EpidemicInfect extends RuleNode {
    int n_cubes_epidemic;
    int max_cubes_per_city;

    public EpidemicInfect(int max_cubes_per_city, int n_cubes_epidemic) {
        super();
        this.n_cubes_epidemic = n_cubes_epidemic;
        this.max_cubes_per_city = max_cubes_per_city;
    }

    @Override
    protected boolean run(GameState gs) {
        // 1. infection counter idx ++
        gs.findCounter("Infection Rate").increment(1);

        // 2. 3 cubes on bottom card in infection deck, then add this card on top of infection discard
        Card c = gs.findDeck("Infections").pickLast();
        new InfectCity(max_cubes_per_city, c, n_cubes_epidemic).execute(gs);
        new AddCardToDeck(c, gs.findDeck("Infection Discard")).execute(gs);
        return true;
    }
}
