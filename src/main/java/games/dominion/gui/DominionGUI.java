package games.dominion.gui;

import core.*;
import games.dominion.*;
import players.human.ActionController;

public class DominionGUI extends AbstractGUI {
    // Settings for display areas
    final static int playerAreaWidth = 300;
    final static int playerAreaHeight = 130;
    final static int cardWidth = 90;
    final static int cardHeight = 115;

    // Currently active player
    int activePlayer = -1;
    int humanId;

    public DominionGUI(Game game, ActionController ac, int humanID) {
        super(null, 20);
        this.humanId = humanID;
        // Now we set up the GUI
    }

    /**
     * Updates all GUI elements. Must be implemented by subclass.
     *
     * @param player    - current player acting.
     * @param gameState - current game state to be used in updating visuals.
     */
    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {

    }
}
