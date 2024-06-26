package games.coltexpress.components;

import core.CoreConstants;
import core.components.Component;
import core.components.Deck;
import core.interfaces.IComponentContainer;
import games.coltexpress.ColtExpressParameters;
import games.coltexpress.ColtExpressTypes;

import java.util.*;

import static core.CoreConstants.VisibilityMode;


public class Compartment extends Component implements IComponentContainer<Deck<Loot>> {

    public Deck<Loot> lootInside;
    public Deck<Loot> lootOnTop;
    public Set<Integer> playersInsideCompartment;
    public Set<Integer> playersOnTopOfCompartment;

    public boolean containsMarshal;
    private final int nPlayers;
    private final int compartmentID;

    private HashMap<ColtExpressTypes.LootType, ArrayList<Integer>> pickedCount;
    private HashMap<ColtExpressTypes.LootType, ArrayList<Integer>> stillAvailableIdx;

    private Compartment(int nPlayers, int compartmentID, int ID){
        super(CoreConstants.ComponentType.BOARD_NODE, ID);
        // Technically loot is face-down, and hence not Visible. But...players know which ones are Jewels and StrongBoxes
        // So all that is actually hidden is which purses are 250 versus 500...on the whole this means that much more information
        // is known than unknown.
        this.lootInside = new Deck<>("lootInside", VisibilityMode.VISIBLE_TO_ALL);
        this.lootOnTop = new Deck<>("lootOntop", VisibilityMode.VISIBLE_TO_ALL);
        this.nPlayers = nPlayers;
        this.compartmentID = compartmentID;
        playersInsideCompartment = new HashSet<>();
        playersOnTopOfCompartment = new HashSet<>();
        containsMarshal = false;
    }

    public Compartment(int nPlayers, int compartmentID, int which, ColtExpressParameters cep, Random rnd){
        super(CoreConstants.ComponentType.BOARD_NODE);
        this.lootInside = new Deck<>("lootInside", VisibilityMode.HIDDEN_TO_ALL);
        this.lootOnTop = new Deck<>("lootOntop", VisibilityMode.HIDDEN_TO_ALL);
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
                lootInside.add(new Loot(e.getKey(), getRandomLootValue(cep, e.getKey(), rnd)));
            }
        }
    }

    private int getRandomLootValue(ColtExpressParameters cep, ColtExpressTypes.LootType t, Random r) {
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

    public static Compartment createLocomotive(int nPlayers, ColtExpressParameters cep, Random rnd){
        // Locomotive is always last in the list of compartment configurations
        Compartment locomotive = new Compartment(nPlayers, nPlayers,cep.trainCompartmentConfigurations.size()-1, cep, rnd);
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

    public Deck<Loot> getLootInside() {
        return lootInside;
    }

    public Deck<Loot> getLootOnTop() {
        return lootOnTop;
    }

    public int getCompartmentID() {
        return compartmentID;
    }

    @Override
    public Component copy() {
        Compartment newCompartment = new Compartment(this.nPlayers, compartmentID, componentID);
        newCompartment.lootInside = lootInside.copy();
        newCompartment.lootOnTop = lootOnTop.copy();
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

    @Override
    public List<Deck<Loot>> getComponents() {
        return Arrays.asList(lootInside, lootOnTop);
    }

    @Override
    public VisibilityMode getVisibilityMode() {
        // The Loot is not visible..but the decks are.
        return VisibilityMode.VISIBLE_TO_ALL;
    }
}
