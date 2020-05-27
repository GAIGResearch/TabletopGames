package games.pandemic.engine.conditions;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;
import core.properties.PropertyString;
import games.pandemic.PandemicGameState;

import static utilities.CoreConstants.nameHash;
import static utilities.CoreConstants.playerHandHash;

@SuppressWarnings("unchecked")
public class HasRPCard extends ConditionNode {

    @Override
    protected boolean test(AbstractGameState gs) {
        int nPlayers = gs.getNPlayers();
        for (int i = 0; i < nPlayers; i++) {
            Deck<Card> ph = (Deck<Card>) ((PandemicGameState)gs).getComponent(playerHandHash, i);
            int nCards = ph.getSize();
            for (int cp = 0; cp < nCards; cp++) {
                Card card = ph.getComponents().get(cp);
                if (((PropertyString)card.getProperty(nameHash)).value.equals("Resilient Population")) {
                    return true;
                }
            }
        }
        return false;
    }
}
