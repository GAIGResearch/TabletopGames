package games.ventlife.ai;

import core.AbstractForwardModel;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.ventlife.VentlifeGameState;
import games.ventlife.VentlifeParameters;
import games.ventlife.actions.PlaceTile;
import games.ventlife.components.Species;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class VentlifeTileSimTest {

    @Test
    public void tileSimMatchesForwardModel() {
        Random rnd = new Random(5);
        int checked = 0;
        for (int nPlayers = 2; nPlayers <= 4; nPlayers++) {
            VentlifeParameters params = new VentlifeParameters();
            params.setParameterValue("nSpecies", 5);
            Game game = GameType.Ventlife.createGameInstance(nPlayers, 13, params);
            for (int g = 0; g < 15; g++) {
                game.reset(List.<AbstractPlayer>of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer(),
                        new RandomPlayer()).subList(0, nPlayers), 200 + g);
                VentlifeGameState state = (VentlifeGameState) game.getGameState();
                AbstractForwardModel fm = game.getForwardModel();
                while (state.isNotTerminal()) {
                    List<AbstractAction> actions = fm.computeAvailableActions(state);
                    for (AbstractAction a : actions) {
                        if (!(a instanceof PlaceTile t) || rnd.nextInt(4) != 0) continue;
                        VentlifeGameState viaFM = (VentlifeGameState) state.copy();
                        fm.next(viaFM, a);
                        VentlifeGameState viaSim = VentlifeTileSim.afterTile(state, t);
                        assertEquals(a.toString(), viaFM.getField(), viaSim.getField());
                        assertEquals(a.toString(), viaFM.getCreatures(), viaSim.getCreatures());
                        for (int p = 0; p < nPlayers; p++)
                            for (Species s : Species.values())
                                assertEquals(viaFM.getSupply(p, s), viaSim.getSupply(p, s));
                        checked++;
                    }
                    fm.next(state, actions.get(rnd.nextInt(actions.size())));
                }
            }
        }
        System.out.println("Checked " + checked + " tile placements");
    }
}
