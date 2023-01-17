package games.pandemic.stats;
import core.components.Card;
import core.components.Component;
import core.components.Counter;
import core.components.Deck;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.GameListener;
import evaluation.metrics.IMetricsCollection;
import games.pandemic.PandemicGameState;
import utilities.Hash;


public class PandemicMetrics implements IMetricsCollection {

    public static class DeckSize extends AbstractMetric {

        public DeckSize() {this("infection");}
        private String deckName = null;

        public DeckSize(String arg)
        {
            addEventType(Event.GameEvent.ACTION_CHOSEN);
            deckName = arg;
        }

        public String name() {return "DeckSize (" + deckName + ")";}

        @Override
        public Object run(GameListener listener, Event e) {
            //if(key == null) throw new AssertionError("Argument for Constructor in " + getClass().getName() + " can't be null");
            Component c = ((PandemicGameState)e.state).getComponent(Hash.GetInstance().hash(deckName));
            //if(c == null) throw new AssertionError("name '" + key + "' does not correspond to any component in this game.");
            return ((Deck<Card>) c).getSize();
        }

        public Object[] getAllowedParameters() { return new String[]{"infection", "Player Deck"}; }
    }

    public static class CounterValue extends AbstractMetric {

        public CounterValue() {this ("Research Stations");}

        private String counterName = null;

        public CounterValue(String arg)
        {
            addEventType(Event.GameEvent.ACTION_CHOSEN);
            counterName = arg;
        }

        public String name() {return "CounterValue (" + counterName + ")";}

        @Override
        public Object run(GameListener listener, Event e) {
            //if(key == null) throw new AssertionError("Argument for Constructor in " + getClass().getName() + " can't be null");
            Component c = ((PandemicGameState)e.state).getComponent(Hash.GetInstance().hash(counterName));
            if(c == null) {
                ((PandemicGameState)e.state).getComponent(Hash.GetInstance().hash(counterName));
                throw new AssertionError("name '" + counterName + "' does not correspond to any component in this game.");
            }
                return ((Counter) c).getValue();
        }

        public Object[] getAllowedParameters() { return new String[]{
                "Disease yellow", "Disease red", "Disease blue", "Disease black",
                "Disease Cube yellow", "Disease Cube red", "Disease Cube blue", "Disease Cube black",
                "Outbreaks", "Infection Rate", "Research Stations"}; }
    }


}
