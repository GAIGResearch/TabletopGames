package players.observers;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;

public interface IAdviceFilter {

    /**
     * Returns true/false whether the Adviser will look at the move further
     * This is the first (low budget) stage in the advice chain. If the answer to this is 'true'
     * then the adviser will go on to invest computation in determining whether advice should be given
     * and if so, precisely what that advise should be
     *
     * @param state          the current game state
     * @param proposedAction the action the advisee will take in the absence of our advice
     * @param advisee        the agent that we may consider advising
     * @return true if we will consider what we would do (low budget)
     */
    boolean payAttention(AbstractGameState state,
                         AbstractAction proposedAction,
                         AbstractPlayer advisee);

    /**
     * Returns true/false whether the Adviser will advise if the player takes the action from the state
     * This is the second (high budget) stage in the advise chain. If the answer to this is true (after computation),
     * then the adviser will intervene.
     * This intervention is controlled by the main GameAdviser (currently)
     *
     * @param state          the current game state
     * @param proposedAction the action the advisee will take in the absence of our advice
     * @param advisee        the agent that we may consider advising
     * @param advice        the action we are considering as best
     * @param adviser       the GameAdviser
     * @return true  if we will intervene and provide advice
     *
     */
    boolean provideAdvice(AbstractGameState state,
                          AbstractAction proposedAction,
                          AbstractPlayer advisee,
                          AbstractAction advice,
                          GameAdviser adviser);


}
