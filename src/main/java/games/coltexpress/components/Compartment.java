package games.coltexpress.components;

import core.components.Component;
import core.components.PartialObservableDeck;
import games.coltexpress.ColtExpressParameters;
import utilities.Utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class Compartment extends Component {

    public PartialObservableDeck<Loot> lootInside;
    public PartialObservableDeck<Loot> lootOnTop;
    public Set<Integer> playersInsideCompartment = new HashSet<>();
    public Set<Integer> playersOnTopOfCompartment = new HashSet<>();

    public boolean containsMarshal = false;
    private final int nPlayers;
    private final int compartmentID;

    private Compartment(int nPlayers, int compartmentID){
        super(Utils.ComponentType.BOARD_NODE);
        this.lootInside = new PartialObservableDeck<>("lootInside", nPlayers);
        this.lootOnTop = new PartialObservableDeck<>("lootOntop", nPlayers);
        this.nPlayers = nPlayers;
        this.compartmentID = compartmentID;
    }

    public Compartment(int nPlayers, int compartmentID, int which, ColtExpressParameters cep){
        this(nPlayers, compartmentID);

        HashMap<ColtExpressParameters.LootType, Integer> configuration = cep.trainCompartmentConfigurations.get(which);
        for (Map.Entry<ColtExpressParameters.LootType, Integer> e : configuration.entrySet()) {
            for (int i = 0; i < e.getValue(); i++) {
                lootInside.add(new Loot(e.getKey(), e.getKey().getRandomValue(cep.getGameSeed())));
            }
        }
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
        Compartment newCompartment = new Compartment(this.nPlayers, compartmentID);

        for (Loot loot : this.lootInside.getComponents())
            newCompartment.lootInside.add((Loot) loot.copy());
        for (Loot loot : this.lootOnTop.getComponents())
            newCompartment.lootOnTop.add((Loot) loot.copy());
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
