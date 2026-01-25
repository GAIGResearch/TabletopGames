package games.monopolydeal.actions.boardmanagement;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.MonopolyDealCard;
import games.monopolydeal.cards.PropertySet;
import games.monopolydeal.cards.SetType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;

/**
 * <p>An intermediary action for choosing which card to add to the player's board</p>
 */
public class AddToBoard extends AbstractAction implements IExtendedSequence {

    // The extended sequence usually keeps record of the player who played this action, to be able to inform the game whose turn it is to make decisions
    final int playerID;
    boolean executed;

    public AddToBoard(int playerID) {
        this.playerID = playerID;
    }
    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        MonopolyDealGameState MDGS = (MonopolyDealGameState) state;
        int playerID = MDGS.getCurrentPlayer();

        // Adding property cards and base wildcards to properties
        List<AbstractAction> availableActions = MDGS.getPlayerHand(playerID).stream()
                .filter(MonopolyDealCard::isPropertyCard)
                .filter(MonopolyDealCard::isNotMulticolor)
                .map(card-> new AddProperty(card.cardType(),playerID))
                .collect(toList());

        // Adding money to bank
        availableActions.addAll(MDGS.getPlayerHand(playerID).stream().filter(((Predicate<? super MonopolyDealCard>)MonopolyDealCard::isPropertyCard).negate()).map(card ->new AddMoney(card.cardType(),playerID)).collect(toList()));

        // Adding multicolor wild to existing sets
        MonopolyDealCard temp = MonopolyDealCard.create(CardType.MulticolorWild);
        if(MDGS.getPlayerHand(playerID).getComponents().contains(temp)){
            availableActions.addAll(Arrays.stream(MDGS.getPropertySets(playerID)).filter(((Predicate<? super PropertySet>)PropertySet::getIsComplete).negate())
                    .map(propertySet -> new AddWildTo(propertySet,playerID)).collect(toList()));
            availableActions.add(new AddProperty(temp.cardType(),playerID));
        }

        // Add house or hotel
        MonopolyDealCard temp1 = MonopolyDealCard.create(CardType.House);
        if(MDGS.getPlayerHand(playerID).getComponents().contains(temp1)){
            PropertySet[] playerProperties = MDGS.getPropertySets(playerID);
            for (PropertySet pSet: playerProperties) {
                if(pSet.isComplete) availableActions.add(new AddBuilding(temp1.cardType(), playerID, pSet.getSetType()));
            }
            availableActions.add(new AddBuilding(temp1.cardType(), playerID, SetType.UNDEFINED));
        }
        MonopolyDealCard temp2 = MonopolyDealCard.create(CardType.Hotel);
        if(MDGS.getPlayerHand(playerID).getComponents().contains(temp2)){
            PropertySet[] playerProperties = MDGS.getPropertySets(playerID);
            for (PropertySet pSet: playerProperties) {
                if(pSet.isComplete && pSet.hasHouse) availableActions.add(new AddBuilding(temp2.cardType(), playerID, pSet.getSetType()));
            }
            availableActions.add(new AddBuilding(temp2.cardType(), playerID, SetType.UNDEFINED));
        }

        // remove duplicate actions
        List<AbstractAction> retActions = new ArrayList<>();
        for (AbstractAction action: availableActions) {
            if(!retActions.contains(action)) retActions.add(action);
        }
        return retActions;
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
    public AddToBoard copy() {
        AddToBoard action = new AddToBoard(playerID);
        action.executed = executed;
        return action;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddToBoard that = (AddToBoard) o;
        return playerID == that.playerID && executed == that.executed;
    }
    @Override
    public int hashCode() {
        return Objects.hash(playerID, executed);
    }
    @Override
    public String toString() { return "Add to Board"; }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
