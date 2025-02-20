package games.root.actions.extended;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import core.interfaces.IExtendedSequence;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.actions.AddCardToSupporters;
import games.root.actions.Move;
import games.root.actions.Pass;
import games.root.actions.choosers.ChooseNumber;
import games.root.actions.choosers.ChooseCardForSupporters;
import games.root.actions.choosers.ChooseNode;
import games.root.components.cards.RootCard;
import games.root.components.RootBoardNodeWithRootEdges;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class March extends AbstractAction implements IExtendedSequence {
    public final int playerID;

    public enum Stage {
        chooseFrom,
        chooseAmount,
        chooseTo,
        Outrage,
        OutrageWoodland;
    }

    Stage stage = Stage.chooseFrom;
    int fromNodeID;
    int toNodeID;
    boolean done = false;
    int amount = 0;
    int movesMade = 0;

    public March(int playerID) {
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.MarquiseDeCat) {
            movesMade = 0;
            currentState.setActionInProgress(this);
            currentState.increaseActionsPlayed();
            return true;
        }
        return false;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        RootGameState currentState = (RootGameState) state;
        List<AbstractAction> actions = new ArrayList<>();
        switch (stage) {
            case chooseFrom:
                for (RootBoardNodeWithRootEdges location:currentState.getGameMap().getNonForrestBoardNodes()){
                    if (location.getWarrior(currentState.getPlayerFaction(playerID)) > 0 && location.canMove(playerID)){
                        actions.add(new ChooseNode(playerID, location.getComponentID()));
                    }
                }
                if (actions.isEmpty()){
                    actions.add(new Pass(playerID, "unable to make a second move"));
                }
                return actions;
            case chooseAmount:
                for (int i = 1; i <= currentState.getGameMap().getNodeByID(fromNodeID).getWarrior(currentState.getPlayerFaction(playerID)); i++){
                    actions.add(new ChooseNumber(playerID,i));
                }
                return actions;
            case chooseTo:
                RootBoardNodeWithRootEdges from = currentState.getGameMap().getNodeByID(fromNodeID);
                for (RootBoardNodeWithRootEdges neighbour: from.getNeighbours()){
                    if(neighbour.getClearingType() != RootParameters.ClearingTypes.Forrest){
                        if (from.rulerID == playerID || neighbour.rulerID == playerID){
                            Move move = new Move(fromNodeID, neighbour.getComponentID(), amount, playerID);
                            actions.add(move);
                        }
                    }
                }
                return actions;
            case Outrage:
                PartialObservableDeck<RootCard> hand = currentState.getPlayerHand(playerID);
                for (int i = 0 ; i < hand.getSize(); i++){
                    if (hand.get(i).suit == currentState.getGameMap().getNodeByID(toNodeID).getClearingType() || hand.get(i).suit == RootParameters.ClearingTypes.Bird){
                        AddCardToSupporters action = new AddCardToSupporters(playerID, i, hand.get(i).getComponentID());
                        if (!actions.contains(action)) actions.add(action);
                    }
                }
                if (actions.isEmpty()){
                    actions.add(new Pass(playerID));
                }
                return actions;
            case OutrageWoodland:
                PartialObservableDeck<RootCard> playerHand = currentState.getPlayerHand(playerID);
                for (int i = 0; i < playerHand.getSize(); i++){
                    ChooseCardForSupporters action = new ChooseCardForSupporters(currentState.getCurrentPlayer(), playerID, i);
                    if(!actions.contains(action)) actions.add(action);
                }
                if (actions.isEmpty()){
                    actions.add(new Pass(currentState.getCurrentPlayer()));
                }
                return actions;
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        RootGameState gs = (RootGameState) state;
        return switch (stage) {
            case OutrageWoodland -> gs.getFactionPlayerID(RootParameters.Factions.WoodlandAlliance);
            default -> playerID;
        };
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        RootGameState gs = (RootGameState) state;
        if (stage == Stage.chooseFrom && action instanceof ChooseNode ca) {
            fromNodeID = ca.nodeID;
            stage = Stage.chooseAmount;
        }
        if (stage == Stage.chooseAmount && action instanceof ChooseNumber a) {
            amount = a.number;
            stage = Stage.chooseTo;
        } else if (stage == Stage.chooseTo && action instanceof Move m) {
            if (gs.getGameMap().getNodeByID(m.to).hasToken(RootParameters.TokenType.Sympathy) && gs.getPlayerFaction(playerID) != RootParameters.Factions.WoodlandAlliance) {
                stage = Stage.Outrage;
                toNodeID = m.to;
                movesMade++;
            } else {
                movesMade++;
                if (movesMade == 2) {
                    done = true;
                } else {
                    stage = Stage.chooseFrom;
                }
            }
        } else if (stage == Stage.Outrage) {
            if (action instanceof Pass) {
                stage = Stage.OutrageWoodland;
                ArrayList<boolean[]> handVisibility = new ArrayList<>();
                for (int i = 0; i <  gs.getPlayerHand(playerID).getSize(); i++) {
                    boolean[] cardVisibility = new boolean[gs.getNPlayers()];
                    cardVisibility[playerID] = true;
                    cardVisibility[gs.getFactionPlayerID(RootParameters.Factions.WoodlandAlliance)] = true;
                    handVisibility.add(cardVisibility);
                }
                gs.getPlayerHand(playerID).setVisibility(handVisibility);
            } else {
                if (movesMade == 2) {
                    done = true;
                } else {
                    stage = Stage.chooseFrom;
                }
            }

        } else if (stage == Stage.OutrageWoodland) {
            if (movesMade == 2) {
                done = true;
            } else {
                stage = Stage.chooseFrom;
            }
        } else if (action instanceof Pass) {
            done = true;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return done;
    }

    @Override
    public March copy() {
        March copy = new March(playerID);
        copy.movesMade = movesMade;
        copy.stage = stage;
        copy.done = done;
        copy.amount = amount;
        copy.fromNodeID = fromNodeID;
        copy.toNodeID = toNodeID;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof March march)) return false;
        return playerID == march.playerID && fromNodeID == march.fromNodeID && toNodeID == march.toNodeID && done == march.done && amount == march.amount && movesMade == march.movesMade && stage == march.stage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, stage, fromNodeID, toNodeID, done, amount, movesMade);
    }

    @Override
    public String toString() {
        return "p" + playerID + " starts marching";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " starts marching";
    }
}
