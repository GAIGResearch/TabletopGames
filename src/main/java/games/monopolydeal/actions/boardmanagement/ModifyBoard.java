package games.monopolydeal.actions.boardmanagement;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.actions.informationcontainer.MoveCardFromTo;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.MonopolyDealCard;
import games.monopolydeal.cards.PropertySet;
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
 * <p>They should also extend the {@link AbstractAction} class, or any other core actions.</p>
 */
public class ModifyBoard extends AbstractAction implements IExtendedSequence {

    // The extended sequence usually keeps record of the player who played this action, to be able to inform the game whose turn it is to make decisions
    final int playerID;
    boolean executed;
    public ModifyBoard(int playerID) { this.playerID = playerID; }
    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        MonopolyDealGameState MDGS = (MonopolyDealGameState) state;
        // move card from to
        // Iterate through sets
        //   find wilds
        //      get alternate
        //      MoveFromTo
        List<AbstractAction> availableActions = new ArrayList<>();
        for (PropertySet pSet: MDGS.getPropertySets(playerID)) {
            if(pSet.hasWild){
                for (int i=0;i<pSet.getSize();i++) {
                    MonopolyDealCard card = pSet.get(i);
                    if(card.isPropertyWildCard()){
                        SetType sType = card.getAlternateSetType(card);
                        if(sType==SetType.UNDEFINED){
                            for (PropertySet propSet:MDGS.getPropertySets(playerID)) {
                                if(propSet.getSetType() != card.getUseAs() && !availableActions.contains(new MoveCardFromTo(playerID,card,pSet.getSetType(),propSet.getSetType()))){
                                    availableActions.add(new MoveCardFromTo(playerID,card,pSet.getSetType(),propSet.getSetType()));
                                }
                            }
                        }
                        if(!availableActions.contains(new MoveCardFromTo(playerID,card,pSet.getSetType(),sType)))
                            availableActions.add(new MoveCardFromTo(playerID,card,pSet.getSetType(),sType));
                    }
                }
            } else if (pSet.hasHouse && !pSet.hasHotel) { // Moving House
                for (PropertySet propSet: MDGS.getPropertySets(playerID)) {
                    if(propSet!= pSet && propSet.isComplete && !propSet.hasHouse){
                        if(!availableActions.contains(new MoveCardFromTo(playerID,MonopolyDealCard.create(CardType.House),pSet.getSetType(),propSet.getSetType())))
                            availableActions.add(new MoveCardFromTo(playerID,MonopolyDealCard.create(CardType.House),pSet.getSetType(),propSet.getSetType()));
                    }
                }
                if(!availableActions.contains(new MoveCardFromTo(playerID,MonopolyDealCard.create(CardType.House),pSet.getSetType(),SetType.UNDEFINED)))
                    availableActions.add(new MoveCardFromTo(playerID,MonopolyDealCard.create(CardType.House),pSet.getSetType(),SetType.UNDEFINED));
            }else if (pSet.hasHotel){ // Moving Hotel
                for (PropertySet propSet: MDGS.getPropertySets(playerID)) {
                    if(propSet!= pSet && propSet.isComplete && propSet.hasHouse && !propSet.hasHotel){
                        if(!availableActions.contains(new MoveCardFromTo(playerID,MonopolyDealCard.create(CardType.Hotel),pSet.getSetType(),propSet.getSetType())))
                            availableActions.add(new MoveCardFromTo(playerID,MonopolyDealCard.create(CardType.Hotel),pSet.getSetType(),propSet.getSetType()));
                    }
                }
                if(!availableActions.contains(new MoveCardFromTo(playerID,MonopolyDealCard.create(CardType.Hotel),pSet.getSetType(),SetType.UNDEFINED)))
                    availableActions.add(new MoveCardFromTo(playerID,MonopolyDealCard.create(CardType.Hotel),pSet.getSetType(),SetType.UNDEFINED));
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
        MoveCardFromTo actionTaken = (MoveCardFromTo) action;
        MonopolyDealCard card = actionTaken.card;

        // Debugging
        if(card.cardType()==CardType.House)
        {
            int i=0;
        }

        SetType from = actionTaken.from;
        SetType to = actionTaken.to;

        MonopolyDealGameState MDGS = (MonopolyDealGameState) state;
        MDGS.removePropertyFrom(playerID,card,from);
        MDGS.addPropertyToSet(playerID,card,to);
        MDGS.modifyBoard();

        // Debugging
        if(card.cardType()==CardType.House)
        {
            int i=0;
        }

        executed = true;

    }
    @Override
    public boolean executionComplete(AbstractGameState state) { return executed; }
    @Override
    public boolean execute(AbstractGameState gs) {
        gs.setActionInProgress(this);
        return true;
    }
    @Override
    public ModifyBoard copy() {
        ModifyBoard action = new ModifyBoard(playerID);
        action.executed = executed;
        return action;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModifyBoard that = (ModifyBoard) o;
        return playerID == that.playerID && executed == that.executed;
    }
    @Override
    public int hashCode() {
        return Objects.hash(playerID, executed);
    }
    @Override
    public String toString() { return "Modify Board"; }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
