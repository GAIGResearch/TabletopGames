package games.hareandtortoise.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import gui.AbstractGUIManager;
import gui.GamePanel;
import players.human.ActionController;

import java.util.Set;

/**
 * GUI for Hare and Tortoise.
 * TODO Stage 4 (GUI): see HareAndTortoise_plan.txt
 */
public class HareAndTortoiseGUIManager extends AbstractGUIManager {

    public HareAndTortoiseGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
    }

    @Override
    public int getMaxActionSpace() {
        return 70;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
    }
}
