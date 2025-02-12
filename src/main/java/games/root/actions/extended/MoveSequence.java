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
import games.root.components.cards.RootCard;
import games.root.components.RootBoardNodeWithRootEdges;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MoveSequence extends AbstractAction implements IExtendedSequence {
    public final int playerID;
    public final int fromNodeID;

    public enum Stage{
        chooseAmount,
        chooseTargetNode,
        Outrage,
        OutrageWoodland,
    }

    int toNodeID;
    Stage stage = Stage.chooseAmount;
    boolean birdPlayed = false;
    boolean done = false;
    int amount = 0;

    public MoveSequence(int playerID, int fromNodeID, boolean birdPlayed){
        this.playerID = playerID;
        this.fromNodeID = fromNodeID;
        this.birdPlayed = birdPlayed;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getGameMap().getNodeByID(fromNodeID).getWarrior(currentState.getPlayerFaction(playerID)) > 0){
            currentState.increaseActionsPlayed();
            if (currentState.getPlayerFaction(playerID) == RootParameters.Factions.EyrieDynasties){
                if (birdPlayed){currentState.addPlayedSuit(RootParameters.ClearingTypes.Bird);}else{currentState.addPlayedSuit(currentState.getGameMap().getNodeByID(fromNodeID).getClearingType());
                }
            }
            currentState.setActionInProgress(this);
            return true;
        }
        return false;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        RootGameState gs = (RootGameState) state;
        List<AbstractAction> actions = new ArrayList<>();
        switch (stage){
            case chooseAmount:
                for (int i = 1; i <= gs.getGameMap().getNodeByID(fromNodeID).getWarrior(gs.getPlayerFaction(playerID)); i++){
                    actions.add(new ChooseNumber(playerID,i));
            }
                return actions;
            case chooseTargetNode:
                RootBoardNodeWithRootEdges from = gs.getGameMap().getNodeByID(fromNodeID);
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
                PartialObservableDeck<RootCard> hand = gs.getPlayerHand(playerID);
                for (int i = 0 ; i < hand.getSize(); i++){
                    if (hand.get(i).suit == gs.getGameMap().getNodeByID(toNodeID).getClearingType() || hand.get(i).suit == RootParameters.ClearingTypes.Bird){
                        AddCardToSupporters action = new AddCardToSupporters(playerID, i, hand.get(i).getComponentID());
                        if (!actions.contains(action)) actions.add(action);
                    }
                }
                if (actions.isEmpty()){
                    actions.add(new Pass(playerID));
                }
                return actions;
            case OutrageWoodland:
                PartialObservableDeck<RootCard> playerHand = gs.getPlayerHand(playerID);
                for (int i = 0; i < playerHand.getSize(); i++){
                    ChooseCardForSupporters action = new ChooseCardForSupporters(gs.getCurrentPlayer(), playerID, i);
                    if(!actions.contains(action)) actions.add(action);
                }
                if (actions.isEmpty()){
                    actions.add(new Pass(gs.getCurrentPlayer()));
                }
                return actions;
            default:
                return null;

        }
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        RootGameState gs = (RootGameState) state;
        return switch (stage){
            case OutrageWoodland -> gs.getFactionPlayerID(RootParameters.Factions.WoodlandAlliance);
            default -> playerID;
        };
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        RootGameState gs = (RootGameState) state;
        if (stage == Stage.chooseAmount && action instanceof ChooseNumber a){
            amount = a.number;
            stage = Stage.chooseTargetNode;
        }else if (stage == Stage.chooseTargetNode && action instanceof Move m){
            if (gs.getGameMap().getNodeByID(m.to).hasToken(RootParameters.TokenType.Sympathy) && gs.getPlayerFaction(playerID)!= RootParameters.Factions.WoodlandAlliance){
                stage = Stage.Outrage;
                toNodeID = m.to;
            }else{
                done = true;
            }
        } else if (stage == Stage.Outrage) {
            if (action instanceof Pass){
                stage = Stage.OutrageWoodland;
                ArrayList<boolean[]> handVisibility = new ArrayList<>();
                for (int i = 0; i <  gs.getPlayerHand(playerID).getSize(); i++) {
                    boolean[] cardVisibility = new boolean[gs.getNPlayers()];
                    cardVisibility[playerID] = true;
                    cardVisibility[gs.getFactionPlayerID(RootParameters.Factions.WoodlandAlliance)] = true;
                    handVisibility.add(cardVisibility);
                }
                gs.getPlayerHand(playerID).setVisibility(handVisibility);
            }else {
                done = true;
            }

        } else if (stage == Stage.OutrageWoodland) {
            done = true;
        }

    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return done;
    }

    @Override
    public MoveSequence copy() {
        MoveSequence copy = new MoveSequence(playerID,fromNodeID, birdPlayed);
        copy.stage = stage;
        copy.done = done;
        copy.amount = amount;
        copy.toNodeID = toNodeID;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MoveSequence that)) return false;
        return playerID == that.playerID && fromNodeID == that.fromNodeID && toNodeID == that.toNodeID && birdPlayed == that.birdPlayed && done == that.done && amount == that.amount && stage == that.stage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, fromNodeID, toNodeID, stage, birdPlayed, done, amount);
    }

    @Override
    public String toString() {
        return "p" + playerID + " wants to move from " + fromNodeID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " wants to move from " + gs.getGameMap().getNodeByID(fromNodeID).identifier;
    }
}
