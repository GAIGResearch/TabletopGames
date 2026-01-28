package games.thegame.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import games.thegame.TheGameGS;
import games.thegame.TheGameParameters;
import games.thegame.components.TheGameCard;
import games.thegame.components.TheGameDeck;
import gametemplate.actions.GTAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>The extended actions framework supports 2 use-cases: <ol>
 *     <li>A sequence of decisions required to complete an action (e.g. play a card in a game area - which card? - which area?).
 *     This avoids very large action spaces in favour of more decisions throughout the game (alternative: all unit actions
 *     with parameters supplied at initialization, all combinations of parameters computed beforehand).</li>
 *     <li>A sequence of actions triggered by specific decisions (e.g. play a card which forces another player to discard a card - other player: which card to discard?)</li>
 * </ol></p>
 * <p>Extended actions should implement the {@link IExtendedSequence} interface and appropriate methods, as detailed below.</p>
 * <p>They should also extend the {@link AbstractAction} class, or any other core actions. As such, all guidelines in {@link GTAction} apply here as well.</p>
 */
public class PlayingCards extends AbstractAction implements IExtendedSequence {

    // The extended sequence usually keeps record of the player who played this action, to be able to inform the game whose turn it is to make decisions
    final int playerID;

    public int cardsPlayed;

    public boolean completed;

    public PlayingCards(int playerID) {
        this.playerID = playerID;
        this.completed = false;
    }

    public boolean canBePlayed(AbstractGameState state)
    {
        return !_computeAvailableActions(state).isEmpty();
    }

    /**
     * Forward Model delegates to this from {@link core.StandardForwardModel#computeAvailableActions(AbstractGameState)}
     * if this Extended Sequence is currently active.
     *
     * @param state The current game state
     * @return the list of possible actions for the {@link AbstractGameState#getCurrentPlayer()}.
     * These may be instances of this same class, with more choices between different values for a not-yet filled in parameter.
     */
    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {

        List<AbstractAction> actions = new ArrayList<>();
        TheGameGS gs = (TheGameGS) state;
        Deck<TheGameCard> playerHand = gs.playerHands.get(playerID);
        int row = 0;
        for(TheGameDeck<TheGameCard> r : gs.cardRows) {
            for (TheGameCard card : playerHand) {
                if (gs.canPlayInRow(card, r)) {
                    actions.add(new PlayCard(card.number, card.getComponentID(), row));
                }
            }
            row++;
        }

        if(cardsPlayed >= gs.getCardsToPlay())
            actions.add(new DoNothing());

        return actions;
    }

    /**
     * TurnOrder delegates to this from {@link core.turnorders.TurnOrder#getCurrentPlayer(AbstractGameState)}
     * if this Extended Sequence is currently active.
     *
     * @param state The current game state
     * @return The player ID whose move it is.
     */
    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    /**
     * <p>This is called by ForwardModel whenever an action is about to be taken. It enables the IExtendedSequence
     * to maintain local state in whichever way is most suitable.</p>
     *
     * <p>After this call, the state of IExtendedSequence should be correct ahead of the next decision to be made.
     * In some cases, there is no need to implement anything in this method - if for example you can tell if all
     * actions are complete from the state directly, then that can be implemented purely in {@link #executionComplete(AbstractGameState)}</p>
     *
     * @param state The current game state
     * @param action The action about to be taken (so the game state has not yet been updated with it)
     */
    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        TheGameGS gs = (TheGameGS) state;
        if(action instanceof PlayCard pc)
        {
            cardsPlayed++;

            boolean mustKeepPlaying = cardsPlayed < gs.getCardsToPlay();
            boolean canPlay = canBePlayed(state);
            if(mustKeepPlaying && !canPlay)
            {
                //Game is over
                gs.gameOver();
                completed = true;
            }

        }else if(action instanceof DoNothing)
        {
            //We're done;
            completed = true;
        }

    }

    /**
     * @param state The current game state
     * @return True if this extended sequence has now completed and there is nothing left to do.
     */
    @Override
    public boolean executionComplete(AbstractGameState state) {
        return completed;
    }

    /**
     * <p>Executes this action, applying its effect to the given game state. Can access any component IDs stored
     * through the {@link AbstractGameState#getComponentById(int)} method.</p>
     * <p>In extended sequences, this function makes a call to the
     * {@link AbstractGameState#setActionInProgress(IExtendedSequence)} method with the argument <code>`this`</code>
     * to indicate that this action has multiple steps and is now in progress. This call could be wrapped in an <code>`if`</code>
     * statement if sometimes the action simply executes an effect in one step, or all parameters have values associated.</p>
     * @param gs - game state which should be modified by this action.
     * @return - true if successfully executed, false otherwise.
     */
    @Override
    public boolean execute(AbstractGameState gs) {
        gs.setActionInProgress(this);
        return true;
    }

    /**
     * @return Make sure to return an exact <b>deep</b> copy of the object, including all of its variables.
     * Make sure the return type is this class (e.g. GTAction) and NOT the super class AbstractAction.
     * <p>If all variables in this class are final or effectively final (which they should be),
     * then you can just return <code>`this`</code>.</p>
     */
    @Override
    public PlayingCards copy() {
        PlayingCards copy = new PlayingCards(this.playerID);
        copy.completed = this.completed;
        copy.cardsPlayed = this.cardsPlayed;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayingCards that = (PlayingCards) o;
        return playerID == that.playerID && cardsPlayed == that.cardsPlayed && completed == that.completed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, cardsPlayed, completed);
    }

    @Override
    public String toString() {
        return "Playing cards. Cards played: " + cardsPlayed + (completed?" (Completed)":"");
    }

    /**
     * @param gameState - game state provided for context.
     * @return A more descriptive alternative to the toString action, after access to the game state to e.g.
     * retrieve components for which only the ID is stored on the action object, and include the name of those components.
     * Optional.
     */
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
