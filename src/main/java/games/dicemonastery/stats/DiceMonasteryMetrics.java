package games.dicemonastery.stats;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.GameListener;
import evaluation.metrics.IMetricsCollection;
import games.dicemonastery.DiceMonasteryConstants;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.DiceMonasteryTurnOrder;
import games.dicemonastery.actions.*;
import games.dicemonastery.components.Monk;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.STOREROOM;
public class DiceMonasteryMetrics implements IMetricsCollection {

    public static class Season extends AbstractMetric {

        public Season() {addEventType(Event.GameEvent.ACTION_CHOSEN);}

        @Override
        public Object run(GameListener listener, Event e) {
            return ((DiceMonasteryTurnOrder) e.state.getTurnOrder()).getSeason();
        }
    }

    public static class Year extends AbstractMetric {

        public Year() {addEventType(Event.GameEvent.ACTION_CHOSEN);}

        @Override
        public Object run(GameListener listener, Event e) {
            return ((DiceMonasteryTurnOrder) e.state.getTurnOrder()).getYear();
        }
    }

    public static class Piety extends AbstractMetric {

        public Piety() {addEventType(Event.GameEvent.ACTION_CHOSEN);}

        @Override
        public Object run(GameListener listener, Event e) {
            if (e.action == null) return 0;
            if (e.action instanceof ChooseMonk) return ((ChooseMonk) e.action).piety;
            if (e.action instanceof PromoteMonk) return ((PromoteMonk) e.action).pietyLevelToPromote;
            return 0;
        }
    }

    public static class Location extends AbstractMetric {

        public Location() {addEventType(Event.GameEvent.ACTION_CHOSEN);}

        @Override
        public Object run(GameListener listener, Event e) {
            if (e.action instanceof PlaceMonk) return ((PlaceMonk) e.action).destination.name();
            if (e.action instanceof ChooseMonk) return ((ChooseMonk) e.action).destination.name();
            if (e.action instanceof PromoteMonk) return ((PromoteMonk) e.action).location.name();
            return ((DiceMonasteryTurnOrder) e.state.getTurnOrder()).getCurrentArea().name();
        }
    }

    public static class Thing extends AbstractMetric {

        public Thing() {addEventType(Event.GameEvent.ACTION_CHOSEN);}

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
    }

    public static class Value extends AbstractMetric {

        public Value() {addEventType(Event.GameEvent.ACTION_CHOSEN);}

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
    }

    public static class ActionPointsLeft extends AbstractMetric {

        public ActionPointsLeft() {addEventType(Event.GameEvent.ACTION_CHOSEN);}

        @Override
        public Object run(GameListener listener, Event e) {
            return  ((DiceMonasteryTurnOrder) e.state.getTurnOrder()).getActionPointsLeft();
        }
    }


    public static class IsSeason extends AbstractMetric {

        public IsSeason(){this(DiceMonasteryConstants.Season.SPRING.name());}
        DiceMonasteryConstants.Season season;
        public IsSeason(String seasonName) {
            addEventType(Event.GameEvent.ROUND_OVER);
            season = DiceMonasteryConstants.Season.valueOf(seasonName);
        }
        public String name() {return getClass().getSimpleName() + " (" + season + ")";}

        @Override
        public Object run(GameListener listener, Event e) {
            return ((DiceMonasteryTurnOrder) e.state.getTurnOrder()).getSeason() == season;
        }

        public Object[] getAllowedParameters() { return DiceMonasteryConstants.Season.values(); }
    }


    public static class Turn extends AbstractMetric {

        public Turn() {addEventType(Event.GameEvent.ROUND_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            DiceMonasteryTurnOrder dmto = (DiceMonasteryTurnOrder) e.state.getTurnOrder();
            return (dmto.getYear() - 1) * 4 + dmto.getSeason().ordinal() + 1;
        }
    }


    public static class MonksIn extends AbstractMetric {

        public MonksIn(){this(STOREROOM.name());}
        private DiceMonasteryConstants.ActionArea where;
        public MonksIn(String where) {
            addEventType(Event.GameEvent.ROUND_OVER);
            this.where = DiceMonasteryConstants.ActionArea.valueOf(where);
        }
        public String name() {return getClass().getSimpleName() + " (" + where + ")";}

        @Override
        public Object run(GameListener listener, Event e) {
            return ((DiceMonasteryGameState)e.state).monksIn(where, e.playerID).size();
        }

        public Object[] getAllowedParameters() { return DiceMonasteryConstants.ActionArea.values(); }
    }


    public static class APIn extends AbstractMetric {

        public APIn() {this (STOREROOM.name());}
        private DiceMonasteryConstants.ActionArea where;
        public APIn(String where) {
            addEventType(Event.GameEvent.ROUND_OVER);
            this.where = DiceMonasteryConstants.ActionArea.valueOf(where);
        }
        public String name() {return getClass().getSimpleName() + " (" + where + ")";}

        @Override
        public Object run(GameListener listener, Event e) {
            return ((DiceMonasteryGameState)e.state).monksIn(where, e.playerID).stream().mapToDouble(Monk::getPiety).sum();
        }

        public Object[] getAllowedParameters() { return DiceMonasteryConstants.ActionArea.values(); }
    }


    public static class MonkPiety extends AbstractMetric {

        public MonkPiety() {this ("4");}
        private int piety;

        public MonkPiety(String piety) {
            addEventType(Event.GameEvent.ROUND_OVER);
            this.piety = Integer.parseInt(piety);
        }
        public String name() {return getClass().getSimpleName() + " (" + piety + ")";}

        @Override
        public Object run(GameListener listener, Event e) {
            return ((DiceMonasteryGameState)e.state).monksIn(null, e.playerID).stream().filter(m -> m.getPiety() == piety).count();
        }

        public Object[] getAllowedParameters() { return new Integer[]{1, 2, 3, 4, 5}; } //TODO: James check these are sensible numbers.
    }


    public static class ResourceInArea extends AbstractMetric {

        private DiceMonasteryConstants.Resource resource;
        private DiceMonasteryConstants.ActionArea area;

        public ResourceInArea() {this(DiceMonasteryConstants.Resource.SHILLINGS.name(), STOREROOM.name());}

        public ResourceInArea(String res, String area) {
            addEventType(Event.GameEvent.ROUND_OVER);
            this.resource = DiceMonasteryConstants.Resource.valueOf(res);
            this.area =  DiceMonasteryConstants.ActionArea.valueOf(area);
        }
        public String name() {return getClass().getSimpleName() + " (" + resource + " in " + area + ")";}

        @Override
        public Object run(GameListener listener, Event e) {
            return ((DiceMonasteryGameState)e.state).getResource(e.playerID, resource, area);
        }

        public Object[] getAllowedParameters() {
            return new Object[] {DiceMonasteryConstants.Resource.values(),  DiceMonasteryConstants.ActionArea.values()};
        }
    }

    public static class WritingSets extends AbstractMetric {

        public WritingSets() {addEventType(Event.GameEvent.ROUND_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            return Math.min(((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.VELLUM, STOREROOM),
                    ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.CANDLE, STOREROOM));
        }
    }

    public static class Pigments extends AbstractMetric {

        public Pigments() {addEventType(Event.GameEvent.ROUND_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            return ((DiceMonasteryGameState)e.state).getStores(e.playerID, r -> r.isPigment).size();
        }
    }

    public static class Inks extends AbstractMetric {

        public Inks() {addEventType(Event.GameEvent.ROUND_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            return ((DiceMonasteryGameState)e.state).getStores(e.playerID, r -> r.isInk).size();
        }
    }

    public static class MonksCount extends AbstractMetric {

        public MonksCount() {addEventType(Event.GameEvent.ROUND_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            return ((DiceMonasteryGameState)e.state).monksIn(null, e.playerID).size();
        }
    }


    public static class PietySum extends AbstractMetric {

        public PietySum() {addEventType(Event.GameEvent.ROUND_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            return ((DiceMonasteryGameState)e.state).monksIn(null, e.playerID).stream().mapToInt(Monk::getPiety).sum();
        }
    }

    public static class VictoryPoints extends AbstractMetric {

        public VictoryPoints() {addEventType(Event.GameEvent.ROUND_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            return ((DiceMonasteryGameState)e.state).getVictoryPoints(e.playerID);
        }
    }

    public static class TreasureSum extends AbstractMetric {

        public TreasureSum() {addEventType(Event.GameEvent.ROUND_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            return ((DiceMonasteryGameState)e.state).getTreasures(e.playerID).stream().mapToInt(t -> t.vp).sum();
        }
    }

}
