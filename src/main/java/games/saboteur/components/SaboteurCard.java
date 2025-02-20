package games.saboteur.components;

import core.components.BoardNode;

import java.util.Objects;

public class SaboteurCard extends BoardNode
{

    public enum SaboteurCardType
    {
        Path,
        Role,
        GoldNugget,
        Action,
    }

    public final SaboteurCardType type;
    public final int nOfNuggets;

    public SaboteurCard (SaboteurCardType type)
    {
        super(type.name());
        this.type = type;
        this.nOfNuggets = 0;
    }

    protected SaboteurCard (SaboteurCardType type, int componentID)
    {
        super(-1, type.name(), componentID);
        this.type = type;
        this.nOfNuggets = 0;
    }

    public SaboteurCard (int nOfNuggets)
    {
        super(SaboteurCardType.GoldNugget.name());
        this.type = SaboteurCardType.GoldNugget;
        this.nOfNuggets = nOfNuggets;
    }

    @Override public SaboteurCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SaboteurCard that)) return false;
        if (!super.equals(o)) return false;
        return nOfNuggets == that.nOfNuggets && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), type, nOfNuggets);
    }
}
