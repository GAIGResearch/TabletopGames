package games.diamant.components;

import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ActionsPlayed extends Component
{
    public Map<Integer, AbstractAction> actions;

    public ActionsPlayed() {
        super(CoreConstants.ComponentType.COUNTER);
        actions = new HashMap<>();
    }

    public ActionsPlayed(int ID) {
        super(CoreConstants.ComponentType.COUNTER, ID);
        actions = new HashMap<>();
    }

    @Override
    public Component copy() {
        ActionsPlayed ap = new ActionsPlayed(componentID);
        ap.actions =new HashMap<>();
        for (Integer key: actions.keySet())
            ap.actions.put(key, actions.get(key).copy());
        return ap;
    }

    public AbstractAction get(Integer key)
    {
        return actions.get(key);
    }

    public void put(Integer key, AbstractAction action)
    {
        actions.put(key, action);
    }

    public boolean containsKey(Integer key)
    {
        return actions.containsKey(key);
    }

    public int size()
    {
        return actions.size();
    }

    public void clear()
    {
        actions.clear();
    }

    public Set<Integer> keySet()
    {
        return actions.keySet();
    }
}
