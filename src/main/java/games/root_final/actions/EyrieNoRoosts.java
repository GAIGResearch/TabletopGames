package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.RootBoardNodeWithRootEdges;

import java.util.Objects;

public class EyrieNoRoosts extends AbstractAction {
    public final int playerID;
    public final int locationID;
    public final boolean passSubGamePhase;

    public EyrieNoRoosts(int playerID, int location, boolean passSubGamePhase){
        this.playerID = playerID;
        this.locationID = location;
        this.passSubGamePhase = passSubGamePhase;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState state = (RootGameState) gs;
        if(state.getCurrentPlayer() == playerID && state.getPlayerFaction(playerID) == RootParameters.Factions.EyrieDynasties){
            try {
                state.increaseActionsPlayed();
                if(passSubGamePhase){
                    state.increaseSubGamePhase();
                }
                state.getGameMap().getNodeByID(locationID).build(RootParameters.BuildingType.Roost);
                state.removeBuilding(RootParameters.BuildingType.Roost);
                for(int i = 0; i < 3; i++){
                    state.getGameMap().getNodeByID(locationID).addWarrior(RootParameters.Factions.EyrieDynasties);
                    state.removeBirdWarrior();
                }
                return true;
            }catch (Exception e){
                System.out.println("Something went wrong when adding a Roost and 3 Eyrie warriors");
                return false;
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new EyrieNoRoosts(playerID, locationID, passSubGamePhase);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){return true;}
        if(obj instanceof EyrieNoRoosts){
            EyrieNoRoosts other = (EyrieNoRoosts) obj;
            return playerID == other.playerID && passSubGamePhase == other.passSubGamePhase && locationID == other.locationID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("EyrieNoRoosts", playerID, locationID, passSubGamePhase);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + "Places a Roost and 3 warriors at " + gs.getGameMap().getNodeByID(locationID).identifier;
    }
}
