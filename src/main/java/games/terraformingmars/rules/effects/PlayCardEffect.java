package games.terraformingmars.rules.effects;

import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.PayForAction;
import games.terraformingmars.actions.PlayCard;
import games.terraformingmars.actions.TMAction;
import games.terraformingmars.components.TMCard;

import java.util.HashSet;

public class PlayCardEffect extends Effect {
    public HashSet<TMTypes.Tag> tagsOnCard;

    public PlayCardEffect(boolean mustBeCurrentPlayer, TMAction effectAction, HashSet<TMTypes.Tag> tags) {
        super(mustBeCurrentPlayer, effectAction);
        this.tagsOnCard = tags;
    }

    @Override
    public boolean canExecute(TMGameState gameState, TMAction actionTaken, int player) {
        if (!(actionTaken instanceof PayForAction) || !super.canExecute(gameState, actionTaken, player)) return false;  // PlayCard is always wrapped in PayForAction
        PayForAction aa = (PayForAction) actionTaken;
        if (!(aa.action instanceof PlayCard)) return false;
        PlayCard action = (PlayCard) aa.action;
        TMCard card = (TMCard) gameState.getComponentById(action.getPlayCardID());
        for (TMTypes.Tag t: card.tags) {
            if (tagsOnCard.contains(t)) {
                return true;
            }
        }
        return false;
    }
}
