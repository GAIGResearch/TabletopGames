package players.observers;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;

public class PlayerNameAdviser extends AbstractMCTSAdviser {

    public final String playerName;

    /**
     * Will advise a player that matches on playerName, if we think our recommendation
     * is more than adviceThreshold better than their proposed move
     *
     * @param playerName      name of advisee
     * @param adviceThreshold threshold for advice to be given (value of proposed action over their action)
     */
    public PlayerNameAdviser(String playerName, double adviceThreshold) {
        super(adviceThreshold);
        this.playerName = playerName;
    }

    @Override
    public boolean payAttention(AbstractGameState state, AbstractAction proposedAction, AbstractPlayer advisee) {
        return advisee.toString().equals(playerName);
    }

}
