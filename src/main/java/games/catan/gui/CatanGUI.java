package games.catan.gui;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import players.human.ActionController;

public class CatanGUI extends AbstractGUI {

    public CatanGUI(Game game, ActionController ac) {
        // todo implement GUI
        super(ac, 25);
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {

    }
}
