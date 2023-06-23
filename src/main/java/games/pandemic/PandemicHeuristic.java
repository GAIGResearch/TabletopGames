package games.pandemic;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.Counter;
import core.components.Deck;
import core.interfaces.IStateHeuristic;
import core.properties.PropertyString;
import evaluation.optimisation.TunableParameters;
import utilities.Hash;

import static games.pandemic.PandemicConstants.*;
import static utilities.Utils.indexOf;

public class PandemicHeuristic extends TunableParameters implements IStateHeuristic {

    double FACTOR_CURES = 1.0;
    double FACTOR_CUBES = 0.2;
    double FACTOR_CARDS_IN_PILE = 0.15;
    double FACTOR_CARDS_IN_HAND = 0.25;
    double FACTOR_OUTBREAKS = -0.5;
    double FACTOR_RS = 0.35;
    double FACTOR_AT_RS = 0.6;

    public PandemicHeuristic() {
        addTunableParameter("FACTOR_CURES", 0.3);
        addTunableParameter("FACTOR_CUBES", 0.2);
        addTunableParameter("FACTOR_CARDS_IN_PILE", 0.15);
        addTunableParameter("FACTOR_CARDS_IN_HAND", 0.15);
        addTunableParameter("FACTOR_OUTBREAKS", -0.2);
        addTunableParameter("FACTOR_RS", 0.2);
        _reset();
    }

    @Override
    public void _reset() {
        FACTOR_CURES = (double) getParameterValue("FACTOR_CURES");
        FACTOR_CUBES = (double) getParameterValue("FACTOR_CUBES");
        FACTOR_CARDS_IN_PILE = (double) getParameterValue("FACTOR_CARDS_IN_PILE");
        FACTOR_CARDS_IN_HAND = (double) getParameterValue("FACTOR_CARDS_IN_HAND");
        FACTOR_OUTBREAKS = (double) getParameterValue("FACTOR_OUTBREAKS");
        FACTOR_RS = (double) getParameterValue("FACTOR_RS");
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        PandemicGameState pgs = (PandemicGameState) gs;
        PandemicParameters pp = (PandemicParameters) gs.getGameParameters();

        if (!pgs.isNotTerminal()) {
            return gs.getGameStatus().value * 10;
        }

        // Compute a score
        Counter outbreaks = (Counter) pgs.getComponent(PandemicConstants.outbreaksHash);
        int nOutbreaks = outbreaks.getValue() / outbreaks.getMaximum();
        int nTotalCardsPlayerDeck = pp.nCityCards + pp.nEventCards + pp.nEpidemicCards;
        int nCardsInPile = ((Deck) pgs.getComponent(PandemicConstants.playerDeckHash)).getSize() / nTotalCardsPlayerDeck;
        int nCardsInHand = ((Deck) pgs.getComponentActingPlayer(CoreConstants.playerHandHash)).getSize() / (pp.maxCardsPerPlayer + 2);
        int nResearchStations = ((Counter) pgs.getComponent(PandemicConstants.researchStationHash)).getValue() / pp.nResearchStations;

        int playerAtResStation = 0;
        for (String resStationLocation: pgs.researchStationLocations){
            if (((PropertyString) pgs.getComponentActingPlayer(playerCardHash).getProperty(playerLocationHash)).value.equals(resStationLocation)){
                playerAtResStation = 1;
            }

        }
        double nCuresDiscovered = 0;
        double nDiseaseCubes = 0;

        for (String color : colors) {
            nDiseaseCubes += ((Counter) pgs.getComponent(Hash.GetInstance().hash("Disease Cube " + color))).getValue();
            if (((Counter) pgs.getComponent(Hash.GetInstance().hash("Disease Cube " + color))).getValue() > 0)
                nCuresDiscovered += 1;
        }

        return (nCuresDiscovered / colors.length) * FACTOR_CURES
                + nCardsInHand * FACTOR_CARDS_IN_HAND
                + (nDiseaseCubes / pp.nInitialDiseaseCubes) * FACTOR_CUBES
                + nCardsInPile * FACTOR_CARDS_IN_PILE
                + nOutbreaks * FACTOR_OUTBREAKS
                + nResearchStations * FACTOR_RS
                + playerAtResStation * FACTOR_AT_RS
                ;
    }

    @Override
    protected PandemicHeuristic _copy() {
        PandemicHeuristic retValue = new PandemicHeuristic();
        retValue.FACTOR_CURES = FACTOR_CURES;
        retValue.FACTOR_CUBES = FACTOR_CUBES;
        retValue.FACTOR_CARDS_IN_HAND = FACTOR_CARDS_IN_HAND;
        retValue.FACTOR_CARDS_IN_PILE = FACTOR_CARDS_IN_PILE;
        retValue.FACTOR_OUTBREAKS = FACTOR_OUTBREAKS;
        retValue.FACTOR_RS = FACTOR_RS;
        return retValue;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof PandemicHeuristic) {
            PandemicHeuristic other = (PandemicHeuristic) o;
            return other.FACTOR_RS == FACTOR_RS && other.FACTOR_OUTBREAKS == FACTOR_OUTBREAKS &&
                    other.FACTOR_CARDS_IN_HAND == FACTOR_CARDS_IN_HAND && other.FACTOR_CARDS_IN_PILE == FACTOR_CARDS_IN_PILE &&
                    other.FACTOR_CUBES == FACTOR_CUBES && other.FACTOR_CURES == FACTOR_CURES;
        }
        return false;
    }

    /**
     * @return Returns Tuned Parameters corresponding to the current settings
     * (will use all defaults if setParameterValue has not been called at all)
     */
    @Override
    public PandemicHeuristic instantiate() {
        return this._copy();
    }

}