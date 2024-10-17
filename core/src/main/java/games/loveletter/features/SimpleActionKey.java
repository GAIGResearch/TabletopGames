package games.loveletter.features;

import core.actions.AbstractAction;
import core.interfaces.IActionKey;
import games.loveletter.actions.PlayCard;

public class SimpleActionKey implements IActionKey {
    @Override
    public String key(AbstractAction action) {
        if (action instanceof PlayCard) {
            PlayCard playCard = (PlayCard) action;
            return playCard.getCardType().name() + "_" + playCard.getTargetCardType();
        }
        return "";
    }
}
