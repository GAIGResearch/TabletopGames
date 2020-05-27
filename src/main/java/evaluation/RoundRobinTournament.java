package evaluation;

import core.AbstractGUI;
import core.AbstractGame;
import core.AbstractPlayer;
import players.RandomPlayer;
import utilities.Utils;

import java.util.LinkedList;


public class RoundRobinTournament extends AbstractTournament {
    int[] pointsPerPlayer;
    LinkedList<Integer> agentIDs;
    private final int gamesPerMatchup;
    private final int playersPerGame;
    private final boolean selfplay;

    private AbstractGame game;
    private AbstractGUI gui;

    public RoundRobinTournament(LinkedList<AbstractPlayer> agents, int playersPerGame, int gamesPerMatchup, boolean selfplay,
                                Game gameToPlay){
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

        this.game = AbstractTournament.createGameInstance(gameToPlay, playersPerGame);
        if (this.game == null) throw new IllegalArgumentException("Chosen game not supported");
        // TODO: init GUI if available
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
            game.reset(matchupPlayers);
            game.run(gui);
            Utils.GameResult[] results = game.getGameState().getPlayerResults();
            for (int j = 0; j < matchupPlayers.size(); j++) {
                pointsPerPlayer[agentIDs.get(j)] += results[j] == Utils.GameResult.GAME_WIN ? 1 : 0;
            }
        }
    }

    public static void main(String[] args){
        LinkedList<AbstractPlayer> agents = new LinkedList<>();
        for (int i = 0; i < 5; i++){
            agents.add(new RandomPlayer());
        }

        AbstractTournament tournament = new RoundRobinTournament(agents, 4, 100, false, Game.ColtExpress);
        tournament.runTournament();
    }
}
