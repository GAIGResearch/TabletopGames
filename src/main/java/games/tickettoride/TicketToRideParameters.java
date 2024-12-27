package games.tickettoride;

import core.AbstractGameState;
import core.AbstractParameters;
import evaluation.optimisation.TunableParameters;

import java.util.HashMap;
import java.util.Map;


/**
 * <p>This class should hold a series of variables representing game parameters (e.g. number of cards dealt to players,
 * maximum number of rounds in the game etc.). These parameters should be used everywhere in the code instead of
 * local variables or hard-coded numbers, by accessing these parameters from the game state via {@link AbstractGameState#getGameParameters()}.</p>
 *
 * <p>It should then implement appropriate {@link #_copy()}, {@link #_equals(Object)} and {@link #hashCode()} functions.</p>
 *
 * <p>The class can optionally extend from {@link TunableParameters} instead, which allows to use
 * automatic game parameter optimisation tools in the framework.</p>
 */
public class TicketToRideParameters extends AbstractParameters {

    int nLocomotiveCards = 14;
    int nCardsPerColor = 12;
    int nTrainCardsDraw = 2;
    int nDestinationCardsDraw = 3;


    int nMaxPlayers = 5;

    String dataPath;

    int nInitialTrainCards = 5;



    int totalDestinationTickets = 30;

    int nTotalTrainCards = 240;
    int nTotalDestinationCards = 5;

    static Map<Integer, Integer> pointsPerRoute = new HashMap<>() {{
        put(1, 1);
        put(2, 2);
        put(3, 4);
        put(4, 7);
        put(5, 10);
        put(6, 15);
    }};

    public String getDataPath() {
        return dataPath;
    }


    public enum TrainCar {
        Red, Green, Blue, Black, White, Purple, Yellow, Orange, Locomotive
    }


    public int getPointsPerRoute(int cost) {
        return pointsPerRoute.get(cost);
    }

    @Override
    protected AbstractParameters _copy() {
        // TODO: deep copy of all variables.
        return this;
    }

    @Override
    protected boolean _equals(Object o) {
        // TODO: compare all variables.
        return o instanceof TicketToRideParameters;
    }

    @Override
    public int hashCode() {
        // TODO: include the hashcode of all variables.
        return super.hashCode();
    }

    public TicketToRideParameters(String dataPath) {
        this.dataPath = dataPath;

    }

    public TicketToRideParameters(TicketToRideParameters ticketToRideParameters) {
        this(ticketToRideParameters.dataPath);
        this.pointsPerRoute = new HashMap<>();
        for(int key : TicketToRideParameters.pointsPerRoute.keySet())
            this.pointsPerRoute.put(key, TicketToRideParameters.pointsPerRoute.get(key));


    }
}