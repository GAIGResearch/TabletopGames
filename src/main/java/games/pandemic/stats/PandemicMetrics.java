package games.pandemic.stats;
import core.components.Card;
import core.components.Component;
import core.components.Counter;
import core.components.Deck;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.GameListener;
import games.pandemic.PandemicGameState;
import utilities.Hash;


public class PandemicMetrics {

    public static class DeckSize extends AbstractMetric {

        private String key = null;

        public DeckSize(String arg)
        {
            addEventType(Event.GameEvent.ACTION_CHOSEN);
            key = arg;
        }

        public String name() {return "DeckSize (" + key + ")";}

        @Override
        public Object run(GameListener listener, Event e) {
            //if(key == null) throw new AssertionError("Argument for Constructor in " + getClass().getName() + " can't be null");
            Component c = ((PandemicGameState)e.state).getComponent(Hash.GetInstance().hash(key));
            //if(c == null) throw new AssertionError("name '" + key + "' does not correspond to any component in this game.");
            return ((Deck<Card>) c).getSize();
        }
    }

    public static class CounterValue extends AbstractMetric {

        private String key = null;

        public CounterValue(String arg)
        {
            addEventType(Event.GameEvent.ACTION_CHOSEN);
            key = arg;
        }

        public String name() {return "CounterValue (" + key + ")";}

        @Override
        public Object run(GameListener listener, Event e) {
            //if(key == null) throw new AssertionError("Argument for Constructor in " + getClass().getName() + " can't be null");
            Component c = ((PandemicGameState)e.state).getComponent(Hash.GetInstance().hash(key));
            //if(c == null) throw new AssertionError("name '" + key + "' does not correspond to any component in this game.");
            return ((Counter) c).getValue();
        }
    }


}
