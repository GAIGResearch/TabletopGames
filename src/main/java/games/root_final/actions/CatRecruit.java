package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.RootBoardNodeWithRootEdges;

import java.util.Objects;

public class CatRecruit extends AbstractAction {
    public final int playerID;

    public CatRecruit(int playerID
    ) {
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if(currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.MarquiseDeCat){
            for(RootBoardNodeWithRootEdges node: currentState.getGameMap().getRecruiters()){
                for(int i = 0 ;i < node.getRecruiters(); i++) {
                    if (currentState.getCatWarriors() > 0) {
                        currentState.removeCatWarrior();
                        node.addCatWarrior();
                    }else{
                        //System.out.println("Not enough warriors to populate each recruiter");
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){return true;}
        if(obj instanceof CatRecruit){
            CatRecruit other = (CatRecruit) obj;
            return playerID == other.playerID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("CatRecruit", playerID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + "Recruits at recruiters";
    }
}
