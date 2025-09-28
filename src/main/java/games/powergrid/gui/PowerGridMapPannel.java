package games.powergrid.gui;

import java.awt.Graphics;
import java.awt.Image;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;

import utilities.ImageIO;
//TODO implement Scaling
public class PowerGridMapPannel extends JComponent {
	
	private final Image backgroundImage;
	private final Map<Integer, Image> regionMasks = Map.of(
		    1, ImageIO.GetInstance().getImage("data/powergrid/yellow_player.png"),
		    2, ImageIO.GetInstance().getImage("data/powergrid/red_player.png"),
		    3, ImageIO.GetInstance().getImage("data/powergrid/green_player.png"),
		    4, ImageIO.GetInstance().getImage("data/powergrid/blue_player.png")


		);
	private Set<Integer> disabledRegions = Set.of();
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
  

}
