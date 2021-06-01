package games.battlelore.components;

import core.components.Component;
import core.interfaces.IComponentContainer;
import games.coltexpress.components.Loot;
import utilities.Utils;

import java.util.function.Function;

public class Unit extends Component
{
    //Variables
    private String id;
    private String name;
    public int move;
    public int strength;
    public int health;
    //Add special power


    public Unit(Utils.ComponentType type, String id, String name, int move, int strength, int health)
    {
        super(type, name);
        this.name = name;
        this.id = id;
        this.move = move;
        this.strength = strength;
        this.health = health;
    }


    @Override
    public Component copy()
    {
        return new Unit(type, id, name, move, strength, health);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof Loot))
        {
            return false;
        }

        if (!super.equals(o))
        {
            return false;
        }
        Unit unit = (Unit) o;
        return id == unit.id &&
                type == unit.type &&
                name == unit.name &&
                move == unit.move &&
                strength == unit.strength &&
                health == unit.health;
    }
}
