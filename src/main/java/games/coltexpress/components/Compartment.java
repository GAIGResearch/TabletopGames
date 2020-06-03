package games.coltexpress.components;

import core.components.Component;
import core.components.PartialObservableDeck;
import games.coltexpress.ColtExpressParameters;
import games.coltexpress.ColtExpressTypes;
import utilities.Utils;

import java.util.*;


public class Compartment extends Component {

    public PartialObservableDeck<Loot> lootInside;
    public PartialObservableDeck<Loot> lootOnTop;
    public Set<Integer> playersInsideCompartment;
    public Set<Integer> playersOnTopOfCompartment;

    public boolean containsMarshal;
    private final int nPlayers;
    private final int compartmentID;

    private HashMap<ColtExpressTypes.LootType, ArrayList<Integer>> pickedCount;
    private HashMap<ColtExpressTypes.LootType, ArrayList<Integer>> stillAvailableIdx;

    private Compartment(int nPlayers, int compartmentID, int ID){
        super(Utils.ComponentType.BOARD_NODE, ID);
        this.lootInside = new PartialObservableDeck<>("lootInside", nPlayers);
        this.lootOnTop = new PartialObservableDeck<>("lootOntop", nPlayers);
        this.nPlayers = nPlayers;
        this.compartmentID = compartmentID;
        playersInsideCompartment = new HashSet<>();
        playersOnTopOfCompartment = new HashSet<>();
        containsMarshal = false;
    }

    public Compartment(int nPlayers, int compartmentID, int which, ColtExpressParameters cep){
        super(Utils.ComponentType.BOARD_NODE);
        this.lootInside = new PartialObservableDeck<>("lootInside", nPlayers);
        this.lootOnTop = new PartialObservableDeck<>("lootOntop", nPlayers);
        this.nPlayers = nPlayers;
        this.compartmentID = compartmentID;
        playersInsideCompartment = new HashSet<>();
        playersOnTopOfCompartment = new HashSet<>();
        containsMarshal = false;

        // Loot distribution setup
        pickedCount = new HashMap<>();
        stillAvailableIdx = new HashMap<>();
        for (ColtExpressTypes.LootType t: ColtExpressTypes.LootType.values()) {
            stillAvailableIdx.put(t, new ArrayList<>());
            pickedCount.put(t, new ArrayList<>());
            for (int i = 0; i < cep.loot.get(t).size(); i++) {
                stillAvailableIdx.get(t).add(i);
                pickedCount.get(t).add(0);
            }
        }

        // Set loot
        HashMap<ColtExpressTypes.LootType, Integer> configuration = cep.trainCompartmentConfigurations.get(which);
        for (Map.Entry<ColtExpressTypes.LootType, Integer> e : configuration.entrySet()) {
            for (int i = 0; i < e.getValue(); i++) {
                lootInside.add(new Loot(e.getKey(), getRandomLootValue(cep, e.getKey(), cep.getGameSeed())));
            }
        }
    }

    private int getRandomLootValue(ColtExpressParameters cep, ColtExpressTypes.LootType t, long seed) {
        Random r = new Random(seed);
        if (stillAvailableIdx.get(t).size() > 0) {
            int idx = stillAvailableIdx.get(t).get(r.nextInt(stillAvailableIdx.get(t).size()));
            if (stillAvailableIdx.get(t).contains(idx)) {
                pickedCount.get(t).set(idx, pickedCount.get(t).get(idx) + 1);
                if (pickedCount.get(t).get(idx) >= cep.loot.get(t).get(idx).b) {
                    stillAvailableIdx.get(t).remove(Integer.valueOf(idx));
                }
                return cep.loot.get(t).get(idx).a;
            }
        }
        return -1;
    }

    public static Compartment createLocomotive(int nPlayers, ColtExpressParameters cep){
        // Locomotive is always last in the list of compartment configurations
        Compartment locomotive = new Compartment(nPlayers, nPlayers,cep.trainCompartmentConfigurations.size()-1, cep);
        locomotive.containsMarshal = true;
        return locomotive;
    }


    public boolean containsPlayer(int playerID) {
        if (playersInsideCompartment.contains(playerID))
            return true;
        return playersOnTopOfCompartment.contains(playerID);
    }

    public void addPlayerInside(int playerID){
        playersInsideCompartment.add(playerID);
    }

    public void removePlayerInside(int playerID){
        playersInsideCompartment.remove(playerID);
    }

    public void addPlayerOnTop(int playerID){
        playersOnTopOfCompartment.add(playerID);
    }

    public void removePlayerOnTop(int playerID){
        playersOnTopOfCompartment.remove(playerID);
    }

    public PartialObservableDeck<Loot> getLootInside() {
        return lootInside;
    }

    public PartialObservableDeck<Loot> getLootOnTop() {
        return lootOnTop;
    }

    public int getCompartmentID() {
        return compartmentID;
    }

    @Override
    public Component copy() {
        Compartment newCompartment = new Compartment(this.nPlayers, compartmentID, componentID);
        for (Loot loot : this.lootInside.getComponents())
            newCompartment.lootInside.add((Loot) loot.copy());
        for (Loot loot : this.lootOnTop.getComponents())
            newCompartment.lootOnTop.add((Loot) loot.copy());
        newCompartment.containsMarshal = containsMarshal;
        newCompartment.playersOnTopOfCompartment.addAll(playersOnTopOfCompartment);
        newCompartment.playersInsideCompartment.addAll(playersInsideCompartment);
        newCompartment.pickedCount = new HashMap<>();  // Copies never need to know this information
        newCompartment.stillAvailableIdx = new HashMap<>();  // Copies never need to know this information
        return newCompartment;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Compartment: Inside=");
        sb.append(playersInsideCompartment.toString());
        sb.append("; Outside=");
        sb.append(playersOnTopOfCompartment.toString());
        sb.append("; Marshal=");
        sb.append(containsMarshal);
        sb.append("; LootInside=");
        sb.append(lootInside.toString());
        sb.append("; LootOntop=");
        sb.append(lootOnTop.toString());

        return sb.toString();
    }

}
