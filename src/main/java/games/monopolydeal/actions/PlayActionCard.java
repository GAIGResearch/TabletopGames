package games.monopolydeal.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.MonopolyDealCard;
import games.monopolydeal.cards.SetType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * <p>The extended actions framework supports 2 use-cases: <ol>
 *     <li>A sequence of decisions required to complete an action (e.g. play a card in a game area - which card? - which area?).
 *     This avoids very large action spaces in favour of more decisions throughout the game (alternative: all unit actions
 *     with parameters supplied at initialization, all combinations of parameters computed beforehand).</li>
 *     <li>A sequence of actions triggered by specific decisions (e.g. play a card which forces another player to discard a card - other player: which card to discard?)</li>
 * </ol></p>
 * <p>Extended actions should implement the {@link IExtendedSequence} interface and appropriate methods, as detailed below.</p>
 * <p>They should also extend the {@link AbstractAction} class, or any other core actions. As such, all guidelines in {@link MonopolyDealAction} apply here as well.</p>
 */
public class PlayActionCard extends AbstractAction implements IExtendedSequence {

    // The extended sequence usually keeps record of the player who played this action, to be able to inform the game whose turn it is to make decisions
    final int playerID;
    boolean executed;

    public PlayActionCard(int playerID) {
        this.playerID = playerID;
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
        // TODO populate this list with available actions
        MonopolyDealGameState MDGS = (MonopolyDealGameState) state;
        Deck<MonopolyDealCard> currentPlayerHand = MDGS.getPlayerHand(playerID);

        int noDoubleTheRent = (int) currentPlayerHand.stream().filter(MonopolyDealCard::isDoubleTheRent).count();
        List<AbstractAction> availableActions = new ArrayList<>();
        // Iterate through player hand and add actions
        for (int i = 0; i <currentPlayerHand.getSize(); i++) {
            if(currentPlayerHand.get(i).isActionCard()) {
                CardType type = currentPlayerHand.get(i).cardType();
                switch (type) {
                    case SlyDeal:
                        if (MDGS.checkForSlyDeal(playerID))
                            availableActions.add(new SlyDealAction(playerID));
                        break;
                    case ForcedDeal:
                        if(MDGS.checkForForcedDeal(playerID))
                            availableActions.add(new ForcedDealAction(playerID));
                        break;
                    case DealBreaker:
                        if(MDGS.checkForDealBreaker(playerID))
                            availableActions.add(new DealBreakerAction(playerID));
                        break;
                    case MulticolorRent:
                        if(MDGS.checkForMulticolorRent(playerID)){
                            for(int j=0; j < MDGS.getActionsLeft() && j < noDoubleTheRent; j++)
                                availableActions.add(new MulticolorRentAction(playerID,j));
                        }
                        break;
                    case GreenBlueRent:
                        if(MDGS.playerHasSet(playerID, SetType.Green)) availableActions.add(new PropertyRentAction(playerID,SetType.Green,type));
                        if(MDGS.playerHasSet(playerID, SetType.Blue)) availableActions.add(new PropertyRentAction(playerID,SetType.Blue,type));
                    case BrownLightBlueRent:
                        if(MDGS.playerHasSet(playerID, SetType.Brown)) availableActions.add(new PropertyRentAction(playerID,SetType.Brown,type));
                        if(MDGS.playerHasSet(playerID, SetType.LightBlue)) availableActions.add(new PropertyRentAction(playerID,SetType.LightBlue,type));
                    case PinkOrangeRent:
                        if(MDGS.playerHasSet(playerID, SetType.Pink)) availableActions.add(new PropertyRentAction(playerID,SetType.Pink,type));
                        if(MDGS.playerHasSet(playerID, SetType.Orange)) availableActions.add(new PropertyRentAction(playerID,SetType.Orange,type));
                    case RedYellowRent:
                        if(MDGS.playerHasSet(playerID, SetType.Red)) availableActions.add(new PropertyRentAction(playerID,SetType.Red,type));
                        if(MDGS.playerHasSet(playerID, SetType.Yellow)) availableActions.add(new PropertyRentAction(playerID,SetType.Yellow,type));
                    case RailRoadUtilityRent:
                        if(MDGS.playerHasSet(playerID, SetType.RailRoad)) availableActions.add(new PropertyRentAction(playerID,SetType.RailRoad,type));
                        if(MDGS.playerHasSet(playerID, SetType.Utility)) availableActions.add(new PropertyRentAction(playerID,SetType.Utility,type));
                    case PassGo:
                        availableActions.add(new PassGoAction());
                        break;
                    case DebtCollector:
                        availableActions.add(new DebtCollectorAction(playerID));
                        break;
                    case ItsMyBirthday:
                        availableActions.add(new ItsMyBirthdayAction(playerID));
                        break;
                    case JustSayNo:
                    case DoubleTheRent:
                    case House:
                    case Hotel:
                        break;
                    default:
                        throw new AssertionError(type.toString() + " not yet Implemented");
                }
            }
        }
        return availableActions;
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
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        // TODO: Process the action that was taken.
        executed = true;
    }

    /**
     * @param state The current game state
     * @return True if this extended sequence has now completed and there is nothing left to do.
     */
    @Override
    public boolean executionComplete(AbstractGameState state) {
        // TODO is execution of this sequence of actions complete?
        return executed;
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
    public PlayActionCard copy() {
        // TODO: copy non-final variables appropriately
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayActionCard that = (PlayActionCard) o;
        return playerID == that.playerID && executed == that.executed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, executed);
    }

    @Override
    public String toString() {
        // TODO: Replace with appropriate string, including any action parameters
        return "Play Action Card";
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
