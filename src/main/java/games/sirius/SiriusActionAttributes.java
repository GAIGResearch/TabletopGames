package games.sirius;

import core.actions.AbstractAction;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.*;
import games.sirius.actions.FavourForCartel;
import games.sirius.actions.SellCards;

import java.util.Collections;
import java.util.Set;

public class SiriusActionAttributes implements IMetricsCollection {

    public static class Location extends AbstractMetric {

        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }
        @Override
        public Object run(MetricsGameListener listener, Event e) {
            SiriusGameState s = (SiriusGameState) e.state;
            return s.getMoon(s.getLocationIndex(s.getCurrentPlayer())).getComponentName();
        }
    }

    public static class Thing extends AbstractMetric {

        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }
        @Override
        public Object run(MetricsGameListener listener, Event e) {
            SiriusGameState s = (SiriusGameState) e.state;
            AbstractAction a = e.action;
            if (a == null) return "";
            if (a instanceof SellCards) return ((SellCards) a).salesType;
            if (a instanceof FavourForCartel) return s.getMoon(((FavourForCartel) a).cartelLocation).getComponentName();
            return "";
        }
    }

    public static class Value extends AbstractMetric {

        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }
        @Override
        public Object run(MetricsGameListener listener, Event e) {
            SiriusGameState s = (SiriusGameState) e.state;
            AbstractAction a = e.action;
            if (a == null) return "";
            if (a instanceof SellCards) return ((SellCards) a).salesType;
            if (a instanceof FavourForCartel) return s.getMoon(((FavourForCartel) a).cartelLocation).getComponentName();
            return "";
        }
    }

}