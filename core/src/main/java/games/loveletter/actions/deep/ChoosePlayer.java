package games.loveletter.actions.deep;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.Objects;

public class ChoosePlayer extends AbstractAction {
    final int player;

    public ChoosePlayer(int player) {
        this.player = player;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        return false;
    }

    @Override
    public ChoosePlayer copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChoosePlayer)) return false;
        ChoosePlayer that = (ChoosePlayer) o;
        return player == that.player;
    }

    @Override
    public int hashCode() {
        return Objects.hash(player);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Choose player " + player;
    }
}
