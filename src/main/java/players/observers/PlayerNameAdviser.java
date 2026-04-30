package players.observers;

import core.AbstractGameState;
import core.AbstractPlayer;

public class PlayerNameAdviser implements IAdviceFilter {

    public final String playerName;

    public PlayerNameAdviser(String playerName) {
        this.playerName = playerName;
    }

    @Override
    public boolean advise(AbstractGameState state, AbstractPlayer player) {
        return player.toString().equals(playerName);
    }
}
