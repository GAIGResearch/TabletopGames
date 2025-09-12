package games.powergrid.gui;

import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JComponent;

import utilities.ImageIO;

public class PowerGridMapPannel extends JComponent {
	
	private final Image backgroundImage;
	
	public PowerGridMapPannel() {
		backgroundImage = ImageIO.GetInstance().getImage("data/powergrid/us_map.png");
	}
    private void drawBackgroundImage(Graphics g) {
        if (backgroundImage != null) {
            drawImage(g, backgroundImage, 0, 0, 1100, 1000);
        } else {
//            System.out.println("NO IMAGE");
        }
    }
    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        drawBackgroundImage(g);
        //drawGraph(g, gameState);
    }
    
    private void drawImage(Graphics g, Image img, int x, int y, int width, int height) {
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        double scaleW = width * 1.0 / w;
        double scaleH = height * 1.0 / h;
        g.drawImage(img, x, y, (int) (w * scaleW), (int) (h * scaleH), null);
    }

}
