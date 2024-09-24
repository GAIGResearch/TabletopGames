package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import core.interfaces.IExtendedSequence;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.cards.RootCard;
import games.root_final.components.RootBoardNodeWithRootEdges;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MoveSequence extends AbstractAction implements IExtendedSequence {
    public final int playerID;
    public final int fromNodeID;
    public int toNodeID;
    public enum Stage{
        chooseAmount,
        chooseTargetNode,
        Outrage,
        OutrageWoodland,
    }

    public Stage stage = Stage.chooseAmount;

    public boolean birdPlayed = false;

    public boolean done = false;

    public int amount = 0;

    public MoveSequence(int playerID, int fromNodeID){
        this.playerID = playerID;
        this.fromNodeID = fromNodeID;
    }

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
                    actions.add(new ChooseAmount(playerID,i));
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
                        AddCardToSupporters action = new AddCardToSupporters(playerID, hand.get(i));
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
        if (stage == Stage.chooseAmount && action instanceof ChooseAmount a){
            amount = a.amount;
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
    public boolean equals(Object obj) {
        if(obj == this){return true;}
        if (obj instanceof MoveSequence){
            MoveSequence other = (MoveSequence) obj;
            return playerID == other.playerID && fromNodeID == other.fromNodeID && birdPlayed == other.birdPlayed;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("MoveSequence", playerID, fromNodeID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " wants to move from " + gs.getGameMap().getNodeByID(fromNodeID).identifier;
    }
}
