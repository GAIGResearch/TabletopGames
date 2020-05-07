package games.pandemic.engine.rules;

import core.AbstractGameState;
import core.actions.DoNothing;
import core.actions.IAction;
import games.pandemic.PandemicTurnOrder;
import games.pandemic.actions.RemoveCardWithCard;
import core.components.Card;
import core.components.Deck;
import core.content.PropertyString;
import games.pandemic.PandemicGameState;

import java.util.ArrayList;

import static games.pandemic.PandemicConstants.*;
import static utilities.CoreConstants.nameHash;
import static utilities.CoreConstants.playerHandHash;

@SuppressWarnings("unchecked")
public class ForceRPReaction extends RuleNode {

    @Override
    protected boolean run(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState)gs;
        int nPlayers = gs.getNPlayers();

        for (int i = 0; i < nPlayers; i++) {
            Deck<Card> ph = (Deck<Card>) pgs.getComponent(playerHandHash, i);
            int nCards = ph.getSize();
            for (int cp = 0; cp < nCards; cp++) {
                Card card = ph.getCards().get(cp);
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
