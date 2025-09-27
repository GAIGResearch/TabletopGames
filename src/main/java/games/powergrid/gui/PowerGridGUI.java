package games.powergrid.gui;

import java.awt.Color;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.border.Border;

import java.awt.*;
import javax.swing.*;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;

import gui.AbstractGUIManager;
import gui.GamePanel;
import players.human.ActionController;
import games.powergrid.PowerGridGameState;
import games.powergrid.PowerGridParameters;

import gui.IScreenHighlight;


public class PowerGridGUI extends AbstractGUIManager {
	private PowerGridMapPannel mapPanel;
    private PowerGridDeckView currentMarketView;
    private PowerGridDeckView futureMarketView;
    private PowerGridPlayerPanel[] playerViews; 
    private JPanel playersColumn;
    private ResourceMarketOverlay marketOverlay;
    private TurnOrderOverlay turnOverlay;



 // in PowerGridGUI fields
    private JPanel[] playerWrappers;
    private static final java.awt.Point[][] SLOT_COORDS_NATIVE = new java.awt.Point[][]{
        // COAL row 
        { 	new java.awt.Point(1061, 894), new java.awt.Point(1037, 894), //9
        	new java.awt.Point(958, 894),new java.awt.Point(934, 894), //8
        	new java.awt.Point(879, 894), new java.awt.Point(855, 894), //7
          new java.awt.Point(801, 894), new java.awt.Point(777, 894),new java.awt.Point(754, 894),//6
          new java.awt.Point(722, 894),new java.awt.Point(698, 894),new java.awt.Point(674, 894),//5
          new java.awt.Point(644,  894), new java.awt.Point(620,  894), new java.awt.Point(596,  894), //4 
          new java.awt.Point(565,  893), new java.awt.Point(541,  893), new java.awt.Point(517,  893), new java.awt.Point(493,  893), //3 
          new java.awt.Point(462,  892), new java.awt.Point(438,  892), new java.awt.Point(414,  892), new java.awt.Point(390,  892), //2 
          new java.awt.Point(359,  892), new java.awt.Point(335,  892), new java.awt.Point(311,  892), new java.awt.Point(287,  892)//1
          },

        // GAS row
        {
		    new java.awt.Point(958,  920), new java.awt.Point(934,  920),new java.awt.Point(910,  920), //8
		    new java.awt.Point(879,  920), new java.awt.Point(855,  920),new java.awt.Point(831,  920), //7
		    new java.awt.Point(801,  920), new java.awt.Point(777,  920), new java.awt.Point(754,  920), //6
		    new java.awt.Point(722,  919), new java.awt.Point(698,  919), new java.awt.Point(674,  919), //5
		    new java.awt.Point(644,  919), new java.awt.Point(620,  919), new java.awt.Point(596,  919), //4 
		    new java.awt.Point(565,  919), new java.awt.Point(541,  919), new java.awt.Point(517,  919), //3 
		    new java.awt.Point(462,  919), new java.awt.Point(438,  919), new java.awt.Point(414,  919), //2 
		    new java.awt.Point(360,  919), new java.awt.Point(336,  919), new java.awt.Point(312,  919),  //1 
        },

        // OIL row
        {
        	new java.awt.Point(1061, 949), new java.awt.Point(1037, 949),new java.awt.Point(1013, 949), new java.awt.Point(989, 949), //9
            new java.awt.Point(958,  949), new java.awt.Point(934,  949), //8
            new java.awt.Point(881,  948), new java.awt.Point(857,  948), //7
            new java.awt.Point(803,  948), new java.awt.Point(779,  948), //6
            new java.awt.Point(724,  948), new java.awt.Point(700,  948), //5
            new java.awt.Point(644,  948), new java.awt.Point(620,  948),  //4 
            new java.awt.Point(565,  947), new java.awt.Point(541,  947), //3 
            new java.awt.Point(462,  947), new java.awt.Point(438,  947),  //2 
            new java.awt.Point(360,  947), new java.awt.Point(336,  947),  //1 
        },



        // URANIUM row
        {
            new java.awt.Point(1061, 975), new java.awt.Point(1037, 975), //9
            new java.awt.Point(958,  975), new java.awt.Point(934,  975), //8
            new java.awt.Point(881,  976), new java.awt.Point(857,  976), //7
            new java.awt.Point(803,  976),  //6
            new java.awt.Point(724,  976),  //5
            new java.awt.Point(644,  976),  //4 
            new java.awt.Point(565,  974),  //3 
            new java.awt.Point(462,  974),  //2 
            new java.awt.Point(360,  974),   //1 
        },
    };
    private java.util.EnumMap<PowerGridParameters.Resource, Integer>
    buildAvail(PowerGridGameState gs) {
        var m = new java.util.EnumMap<PowerGridParameters.Resource, Integer>(PowerGridParameters.Resource.class);
        for (PowerGridParameters.Resource r : PowerGridParameters.Resource.values()) {
            m.put(r, gs.getResourceMarket().getAvailable(r)); // or your accessor
        }
        return m;
    }

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
        layered.setPreferredSize(new Dimension(1100, 1120));
        parent.add(layered, BorderLayout.CENTER);

        // Background map
        mapPanel = new PowerGridMapPannel();
        mapPanel.setBounds(0, 0, 1100, 1120);
        layered.add(mapPanel, JLayeredPane.DEFAULT_LAYER);
        
        // Turn order overlay 
        turnOverlay = new TurnOrderOverlay();      
        turnOverlay.setBounds(mapPanel.getBounds());
        turnOverlay.setOpaque(false);
        turnOverlay.setAnchorPoint(new Point(1021, 510));

        layered.add(turnOverlay, Integer.valueOf(60));

        // Keep overlays sized with the layered pane
        layered.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                Dimension sz = layered.getSize();
                mapPanel.setBounds(0, 0, sz.width, sz.height);
                marketOverlay.setBounds(0, 0, sz.width, sz.height);
                turnOverlay.setBounds(0, 0, sz.width, sz.height);
                layered.revalidate();
                layered.repaint();
            }
        });
        
        
        
        // Resource market overlay 
        var gs0 = (PowerGridGameState) game.getGameState();
        marketOverlay = new ResourceMarketOverlay(
                mapPanel,
                SLOT_COORDS_NATIVE,
                buildAvail(gs0)  // initial availability
        );
        marketOverlay.setBounds(mapPanel.getBounds());
        marketOverlay.setOpaque(false);

        
        layered.add(marketOverlay, Integer.valueOf(50));
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
        int topWidth = playersColumn.getPreferredSize().width + layered.getPreferredSize().width;

        JPanel infoPanel = createGameStateInfoPanel(
                "Power Grid",
                (PowerGridGameState) game.getGameState(),
                topWidth,
                defaultInfoPanelHeight  // provided by AbstractGUIManager
        );
        parent.add(infoPanel, BorderLayout.NORTH);

	     // (Optional) If you also want the default action buttons bar at the bottom:
	     JComponent actionPanel = createActionPanel(new IScreenHighlight[0], topWidth, defaultActionPanelHeight);
	     parent.add(actionPanel, BorderLayout.SOUTH);
	
	     // Make sure the overall preferred size accounts for the new bars
	     parent.setPreferredSize(new Dimension(
	             topWidth,
	             layered.getPreferredSize().height + defaultInfoPanelHeight + /* + defaultActionPanelHeight if added */ 0 + 10
	     ));
	     parent.revalidate();
	     parent.repaint();
       

     // Players column population
        if (game != null) {
            AbstractGameState s = game.getGameState();
            int n = s.getNPlayers();

            playerViews = new PowerGridPlayerPanel[n];
            playerWrappers = new JPanel[n];

            for (int p = 0; p < n; p++) {
                playerViews[p] = new PowerGridPlayerPanel(p, PLAYER_COLORS[p % PLAYER_COLORS.length]);
                playerViews[p].setAlignmentX(Component.LEFT_ALIGNMENT);

                JPanel wrapper = wrapTitled("Player " + p, playerViews[p]);
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
	    

	    // refresh markets
	    currentMarketView.updateComponent(gs.getCurrentMarket());
	    futureMarketView.updateComponent(gs.getFutureMarket());
	    marketOverlay.updateComponent(gs.resourceMarket.snapshot());
	    turnOverlay.setTurnOrder(buildTurnEntries(gs));

	    // update each player panel with its own data
	    for (int i = 0; i < playerViews.length; i++) {
	        playerViews[i].setMoney(gs.getPlayersMoney(i));
	        playerViews[i].setPlantDeck(gs.getPlayerPlantDeck(i));
	        playerViews[i].setResources(
	        		gs.getFuel(i, PowerGridParameters.Resource.COAL),
	        		gs.getFuel(i, PowerGridParameters.Resource.OIL),
	        		gs.getFuel(i, PowerGridParameters.Resource.GAS),
	        		gs.getFuel(i, PowerGridParameters.Resource.URANIUM));
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
	        Border title = BorderFactory.createTitledBorder("Player " + (i));
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
	
	private java.util.List<TurnOrderOverlay.Entry> buildTurnEntries(PowerGridGameState gs) {
	    
	    final java.util.List<Integer> turnOrder = gs.getTurnOrder();   
	    final java.util.List<Integer> roundOrderList = gs.getRoundOrder(); 

	    
	    final java.util.List<Integer> safeTurnOrder = (turnOrder != null) ? turnOrder : java.util.List.of();
	    final java.util.Set<Integer> roundOrder =
	            (roundOrderList != null) ? new java.util.HashSet<>(roundOrderList) : java.util.Set.of();

	    final java.util.List<TurnOrderOverlay.Entry> list = new java.util.ArrayList<>(safeTurnOrder.size());

	    for (int pid : safeTurnOrder) {
	        boolean active = roundOrder.contains(pid);
	        Color c = PLAYER_COLORS[pid % PLAYER_COLORS.length];
	        String label = "P" + (pid);
	        list.add(new TurnOrderOverlay.Entry(label, c, active));
	    }
	    return list;
	}




}
