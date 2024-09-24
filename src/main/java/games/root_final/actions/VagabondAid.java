package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import core.interfaces.IExtendedSequence;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.cards.RootCard;
import games.root_final.components.Item;
import games.root_final.components.RootBoardNodeWithRootEdges;

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

    public Stage stage = Stage.chooseTarget;
    public int targetID;
    public boolean done = false;

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
                actions.add(new ChooseTargetPlayer(playerID, i));
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
                ExhaustItem action = new ExhaustItem(playerID, bag);
                if (!actions.contains(action)) actions.add(action);
            }
        }
        for (Item tea : gs.getTeas()) {
            if (tea.refreshed && !tea.damaged) {
                ExhaustItem action = new ExhaustItem(playerID, tea);
                if (!actions.contains(action)) actions.add(action);
            }
        }
        for (Item coin : gs.getCoins()) {
            if (coin.refreshed && !coin.damaged) {
                ExhaustItem action = new ExhaustItem(playerID, coin);
                if (!actions.contains(action)) actions.add(action);
            }
        }
        for (Item item : gs.getSachel()) {
            if (item.refreshed && !item.damaged) {
                ExhaustItem action = new ExhaustItem(playerID, item);
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
                GiveCard action = new GiveCard(playerID, targetID, hand.get(i));
                if (!actions.contains(action)) actions.add(action);
            }
        }
        return actions;
    }

    private List<AbstractAction> chooseItemToGet(RootGameState gs) {
        List<AbstractAction> actions = new ArrayList<>();
        for (Item craftedItem : gs.getPlayerCraftedItems(targetID)) {
            TakeItem action = new TakeItem(playerID, targetID, craftedItem);
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
                if (action instanceof ChooseTargetPlayer ctp) {
                    targetID = ctp.targetID;
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
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof VagabondAid va) {
            return playerID == va.playerID && done == va.done && stage == va.stage;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("Aid", playerID, targetID, stage, done);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " wants to aid";
    }
}
