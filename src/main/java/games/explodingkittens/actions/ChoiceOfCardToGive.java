package games.explodingkittens.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import core.interfaces.IToJSON;
import games.explodingkittens.ExplodingKittensGameState;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.stream.Collectors;

public class ChoiceOfCardToGive implements IExtendedSequence, IToJSON {

    public final int giver;
    public final int recipient;
    public boolean executed;

    public ChoiceOfCardToGive(int giver, int recipient) {
        this.giver = giver;
        this.recipient = recipient;
    }

    public ChoiceOfCardToGive(JSONObject json) {
        this(((Number) json.get("giver")).intValue(), ((Number) json.get("recipient")).intValue());
        this.executed = (boolean) json.get("executed");
    }

    @Override
    @SuppressWarnings("unchecked")
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("class", getClass().getName());
        json.put("giver", giver);
        json.put("recipient", recipient);
        json.put("executed", executed);
        return json;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        ExplodingKittensGameState state = (ExplodingKittensGameState) gs;
        // consider all cards in hand
        List<GiveCard> actions = state.getPlayerHand(giver).stream()
                .map(c -> new GiveCard(giver, recipient, c.cardType))
                .toList();
        return actions.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return giver;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof GiveCard)
            executed = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }

    @Override
    public ChoiceOfCardToGive copy() {
        ChoiceOfCardToGive retValue = new ChoiceOfCardToGive(giver, recipient);
        retValue.executed = executed;
        return retValue;
    }

    @Override
    public String toString() {
        return "Choice of card to give";
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof ChoiceOfCardToGive cc && cc.giver == giver && cc.recipient == recipient && cc.executed == executed;
    }
    @Override
    public int hashCode() {
        return giver * 31 + recipient * 31 * 31 + (executed ? 1 : 0)  + 2901;
    }
}
