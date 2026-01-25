package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;

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
    public EyrieNoRoosts copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EyrieNoRoosts that = (EyrieNoRoosts) o;
        return playerID == that.playerID && locationID == that.locationID && passSubGamePhase == that.passSubGamePhase;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, locationID, passSubGamePhase);
    }

    @Override
    public String toString() {
        return "p" + playerID + " places a roost and 3 warriors at location " + locationID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " places a Roost and 3 warriors at " + gs.getGameMap().getNodeByID(locationID).identifier;
    }
}
