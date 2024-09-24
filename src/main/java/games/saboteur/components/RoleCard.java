package games.saboteur.components;

import java.util.Objects;

public class RoleCard extends SaboteurCard
{
    public final RoleCardType type;

    public enum RoleCardType
    {
        GoldMiner,
        Saboteur,
    }

    public RoleCard(RoleCardType type)
    {
        super(SaboteurCardType.Role);
        this.type = type;
    }


    @Override
    public String toString()
    {
        return "Role: " + type.toString();
    }

    public RoleCard copy()
    {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoleCard roleCard)) return false;
        if (!super.equals(o)) return false;
        return type == roleCard.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), type);
    }
}
