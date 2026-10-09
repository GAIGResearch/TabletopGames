package games.ventlife.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import gui.AbstractGUIManager;
import gui.GamePanel;
import players.human.ActionController;

import java.util.Set;

/**
 * TODO: the GUI is a later stage (Stage 4 of tag-game-implement), with a clickable board.
 */
public class VentlifeGUIManager extends AbstractGUIManager {

    public VentlifeGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
    }

    @Override
    public int getMaxActionSpace() {
        return 10;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
    }
}
