package evaluation;

import core.AbstractPlayer;
import games.coltexpress.ColtExpressRunner;
import players.RandomPlayer;
import utilities.Utils;

import java.util.LinkedList;


public class RoundRobinTournament extends AbstractTournament {
    int[] pointsPerPlayer;
    LinkedList<Integer> agentIDs;
    ColtExpressRunner runner = new ColtExpressRunner();
    private final int gamesPerMatchup;
    private final int playersPerGame;
    private final boolean selfplay;

    public RoundRobinTournament(LinkedList<AbstractPlayer> agents, int playersPerGame, int gamesPerMatchup, boolean selfplay){
        super(agents);
        if (!selfplay && playersPerGame >= this.agents.size())
            throw new IllegalArgumentException("Not enough agents to fill a match without selfplay." +
                    "Either add more agents, reduce the number of players per game, or allow selfplay.");

        this.agentIDs = new LinkedList<>();
        for (int i = 0; i < this.agents.size(); i++)
            this.agentIDs.add(i);

        this.gamesPerMatchup = gamesPerMatchup;
        this.playersPerGame = playersPerGame;
        this.selfplay = selfplay;
        this.pointsPerPlayer = new int[agents.size()];
    }

    @Override
    public void runTournament() {
        LinkedList<Integer> matchup = new LinkedList<>();
        createAndRunMatchup(matchup);

        for (int i=0; i < this.agents.size(); i++)
            System.out.println(this.agents.get(i).toString() + " got " + pointsPerPlayer[i] + " points");
    }

    public void createAndRunMatchup(LinkedList<Integer> matchup){
        if (matchup.size() == playersPerGame){
            evaluateMatchup(matchup);
        }
        else {
            for (Integer agentID : this.agentIDs){
                if (selfplay || !matchup.contains(agentID)) {
                    matchup.add(agentID);
                    createAndRunMatchup(matchup);
                    matchup.remove(agentID);
                }
            }
        }
    }

    private void evaluateMatchup(LinkedList<Integer> agentIDs){
        System.out.println("Evaluate " + agentIDs.toString());
        LinkedList<AbstractPlayer> matchupPlayers = new LinkedList<>();
        for (int agentID : agentIDs)
            matchupPlayers.add(this.agents.get(agentID));

        for (int i = 0; i < this.gamesPerMatchup; i++) {
            Utils.GameResult[] results = this.runner.runGame(matchupPlayers);
            for (int j = 0; j < matchupPlayers.size(); j++){
                pointsPerPlayer[agentIDs.get(j)] += results[j] == Utils.GameResult.GAME_WIN ? 1 : 0;
            }
        }
    }

    public static void main(String[] args){
        LinkedList<AbstractPlayer> agents = new LinkedList<>();
        for (int i = 0; i < 5; i++){
            agents.add(new RandomPlayer());
        }

        AbstractTournament tournament = new RoundRobinTournament(agents, 4, 100, false);
        tournament.runTournament();
    }
}
