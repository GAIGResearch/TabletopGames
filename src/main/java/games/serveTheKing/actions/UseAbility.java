package games.serveTheKing.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IExtendedSequence;
import games.serveTheKing.STKGameState;
import games.serveTheKing.components.PlateCard;
import org.apache.spark.sql.catalyst.expressions.Abs;
import org.apache.xmlbeans.impl.xb.xsdschema.Attribute;

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
 * <p>They should also extend the {@link AbstractAction} class, or any other core actions. As such, all guidelines in {@link TrashPlate} apply here as well.</p>
 */
public class UseAbility extends AbstractAction implements IExtendedSequence {

    // The extended sequence usually keeps record of the player who played this action, to be able to inform the game whose turn it is to make decisions
    final int playerID;
    int currentPlayer;
    int cardValue;
    boolean abilityFinished;
    boolean hasTrashed;

    public UseAbility(int playerID) {
        this.playerID = playerID;
        this.currentPlayer = playerID;
        this.cardValue= -1000;
        this.abilityFinished = false;
        this.hasTrashed=false;
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
        ArrayList<AbstractAction> available = new ArrayList<>();
        STKGameState stkgs = (STKGameState) state;
        // find if the ability should be executed
        if(abilityFinished){
            // find if the player can trash any card in his plate
            if(!hasTrashed) {
                int topDiscard = stkgs.getDiscardPile().peek().getValue();
                PartialObservableDeck<PlateCard> playerPlates = stkgs.getPlayersPlates().get(stkgs.getCurrentPlayer());
                for (PlateCard c : playerPlates.getComponents()) {
                    if (c.getValue() == topDiscard) {
                        TrashPlate trashAction = new TrashPlate(playerPlates.getComponents().indexOf(c));
                        available.add(trashAction);
                    }
                }
            }
            // a player can always pass
            available.add(new Pass());
        }
        else {
            AbstractAction ability;
            if(cardValue == -1 || cardValue ==15){
                ability = new LookSwap(playerID);
                available.add(ability);
            }
            else if(cardValue == 0 || cardValue ==10){
                ability = new Swap(playerID);
                available.add(ability);
            }
            else {
                for ( int i =0; i< stkgs.getNPlayers(); i ++) {
                    PartialObservableDeck<PlateCard> playerPlate = stkgs.getPlayersPlates().get(i);
                    for (PlateCard c : playerPlate.getComponents()) {
                        ChooseCard choice = new ChooseCard(playerPlate.getComponents().indexOf(c),i,true);
                        available.add(choice);
                    }
                }
            }

        }
        System.out.println("[UseAbility] status of ability:  "+abilityFinished);
        System.out.println("[UseAbility] player "+stkgs.getCurrentPlayer()+ " has these actions:"+available);
        return available;
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
        return currentPlayer;
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
        STKGameState stkgs = (STKGameState) state;
        if(action instanceof Swap|| action instanceof LookSwap|| action instanceof ChooseCard){
            abilityFinished=true;
        }
        if(abilityFinished){
            currentPlayer = (currentPlayer + 1) % stkgs.getNPlayers();
        }

    }

    /**
     * @param state The current game state
     * @return True if this extended sequence has now completed and there is nothing left to do.
     */
    @Override
    public boolean executionComplete(AbstractGameState state) {
        if(abilityFinished && currentPlayer==playerID){
            return true;
        }
        else {
            return false;
        }

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
        STKGameState stkgs = (STKGameState) gs;
        PartialObservableDeck<PlateCard> hand = stkgs.getPlayersHands().get(playerID);
        // put the used card in the discard pile
        Deck<PlateCard> discard = stkgs.getDiscardPile();
        System.out.println("[UseAbility]Hand size of player "+playerID+" hand size is: "+hand.getComponents().size());
        PlateCard discarded = hand.peek();
        hand.remove(discarded);
        discard.add(discarded);
        // get the value of the card discarded
        cardValue=discarded.getValue();
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
    public UseAbility copy() {
        UseAbility copy = new UseAbility(playerID);
        copy.hasTrashed=this.hasTrashed;
        copy.abilityFinished=this.abilityFinished;
        copy.currentPlayer=this.currentPlayer;
        copy.cardValue=this.cardValue;
        return copy;
    }

    @Override
    public boolean equals(Object obj) {

        return obj instanceof UseAbility
                && ((UseAbility) obj).abilityFinished==abilityFinished
                && ((UseAbility) obj).hasTrashed==hasTrashed
                && ((UseAbility) obj).cardValue==cardValue
                && ((UseAbility) obj).currentPlayer==currentPlayer
                && ((UseAbility) obj).playerID==playerID;
    }

    @Override
    public int hashCode() {

        return Objects.hash(abilityFinished,hasTrashed,cardValue,currentPlayer,playerID);
    }

    @Override
    public String toString() {
        // TODO: Replace with appropriate string, including any action parameters
        return "Using Ability!";
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
