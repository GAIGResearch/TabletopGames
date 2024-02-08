package evaluation.tournamentSeeds;

import evaluation.RunGames;

public class TestRandomSeedsInTournaments {

    public static void main(String[] args) {

        // We run each type of tournament (and then manually check the files for seeds)

        RunGames.main(new String[]{"config=Tournament_Random_Base.json"});
        // Should create 10 rows, each with a different seed
        RunGames.main(new String[]{"config=Tournament_Exhaustive_Base.json"});
        // Should create 10 * 6 = 60 rows, with each set of 6 rows having the same seed
        RunGames.main(new String[]{"config=Tournament_Seq_Base.json"});
        // Three directories, one for each pair of agents
        // 10 games in each, with 10 different seeds (seeds the same across each pair of agents)
        RunGames.main(new String[]{"config=Tournament_Focus_Base.json"});
        // 30 games, with 10 different seeds...in each set the Focus player is p1, p1, p2 respectively

        // the ones below should then replicate all of the above, but with 3 seeds
        RunGames.main(new String[]{"config=Tournament_Random_ThreeSeeds.json"});
        // 30 games, each set of 10 with a different seed
        RunGames.main(new String[]{"config=Tournament_Exhaustive_ThreeSeeds.json"});
        // 3  * 60 games, with each set of 60 having the same seed
        RunGames.main(new String[]{"config=Tournament_Seq_ThreeSeeds.json"});
        // 3 directories, one for each pair of agents
        // 3 * 10 games in each, with 3 different seeds (seeds the same across each pair of agents)
        RunGames.main(new String[]{"config=Tournament_Focus_ThreeSeeds.json"});
        // 3 * 30 games, with 1 seed per set of 30...in each set the Focus player is p1, p1, p2 respectively
    }

}
