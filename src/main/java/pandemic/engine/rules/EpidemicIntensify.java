package pandemic.engine.rules;

import actions.AddCardToDeck;
import components.Card;
import components.IDeck;
import core.GameState;

import java.util.Random;

public class EpidemicIntensify extends RuleNode {
    Random rnd;

    public EpidemicIntensify(Random rnd) {
        super();
        this.rnd = rnd;
    }

    @Override
    protected boolean run(GameState gs) {
        IDeck infectionDiscard = gs.findDeck("Infection Discard");
        // 3. shuffle infection discard deck, add back on top of infection deck
        infectionDiscard.shuffle(rnd);
        for (Card card: infectionDiscard.getCards()) {
            new AddCardToDeck(card, gs.findDeck("Infections")).execute(gs);
        }
        return true;
    }
}
