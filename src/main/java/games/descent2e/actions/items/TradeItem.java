package games.descent2e.actions.items;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Hero;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TradeItem extends DescentAction implements IExtendedSequence {

    public List<Integer> traded = new ArrayList<>();

    public int userID = -1;
    public int targetID = -1;
    public int itemID = -1;
    private boolean complete = false;

    public TradeItem() {
        super(Triggers.ANYTIME);
    }

    public TradeItem(int userID, int targetID) {
        super(Triggers.MOVE_INTO_SPACE);
        this.userID = userID;
        this.targetID = targetID;
    }

    public TradeItem(int userID, int targetID, int itemID, List<Integer> traded) {
        super(Triggers.ANYTIME);
        this.userID = userID;
        this.targetID = targetID;
        this.itemID = itemID;
        this.traded = new ArrayList<>(traded);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {

        List<TradeItem> trades = new ArrayList<>();

        DescentGameState dgs = (DescentGameState) state;
        Hero user = (Hero) dgs.getComponentById(userID);
        Hero target = (Hero) dgs.getComponentById(targetID);

        for (DescentCard item : user.getInventory()) {
            TradeItem trade = new TradeItem(userID, targetID, item.getComponentID(), traded);
            if (trade.canExecute(dgs))
                trades.add(trade);
        }

        for (DescentCard item : target.getInventory()) {
            TradeItem trade = new TradeItem(userID, targetID, item.getComponentID(), traded);
            if (trade.canExecute(dgs))
                trades.add(trade);
        }

        TradeItem endTrade = new TradeItem();
        if (endTrade.canExecute(dgs))
            trades.add(endTrade);

        return new ArrayList<>(trades);
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return state.getComponentById(userID).getOwnerId();
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof TradeItem trade) {
            if (trade.isEndTrade()) {
                complete = true;
            }
            DescentCard item = (DescentCard) state.getComponentById(trade.itemID);
            if (item != null)
                traded.add(trade.itemID);
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return complete;
    }

    @Override
    public boolean execute(DescentGameState dgs) {

        if (isEndTrade()) {
            complete = true;
            return true;
        }

        Hero user = (Hero) dgs.getComponentById(userID);
        user.addActionTaken(toString());

        if (itemID != -1) {
            DescentCard item = (DescentCard) dgs.getComponentById(itemID);

            Hero target = (Hero) dgs.getComponentById(targetID);

            boolean giving = user.getInventory().contains(item);

            if (giving) {
                user.getInventory().remove(item);
                target.getInventory().add(item);
            }
            else {
                user.getInventory().add(item);
                target.getInventory().remove(item);
            }
            complete = true;
            return true;
        }

        dgs.setActionInProgress(this);


        return true;
    }

    @Override
    public TradeItem copy() {
        TradeItem trade = new TradeItem(userID, targetID, itemID, traded);
        trade.complete = complete;
        return trade;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TradeItem other)) return false;
        return userID == other.userID && targetID == other.targetID &&
                itemID == other.itemID && complete == other.complete &&
                Objects.equals(traded, other.traded);
    }

    @Override
    public int hashCode(){
        return Objects.hash(super.hashCode(), userID, targetID, itemID, complete, traded);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        if (isEndTrade())
            return "End Trade";

        Hero target = (Hero) gameState.getComponentById(targetID);
        String targetName = target.getComponentName().replace("Hero: ", "");

        if (itemID == -1) {
            return "Begin Trade with " + targetName;
        }

        DescentCard item = (DescentCard) gameState.getComponentById(itemID);

        boolean receiving = target.getInventory().contains(item);
        if (receiving)
            return "Receive " + item.getComponentName() + " from " + targetName;
        return "Give " + item.getComponentName() + " to " + targetName;
    }

    @Override
    public String toString() {
        if (itemID == -1) {
            if (isEndTrade())
                return "End Trade";
            else
                return "Begin Trade";
        }
        return "Trade Item " + itemID + " between " + userID + " and " + targetID;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        if (userID == itemID)
            return true;

        if (userID == -1 || targetID == -1) return false;
        Hero user = (Hero) dgs.getComponentById(userID);
        if (user == null) return false;
        Hero target = (Hero) dgs.getComponentById(targetID);
        if (target == null) return false;

        // Prevent initiating trades with the same Heroes after we've just finished one
        if (user.getActionsTaken().contains(toString()))
            return false;

        if (!DescentHelper.checkAdjacent(dgs, user, target)) return false;

        if (itemID != -1) {
            DescentCard item = (DescentCard) dgs.getComponentById(itemID);
            if (item == null) return false;
            // Can't trade something if we've already traded it this action
            if (traded.contains(item)) return false;
            return user.getInventory().contains(item) || target.getInventory().contains(item);
        }

        return !user.getInventory().getComponents().isEmpty() || !target.getInventory().getComponents().isEmpty();
    }

    public boolean isEndTrade(){
        return userID == itemID;
    }
}
