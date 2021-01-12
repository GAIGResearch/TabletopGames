package core.interfaces;

import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;;

public interface IGameListener {

    public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction action);

}
