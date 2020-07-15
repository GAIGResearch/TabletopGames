package games.pandemic;
import core.AbstractGameState;
import core.CoreConstants;
import core.components.Counter;
import core.components.Deck;
import core.interfaces.IStateHeuristic;
import utilities.Hash;
import utilities.Utils;

public class PandemicHeuristic implements IStateHeuristic {

    double FACTOR_CURES = 0.3;
    double FACTOR_CUBES = 0.2;
    double FACTOR_CARDS_IN_PILE = 0.15;
    double FACTOR_CARDS_IN_HAND = 0.15;
    double FACTOR_OUTBREAKS = -0.2;
    double FACTOR_RS = 0.2;

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        PandemicGameState pgs = (PandemicGameState) gs;
        PandemicParameters pp = (PandemicParameters)gs.getGameParameters();
        Utils.GameResult gameStatus = gs.getGameStatus();

        if(gameStatus == Utils.GameResult.LOSE)
            return -1;
        if(gameStatus == Utils.GameResult.WIN)
            return 1;

        // Compute a score
        Counter outbreaks = (Counter)pgs.getComponent(PandemicConstants.outbreaksHash);
        int nOutbreaks = outbreaks.getValue()/outbreaks.getMaximum();
        int nTotalCardsPlayerDeck = pp.n_city_cards + pp.n_event_cards + pp.n_epidemic_cards;
        int nCardsInPile = ((Deck)pgs.getComponent(PandemicConstants.playerDeckHash)).getSize()/nTotalCardsPlayerDeck;
        int nCardsInHand = ((Deck)pgs.getComponentActingPlayer(CoreConstants.playerHandHash)).getSize()/(pp.max_cards_per_player+2);
        int nResearchStations = ((Counter)pgs.getComponent(PandemicConstants.researchStationHash)).getValue()/pp.n_research_stations;
        double nCuresDiscovered = 0;
        double nDiseaseCubes = 0;

        for (int i = 0; i < PandemicConstants.colors.length; i++){
            nDiseaseCubes += ((Counter)pgs.getComponent(Hash.GetInstance().hash("Disease Cube " + PandemicConstants.colors[i]))).getValue();
            if (((Counter)pgs.getComponent(Hash.GetInstance().hash("Disease Cube " + PandemicConstants.colors[i]))).getValue() > 0)
                nCuresDiscovered += 1;
        }

        return (nCuresDiscovered/PandemicConstants.colors.length) * FACTOR_CURES
                + nCardsInHand * FACTOR_CARDS_IN_HAND
                + (nDiseaseCubes/pp.n_initial_disease_cubes) * FACTOR_CUBES
                + nCardsInPile * FACTOR_CARDS_IN_PILE
                + nOutbreaks * FACTOR_OUTBREAKS
                + nResearchStations * FACTOR_RS
                ;
    }

}