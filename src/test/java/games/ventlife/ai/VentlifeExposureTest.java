package games.ventlife.ai;

import core.AbstractForwardModel;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.ventlife.VentlifeGameState;
import games.ventlife.VentlifeUtils;
import games.ventlife.components.Hex;
import games.ventlife.components.HexCell;
import games.ventlife.components.Terrain;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class VentlifeExposureTest {

    // the definition, by way of VentlifeUtils.isPlateauPlacement
    private static Map<Hex, Integer> reference(VentlifeGameState state) {
        Map<Hex, Integer> retValue = new HashMap<>();
        for (Map.Entry<Hex, HexCell> e : state.getField().entrySet()) {
            if (e.getValue().terrain() != Terrain.BLACK_SMOKER) continue;
            for (int o = 0; o < 6; o++)
                if (VentlifeUtils.isPlateauPlacement(state, e.getKey(), o)) {
                    List<Hex> hexes = VentlifeUtils.tileHexes(e.getKey(), o);
                    retValue.merge(hexes.get(0), 2, (a, b) -> a | b);
                    retValue.merge(hexes.get(1), 1, (a, b) -> a | b);
                    retValue.merge(hexes.get(2), 1, (a, b) -> a | b);
                }
        }
        return retValue;
    }

    @Test
    public void exposedPositionsAreThoseAPlateauTileCouldCover() {
        Random rnd = new Random(9);
        Game game = GameType.Ventlife.createGameInstance(4, 17);
        for (int g = 0; g < 30; g++) {
            game.reset(List.<AbstractPlayer>of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer(),
                    new RandomPlayer()), 300 + g);
            VentlifeGameState state = (VentlifeGameState) game.getGameState();
            AbstractForwardModel fm = game.getForwardModel();
            while (state.isNotTerminal()) {
                assertEquals(reference(state), VentlifeScorer.exposedPositions(state));
                List<AbstractAction> actions = fm.computeAvailableActions(state);
                fm.next(state, actions.get(rnd.nextInt(actions.size())));
            }
        }
    }
}
