package games.risk;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.risk.actions.ChooseAttack;
import games.risk.actions.PlaceArmy;
import games.risk.gui.RiskMapView;
import gui.AbstractGUIManager;
import gui.GamePanel;
import org.junit.Before;
import org.junit.Test;
import players.human.ActionController;
import players.human.HumanGUIPlayer;
import players.simple.RandomPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.List;

import static games.risk.RiskTestUtils.*;
import static games.risk.WorldMap.*;
import static org.junit.Assert.*;

/**
 * Acting on the Risk GUI's map with the mouse: player 0 (a human) holds Alaska and Alberta, player 1 everything else.
 */
public class RiskMapClickTest {

    ActionController ac = new ActionController();
    Game game;
    RiskGameState state;
    AbstractGUIManager gui;
    RiskMapView map;

    @Before
    public void setup() {
        game = GameType.Risk.createGameInstance(3, 7);
        game.reset(List.of(new HumanGUIPlayer(ac), new RandomPlayer(), new RandomPlayer()));
        state = (RiskGameState) game.getGameState();
        fillBoard(state, 1);
        give(state, 0, 6, ALASKA, ALBERTA);
        GamePanel panel = new GamePanel();
        gui = GameType.Risk.createGUIManager(panel, game, ac);
        map = find(panel, RiskMapView.class);
        map.setSize(map.getPreferredSize());
    }

    static <T> T find(Container c, Class<T> type) {
        for (Component child : c.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container inner && find(inner, type) != null) return find(inner, type);
        }
        return null;
    }

    /** Shows the state with its actions offered to the human player, as the game loop would. */
    void show() {
        gui.update(game.getPlayers().get(0), state, true);
    }

    void click(RiskTerritory t, int button) {
        Shape shape = map.territoryShape(t);
        Rectangle2D b = shape.getBounds2D();
        for (double x = b.getMinX(); x < b.getMaxX(); x++)
            for (double y = b.getMinY(); y < b.getMaxY(); y++)
                if (shape.contains(x, y) && t.equals(map.territoryAt(new Point((int) x, (int) y)))) {
                    map.dispatchEvent(new MouseEvent(map, MouseEvent.MOUSE_CLICKED, 0, 0, (int) x, (int) y, 1, false,
                            button));
                    return;
                }
        fail("no point in " + t);
    }

    AbstractAction chosen() throws InterruptedException {
        assertTrue("nothing chosen", ac.hasAction());
        return ac.getAction();
    }

    @Test
    public void aClickOnATerritoryPlacesArmiesThere() throws Exception {
        startPlay(state, 0, RiskGamePhase.REINFORCE, 3);
        show();
        click(ALBERTA, MouseEvent.BUTTON1);
        AbstractAction a = chosen();
        assertTrue(a instanceof PlaceArmy p && p.territory.equals(ALBERTA));
    }

    @Test
    public void anAttackIsAClickOnTheAttackerAndThenOnTheDefender() throws Exception {
        startPlay(state, 0, RiskGamePhase.ATTACK, 0);
        show();
        click(ALBERTA, MouseEvent.BUTTON1);
        assertFalse(ac.hasAction());
        click(ONTARIO, MouseEvent.BUTTON1);
        assertEquals(new ChooseAttack(ALBERTA, ONTARIO), chosen());
    }

    @Test
    public void aRightClickDropsTheTerritoryPicked() {
        startPlay(state, 0, RiskGamePhase.ATTACK, 0);
        show();
        click(ALBERTA, MouseEvent.BUTTON1);
        click(ALBERTA, MouseEvent.BUTTON3);
        click(ONTARIO, MouseEvent.BUTTON1);
        assertFalse(ac.hasAction());
    }
}
