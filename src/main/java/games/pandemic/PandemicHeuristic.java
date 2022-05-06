package games.pandemic;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import core.interfaces.IStateHeuristic;
import core.properties.PropertyColor;
import core.properties.PropertyString;
import evaluation.TunableParameters;
import utilities.Hash;
import utilities.Utils;

import static core.CoreConstants.colorHash;
import static games.pandemic.PandemicConstants.*;
import static utilities.Utils.indexOf;

public class PandemicHeuristic extends TunableParameters implements IStateHeuristic {

    double FACTOR_CURES = 0.6;
    double FACTOR_CUBES = 0.1;
    double FACTOR_CARDS_IN_PILE = 0.1;
    double FACTOR_CARDS_IN_HAND = 0.2;
    double FACTOR_OUTBREAKS = -0.2;
    double FACTOR_RS = 0.2; // Factor for having more RS
    double FACTOR_AT_RS = 0.3; // Factor for current player standing at research station
    double FACTOR_GOOD_CARDS = 0.3; // number of cards for uncured disease
    // TODO: heuristic factors should be normalised

    public PandemicHeuristic() {
        addTunableParameter("FACTOR_CURES", 0.3);
        addTunableParameter("FACTOR_CUBES", 0.1);
        addTunableParameter("FACTOR_CARDS_IN_PILE", 0.1);
        addTunableParameter("FACTOR_CARDS_IN_HAND", 0.2);
        addTunableParameter("FACTOR_OUTBREAKS", -0.2);
        addTunableParameter("FACTOR_RS", 0.2);
        addTunableParameter("FACTOR_GOOD_CARDS", 0.3);
    }

    @Override
    public void _reset() {
        FACTOR_CURES = (double) getParameterValue("FACTOR_CURES");
        FACTOR_CUBES = (double) getParameterValue("FACTOR_CUBES");
        FACTOR_CARDS_IN_PILE = (double) getParameterValue("FACTOR_CARDS_IN_PILE");
        FACTOR_CARDS_IN_HAND = (double) getParameterValue("FACTOR_CARDS_IN_HAND");
        FACTOR_OUTBREAKS = (double) getParameterValue("FACTOR_OUTBREAKS");
        FACTOR_RS = (double) getParameterValue("FACTOR_RS");
        FACTOR_AT_RS = (double) getParameterValue("FACTOR_AT_RS");
        FACTOR_GOOD_CARDS = (double) getParameterValue("FACTOR_GOOD_CARDS");
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        PandemicGameState pgs = (PandemicGameState) gs;
        PandemicParameters pp = (PandemicParameters) gs.getGameParameters();
        Utils.GameResult gameStatus = gs.getGameStatus();

        if (!pgs.isNotTerminal()) {
            return pgs.getGameStatus().value * 10;
        }

        // Compute a score
        Counter outbreaks = (Counter) pgs.getComponent(PandemicConstants.outbreaksHash);
        int nOutbreaks = outbreaks.getValue() / outbreaks.getMaximum();
        int nTotalCardsPlayerDeck = pp.nCityCards + pp.nEventCards + pp.nEpidemicCards;
        int nCardsInPile = ((Deck) pgs.getComponent(PandemicConstants.playerDeckHash)).getSize() / nTotalCardsPlayerDeck;
        int nCardsInHand = ((Deck) pgs.getComponentActingPlayer(CoreConstants.playerHandHash)).getSize() / (pp.maxCardsPerPlayer + 2);
        int nResearchStations = ((Counter) pgs.getComponent(PandemicConstants.researchStationHash)).getValue() / pp.nResearchStations;

        int[] colourCount = new int[colors.length]; // keeps track of the cards required to cure diseases
        for( Card c : ((Deck<Card>) pgs.getComponentActingPlayer(CoreConstants.playerHandHash)).getComponents()) {
            if (c.getProperty(colorHash) != null) {
                colourCount[Utils.indexOf(colors, ((PropertyColor) c.getProperty(colorHash)).valueStr)]++;
            }
        }

        int playerAtResStation = 0;
        for (String resStationLocation: pgs.researchStationLocations){
            if (((PropertyString) pgs.getComponentActingPlayer(playerCardHash).getProperty(playerLocationHash)).value.equals(resStationLocation)){
                playerAtResStation = 1;
            }

        }
        double nCuresDiscovered = 0;
        double nDiseaseCubes = 0;

        for (int i = 0; i < colors.length; i++) {
            nDiseaseCubes += ((Counter) pgs.getComponent(Hash.GetInstance().hash("Disease Cube " + colors[i]))).getValue();
            if (((Counter) pgs.getComponent(Hash.GetInstance().hash("Disease " + colors[i]))).getValue() > 0) {
                nCuresDiscovered += 1;
                colourCount[i] = 0; // remove cards for cured diseases from the count
            }
        }

        double usefulCards = 0.0;
        for (int i = 0; i < colourCount.length; i++){
            usefulCards += colourCount[i];
        }
        usefulCards = usefulCards / pp.maxCardsPerPlayer; // normalisation

        return (nCuresDiscovered / colors.length) * FACTOR_CURES
                + nCardsInHand * FACTOR_CARDS_IN_HAND
                + (nDiseaseCubes / pp.nInitialDiseaseCubes) * FACTOR_CUBES
                + nCardsInPile * FACTOR_CARDS_IN_PILE
                + nOutbreaks * FACTOR_OUTBREAKS
                + nResearchStations * FACTOR_RS
                + playerAtResStation * FACTOR_AT_RS
                + usefulCards * FACTOR_GOOD_CARDS
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