package games.root.actions.extended;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import core.interfaces.IExtendedSequence;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.actions.ExhaustItem;
import games.root.actions.GiveCard;
import games.root.actions.TakeItem;
import games.root.actions.choosers.ChooseNumber;
import games.root.components.cards.RootCard;
import games.root.components.Item;
import games.root.components.RootBoardNodeWithRootEdges;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VagabondAid extends AbstractAction implements IExtendedSequence {
    public final int playerID;

    public VagabondAid(int playerID) {
        this.playerID = playerID;
    }

    public enum Stage {
        chooseTarget,
        chooseItemExhaust,
        chooseCard,
        chooseItemGet,
    }

    Stage stage = Stage.chooseTarget;
    int targetID;
    boolean done = false;

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond) {
            currentState.increaseActionsPlayed();
            currentState.setActionInProgress(this);
            return true;
        }
        return false;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        RootGameState gs = (RootGameState) state;
        List<AbstractAction> actions = new ArrayList<>();
        switch (stage) {
            case chooseTarget:
                actions.addAll(getChooseTargetPlayerActions(gs));
                break;
            case chooseItemExhaust:
                actions.addAll(chooseItemToExhaust(gs));
                break;
            case chooseCard:
                actions.addAll(chooseCard(gs));
                break;
            case chooseItemGet:
                actions.addAll(chooseItemToGet(gs));
                break;
        }
        return actions;
    }

    private List<AbstractAction> getChooseTargetPlayerActions(RootGameState gs) {
        List<AbstractAction> actions = new ArrayList<>();
        RootBoardNodeWithRootEdges clearing = gs.getGameMap().getVagabondClearing();
        for (int i = 0; i < gs.getNPlayers(); i++) {
            if (i != playerID && clearing.isAttackable(gs.getPlayerFaction(i))) {
                actions.add(new ChooseNumber(playerID, i));
            }
        }
        //can never be empty
        return actions;
    }

    private List<AbstractAction> chooseItemToExhaust(RootGameState gs) {
        List<AbstractAction> actions = new ArrayList<>();
        //exhaust item actions
        for (Item bag : gs.getBags()) {
            if (bag.refreshed && !bag.damaged) {
                ExhaustItem action = new ExhaustItem(playerID, bag.itemType);
                if (!actions.contains(action)) actions.add(action);
            }
        }
        for (Item tea : gs.getTeas()) {
            if (tea.refreshed && !tea.damaged) {
                ExhaustItem action = new ExhaustItem(playerID, tea.itemType);
                if (!actions.contains(action)) actions.add(action);
            }
        }
        for (Item coin : gs.getCoins()) {
            if (coin.refreshed && !coin.damaged) {
                ExhaustItem action = new ExhaustItem(playerID, coin.itemType);
                if (!actions.contains(action)) actions.add(action);
            }
        }
        for (Item item : gs.getSatchel()) {
            if (item.refreshed && !item.damaged) {
                ExhaustItem action = new ExhaustItem(playerID, item.itemType);
                if (!actions.contains(action)) actions.add(action);
            }
        }
        return actions;
    }

    private List<AbstractAction> chooseCard(RootGameState gs) {
        List<AbstractAction> actions = new ArrayList<>();
        //give card actions
        RootBoardNodeWithRootEdges clearing = gs.getGameMap().getVagabondClearing();
        PartialObservableDeck<RootCard> hand = gs.getPlayerHand(playerID);
        for (int i = 0; i < hand.getSize(); i++) {
            if (hand.get(i).suit == clearing.getClearingType() || hand.get(i).suit == RootParameters.ClearingTypes.Bird) {
                GiveCard action = new GiveCard(playerID, targetID, i, hand.get(i).getComponentID());
                if (!actions.contains(action)) actions.add(action);
            }
        }
        return actions;
    }

    private List<AbstractAction> chooseItemToGet(RootGameState gs) {
        List<AbstractAction> actions = new ArrayList<>();
        for (Item craftedItem : gs.getPlayerCraftedItems(targetID)) {
            TakeItem action = new TakeItem(playerID, targetID, craftedItem.itemType);
            if (!actions.contains(action)) actions.add(action);
        }

        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        RootGameState gs = (RootGameState) state;
        switch (stage) {
            case chooseTarget:
                if (action instanceof ChooseNumber ctp) {
                    targetID = ctp.number;
                    stage = Stage.chooseItemExhaust;
                    gs.aid(playerID, gs.getPlayerFaction(targetID));
                }
                break;
            case chooseItemExhaust:
                stage = Stage.chooseCard;
                break;
            case chooseCard:
                if (!gs.getPlayerCraftedItems(targetID).isEmpty()) {
                    stage = Stage.chooseItemGet;
                } else {
                    done = true;
                }
                break;
            case chooseItemGet:
                done = true;
        }

    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return done;
    }

    @Override
    public VagabondAid copy() {
        VagabondAid copy = new VagabondAid(playerID);
        copy.stage = stage;
        copy.targetID = targetID;
        copy.done = done;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VagabondAid that)) return false;
        return playerID == that.playerID && targetID == that.targetID && done == that.done && stage == that.stage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, stage, targetID, done);
    }

    @Override
    public String toString() {
        return "p" + playerID + " wants to aid";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " wants to aid";
    }
}
