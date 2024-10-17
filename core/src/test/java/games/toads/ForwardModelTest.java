package games.toads;

import evaluation.ForwardModelTester;
import org.junit.Test;

public class ForwardModelTest {

    @Test
    public void rndTest() {
        new ForwardModelTester("game=WarOfTheToads", "nGames=5", "nPlayers=2");
    }

    @Test
    public void mctsTest() {
        new ForwardModelTester("game=WarOfTheToads", "nGames=5", "nPlayers=2", "agentToPlay=json\\players\\gameSpecific\\Diamant.json");
    }
}
