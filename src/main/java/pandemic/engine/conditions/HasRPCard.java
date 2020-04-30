package pandemic.engine.conditions;

import components.Card;
import components.Deck;
import content.PropertyString;
import core.GameState;
import pandemic.engine.Node;

import static pandemic.Constants.nameHash;
import static pandemic.Constants.playerHandHash;

public class HasRPCard extends ConditionNode {

    public HasRPCard(Node yes, Node no) {
        super(yes, no);
    }

    @Override
    protected boolean test(GameState gs) {
        int nPlayers = gs.getNPlayers();
        for (int i = 0; i < nPlayers; i++) {
            Deck ph = (Deck) gs.getAreas().get(i).getComponent(playerHandHash);
            int nCards = ph.getCards().size();
            for (int cp = 0; cp < nCards; cp++) {
                Card card = ph.getCards().get(cp);
                if (((PropertyString)card.getProperty(nameHash)).value.equals("Resilient Population")) {
                    return true;
                }
            }
        }
        return false;
    }
}
