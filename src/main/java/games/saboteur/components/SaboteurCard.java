package games.saboteur.components;

import core.components.Card;

public class SaboteurCard extends Card
{
    public final SaboteurCardType type;
    public final int nOfNuggets;
    public enum SaboteurCardType
    {
        Path,
        Role,
        GoldNugget,
        Action,
    }

    public SaboteurCard (SaboteurCardType type)
    {
        super(type.toString());
        this.type = type;
        this.nOfNuggets = 0;
    }

    public SaboteurCard(int nOfNuggets)
    {
        super(SaboteurCardType.GoldNugget.toString());
        this.type = SaboteurCardType.GoldNugget;
        this.nOfNuggets = nOfNuggets;
    }

    public SaboteurCard(SaboteurCardType type, int nOfNuggets, int componentID)
    {
        super(type.toString(), componentID);
        this.type = type;
        this.nOfNuggets = nOfNuggets;
    }

    public SaboteurCard(SaboteurCardType type, int componentID)
    {
        super(type.toString(), componentID);
        this.type = type;
        this.nOfNuggets = 0;
    }

    @Override public SaboteurCard copy() {
        return new SaboteurCard(type, this.nOfNuggets, componentID);
    }
}
