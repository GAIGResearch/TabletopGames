package gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.CoreConstants;
import core.actions.AbstractAction;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

@SuppressWarnings("rawtypes")
public abstract class AbstractGUIManager {

    public static int defaultItemSize = 50;
    public static int defaultActionPanelHeight = 100;
    public static int defaultInfoPanelHeight = 180;
    public static int defaultCardWidth = 100, defaultCardHeight = 80;
    public static int defaultBoardWidth = 400, defaultBoardHeight = 300;
    public static int defaultDisplayWidth = 500, defaultDisplayHeight = 400;

    protected int maxActionSpace;
    protected ActionController ac;
    protected int actionsAtLastUpdate;

    protected int width, height;

    public AbstractGUIManager(ActionController ac, int maxActionSpace) {
        this.ac = ac;
        this.maxActionSpace = maxActionSpace;
    }

    /* Methods that should/can be implemented by subclass */

    /**
     * Updates all GUI elements. Must be implemented by subclass.
     *
     * @param player    - current player acting.
     * @param gameState - current game state to be used in updating visuals.
     */
    protected abstract void _update(AbstractPlayer player, AbstractGameState gameState);

    protected abstract void updateGameStateInfo(AbstractGameState gameState);

    public void update(AbstractPlayer player, AbstractGameState gameState, boolean showActions)
    {
        updateGameStateInfo(gameState);
        _update(player, gameState);
    }
}
