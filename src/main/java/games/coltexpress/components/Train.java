package games.coltexpress.components;

import java.util.LinkedList;

public class Train {

    private LinkedList<Compartment> compartments;

    public Train(int nPlayers){
        compartments = new LinkedList<>();
        for (int i = 0; i < nPlayers; i++)
            compartments.add(new Compartment());
    }
}
