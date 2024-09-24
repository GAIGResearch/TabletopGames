package games.saboteur.components;

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
    public RoleCard(RoleCardType type, int componentID)
    {
        super(SaboteurCardType.Role, componentID);
        this.type = type;
    }


    @Override
    public String toString()
    {
        return type.toString();
    }

    public RoleCard copy()
    {
        return new RoleCard(type, componentID);
    }
}
