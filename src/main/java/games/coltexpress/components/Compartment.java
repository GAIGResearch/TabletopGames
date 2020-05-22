package games.coltexpress.components;

import core.components.Component;

import java.util.ArrayList;
import java.util.List;

public class Compartment extends Component {
    public Compartment() {
        super(null);
    }

    @Override
    public Component copy() {
        return null;
    }

    public List<Loot> getLoot() {
        return new ArrayList<>();
    }
}
