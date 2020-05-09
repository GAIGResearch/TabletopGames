package games.coltexpress.components;

import java.util.LinkedList;

public class Train {

    private final LinkedList<Compartment> compartments = new LinkedList<>();

    public Train(int nPlayers){
        for (int i = 0; i < nPlayers; i++)
            compartments.add(new Compartment(nPlayers, i));

        compartments.add(Compartment.createLocomotive(nPlayers));
    }

    public int getSize(){
        return compartments.size();
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Train:\n");
        for (Compartment compartment : compartments)
        {
            sb.append(compartment.toString());
            sb.append("\n");
        }
        sb.deleteCharAt(sb.length()-1);

        return sb.toString();
    }

    public Compartment getCompartment(int compartmentIndex){
        if (compartmentIndex < compartments.size() && compartmentIndex >= 0){
            return compartments.get(compartmentIndex);
        }
        throw new IllegalArgumentException("compartmentIndex out of bounds");
    }
}
