package core.actions;

import core.AbstractGameState;
import java.util.LinkedHashMap;
import java.util.Map;

public class SimultaneousAction extends AbstractAction {
    private final Map<Integer, AbstractAction> playerActions;

    public SimultaneousAction(Map<Integer, AbstractAction> playerActions) {
        this.playerActions = new LinkedHashMap<>(playerActions);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        for (AbstractAction action : playerActions.values()) {
            action.execute(gs);
        }
        return true;
    }

    public Map<Integer, AbstractAction> getPlayerActions() {
        return playerActions;
    }

    @Override
    public AbstractAction copy() {
        Map<Integer, AbstractAction> copiedActions = new LinkedHashMap<>();
        for (Map.Entry<Integer, AbstractAction> entry : playerActions.entrySet()) {
            copiedActions.put(entry.getKey(), entry.getValue().copy());
        }
        return new SimultaneousAction(copiedActions);
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "";
    }


    // AI GENERATED toString debugging helper func......
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SimultaneousAction{");
        for (Map.Entry<Integer, AbstractAction> entry : playerActions.entrySet()) {
            sb.append("player").append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
        }
        sb.append("}");
        return sb.toString();
    }
}



