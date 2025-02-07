package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;

import java.util.Objects;

public class Build extends AbstractAction {
    public final int playerID;
    public final int locationID;
    public final RootParameters.BuildingType buildingType;
    public final boolean endSubGamePhase;
    public final boolean playedBird;

    public Build(int location, int playerID, RootParameters.BuildingType buildingType, boolean endSubGamePhase) {
        this.playerID = playerID;
        this.locationID = location;
        this.buildingType = buildingType;
        this.endSubGamePhase = endSubGamePhase;
        this.playedBird = false;
    }

    public Build(int location, int playerID, RootParameters.BuildingType buildingType, boolean endSubGamePhase, boolean playedBird) {
        this.playerID = playerID;
        this.locationID = location;
        this.buildingType = buildingType;
        this.endSubGamePhase = endSubGamePhase;
        this.playedBird = playedBird;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        RootParameters rp = (RootParameters) gs.getGameParameters();
        if (currentState.getCurrentPlayer() == playerID) {
            currentState.increaseActionsPlayed();
            if (endSubGamePhase) {
                currentState.increaseSubGamePhase();
            }
            switch (buildingType) {
                case Roost:
                    if (currentState.getPlayerFaction(playerID) == RootParameters.Factions.EyrieDynasties && currentState.getBuildingCount(RootParameters.BuildingType.Roost) > 0) {
                        currentState.getGameMap().getNodeByID(locationID).build(RootParameters.BuildingType.Roost);
                        if(playedBird){currentState.addPlayedSuit(RootParameters.ClearingTypes.Bird);}else{currentState.addPlayedSuit(currentState.getGameMap().getNodeByID(locationID).getClearingType());}
                        try {
                            currentState.removeBuilding(buildingType);
                            return true;
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    break;
                case Sawmill:
                    if (currentState.getPlayerFaction(playerID) == RootParameters.Factions.MarquiseDeCat) {
                        currentState.getGameMap().getNodeByID(locationID).build(RootParameters.BuildingType.Sawmill);
                        try {
                            currentState.addGameScorePlayer(playerID, rp.sawmillPoints.get(currentState.getBuildingCount(RootParameters.BuildingType.Sawmill)));
                            currentState.removeBuilding(buildingType);
                            return true;
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    break;
                case Workshop:
                    if (currentState.getPlayerFaction(playerID) == RootParameters.Factions.MarquiseDeCat) {
                        currentState.getGameMap().getNodeByID(locationID).build(RootParameters.BuildingType.Workshop);
                        try {
                            currentState.addGameScorePlayer(playerID, rp.workshopPoints.get(currentState.getBuildingCount(RootParameters.BuildingType.Workshop)));
                            currentState.removeBuilding(buildingType);
                            return true;
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    break;
                case Recruiter:
                    if (currentState.getPlayerFaction(playerID) == RootParameters.Factions.MarquiseDeCat) {
                        currentState.getGameMap().getNodeByID(locationID).build(RootParameters.BuildingType.Recruiter);
                        try {
                            currentState.addGameScorePlayer(playerID, rp.recruiterPoints.get(currentState.getBuildingCount(RootParameters.BuildingType.Recruiter)));
                            currentState.removeBuilding(buildingType);
                            return true;
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    break;
                case FoxBase:
                    if (currentState.getPlayerFaction(playerID) == RootParameters.Factions.WoodlandAlliance && currentState.getGameMap().getNodeByID(locationID).getClearingType() == RootParameters.ClearingTypes.Fox) {
                        currentState.getGameMap().getNodeByID(locationID).build(RootParameters.BuildingType.FoxBase);
                        try {
                            currentState.removeBuilding(buildingType);
                            return true;
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    break;
                case MouseBase:
                    if (currentState.getPlayerFaction(playerID) == RootParameters.Factions.WoodlandAlliance && currentState.getGameMap().getNodeByID(locationID).getClearingType() == RootParameters.ClearingTypes.Mouse) {
                        currentState.getGameMap().getNodeByID(locationID).build(RootParameters.BuildingType.MouseBase);
                        try {
                            currentState.removeBuilding(buildingType);
                            return true;
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    break;
                case RabbitBase:
                    if (currentState.getPlayerFaction(playerID) == RootParameters.Factions.WoodlandAlliance && currentState.getGameMap().getNodeByID(locationID).getClearingType() == RootParameters.ClearingTypes.Rabbit) {
                        currentState.getGameMap().getNodeByID(locationID).build(RootParameters.BuildingType.RabbitBase);
                        try {
                            currentState.removeBuilding(buildingType);
                            return true;
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    break;
            }
        }
        return false;
    }

    @Override
    public Build copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Build build = (Build) o;
        return playerID == build.playerID && locationID == build.locationID && endSubGamePhase == build.endSubGamePhase && playedBird == build.playedBird && buildingType == build.buildingType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, locationID, buildingType, endSubGamePhase, playedBird);
    }

    @Override
    public String toString() {
        return "p" + playerID + " builds " + buildingType.toString() + " at location " + locationID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " builds " + buildingType.toString() + " at location " + gs.getGameMap().getNodeByID(locationID).identifier;
    }

}
