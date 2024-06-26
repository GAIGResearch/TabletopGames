package games.pandemic.rules.conditions;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;
import core.properties.PropertyString;
import core.rules.Node;
import core.rules.nodetypes.ConditionNode;
import games.pandemic.PandemicGameState;

import static core.CoreConstants.nameHash;
import static core.CoreConstants.playerHandHash;

@SuppressWarnings("unchecked")
public class HasRPCard extends ConditionNode {

    public HasRPCard() {
        super();
    }

    /**
     * Copy constructor
     * @param hasRPCard - Node to be copied
     */
    public HasRPCard(HasRPCard hasRPCard) {
        super(hasRPCard);
    }

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

    @Override
    protected Node _copy() {
        return new HasRPCard(this);
    }
}
