package games.saboteur.components;

import java.util.Arrays;
import java.util.Objects;

public class ActionCard extends SaboteurCard
{
    public final ActionCardType actionType;
    public final ToolCardType[] toolTypes;

    public enum ToolCardType
    {
        MineCart,
        Lantern,
        Pickaxe;
        public String shortString() {
            switch(this) {
                case MineCart -> { return "MC";
                }
                case Lantern -> { return "Lant";
                }
                case Pickaxe -> { return "PAxe";
                }
            }
            return "";
        }
    }

    public enum ActionCardType
    {
        RockFall,
        BrokenTools,
        FixTools,
        Map;
        public String shortString() {
            switch(this) {
                case RockFall -> { return "Rocks";
                }
                case Map -> { return "Map";
                }
                case BrokenTools -> { return "Brk";
                }
                case FixTools -> { return "Fix";
                }
            }
            return "";
        }
    }

    public ActionCard(ActionCardType actionType, ToolCardType[] toolTypes)
    {
        super(SaboteurCardType.Action);
        this.actionType = actionType;
        this.toolTypes = toolTypes;
    }

    private ActionCard(ActionCardType actionType, ToolCardType[] toolTypes, int componentID)
    {
        super(SaboteurCardType.Action, componentID);
        this.actionType = actionType;
        this.toolTypes = toolTypes;
    }

    @Override
    public String toString()
    {
        return switch (actionType) {
            case Map, RockFall -> actionType.toString();
            case BrokenTools, FixTools -> actionType + Arrays.toString(toolTypes);
        };
    }

    @Override
    public ActionCard copy()
    {
        return new ActionCard(actionType, toolTypes.clone(), componentID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActionCard that)) return false;
        if (!super.equals(o)) return false;
        return actionType == that.actionType && Arrays.equals(toolTypes, that.toolTypes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), actionType);
        result = 31 * result + Arrays.hashCode(toolTypes);
        return result;
    }
}
