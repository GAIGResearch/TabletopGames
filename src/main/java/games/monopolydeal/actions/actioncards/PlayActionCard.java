package games.monopolydeal.actions.actioncards;

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
    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
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
                            if(!availableActions.contains(new SlyDealAction(playerID)))
                                availableActions.add(new SlyDealAction(playerID));
                        break;
                    case ForcedDeal:
                        if(MDGS.checkForForcedDeal(playerID))
                            if(!availableActions.contains(new ForcedDealAction(playerID)))
                                availableActions.add(new ForcedDealAction(playerID));
                        break;
                    case DealBreaker:
                        if(MDGS.checkForDealBreaker(playerID))
                            if(!availableActions.contains(new DealBreakerAction(playerID)))
                                availableActions.add(new DealBreakerAction(playerID));
                        break;
                    case MulticolorRent:
                        if(MDGS.checkForMulticolorRent(playerID)){
                            for(int j=0; j < MDGS.getActionsLeft() && j <= noDoubleTheRent; j++)
                                if(!availableActions.contains(new MulticolorRentAction(playerID,j)))
                                    availableActions.add(new MulticolorRentAction(playerID,j));
                        }
                        break;
                    case GreenBlueRent:
                        if(MDGS.playerHasSet(playerID, SetType.Green)) {
                            for(int j=0; j < MDGS.getActionsLeft() && j <= noDoubleTheRent; j++)
                                if(!availableActions.contains(new PropertyRentAction(playerID,SetType.Green,type,j)))
                                    availableActions.add(new PropertyRentAction(playerID,SetType.Green,type,j));
                        }
                        if(MDGS.playerHasSet(playerID, SetType.Blue)) {
                            for(int j=0; j < MDGS.getActionsLeft() && j <= noDoubleTheRent; j++)
                                if(!availableActions.contains(new PropertyRentAction(playerID,SetType.Blue,type,j)))
                                    availableActions.add(new PropertyRentAction(playerID,SetType.Blue,type,j));
                        }
                        break;
                    case BrownLightBlueRent:
                        if(MDGS.playerHasSet(playerID, SetType.Brown)) {
                            for(int j=0; j < MDGS.getActionsLeft() && j <= noDoubleTheRent; j++)
                                if(!availableActions.contains(new PropertyRentAction(playerID,SetType.Brown,type,j)))
                                    availableActions.add(new PropertyRentAction(playerID,SetType.Brown,type,j));
                        }
                        if(MDGS.playerHasSet(playerID, SetType.LightBlue)) {
                            for(int j=0; j < MDGS.getActionsLeft() && j <= noDoubleTheRent; j++)
                                if(!availableActions.contains(new PropertyRentAction(playerID,SetType.LightBlue,type,j)))
                                    availableActions.add(new PropertyRentAction(playerID,SetType.LightBlue,type,j));
                        }
                        break;
                    case PinkOrangeRent:
                        if(MDGS.playerHasSet(playerID, SetType.Pink)) {
                            for(int j=0; j < MDGS.getActionsLeft() && j <= noDoubleTheRent; j++)
                                if(!availableActions.contains(new PropertyRentAction(playerID,SetType.Pink,type,j)))
                                    availableActions.add(new PropertyRentAction(playerID,SetType.Pink,type,j));
                        }
                        if(MDGS.playerHasSet(playerID, SetType.Orange)) {
                            for(int j=0; j < MDGS.getActionsLeft() && j <= noDoubleTheRent; j++)
                                if(!availableActions.contains(new PropertyRentAction(playerID,SetType.Orange,type,j)))
                                    availableActions.add(new PropertyRentAction(playerID,SetType.Orange,type,j));
                        }
                        break;
                    case RedYellowRent:
                        if(MDGS.playerHasSet(playerID, SetType.Red)) {
                            for(int j=0; j < MDGS.getActionsLeft() && j <= noDoubleTheRent; j++)
                                if(!availableActions.contains(new PropertyRentAction(playerID,SetType.Red,type,j)))
                                    availableActions.add(new PropertyRentAction(playerID,SetType.Red,type,j));
                        }
                        if(MDGS.playerHasSet(playerID, SetType.Yellow)) {
                            for(int j=0; j < MDGS.getActionsLeft() && j <= noDoubleTheRent; j++)
                                if(!availableActions.contains(new PropertyRentAction(playerID,SetType.Yellow,type,j)))
                                    availableActions.add(new PropertyRentAction(playerID,SetType.Yellow,type,j));
                        }
                        break;
                    case RailRoadUtilityRent:
                        if(MDGS.playerHasSet(playerID, SetType.RailRoad)) {
                            for(int j=0; j < MDGS.getActionsLeft() && j <= noDoubleTheRent; j++)
                                if(!availableActions.contains(new PropertyRentAction(playerID,SetType.RailRoad,type,j)))
                                    availableActions.add(new PropertyRentAction(playerID,SetType.RailRoad,type,j));
                        }
                        if(MDGS.playerHasSet(playerID, SetType.Utility)) {
                            for(int j=0; j < MDGS.getActionsLeft() && j <= noDoubleTheRent; j++)
                                if(!availableActions.contains(new PropertyRentAction(playerID,SetType.Utility,type,j)))
                                    availableActions.add(new PropertyRentAction(playerID,SetType.Utility,type,j));
                        }
                        break;
                    case PassGo:
                        if(!availableActions.contains(new PassGoAction()))
                            availableActions.add(new PassGoAction());
                        break;
                    case DebtCollector:
                        if(!availableActions.contains(new DebtCollectorAction(playerID)))
                            availableActions.add(new DebtCollectorAction(playerID));
                        break;
                    case ItsMyBirthday:
                        if(!availableActions.contains(new ItsMyBirthdayAction(playerID)))
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
    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }
    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        executed = true;
    }
    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        gs.setActionInProgress(this);
        return true;
    }
    @Override
    public PlayActionCard copy() {
        PlayActionCard action = new PlayActionCard(playerID);
        action.executed = executed;
        return action;
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
        return "Play Action Card";
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
