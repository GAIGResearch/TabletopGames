package games.loveletter;

import core.actions.AbstractAction;
import core.interfaces.IActionKey;
import games.loveletter.actions.PlayCard;

public class SimpleActionKey implements IActionKey {
    @Override
    public String key(AbstractAction action) {
        if (action instanceof PlayCard) {
            return ((PlayCard) action).getCardType().name();
        }
        return "";
    }
}
