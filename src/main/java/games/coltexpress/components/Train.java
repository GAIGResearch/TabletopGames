package games.coltexpress.components;

import core.components.Component;
import utilities.Utils;

import java.util.LinkedList;

public class Train extends Component {

    private LinkedList<Compartment> compartments;

    public Train(int nPlayers) {
        super(Utils.ComponentType.BOARD);
        compartments = new LinkedList<>();
        for (int i = 0; i < nPlayers; i++)
            compartments.add(new Compartment());
    }

    @Override
    public Component copy() {
        return null;
    }

    public LinkedList<Compartment> getCompartments() {
        return compartments;
    }
}
