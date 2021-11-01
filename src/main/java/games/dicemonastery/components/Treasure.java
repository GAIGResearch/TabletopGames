package games.dicemonastery.components;

import core.components.Component;
import core.properties.*;
import utilities.Hash;
import utilities.Utils;

import java.util.Objects;

public class Treasure extends Component {


    final static int costHash = Hash.GetInstance().hash("cost");
    final static int vpHash = Hash.GetInstance().hash("vp");
    final static int limitHash = Hash.GetInstance().hash("limit");
    final static int nameHash = Hash.GetInstance().hash("name");

    public final int cost;
    public final int vp;
    public final int limit;

    public static Treasure create(Component c) {
        String name = ((PropertyString) c.getProperty(nameHash)).value;
        int cost = ((PropertyInt) c.getProperty(costHash)).value;
        int vp = ((PropertyInt) c.getProperty(vpHash)).value;
        int limit = ((PropertyInt) c.getProperty(limitHash)).value;
        return new Treasure(name, vp, cost, limit);
    }

    private Treasure(String name, int vp, int cost, int limit) {
        super(Utils.ComponentType.CARD, name);
        this.cost = cost;
        this.vp = vp;
        this.limit = limit;
    }

    @Override
    public Component copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Treasure) {
            Treasure other = (Treasure) o;
            return other.cost == cost && other.limit == limit && other.vp == vp && other.componentName.equals(componentName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentName, cost, vp, limit);
    }
}
