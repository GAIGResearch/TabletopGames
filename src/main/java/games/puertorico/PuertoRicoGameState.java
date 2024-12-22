package games.puertorico;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import games.GameType;
import games.puertorico.PuertoRicoConstants.Role;
import games.puertorico.PuertoRicoConstants.Crop;
import games.puertorico.components.*;

import java.util.*;

import static java.util.stream.Collectors.*;


public class PuertoRicoGameState extends AbstractGameState {

    List<Ship> ships;
    List<Crop> soldInMarket;
    Deck<Plantation> plantationDeck, visiblePlantations, plantationDiscards;
    List<Plantation> quarries;
    Map<PuertoRicoConstants.BuildingType, Integer> buildingsAvailable;
    List<PRPlayerBoard> playerBoards;
    EnumMap<PuertoRicoConstants.Crop, Integer> cropSupply;
    EnumMap<Role, Boolean> rolesAvailable;
    Map<Role, Integer> moneyOnRoles;
    Role currentRole;
    int roleOwner;

    int colonistsInSupply;
    int colonistsOnShip;
    int vpSupply;
    boolean gameEndTriggered;

    /**
     * @param gameParameters - game parameters.
     * @param nPlayers
     */
    public PuertoRicoGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.PuertoRico;
    }

    public PRPlayerBoard getPlayerBoard(int player) {
        return playerBoards.get(player);
    }

    public void setCurrentRole(Role role) {
        if (role != null && !rolesAvailable.get(role)) {
            throw new IllegalArgumentException("Role " + role + " is not available");
        }
        if (role != null) {
            rolesAvailable.put(role, false);
        }
        currentRole = role;
        this.roleOwner = getCurrentPlayer();
    }

    // First player (the one who chose the role)
    public int getRoleOwner() {
        return roleOwner;
    }

    public Map<Role, Integer> getAvailableRoles() {
        Map<Role, Integer> retValue = new HashMap<>();
        for (Role r : rolesAvailable.keySet()) {
            if (rolesAvailable.get(r))
                retValue.put(r, moneyOnRoles.get(r));
        }
        return retValue;
    }

    public boolean isRoleAvailable(Role r) {
        if (rolesAvailable.containsKey(r)) return rolesAvailable.get(r);
        return false;
    }

    public Role getCurrentRole() {
        return currentRole;
    }

    public int getMoneyOnRole(Role r) {
        if (moneyOnRoles.containsKey(r)) return moneyOnRoles.get(r);
        return 0;
    }

    public Deck<Plantation> getAvailablePlantations() {
        return visiblePlantations;
    }

    public int getQuarriesLeft() {
        return quarries.size();
    }
    public Plantation removeQuarry() {
        return quarries.remove(0);
    }

    public int numberOfPlantationsInStack() {
        return plantationDeck.getSize();
    }

    public int numberOfPlantationsInDiscard() {
        return plantationDiscards.getSize();
    }

    public Ship getShip(int shipId) {
        return ships.get(shipId);
    }

    public List<Ship> getShips() {
        return ships;
    }

    public void addVP(int playerId, int amount) {
        playerBoards.get(playerId).vp += amount;
        vpSupply -= amount;
        if (vpSupply <= 0)
            gameEndTriggered = true;
    }

    public Plantation drawPlantationFromStack() {
        return plantationDeck.draw();
    }

    public void discardRemainingPlantations() {
        plantationDiscards.add(visiblePlantations);
        visiblePlantations.clear();
    }

    public void drawNewVisiblePlantations() {
        PuertoRicoParameters params = (PuertoRicoParameters) gameParameters;
        for (int i = 0; i < nPlayers + params.extraVisiblePlantations; i++) {
            if (plantationDeck.getSize() == 0) {
                shufflePlantationDiscardsIntoStack();
            }
            if (numberOfPlantationsInStack() > 0)
                visiblePlantations.add(plantationDeck.draw());
        }
    }

    public void shufflePlantationDiscardsIntoStack() {
        plantationDeck.add(plantationDiscards);
        plantationDiscards.clear();
    }

    public Plantation drawPlantation(Crop crop) {
        Plantation retValue = null;
        for (Plantation p : visiblePlantations.getComponents()) {
            if (p.crop == crop) {
                retValue = p;
                break;
            }
        }
        if (retValue == null)
            throw new IllegalArgumentException("No plantations of type " + crop + " available");
        visiblePlantations.remove(retValue);
        return retValue;
    }

    public void addPlantation(int playerId, Plantation plantation) {
        PuertoRicoParameters params = (PuertoRicoParameters) gameParameters;
        if (playerBoards.get(playerId).plantations.size() >= params.plantationSlotsOnBoard)
            throw new IllegalArgumentException("Player " + playerId + " already has " + params.plantationSlotsOnBoard + " plantations");
        playerBoards.get(playerId).addPlantation(plantation);
    }

    public Building build(int playerId, PuertoRicoConstants.BuildingType type) {
        PuertoRicoParameters params = (PuertoRicoParameters) gameParameters;
        if (playerBoards.get(playerId).buildings.stream().anyMatch(b -> b.buildingType == type))
            throw new IllegalArgumentException("Player " + playerId + " already has a building of type " + type);
        if (buildingsAvailable.getOrDefault(type, 0) < 1)
            throw new IllegalArgumentException("No buildings of type " + type + " available");
        if (playerBoards.get(playerId).getTownSize() + type.size > params.townSlotsOnBoard)
            throw new IllegalArgumentException("Player " + playerId + " has no space available for " + type);
        Building newBuilding = type.instantiate();
        buildingsAvailable.put(type, buildingsAvailable.get(type) - 1);
        playerBoards.get(playerId).buildings.add(newBuilding);
        if (playerBoards.get(playerId).getTownSize() >= params.townSlotsOnBoard)
            gameEndTriggered = true;
        return newBuilding;
    }

    public List<PuertoRicoConstants.BuildingType> getAvailableBuildings() {
        return buildingsAvailable.keySet().stream()
                .filter(type -> buildingsAvailable.get(type) > 0)
                .collect(toList());
    }

    public int getBuildingsOfType(PuertoRicoConstants.BuildingType type) {
        return buildingsAvailable.get(type);
    }

    public boolean hasActiveBuilding(int playerID, PuertoRicoConstants.BuildingType type) {
        return playerBoards.get(playerID).buildings.stream().anyMatch(b -> b.buildingType == type && b.getOccupation() > 0 && !b.hasBeenUsed());
    }

    // The stores of the crop type that the playerId has
    public int getStoresOf(int playerId, Crop crop) {
        return playerBoards.get(playerId).getStoresOf(crop);
    }

    // the amount of money that the specified player has
    public int getDoubloons(int playerId) {
        return playerBoards.get(playerId).doubloons;
    }

    public void changeDoubloons(int playerId, int amount) {
        playerBoards.get(playerId).changeDoubloons(amount);
    }

    // Amount of crop that is available in the general supply
    public int getSupplyOf(Crop crop) {
        return cropSupply.get(crop);
    }

    public void changeSupplyOf(Crop crop, int amount) {
        if (cropSupply.get(crop) + amount < 0)
            throw new IllegalArgumentException("Cannot have negative supply of " + crop);
        cropSupply.put(crop, cropSupply.get(crop) + amount);
    }

    public int getColonistsInSupply() {
        return colonistsInSupply;
    }

    public int getColonistsOnShip() {
        return colonistsOnShip;
    }

    public void changeColonistsInSupply(int amount) {
        if (colonistsInSupply + amount < 0)
            throw new IllegalArgumentException("Cannot have negative colonists in supply");
        colonistsInSupply += amount;
    }

    public void changeColonistsOnShip(int amount) {
        if (colonistsOnShip + amount < 0)
            throw new IllegalArgumentException("Cannot have negative colonists on ship");
        colonistsOnShip += amount;
    }

    // Which crops have been sold in the market, and therefore cannot be sold again
    public List<Crop> getMarket() {
        return new ArrayList<>(soldInMarket);
    }

    // This moves good from player board to market, but does not do the finance side of things
    public void sellGood(int player, Crop crop) {
        if (soldInMarket.size() >= ((PuertoRicoParameters) gameParameters).marketCapacity) {
            throw new IllegalArgumentException("Cannot sell more than " + ((PuertoRicoParameters) gameParameters).marketCapacity + " goods");
        }
        if (getStoresOf(player, crop) == 0) {
            throw new IllegalArgumentException("Player " + player + " has no " + crop + " to sell");
        }
        playerBoards.get(player).harvest(crop, -1);
        soldInMarket.add(crop);
    }

    public void emptyMarket() {
        for (Crop crop : soldInMarket) {
            changeSupplyOf(crop, 1);
        }
        soldInMarket.clear();
    }

    public void setGameEndTriggered() {
        gameEndTriggered = true;
    }

    // Has a game end condition been triggered
    public boolean isLastRound() {
        return gameEndTriggered;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> retValue = playerBoards.stream().flatMap(pb -> pb.plantations.stream()).collect(toList());
        retValue.addAll(plantationDeck.getComponents());
        retValue.addAll(visiblePlantations.getComponents());
        return retValue;
    }

    @Override
    protected PuertoRicoGameState _copy(int playerId) {
        PuertoRicoGameState retValue = new PuertoRicoGameState(gameParameters.copy(), nPlayers);
        retValue.ships = ships.stream().map(Ship::copy).collect(toList());
        retValue.plantationDeck = plantationDeck.copy();
        if (playerId != -1) {
            retValue.plantationDeck.shuffle(redeterminisationRnd);
        }
        retValue.visiblePlantations = visiblePlantations.copy();
        retValue.plantationDiscards = plantationDiscards.copy();
        retValue.playerBoards = playerBoards.stream().map(PRPlayerBoard::copy).collect(toList());
        retValue.cropSupply = new EnumMap<>(cropSupply);
        retValue.rolesAvailable = new EnumMap<>(rolesAvailable);
        retValue.colonistsInSupply = colonistsInSupply;
        retValue.colonistsOnShip = colonistsOnShip;
        retValue.vpSupply = vpSupply;
        retValue.currentRole = currentRole;
        retValue.gameEndTriggered = gameEndTriggered;
        retValue.moneyOnRoles = new EnumMap<>(moneyOnRoles);
        retValue.soldInMarket = new ArrayList<>(soldInMarket);
        retValue.buildingsAvailable = new EnumMap<>(buildingsAvailable);
        retValue.quarries = new ArrayList<>();
        for (Plantation q : quarries) {
            retValue.quarries.add(q.copy());
        }
        return retValue;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return playerBoards.get(playerId).vp / 50.0;
    }

    public int getVPSupply() {
        return vpSupply;
    }

    @Override
    public double getGameScore(int playerId) {
        return playerBoards.get(playerId).vp +
                playerBoards.get(playerId).getBuildings().stream().mapToInt(b -> b.buildingType.vp).sum() +
                playerBoards.get(playerId).getBuildings().stream().filter(b -> b instanceof VictoryBuilding && b.getOccupation() > 0)
                        .map(b -> ((VictoryBuilding)b).getVictoryPoints(this, playerId)).mapToInt(Integer::intValue).sum();
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof PuertoRicoGameState) {
            PuertoRicoGameState other = (PuertoRicoGameState) o;
            return ships.equals(other.ships) &&
                    plantationDeck.equals(other.plantationDeck) &&
                    visiblePlantations.equals(other.visiblePlantations) &&
                    plantationDiscards.equals(other.plantationDiscards) &&
                    rolesAvailable.equals(other.rolesAvailable) &&
                    cropSupply.equals(other.cropSupply) &&
                    vpSupply == other.vpSupply &&
                    currentRole == other.currentRole &&
                    colonistsInSupply == other.colonistsInSupply &&
                    colonistsOnShip == other.colonistsOnShip &&
                    gameEndTriggered == other.gameEndTriggered &&
                    moneyOnRoles.equals(other.moneyOnRoles) &&
                    soldInMarket.equals(other.soldInMarket) &&
                    buildingsAvailable.equals(other.buildingsAvailable) &&
                    quarries.equals(other.quarries) &&
                    playerBoards.equals(other.playerBoards);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ships, plantationDeck, playerBoards, visiblePlantations, currentRole, rolesAvailable, vpSupply, soldInMarket,
                colonistsInSupply, colonistsOnShip, cropSupply, plantationDiscards, gameEndTriggered, moneyOnRoles, quarries, buildingsAvailable);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ships.hashCode()).append("|");
        sb.append(Objects.hash(plantationDiscards, plantationDeck)).append("|");
        sb.append(playerBoards.hashCode()).append("|");
        sb.append(visiblePlantations.hashCode()).append("|");
        sb.append(currentRole).append("|");
        sb.append(rolesAvailable.hashCode()).append("|");
        sb.append(cropSupply.hashCode()).append("|");
        sb.append(Objects.hash(colonistsInSupply, colonistsOnShip, gameEndTriggered, vpSupply)).append("|");
        sb.append(super.hashCode()).append("|");
        sb.append(moneyOnRoles.hashCode()).append("|");
        sb.append(soldInMarket.hashCode()).append("|");
        sb.append(quarries.hashCode()).append("|");
        sb.append(buildingsAvailable.hashCode()).append("|");
        return sb.toString();
    }
}
