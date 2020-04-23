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
        int curesDiscovered;
        int nDiseaseCubes;
        int nCardsInPile;
        int nCardsInHand;
        int nOutbreaks;

        double FACTOR_CURES = 0.5;
        double FACTOR_CARDS_IN_HAND = 0.1;

        BoardStats(GameState gs) {
            // iterate over the game state and get the value for the variables
//            int counterValue = gs.findCounter("Disease counter").getValue();

            Deck playerHand = ((Deck)gs.getAreas().get(gs.getActingPlayer()).getComponent(Constants.playerHandHash));
            nCardsInHand = playerHand.size();
        }

        /**
         * Computes score for a game, in relation to the initial state at the root.
         * @param futureState the stats of the board at the end of the rollout.
         * @return a score [0, 1]
         */
        double score(BoardStats futureState)
        {
            int diffCures = futureState.curesDiscovered - this.curesDiscovered;
            int diffCardsInHand = futureState.nCardsInHand - this.nCardsInHand;

            System.out.println("OSLA evaluated = " + (diffCures * FACTOR_CURES + diffCardsInHand * FACTOR_CARDS_IN_HAND));

            return diffCures * FACTOR_CURES + diffCardsInHand * FACTOR_CARDS_IN_HAND;
        }
    }
}