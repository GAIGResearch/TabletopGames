package games.serveTheKing.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IExtendedSequence;
import games.serveTheKing.STKGameState;
import games.serveTheKing.components.PlateCard;

import javax.servlet.http.Part;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
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
public class LookSwap extends AbstractAction implements IExtendedSequence {

    // The extended sequence usually keeps record of the player who played this action, to be able to inform the game whose turn it is to make decisions
    final int playerID;
    int currentPlayer;
    int firstChoice[];
    int secondChoice[];
    boolean hasSwapped;

    public LookSwap(int playerID) {
        this.playerID = playerID;
        currentPlayer = playerID;
        int fc[] = {-1,-1};
        firstChoice= fc;
        int sc[] = {-1,-1};
        secondChoice=sc;
        hasSwapped=false;
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

        if(firstChoice[0]<0 && firstChoice[1]<0){
            // a player can always pass the ability
            available.add(new Pass());
        }
        // see if there are two cards or more in the game
        List<PartialObservableDeck<PlateCard>> plates = stkgs.getPlayersPlates();
        int totalPlates =0;
        for(int i =0;i<stkgs.getNPlayers();i++){
            totalPlates= totalPlates+ plates.get(i).getSize();
        }
        if(totalPlates>=2) {
            // create a combination of all possible choices
            if (secondChoice[0] < 0 && secondChoice[1] < 0) {
                for (int i = 0; i < stkgs.getNPlayers(); i++) {
                    PartialObservableDeck<PlateCard> playerPlate = stkgs.getPlayersPlates().get(i);
                    for (PlateCard c : playerPlate.getComponents()) {
                        if (firstChoice[0] == i && firstChoice[1] == playerPlate.getComponents().indexOf(c)) {
                            continue;
                        }
                        ChooseCard choice = new ChooseCard(playerPlate.getComponents().indexOf(c), i, true);
                        available.add(choice);
                    }
                }
            } else {
                ChooseSwap swapY = new ChooseSwap(true);
                ChooseSwap swapN = new ChooseSwap(false);
                available.add(swapN);
                available.add(swapY);
            }
        }
        else {
            available.add(new Pass());
        }

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
        if(action instanceof Pass){
            hasSwapped=true;
        }
        else if(firstChoice[0]<0 && firstChoice[1]<0 ){
            firstChoice[0]= ((ChooseCard) action).getChosenPlayer();
            firstChoice[1] = ((ChooseCard) action).getChosenCard();
        } else if (secondChoice[0]<0 && secondChoice[1]<0) {
            secondChoice[0]= ((ChooseCard) action).getChosenPlayer();
            secondChoice[1] = ((ChooseCard) action).getChosenCard();
        }
        else if (((ChooseSwap) action).getChoice()){
            // the swap
            PartialObservableDeck<PlateCard> p1Plates=  stkgs.getPlayersPlates().get(firstChoice[0]);
            PartialObservableDeck<PlateCard> p2Plates=  stkgs.getPlayersPlates().get(secondChoice[0]);
            PlateCard firstCard = p1Plates.get(firstChoice[1]);
            PlateCard secondCard =p2Plates.get(secondChoice[1]);
            p1Plates.remove(firstCard);
            p2Plates.remove(secondCard);
            p1Plates.add(secondCard);
            p2Plates.add(firstCard);
            stkgs.getPlayersPlates().get(playerID).setVisibilityOfComponent(firstChoice[1],firstChoice[0],true);
            stkgs.getPlayersPlates().get(playerID).setVisibilityOfComponent(secondChoice[1],secondChoice[0],true);
            hasSwapped=true;
        }
        else {
            hasSwapped=true;
        }
    }

    /**
     * @param state The current game state
     * @return True if this extended sequence has now completed and there is nothing left to do.
     */
    @Override
    public boolean executionComplete(AbstractGameState state) {
        return hasSwapped;
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
        STKGameState stkgs = (STKGameState) gs;

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
    public LookSwap copy() {
        // TODO: copy non-final variables appropriately
        LookSwap  copy = new LookSwap(playerID);
        copy.firstChoice=firstChoice;
        copy.secondChoice=secondChoice;
        copy.currentPlayer=currentPlayer;
        copy.hasSwapped=hasSwapped;
        return copy ;
    }

    @Override
    public boolean equals(Object obj) {
        // TODO: compare all other variables in the class
        return obj instanceof LookSwap
                && ((LookSwap) obj).playerID==playerID
                && ((LookSwap) obj).currentPlayer==currentPlayer
                && ((LookSwap) obj).hasSwapped == hasSwapped
                && ((LookSwap) obj).firstChoice == firstChoice
                && ((LookSwap) obj).secondChoice==secondChoice;
    }

    @Override
    public int hashCode() {
        // TODO: return the hash of all other variables in the class
        int hash= Objects.hash(playerID,hasSwapped,currentPlayer);
        hash= hash + 99 * Arrays.hashCode(firstChoice);
        hash= hash + 99 * Arrays.hashCode(secondChoice);
        return hash;
    }

    @Override
    public String toString() {
        // TODO: Replace with appropriate string, including any action parameters
        return "Swaped and looked at card "+firstChoice[1]+" from player"+firstChoice[0]+" with card "+secondChoice[1]+" from player"+secondChoice[0];
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
