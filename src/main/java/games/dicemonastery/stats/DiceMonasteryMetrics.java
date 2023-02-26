package games.dicemonastery.stats;
import evaluation.listeners.GameListener;
import evaluation.metrics.*;
import games.dicemonastery.DiceMonasteryConstants;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.actions.*;
import games.dicemonastery.components.Monk;
import utilities.Group;

import java.util.*;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.STOREROOM;

@SuppressWarnings("unused")
public class DiceMonasteryMetrics implements IMetricsCollection {

    public static class Season extends AbstractMetric {
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }
        @Override
        public Object run(GameListener listener, Event e) {
            DiceMonasteryGameState s = (DiceMonasteryGameState) e.state;
            return s.getSeason();
        }
    }

    public static class Year extends AbstractMetric {
        @Override
        public Object run(GameListener listener, Event e) {
            DiceMonasteryGameState s = (DiceMonasteryGameState) e.state;
            return s.getYear();
        }
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }
    }

    public static class Piety extends AbstractMetric {
        @Override
        public Object run(GameListener listener, Event e) {
            if (e.action == null) return 0;
            if (e.action instanceof ChooseMonk) return ((ChooseMonk) e.action).piety;
            if (e.action instanceof PromoteMonk) return ((PromoteMonk) e.action).pietyLevelToPromote;
            return 0;
        }
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }
    }

    public static class Location extends AbstractMetric {
        @Override
        public Object run(GameListener listener, Event e) {
            if (e.action instanceof PlaceMonk) return ((PlaceMonk) e.action).destination.name();
            if (e.action instanceof ChooseMonk) return ((ChooseMonk) e.action).destination.name();
            if (e.action instanceof PromoteMonk) return ((PromoteMonk) e.action).location.name();
            DiceMonasteryGameState s = (DiceMonasteryGameState) e.state;
            return s.getCurrentArea().name();
        }
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }
    }

    public static class Thing extends AbstractMetric {
        @Override
        public Object run(GameListener listener, Event e) {
            if (e.action == null) return "";
            if (e.action instanceof Buy) return ((Buy) e.action).resource.name();
            if (e.action instanceof Sell) return ((Sell) e.action).resource.name();
            if (e.action instanceof BuyTreasure) return ((BuyTreasure) e.action).treasure.getComponentName();
            if (e.action instanceof WriteText) return ((WriteText) e.action).textType.getComponentName();
            if (e.action instanceof TakeToken) return ((TakeToken) e.action).token.name();
            if (e.action instanceof GoOnPilgrimage) return ((GoOnPilgrimage) e.action).destination.destination;
            return "";
        }
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }
    }

    public static class Value extends AbstractMetric {
        @Override
        public Object run(GameListener listener, Event e) {
            if (e.action == null) return 0;
            if (e.action instanceof Buy) return ((Buy) e.action).cost;
            if (e.action instanceof Sell) return ((Sell) e.action).price;
            if (e.action instanceof SummerBid)
                return ((SummerBid) e.action).beer + 2 * ((SummerBid) e.action).mead;
            if (e.action instanceof Pray) return ((Pray) e.action).prayerCount;
            if (e.action instanceof TakeToken)
                return 2 - ((DiceMonasteryGameState)e.state).availableBonusTokens(((TakeToken) e.action).fromArea).size();
            if (e.action instanceof UseMonk) return ((UseMonk) e.action).getActionPoints();
            return 0;
        }
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }
    }

    public static class ActionPointsLeft extends AbstractMetric {
        @Override
        public Object run(GameListener listener, Event e) {
            DiceMonasteryGameState s = (DiceMonasteryGameState) e.state;
            return  s.getActionPointsLeft();
        }
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }
    }


    public static class IsSeason extends AbstractParameterizedMetric {
        public IsSeason(){super();}
        public IsSeason(Object arg){super(arg);}
        @Override
        public Object run(GameListener listener, Event e) {
            DiceMonasteryConstants.Season season = (DiceMonasteryConstants.Season) getParameterValue("season");
            DiceMonasteryGameState s = (DiceMonasteryGameState) e.state;
            return s.getSeason() == season;
        }
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }
        public List<Group<String, List<?>, ?>> getAllowedParameters() {
            return Collections.singletonList(new Group<>("season", Arrays.asList(DiceMonasteryConstants.Season.values()), DiceMonasteryConstants.Season.SPRING));
        }
    }


    public static class Turn extends AbstractMetric {
        @Override
        public Object run(GameListener listener, Event e) {
            DiceMonasteryGameState s = (DiceMonasteryGameState) e.state;
            return (s.getYear() - 1) * 4 + s.getSeason().ordinal() + 1;
        }
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }
    }


    public static class MonksIn extends AbstractParameterizedMetric {
        public MonksIn(){super();}
        public MonksIn(Object arg){super(arg);}
        @Override
        public Object run(GameListener listener, Event e) {
            DiceMonasteryConstants.ActionArea where = (DiceMonasteryConstants.ActionArea) getParameterValue("where");
            return ((DiceMonasteryGameState)e.state).monksIn(where, e.playerID).size();
        }
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }
        public List<Group<String, List<?>, ?>> getAllowedParameters() {
            return Collections.singletonList(new Group<>("where", Arrays.asList(DiceMonasteryConstants.ActionArea.values()), STOREROOM));
        }
    }

    public static class APIn extends AbstractParameterizedMetric {
        public APIn(){super();}
        public APIn(Object arg){super(arg);}
        @Override
        public Object run(GameListener listener, Event e) {
            DiceMonasteryConstants.ActionArea where = (DiceMonasteryConstants.ActionArea) getParameterValue("where");
            return ((DiceMonasteryGameState)e.state).monksIn(where, e.playerID).stream().mapToDouble(Monk::getPiety).sum();
        }
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }
        public List<Group<String, List<?>, ?>> getAllowedParameters() {
            return Collections.singletonList(new Group<>("where", Arrays.asList(DiceMonasteryConstants.ActionArea.values()), STOREROOM));
        }
    }

    public static class MonkPiety extends AbstractParameterizedMetric {
        public MonkPiety(){super();}
        public MonkPiety(Object arg){super(arg);}
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }
        @Override
        public Object run(GameListener listener, Event e) {
            int piety = (int) getParameterValue("piety");
            return ((DiceMonasteryGameState)e.state).monksIn(null, e.playerID).stream().filter(m -> m.getPiety() == piety).count();
        }
        public List<Group<String, List<?>, ?>> getAllowedParameters() {
            return Collections.singletonList(new Group<>("piety", Arrays.asList(1, 2, 3, 4, 5), 4)); //TODO: James check these are sensible numbers.
        }
    }


    public static class ResourceInArea extends AbstractParameterizedMetric {
        public ResourceInArea(){super();}
        public ResourceInArea(Object... arg){super(arg);}
        @Override
        public Object run(GameListener listener, Event e) {
            DiceMonasteryConstants.Resource resource = (DiceMonasteryConstants.Resource) getParameterValue("resource");
            DiceMonasteryConstants.ActionArea area = (DiceMonasteryConstants.ActionArea) getParameterValue("area");
            return ((DiceMonasteryGameState)e.state).getResource(e.playerID, resource, area);
        }
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }
        public List<Group<String, List<?>, ?>> getAllowedParameters() {
            return new ArrayList<>(Arrays.asList(
                    new Group<>("resource", Arrays.asList(DiceMonasteryConstants.Resource.values()), DiceMonasteryConstants.Resource.SHILLINGS),
                    new Group<>("area", Arrays.asList(DiceMonasteryConstants.ActionArea.values()), STOREROOM)
                    ));
        }
    }

    public static class WritingSets extends AbstractMetric {
        @Override
        public Object run(GameListener listener, Event e) {
            return Math.min(((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.VELLUM, STOREROOM),
                    ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.CANDLE, STOREROOM));
        }
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }
    }

    public static class Pigments extends AbstractMetric {
        @Override
        public Object run(GameListener listener, Event e) {
            return ((DiceMonasteryGameState)e.state).getStores(e.playerID, r -> r.isPigment).size();
        }
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }
    }

    public static class Inks extends AbstractMetric {
        @Override
        public Object run(GameListener listener, Event e) {
            return ((DiceMonasteryGameState)e.state).getStores(e.playerID, r -> r.isInk).size();
        }
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }
    }

    public static class MonksCount extends AbstractMetric {
        @Override
        public Object run(GameListener listener, Event e) {
            return ((DiceMonasteryGameState)e.state).monksIn(null, e.playerID).size();
        }
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }
    }


    public static class PietySum extends AbstractMetric {
        @Override
        public Object run(GameListener listener, Event e) {
            return ((DiceMonasteryGameState)e.state).monksIn(null, e.playerID).stream().mapToInt(Monk::getPiety).sum();
        }
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }
    }

    public static class VictoryPoints extends AbstractMetric {
        @Override
        public Object run(GameListener listener, Event e) {
            return ((DiceMonasteryGameState)e.state).getVictoryPoints(e.playerID);
        }
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }
    }

    public static class TreasureSum extends AbstractMetric {
        @Override
        public Object run(GameListener listener, Event e) {
            return ((DiceMonasteryGameState)e.state).getTreasures(e.playerID).stream().mapToInt(t -> t.vp).sum();
        }
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }
    }

}
