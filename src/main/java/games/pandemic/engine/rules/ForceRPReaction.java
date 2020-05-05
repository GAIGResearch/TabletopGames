package games.pandemic.engine.rules;

import core.AbstractGameState;
import games.pandemic.PandemicTurnOrder;
import core.components.Card;
import core.components.Deck;
import core.content.PropertyString;
import games.pandemic.PandemicGameState;

import static games.pandemic.PandemicConstants.*;

@SuppressWarnings("unchecked")
public class ForceRPReaction extends RuleNode {

    @Override
    protected boolean run(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState)gs;
        int nPlayers = gs.getNPlayers();

        for (int i = 0; i < nPlayers; i++) {
            Deck<Card> ph = (Deck<Card>) pgs.getComponent(playerHandHash, i);
            int nCards = ph.getElements().size();
            for (int cp = 0; cp < nCards; cp++) {
                Card card = ph.getElements().get(cp);
                if (((PropertyString)card.getProperty(nameHash)).value.equals("Resilient Population")) {
                    ((PandemicTurnOrder)pgs.getTurnOrder()).addReactivePlayer(i);
                    pgs.setGamePhase(PandemicGameState.GamePhase.RPReaction);
                    return false;
                }
            }
        }
        return true;
    }
}
