package games.coltexpress.components;

import core.components.PartialObservableDeck;

import java.util.HashSet;
import java.util.Set;

public class Compartment {
    public Set<Integer> playersInsideCompartment = new HashSet<>();
    public Set<Integer> playersOnTopOfCompartment = new HashSet<>();

    public boolean containsMarshal = false;
    public PartialObservableDeck<Loot> loot;
    public final int id;

    public Compartment(int nPlayers, int id){
        this(new PartialObservableDeck<>("loot", nPlayers), id);
    }

    public Compartment(PartialObservableDeck<Loot> loot, int id){
        this.loot = loot;
        this.id = id;
        if (loot.getSize() == 0){
            loot.add(new Loot(Loot.LootType.Purse, 250));
            loot.add(new Loot(Loot.LootType.Purse, 250));
            loot.add(new Loot(Loot.LootType.Purse, 500));
        }
        //todo add varying compartments
    }

    public boolean containsPlayer(int playerID) {
        if (playersInsideCompartment.contains(playerID))
            return true;
        return playersOnTopOfCompartment.contains(playerID);
    }

    public static Compartment createLocomotive(int nPlayers){
        PartialObservableDeck<Loot> loot = new PartialObservableDeck<>("loot", nPlayers);
        loot.add(new Loot(Loot.LootType.Strongbox, 1000));
        Compartment locomotive = new Compartment(loot, nPlayers);
        locomotive.containsMarshal = true;
        return locomotive;
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

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Compartment: Inside=");
        sb.append(playersInsideCompartment.toString());
        sb.append("; Outside=");
        sb.append(playersOnTopOfCompartment.toString());
        sb.append("; Marshal=");
        sb.append(containsMarshal);
        sb.append("; Loot=");
        sb.append(loot.toString());

        return sb.toString();
    }
}
