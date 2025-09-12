package games.powergrid.gui;

import java.awt.Color;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.border.Border;

import java.awt.*;
import javax.swing.*;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.components.Deck;
import gui.AbstractGUIManager;
import gui.GamePanel;
import players.human.ActionController;
import games.powergrid.PowerGridGameState;
import games.powergrid.PowerGridParameters;
import games.powergrid.components.PowerGridCard;
import gui.views.DeckView;
import java.util.Arrays;
import java.util.stream.IntStream;

public class PowerGridGUI extends AbstractGUIManager {
	private PowerGridMapPannel mapPanel;
    private PowerGridDeckView currentMarketView;
    private PowerGridDeckView futureMarketView;
    private PowerGridPlayerPanel[] playerViews; // your own player panel class
    private JPanel playersColumn;
 // in PowerGridGUI fields
    private JPanel[] playerWrappers;


    public PowerGridGUI(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        PowerGridGameState gs = (PowerGridGameState) game.getGameState();
        parent.setLayout(new BorderLayout(8, 8));

        // LEFT: players
        playersColumn = new JPanel();
        playersColumn.setLayout(new BoxLayout(playersColumn, BoxLayout.Y_AXIS));
        playersColumn.setPreferredSize(new Dimension(420, 0));
        parent.add(playersColumn, BorderLayout.WEST);

        // CENTER: map + overlays using a JLayeredPane
        JLayeredPane layered = new JLayeredPane();
        layered.setPreferredSize(new Dimension(1100, 1000));
        parent.add(layered, BorderLayout.CENTER);

        // Background map
        mapPanel = new PowerGridMapPannel();
        mapPanel.setBounds(0, 0, 1100, 1000);
        layered.add(mapPanel, JLayeredPane.DEFAULT_LAYER);

        if (game != null) {
            Rectangle viewArea = new Rectangle(0, 0, 80, 80);
            currentMarketView = new PowerGridDeckView(-1, gs.getCurrentMarket(), true, PowerGridParameters.CARD_ASSET_PATH, viewArea);
            futureMarketView  = new PowerGridDeckView(-1, gs.getFutureMarket(),  true, PowerGridParameters.CARD_ASSET_PATH, viewArea);


            // Place them directly on the layered pane (no wrapper, no border)
            currentMarketView.setBounds(555,  15, 450, 110);  // x,y,w,h — wide enough for 4 cards
            futureMarketView.setBounds (555, 135, 450, 110);

            layered.add(currentMarketView, JLayeredPane.PALETTE_LAYER);
            layered.add(futureMarketView,  JLayeredPane.PALETTE_LAYER);
            layered.revalidate();
            layered.repaint();
        }

     // Players column population
        if (game != null) {
            AbstractGameState s = game.getGameState();
            int n = s.getNPlayers();

            playerViews = new PowerGridPlayerPanel[n];
            playerWrappers = new JPanel[n];

            for (int p = 0; p < n; p++) {
                playerViews[p] = new PowerGridPlayerPanel(p, PLAYER_COLORS[p % PLAYER_COLORS.length]);
                playerViews[p].setAlignmentX(Component.LEFT_ALIGNMENT);

                JPanel wrapper = wrapTitled("Player " + (p + 1), playerViews[p]);
                playerWrappers[p] = wrapper;              // <-- keep the wrapper so we can highlight later
                playersColumn.add(wrapper);
            }
        }

        parent.revalidate();
        parent.repaint();
    }


	@Override
	public int getMaxActionSpace() {
		// TODO Auto-generated method stub
		return 0;
	}

	//called whenever the state changes
	@Override
	protected void _update(AbstractPlayer player, AbstractGameState gameState) {
	    PowerGridGameState gs = (PowerGridGameState) gameState;
	    System.out.printf("[GUI::_update] current=P%d phase=%s live=%s bid=%d on %d money=%s%n",
	            gameState.getCurrentPlayer(),
	            gs.getPhase(),
	            gs.isAuctionLive(),
	            gs.getCurrentBid(),
	            gs.getAuctionPlantNumber(),
	            Arrays.toString(IntStream.range(0, gameState.getNPlayers())
	                    .map(p -> gs.getPlayersMoney(p)).toArray()));

	    // refresh markets
	    currentMarketView.updateComponent(gs.getCurrentMarket());
	    futureMarketView.updateComponent(gs.getFutureMarket());

	    // update each player panel with its own data
	    for (int i = 0; i < playerViews.length; i++) {
	        playerViews[i].setMoney(gs.getPlayersMoney(i));
	        playerViews[i].setPlantDeck(gs.getPlayerPlantDeck(i));
	    }

	    setActivePlayer(gameState.getCurrentPlayer());
	}
	private static JPanel wrapTitled(String title, JComponent inner) {
	    JPanel wrapper = new JPanel(new BorderLayout());
	    wrapper.setOpaque(false);
	    wrapper.setBorder(BorderFactory.createTitledBorder(title));
	    wrapper.add(inner, BorderLayout.CENTER);
	    // ensure the wrapper itself has a size when used with absolute positioning
	    wrapper.setPreferredSize(new Dimension(180, 180));
	    return wrapper;
	}
	
	private static final Color[] PLAYER_COLORS = {
		    new Color(255, 153, 153,200),  // light red / pink
		    new Color(255, 255, 153),  // light yellow
		    new Color(153, 204, 255),  // light blue
		    new Color(204, 153, 255),  // light purple
		    new Color(153, 255, 153),  // light green
		    new Color(224, 224, 224)   // light gray
		};
	private void setActivePlayer(int activeId) {
	    for (int i = 0; i < playerWrappers.length; i++) {
	        Border title = BorderFactory.createTitledBorder("Player " + (i + 1));
	        if (i == activeId) {
	            playerWrappers[i].setBorder(
	                BorderFactory.createCompoundBorder(
	                    BorderFactory.createLineBorder(Color.BLACK, 3),
	                    title
	                )
	            );
	        } else {
	            playerWrappers[i].setBorder(title);
	        }
	    }
	    playersColumn.revalidate();
	    playersColumn.repaint();
	}


}
