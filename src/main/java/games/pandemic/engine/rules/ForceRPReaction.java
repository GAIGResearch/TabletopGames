package games.pandemic.engine.rules;

import core.AbstractGameState;
import core.actions.DoNothing;
import core.actions.IAction;
import core.actions.RemoveCardWithCard;
import core.components.Card;
import core.components.Deck;
import core.content.PropertyString;
import games.pandemic.PandemicGameState;

import java.util.ArrayList;

import static games.pandemic.PandemicConstants.*;

@SuppressWarnings("unchecked")
public class ForceRPReaction extends RuleNode {

    @Override
    protected boolean run(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState)gs;
        Deck<Card> infectionDiscard = (Deck<Card>) pgs.getComponent(infectionDiscardHash);
        int nPlayers = gs.getNPlayers();
        int nInfectDiscards = infectionDiscard.getCards().size();

        for (int i = 0; i < nPlayers; i++) {
            Deck<Card> ph = (Deck<Card>) pgs.getComponent(playerHandHash, i);
            int nCards = ph.getCards().size();
            for (int cp = 0; cp < nCards; cp++) {
                Card card = ph.getCards().get(cp);
                if (((PropertyString)card.getProperty(nameHash)).value.equals("Resilient Population")) {
                    ArrayList<IAction> acts = new ArrayList<>();
                    acts.add(new DoNothing());
                    for (int idx = 0; idx < nInfectDiscards; idx++) {
                        acts.add(new RemoveCardWithCard(infectionDiscard, idx, card));
                    }
                    // Set discarding infection discarded cards (or do nothing) as the only options and ask player if they want to play their card
                    pgs.addReactivePlayer(i, acts);
                    return false;
                }
            }
        }
        return true;
    }
}
