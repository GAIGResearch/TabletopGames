package games.wonders7;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameAttribute;
import core.interfaces.IGameListener;
import core.interfaces.IStatisticLogger;
import utilities.ActionSimpleAttributes;
import utilities.Pair;
import utilities.TAGStatSummary;
import utilities.TAGSummariser;


import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static core.CoreConstants.GameEvents.*;

public class Wonders7Listener implements IGameListener {

    IStatisticLogger logger;

    public Wonders7Listener(IStatisticLogger logger) {
        this.logger = logger;
    }

    @Override
    public void onGameEvent(CoreConstants.GameEvents type, Game game) {
        //if (type == ACTION_TAKEN || type == GAME_OVER) {
        //    AbstractGameState state = game.getGameState();
        //    Map<String, Object> data = Arrays.stream(Wonders7Attributes.values())
        //            .collect(Collectors.toMap(IGameAttribute::name, attr -> attr.get(state, null)));
        //    logger.record(data);
        //}
    }

    @Override
    public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction action) {
        if (type == GAME_OVER) {
            Map<String, Object> data = Arrays.stream(Wonders7Attributes.values())
                    .collect(Collectors.toMap(IGameAttribute::name, attr -> attr.get(state, null)));
            logger.record(data);
        }
    }

    @Override
    public void allGamesFinished() {
        logger.processDataAndFinish();
    }
}