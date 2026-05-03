package players.observers;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import players.mcts.MCTSPlayer;

public class PlayerNameAdviser implements IAdviceFilter {

    public final String playerName;
    protected double adviceThreshold;

    /**
     * Will advise a player that matches on playerName, if we think our recommendation
     * is more than adviceThreshold better than their proposed move
     *
     * @param playerName      name of advisee
     * @param adviceThreshold threshold for advice to be given (value of proposed action over their action)
     */
    public PlayerNameAdviser(String playerName, double adviceThreshold) {
        this.playerName = playerName;
        this.adviceThreshold = adviceThreshold;
    }

    @Override
    public boolean payAttention(AbstractGameState state, AbstractAction proposedAction, AbstractPlayer advisee) {
        return advisee.toString().equals(playerName);
    }

    @Override
    public boolean provideAdvice(AbstractGameState state, AbstractAction proposedAction, AbstractPlayer advisee,
                                 AbstractAction advice, GameAdviser adviser) {
        if (adviser.player instanceof MCTSPlayer mctsPlayer) {
            double valueOfProposedAction = mctsPlayer.getValue(proposedAction);
            double valueOfOurAction = mctsPlayer.getValue(advice);
            return valueOfOurAction - valueOfProposedAction > adviceThreshold;
        } else {
            throw new AssertionError("PlayerNameAdviser only supports MCTS players");
        }
    }
}
