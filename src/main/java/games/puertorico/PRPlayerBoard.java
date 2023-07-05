package games.puertorico;

import core.CoreConstants;
import core.components.Component;
import games.puertorico.PuertoRicoConstants.Crop;
import games.puertorico.components.Building;
import games.puertorico.components.Plantation;

import java.util.*;

import static java.util.stream.Collectors.toList;

public class PRPlayerBoard extends Component {
    List<Plantation> plantations;
    List<Building> buildings;
    Map<Crop, Integer> stores;
    Set<Crop> cropsInWarehouses;
    int vp;
    int colonistsInSupply;
    int doubloons;

    public PRPlayerBoard(int player) {
        super(CoreConstants.ComponentType.BOARD, "Player Board of Player " + player);
        this.plantations = new ArrayList<>();
        this.buildings = new ArrayList<>();
        this.stores = new HashMap<>();
        this.vp = 0;
        this.ownerId = player;
        this.colonistsInSupply = 0;
        this.doubloons = 0;
        this.cropsInWarehouses = new HashSet<>();
    }
    private PRPlayerBoard(int player, int componentID) {
        super(CoreConstants.ComponentType.BOARD, "Player Board of Player " + player, componentID);
        ownerId = player;
    }

    protected void addPlantation(Plantation p) {
        this.plantations.add(p);
    }
    public int getStoresOf(Crop crop) {
        return this.stores.getOrDefault(crop, 0);
    }
    public int getVP() {
        return this.vp;
    }
    public int getDoubloons() {
        return this.doubloons;
    }
    public void changeDoubloons(int amount) {
        this.doubloons += amount;
        if (this.doubloons < 0)
            throw new IllegalArgumentException("Not enough doubloons");
    }

    public int getTownSize() {
        return buildings.stream().mapToInt(b -> b.buildingType.size).sum();
    }
    public int getPlantationSize() {
        return plantations.size();
    }

    public void harvest(Crop crop, int amount) {
        // If amount is negative, then check that we have enough in stores
        if (amount < 0 && this.getStoresOf(crop) < -amount) {
            throw new IllegalArgumentException("Not enough " + crop + " in stores");
        }
        this.stores.put(crop, this.getStoresOf(crop) + amount);
    }
    public Map<Crop, Integer> getStores() {
        return new HashMap<>(this.stores);
    }

    public int getPlantationsOf(Crop crop) {
        return (int) plantations.stream().filter(p -> p.crop == crop).count();
    }

    public List<Plantation> getPlantations() {
        return plantations;
    }
    public List<Building> getBuildings() {
        return buildings;
    }

    public int getPlantationVacancies() {
        return (int) plantations.stream().filter(p -> !p.isOccupied()).count();
    }
    public int getTownVacancies() {
        return buildings.stream().mapToInt(b -> b.buildingType.capacity - b.getOccupation()).sum();
    }

    public void addColonists(int amount) {
        this.colonistsInSupply += amount;
        if (this.colonistsInSupply < 0)
            throw new IllegalArgumentException("Not enough colonists in San Juan");
    }
    public int getUnassignedColonists() {
        return this.colonistsInSupply;
    }

    public int getTotalColonists() {
        return colonistsInSupply +
                buildings.stream().map(Building::getOccupation).reduce(0, Integer::sum) +
                plantations.stream().mapToInt(p -> p.isOccupied() ? 1 : 0).sum();
    }

    public Set<Crop> getCropsInWarehouses() {
        return cropsInWarehouses;
    }
    public void store(Crop crop) {
        if (cropsInWarehouses.contains(crop))
            throw new IllegalArgumentException( crop + " already in warehouse");
        this.cropsInWarehouses.add(crop);
    }
    public void clearCropsInWarehouses() {
        this.cropsInWarehouses.clear();
    }

    public PRPlayerBoard copy() {
        PRPlayerBoard copy = new PRPlayerBoard(ownerId, componentID);
        copy.plantations = plantations.stream().map(Plantation::copy).collect(toList());
        copy.stores = new HashMap<>(this.stores);
        copy.vp = this.vp;
        copy.colonistsInSupply = this.colonistsInSupply;
        copy.buildings = buildings.stream().map(Building::copy).collect(toList());
        copy.doubloons = this.doubloons;
        copy.cropsInWarehouses = new HashSet<>(this.cropsInWarehouses);
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PRPlayerBoard) {
            PRPlayerBoard p = (PRPlayerBoard) obj;
            return this.plantations.equals(p.plantations) && this.stores.equals(p.stores) && this.vp == p.vp &&
                    buildings.equals(p.buildings) && ownerId == p.ownerId && this.colonistsInSupply == p.colonistsInSupply &&
                    this.doubloons == p.doubloons && this.cropsInWarehouses.equals(p.cropsInWarehouses);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(plantations, stores, vp, ownerId, colonistsInSupply, buildings, doubloons, cropsInWarehouses);
    }
}
