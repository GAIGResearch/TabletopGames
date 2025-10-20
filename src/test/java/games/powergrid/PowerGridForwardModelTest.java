package games.powergrid;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;


import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import evaluation.ForwardModelTester;
import games.powergrid.PowerGridForwardModel;
import games.powergrid.PowerGridGameState;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;

public class PowerGridForwardModelTest {

        @Test
        public void testForwardModelConsistency() {
            // Adjust game name, players, seed, etc. to your setup
        	ForwardModelTester fmt = new ForwardModelTester("game=PowerGrid","nGames=1", "nPlayers=2");
        }
            // If the tester throws no AssertionError, the test passes.
        
        
        @Test
        public void testForwardModelMCTS() {
        	// Adjust game name, players, seed, etc. to your setup
            new ForwardModelTester(
                "game=PowerGrid",   // matches your GameType enum
                "nPlayers=4",
                "agent=mcts",
                "nGames=1"       // optional
            );
        }

}
