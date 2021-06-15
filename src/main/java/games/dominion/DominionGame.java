package games.dominion;

import core.*;
import games.GameType;

import java.util.*;

public class DominionGame extends Game {

    public DominionGame(List<AbstractPlayer> players, DominionParameters params) {
        super(GameType.Dominion, players, new DominionForwardModel(), new DominionGameState(params, players.size()));
    }
    public DominionGame(DominionParameters params, int nPlayers) {
        super(GameType.Dominion, new DominionForwardModel(), new DominionGameState(params, nPlayers));
    }
}
