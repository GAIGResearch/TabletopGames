package games.thegame.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import games.root.actions.choosers.ChooseNumber;
import games.thegame.TheGameGS;
import games.thegame.TheGameParameters;
import games.thegame.components.TheGameCard;
import games.thegame.components.TheGameDeck;
import gametemplate.actions.GTAction;

import java.util.ArrayList;
import java.util.List;

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
public class SelectRows extends AbstractAction implements IExtendedSequence {

    // The extended sequence usually keeps record of the player who played this action, to be able to inform the game whose turn it is to make decisions
    final int playerID;

    public SelectRows(int playerID) {
        this.playerID = playerID;
    }


    private boolean canPlayInRow(TheGameCard playerCard, TheGameCard top, boolean ascending, int backwardsTrickValue)
    {
        if(ascending && (playerCard.number > top.number || playerCard.number == top.number - backwardsTrickValue)) return true; //ascending valid card
        return !ascending && (playerCard.number < top.number || playerCard.number == top.number + backwardsTrickValue); //descending valid card
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
        TheGameParameters params = (TheGameParameters) gs.getGameParameters();

        Deck<TheGameCard> playerHand = gs.playerHands.get(playerID);
        int deckN = 0;
        for(TheGameDeck<TheGameCard> d : gs.cardRows)
        {
            boolean canPlay = false;
            TheGameCard top = d.peek();
            for(TheGameCard c : playerHand)
            {
                if(canPlayInRow(c, top, d.isAscending(), params.backwardsTrickValue))
                {
                    canPlay = true;
                    break;
                }
            }
            if(canPlay)
                actions.add(new ChooseNumber(playerID, deckN));
            deckN++;
        }

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
        // TODO: Process the action that was taken.
    }

    /**
     * @param state The current game state
     * @return True if this extended sequence has now completed and there is nothing left to do.
     */
    @Override
    public boolean executionComplete(AbstractGameState state) {
        // TODO is execution of this sequence of actions complete?
        return true;
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
        // TODO: Some functionality applied which changes the given game state.
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
    public SelectRows copy() {
        // TODO: copy non-final variables appropriately
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        // TODO: compare all other variables in the class
        return obj instanceof GTAction;
    }

    @Override
    public int hashCode() {
        // TODO: return the hash of all other variables in the class
        return 0;
    }

    @Override
    public String toString() {
        // TODO: Replace with appropriate string, including any action parameters
        return "My action name";
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
