package games.explodingkittens.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IToJSON;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittensCard.CardType;
import org.json.simple.JSONObject;

import java.util.Objects;

public class PlayEKCard extends AbstractAction implements IToJSON {

    public final CardType cardType;
    public final int target;

    public PlayEKCard(CardType cardType) {
        this.cardType = cardType;
        target = -1;
    }

    public PlayEKCard(CardType cardType, int target) {
        this.cardType = cardType;
        this.target = target;
    }

    public PlayEKCard(JSONObject json) {
        this(CardType.valueOf((String) json.get("cardType")), ((Number) json.get("target")).intValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("class", getClass().getName());
        json.put("cardType", cardType.name());
        json.put("target", target);
        return json;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ExplodingKittensGameState state = (ExplodingKittensGameState) gs;
        state.setInPlay(cardType, state.getCurrentPlayer());
        if (cardType.catCard)
            state.setInPlay(cardType, state.getCurrentPlayer());  // add an extra one

        if (cardType.nopeable) {
            NopeableAction nopeable = new NopeableAction(state.getCurrentPlayer(), this, state);
            // If no one else holds a Nope, there is nothing to wait for. We must resolve it now, as a sequence is
            // never told about the action that created it (so would otherwise just be removed from the stack unresolved)
            if (nopeable.executionComplete(state))
                nopeable.resolve(state);
            else
                state.setActionInProgress(nopeable);
        } else {
            // if not Nopeable, execute the card
            cardType.execute(state, target);
        }

        return true;
    }

    @Override
    public PlayEKCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PlayEKCard pek && pek.cardType == cardType && pek.target == target;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardType, target) + 2901;
    }

    @Override
    public String toString() {
        if (target == -1)
            return "Play " + cardType;
        return "Play " + cardType + " on player " + target;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
