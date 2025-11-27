package games.seasaltpaper.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.Objects;

public class Stop extends AbstractAction {

    final int playerId;

    public Stop(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        return false;
    }

    @Override
    public Stop copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stop stop = (Stop) o;
        return playerId == stop.playerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Player " + playerId + ": \"STOP!\"";
    }
}
