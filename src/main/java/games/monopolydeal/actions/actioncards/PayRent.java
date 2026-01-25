package games.monopolydeal.actions.actioncards;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.cards.BoardType;
import games.monopolydeal.actions.informationcontainer.PayCardFrom;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.MonopolyDealCard;
import games.monopolydeal.cards.PropertySet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p> PayRent uses EAS for the payment of rent in response to a played action card. This EAS calls upon itself recursively paying with a card in each iteration until either the rent has been completely paid or the player has no more cards to pay the rent with.
 * </p>
 */
public class PayRent extends AbstractAction implements IExtendedSequence, IActionCard {

    // The extended sequence usually keeps record of the player who played this action, to be able to inform the game whose turn it is to make decisions
    final int payer; // current player
    final int payee; // pays to
    int amtToPay;

    boolean boardEmpty;
    CardType cardToPay;
    BoardType boardType;

    public PayRent(int payer, int payee, int amtToPay) {
        this.payer = payer;
        this.payee = payee;
        this.amtToPay = amtToPay;
    }
    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        MonopolyDealGameState MDGS = (MonopolyDealGameState) state;
        Deck<MonopolyDealCard> payerBank = MDGS.getPlayerBank(payer);
        PropertySet[] payerPropertySets = MDGS.getPropertySets(payer);
        List<AbstractAction> availableActions = new ArrayList<>();

        if(boardEmpty || amtToPay <= 0) availableActions.add(new DoNothing());
        else {
            // iterate through bank and add action
            // iterate through properties and add action
            for(int i=0;i<payerBank.getSize();i++){
                if(!availableActions.contains(new PayCardFrom(payerBank.get(i).cardType())))
                    availableActions.add(new PayCardFrom(payerBank.get(i).cardType()));
            }
            for (PropertySet pSet: payerPropertySets) {
                for(int i=0;i<pSet.getSize();i++)
                    if(pSet.get(i).cardType()!= (CardType.MulticolorWild) && !availableActions.contains(new PayCardFrom(pSet.get(i).cardType(),pSet.getSetType())))
                        availableActions.add(new PayCardFrom(pSet.get(i).cardType(),pSet.getSetType()));
            }
        }

        return availableActions;
    }
    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return payer;
    }
    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if(!(action instanceof DoNothing)){
            MonopolyDealGameState MDGS = (MonopolyDealGameState) state;
            cardToPay = ((PayCardFrom) action).cardType;
            boardType = ((PayCardFrom) action).type;
            switch (boardType){
                case Bank:
                    MDGS.removeMoneyFrom(payer,cardToPay);
                    MDGS.addMoney(payee,cardToPay);
                    break;
                case PropertySet:
                    MDGS.removePropertyFrom(payer,cardToPay,((PayCardFrom) action).from);
                    MDGS.addProperty(payee,cardToPay);
                    break;
            }
            amtToPay = amtToPay - cardToPay.moneyValue;
            if(MDGS.isBoardEmpty(payer)) boardEmpty = true;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return amtToPay <= 0 || boardEmpty;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        gs.setActionInProgress(this);
        return true;
    }
    @Override
    public PayRent copy() {
        PayRent action = new PayRent(payer,payee,amtToPay);
        action.boardEmpty = boardEmpty;
        action.cardToPay = cardToPay;
        action.boardType = boardType;
        return action;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PayRent payRent = (PayRent) o;
        return payer == payRent.payer && payee == payRent.payee && amtToPay == payRent.amtToPay &&
                boardEmpty == payRent.boardEmpty && cardToPay == payRent.cardToPay && boardType == payRent.boardType;
    }
    @Override
    public int hashCode() {
        return Objects.hash(payer, payee, amtToPay, boardEmpty, cardToPay, boardType);
    }
    @Override
    public String toString() { return "PayRent action"; }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    public int getTarget(MonopolyDealGameState gs) {
        return payee;
    }
}
