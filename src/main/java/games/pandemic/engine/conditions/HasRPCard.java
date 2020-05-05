package games.pandemic.engine.conditions;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;
import core.content.PropertyString;
import games.pandemic.PandemicGameState;

import static games.pandemic.PandemicConstants.nameHash;
import static games.pandemic.PandemicConstants.playerHandHash;

@SuppressWarnings("unchecked")
public class HasRPCard extends ConditionNode {

    @Override
    protected boolean test(AbstractGameState gs) {
        int nPlayers = gs.getNPlayers();
        for (int i = 0; i < nPlayers; i++) {
            Deck<Card> ph = (Deck<Card>) ((PandemicGameState)gs).getComponent(playerHandHash, i);
            int nCards = ph.getElements().size();
            for (int cp = 0; cp < nCards; cp++) {
                Card card = ph.getElements().get(cp);
                if (((PropertyString)card.getProperty(nameHash)).value.equals("Resilient Population")) {
                    return true;
                }
            }
        }
        return false;
    }
}
