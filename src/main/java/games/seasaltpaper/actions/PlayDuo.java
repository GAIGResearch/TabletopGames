package games.seasaltpaper.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

public class PlayDuo extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj==this;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "PlaceHolder for Duo Action";
    }
}
