package games.terraformingmars.rules.effects;

import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.PayForAction;
import games.terraformingmars.actions.PlayCard;
import games.terraformingmars.actions.TMAction;
import games.terraformingmars.components.TMCard;

public class PlayCardEffect extends Effect {
    public TMTypes.Tag tagOnCard;

    public PlayCardEffect(boolean mustBeCurrentPlayer, TMAction effectAction, TMTypes.Tag tag) {
        super(mustBeCurrentPlayer, effectAction);
        this.tagOnCard = tag;
    }
    public PlayCardEffect(boolean mustBeCurrentPlayer, String effectAction, TMTypes.Tag tag) {
        super(mustBeCurrentPlayer, effectAction);
        this.tagOnCard = tag;
    }

    @Override
    public boolean canExecute(TMGameState gameState, TMAction actionTaken, int player) {
        if (!(actionTaken instanceof PayForAction)) return false;
        PayForAction aa = (PayForAction) actionTaken;
        if (!(aa.action instanceof PlayCard)) return false;
        PlayCard action = (PlayCard) aa.action;
        TMCard card = (TMCard) gameState.getComponentById(action.getPlayCardID());
        for (TMTypes.Tag t: card.tags) {
            if (t == tagOnCard) {
                return super.canExecute(gameState, actionTaken, player);
            }
        }
        return false;
    }
}
