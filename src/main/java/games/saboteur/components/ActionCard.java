package games.saboteur.components;

import java.util.Arrays;

public class ActionCard extends SaboteurCard
{
    public final ActionCardType actionType;
    public final ToolCardType[] toolTypes;

    public enum ToolCardType
    {
        MineCart,
        Lantern,
        Pickaxe
    }

    public enum ActionCardType
    {
        RockFall,
        BrokenTools,
        FixTools,
        Map
    }

    public ActionCard(ActionCardType actionType)
    {
        super(SaboteurCardType.Action);
        this.actionType = actionType;
        toolTypes = null;
    }

    public ActionCard(ActionCardType actionType, ToolCardType[] toolTypes)
    {
        super(SaboteurCardType.Action);
        this.actionType = actionType;
        this.toolTypes = toolTypes;
    }

    public ActionCard(ActionCardType actionType, ToolCardType[] toolTypes, int componentID)
    {
        super(SaboteurCardType.Action, componentID);
        this.actionType = actionType;
        this.toolTypes = toolTypes;
    }

    public ActionCard(ActionCardType actionType, ToolCardType toolTypes)
    {
        super(SaboteurCardType.Action);
        this.actionType = actionType;
        this.toolTypes = new ToolCardType[] {toolTypes};
    }

    @Override
    public String toString()
    {
        switch(actionType)
        {
            case Map:
            case RockFall:
                return actionType.toString();

            case BrokenTools:
            case FixTools:
                return actionType + Arrays.toString(toolTypes);
        }
        return null;
    }

    @Override
    public ActionCard copy()
    {
        return new ActionCard(actionType, toolTypes, componentID);
    }
}
