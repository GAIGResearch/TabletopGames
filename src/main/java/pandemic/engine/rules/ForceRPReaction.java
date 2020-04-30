package pandemic.engine.rules;

import actions.Action;
import actions.DoNothing;
import actions.RemoveCardWithCard;
import components.Card;
import components.Deck;
import content.PropertyString;
import core.GameState;

import java.util.ArrayList;

import static pandemic.Constants.nameHash;
import static pandemic.Constants.playerHandHash;

public class ForceRPReaction extends RuleNode {

    @Override
    protected boolean run(GameState gs) {
        Deck infectionDiscard = (Deck) gs.findDeck("Infection Discard");
        int nPlayers = gs.getNPlayers();
        int nInfectDiscards = infectionDiscard.getCards().size();

        for (int i = 0; i < nPlayers; i++) {
            Deck ph = (Deck) gs.getAreas().get(i).getComponent(playerHandHash);
            int nCards = ph.getCards().size();
            for (int cp = 0; cp < nCards; cp++) {
                Card card = ph.getCards().get(cp);
                if (((PropertyString)card.getProperty(nameHash)).value.equals("Resilient Population")) {
                    ArrayList<Action> acts = new ArrayList<>();
                    acts.add(new DoNothing());
                    for (int idx = 0; idx < nInfectDiscards; idx++) {
                        acts.add(new RemoveCardWithCard(infectionDiscard, idx, card));
                    }
                    // Set discarding infection discarded cards (or do nothing) as the only options and ask player if they want to play their card
                    gs.addReactivePlayer(i, acts);
                    return false;
                }
            }
        }
        return true;
    }
}
