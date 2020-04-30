package players.heuristics;
import components.Deck;
import core.GameState;
import pandemic.Constants;

public class PandemicHeuristic extends StateHeuristic {
    private BoardStats rootBoardStats;

    public PandemicHeuristic(GameState root) {
        rootBoardStats = new BoardStats(root);
    }

    @Override
    public double evaluateState(GameState gs) {
        int gamestatus = gs.getGameStatus();

        // Compute a score relative to the root's state.
        BoardStats lastBoardState = new BoardStats(gs);
        double rawScore = rootBoardStats.score(lastBoardState);

        if(gamestatus == Constants.GAME_LOSE)
            rawScore = -1;

        if(gamestatus == Constants.GAME_WIN)
            rawScore = 1;

        return rawScore;
    }

    public static class BoardStats
    {
        int nCuresDiscovered;
        int nDiseaseCubes;
        int nCardsInPile;
        int nCardsInHand;
        int nOutbreaks;
        int nResearchStations;

        double FACTOR_CURES = 0.3;
        double FACTOR_CUBES = 0.2;
        double FACTOR_CARDS_IN_PILE = 0.15;
        double FACTOR_CARDS_IN_HAND = 0.15;
        double FACTOR_OUTBREAKS = -0.2;
        double FACTOR_RS = 0.2;

        BoardStats(GameState gs) {
            // iterate over the game state and get the value for the variables
//            int counterValue = gs.findCounter("Disease counter").getValue();
            nOutbreaks = gs.findCounter("Outbreaks").getValue();
            nCardsInPile = gs.findDeck("Player Deck").getCards().size();
            nCardsInHand = ((Deck)gs.getAreas().get(gs.getActingPlayer()).getComponent(Constants.playerHandHash)).size();

            // get disease cubes
            for (int i = 0; i < 4; i++){
                nDiseaseCubes += gs.findCounter("Disease cube " + Constants.colors[i]).getValue();
                if (gs.findCounter("Disease " + Constants.colors[i]).getValue() > 0)
                    nCuresDiscovered += 1;
            }

            nResearchStations = gs.findCounter("Research Stations").getValue();


        }

        /**
         * Computes score for a game, in relation to the initial state at the root.
         * @param futureState the stats of the board at the end of the rollout.
         * @return a score [0, 1]
         */
        double score(BoardStats futureState)
        {
            int diffCures = futureState.nCuresDiscovered - this.nCuresDiscovered;
            int diffCardsInHand = futureState.nCardsInHand - this.nCardsInHand;
            int diffCubes = futureState.nDiseaseCubes - this.nDiseaseCubes;
            int diffCardsInPile = futureState.nCardsInPile - this.nCardsInPile;
            int diffOutbreaks = futureState.nOutbreaks - this.nOutbreaks;
            int diffResearchStations = futureState.nResearchStations - this.nResearchStations;

            double score = diffCures * FACTOR_CURES + diffCardsInHand * FACTOR_CARDS_IN_HAND + diffCubes * FACTOR_CUBES +
                    diffCardsInPile * FACTOR_CARDS_IN_PILE + diffOutbreaks * FACTOR_OUTBREAKS + diffResearchStations * FACTOR_RS;

//            System.out.println("OSLA evaluated = " + score);

            return score;
        }
    }
}