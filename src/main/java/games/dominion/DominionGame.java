package games.dominion;

import core.*;
import evaluation.AbstractTournament;
import evaluation.RoundRobinTournament;
import games.GameType;
import games.GameType.*;
import players.mcts.MCTSEnums;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.rmhc.RMHCPlayer;
import players.simple.*;

import java.util.*;

public class DominionGame extends Game {

    public DominionGame(List<AbstractPlayer> players, DominionParameters params) {
        super(GameType.Dominion, players, new DominionForwardModel(), new DominionGameState(params, players.size()));
    }
    public DominionGame(DominionParameters params, int nPlayers) {
        super(GameType.Dominion, new DominionForwardModel(), new DominionGameState(params, nPlayers));
    }
}
