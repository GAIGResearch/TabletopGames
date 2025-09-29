package games.powergrid.gui;

import java.awt.Graphics;
import java.awt.Image;
import java.util.Map;
import java.util.Set;
import java.awt.*;

import javax.swing.JComponent;

import utilities.ImageIO;
//TODO implement Scaling
public class PowerGridMapPannel extends JComponent {
	
	private final Image backgroundImage;
	private final Map<Integer, Image> regionMasks = Map.of(
			1, ImageIO.GetInstance().getImage("data/powergrid/region_1.png"),
			2, ImageIO.GetInstance().getImage("data/powergrid/region_2.png"),
			3, ImageIO.GetInstance().getImage("data/powergrid/region_3.png"),
			4, ImageIO.GetInstance().getImage("data/powergrid/region_4.png"),
			5, ImageIO.GetInstance().getImage("data/powergrid/region_5.png"),
			6, ImageIO.GetInstance().getImage("data/powergrid/region_6.png"),
		    7, ImageIO.GetInstance().getImage("data/powergrid/region_7.png")
		);
    private static final Point[] CITY_COORDS_NATIVE = new java.awt.Point[] {
            // index = cityId
            new java.awt.Point(1037, 221), // 0 Quebec
            new java.awt.Point(1017, 275), // 1 Montreal
            new java.awt.Point(1041, 335), // 2 Boston
            new java.awt.Point(950, 347), // 3 New_York
            new java.awt.Point(941, 431), // 4 Philadelphia
            new java.awt.Point(916, 273), // 5 Ottawa
            new java.awt.Point(826, 301), // 6 Toronto
            new java.awt.Point(786, 343), // 7 Detroit
            new java.awt.Point(847, 387), // 8 Pittsburgh
            new java.awt.Point(779, 427), // 9 Columbus
            new java.awt.Point(870, 475), // 10 Washington
            new java.awt.Point(831, 544), // 11 Charlotte 
            new java.awt.Point(690, 474), // 12 Nashville
            new java.awt.Point(751, 583), // 13 Atlanta
            new java.awt.Point(784, 651), // 14 Jacksonville
            new java.awt.Point(789, 720), // 15 Miami
            new java.awt.Point(647, 640), // 16 New_Orleans
            new java.awt.Point(538, 633),  // 17 Houston
            new java.awt.Point(501, 580),  // 18 DallasFort_Worth
            new java.awt.Point(465, 682), // 19 San_Antonio
            new java.awt.Point(619, 541), // 20 Memphis
            new java.awt.Point(479, 522),  // 21 Oklahoma_City
            new java.awt.Point(593, 448),  // 22 StLouis
            new java.awt.Point(678, 403), // 23 Indianapolis
            new java.awt.Point(653, 348), // 24 Chicago
            new java.awt.Point(641, 296), // 25 Milwaukee
            new java.awt.Point(495, 418),  // 26 Kansas City
            new java.awt.Point(525, 278),  // 27 Minneapolis
            new java.awt.Point(478, 153),  // 28 Winnipeg
            new java.awt.Point(324, 153),  // 29 Regina
            new java.awt.Point(205, 27),   // 30 Edmonton
            new java.awt.Point(159, 88),  // 31 Calgary
            new java.awt.Point(73, 145),  // 32 Vancouver
            new java.awt.Point(78, 211),  // 33 Seattle
            new java.awt.Point(76, 277),  // 34 Portland
            new java.awt.Point(241, 341),  // 35 Salt_Lake_City
            new java.awt.Point(326, 399),  // 36 Denver
            new java.awt.Point(195, 435),  // 37 Las_Vegas
            new java.awt.Point(63, 405),  // 38 San_Francisco
            new java.awt.Point(100, 523),  // 39 Los_Angeles
            new java.awt.Point(147, 584),  // 40 San_Diego
            new java.awt.Point(312, 537),  // 41 Albuquerque
            new java.awt.Point(310, 606),  // 42 Juarez
            new java.awt.Point(328, 690), // 43 Chihuahua
            new java.awt.Point(447, 740), // 44 Monterrey
            new java.awt.Point(418, 803), // 45 Guadalajara
            new java.awt.Point(527, 816)  // 46 Mexico_City
        };
    private static final Point[] GENERATOR_TRACK = new java.awt.Point[] {
            // index = cityId
            new java.awt.Point(621, 759), // 0 
            new java.awt.Point(621, 797), // 1
            new java.awt.Point(684, 797), // 2 
            new java.awt.Point(738, 797), // 3 
            new java.awt.Point(792, 797), // 4 
            new java.awt.Point(847, 797), // 5 
            new java.awt.Point(901, 797), // 6 
            new java.awt.Point(955, 797), // 7 
            new java.awt.Point(1009, 797), // 8 
            new java.awt.Point(1066, 797), // 9 
            new java.awt.Point(607, 836), // 10 
            new java.awt.Point(649, 836), // 11
            new java.awt.Point(691, 836), // 12 
            new java.awt.Point(736, 836), // 13
            new java.awt.Point(780, 836), // 14
            new java.awt.Point(820, 836), // 15
            new java.awt.Point(861, 836), // 16 
            new java.awt.Point(903, 836),  // 17 
            new java.awt.Point(944, 836),  // 18 
            new java.awt.Point(986, 836), // 19 
            new java.awt.Point(1028, 836),  // 20 
            new java.awt.Point(1071, 836), // 21 
            
        };
    
	private Set<Integer> disabledRegions = Set.of();
	private int[][] citySlotsById = null;
	private int[] cityCountByPlayer = null;

	// Player colors (playerId -> Color)
	private Map<Integer, Color> playerColors = Map.of();

	// Call these from your GUI manager when state changes:
	public void setCitySlotsById(int[][] slots) {
	    this.citySlotsById = slots;
	    repaint();
	}
	
	public void setCityCountByPlayer(int[] counts) {
	    this.cityCountByPlayer = counts;
	    repaint();   // force a redraw so drawGeneratorTrack() sees the new data
	}
	public void setPlayerColors(Map<Integer, Color> colors) {
	    this.playerColors = (colors == null) ? Map.of() : colors;
	    repaint();
	}
	
	
	public void setDisabledRegions(Set<Integer> regions) {
	    this.disabledRegions = regions;
	    repaint();
	}

	
	public PowerGridMapPannel() {
		backgroundImage = ImageIO.GetInstance().getImage("data/powergrid/us_map.png");
	}
    private void drawBackgroundImage(Graphics g) {
        if (backgroundImage != null) {
            drawImage(g, backgroundImage, 0, 0, 1100, 1000);
        } else {
        }
    }
    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        drawBackgroundImage(g);

        for (int r : disabledRegions) {
            Image mask = regionMasks.get(r);
            if (mask != null) g.drawImage(mask, 0, 0, 1100, 1000, null);
        }

        // Draw ownership markers (no scaling)
        drawOwnershipSquares((Graphics2D) g);
        drawGeneratorTrack((Graphics2D)g);
    }

    
    private void drawImage(Graphics g, Image img, int x, int y, int width, int height) {
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        double scaleW = width * 1.0 / w;
        double scaleH = height * 1.0 / h;
        g.drawImage(img, x, y, (int) (w * scaleW), (int) (h * scaleH), null);
    }
	public double getScaleX() {
		return 1.0;
	}
	public double getScaleY() {
		
		return 1.0;
	}
	
	private void drawOwnershipSquares(Graphics2D g2) {
	    if (citySlotsById == null) return;

	    final int size = 15; // square size
	    final int pad  = 8;  // gap between squares
	    final int lift = 6; // offset above center of city
	    final int left_offset = -30;

	    int maxCity = Math.min(CITY_COORDS_NATIVE.length, citySlotsById.length);
	    for (int cityId = 0; cityId < maxCity; cityId++) {
	        Point base = CITY_COORDS_NATIVE[cityId];
	        int[] slots = citySlotsById[cityId];
	        if (slots == null) continue;

	        int drawn = 0;
	        for (int s = 0; s < slots.length; s++) {
	            int pid = slots[s];
	            if (pid < 0) continue; 

	            Color fill = playerColors.getOrDefault(pid, Color.GRAY);

	            int x = (base.x + left_offset) + drawn * (size + pad);
	            int y = base.y - lift;

	            g2.setColor(fill);
	            g2.fillRect(x, y, size, size);

	            g2.setColor(Color.BLACK);
	            g2.drawRect(x, y, size, size);

	            drawn++;
	        }
	    }
	}
	

	private void drawGeneratorTrack(Graphics2D g2) {
	    if (cityCountByPlayer == null || GENERATOR_TRACK == null) return;

	    // Smooth edges for nicer circles
	    Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
	    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

	    // Build: count -> list of players at that count
	    Map<Integer, java.util.List<Integer>> byCount = new java.util.HashMap<>();
	    for (int pid = 0; pid < cityCountByPlayer.length; pid++) {
	        int raw = cityCountByPlayer[pid];
	        int count = clamp(raw, 0, GENERATOR_TRACK.length - 1);
	        byCount.computeIfAbsent(count, k -> new java.util.ArrayList<>()).add(pid);
	    }

	    final int radius = 12;               // circle radius in px
	    final int diam   = radius * 2;
	    final int pad    = -12;               // spacing between markers negative so they overlap
	    final int liftY  = -3;              

	    for (Map.Entry<Integer, java.util.List<Integer>> e : byCount.entrySet()) {
	        int count = e.getKey();
	        java.awt.Point base = GENERATOR_TRACK[count];
	        java.util.List<Integer> players = e.getValue();
	        java.util.Collections.sort(players); 

	        int m = players.size();
	        int step = diam + pad; 
	        double startOffset = -((m - 1) * step) / 2.0;

	        for (int j = 0; j < m; j++) {
	            int pid = players.get(j);
	            int cx = (int) Math.round(base.x + startOffset + j * step);
	            int cy = base.y + liftY;

	            Color fill = playerColors.get(pid);
	            g2.setColor(fill);
	            g2.fillOval(cx - radius, cy - radius, diam, diam);

	            g2.setColor(Color.BLACK);
	            g2.setStroke(new BasicStroke(2f));
	            g2.drawOval(cx - radius, cy - radius, diam, diam);

	            String label = "P" + pid;

	            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
	            FontMetrics fm = g2.getFontMetrics();

	            int tx = cx - fm.stringWidth(label) / 2;
	            int ty = cy + (fm.getAscent() - fm.getDescent()) / 2;

	            g2.setPaint(Color.BLACK);
	            g2.drawString(label, tx, ty);
	        }
	    }
	    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
	}

	private int clamp(int v, int lo, int hi) {
	    return (v < lo) ? lo : (v > hi) ? hi : v;
	}
  

}
