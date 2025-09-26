package games.powergrid.gui;

import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JComponent;

import utilities.ImageIO;

public class PowerGridMarketView extends JComponent {
	
	private final Image backgroundImage;
	
	public PowerGridMarketView() {
		//TODO
	}
    private void drawBackgroundImage(Graphics g) {
        if (backgroundImage != null) {
            drawImage(g, backgroundImage, 0, 0, 1100, 1000);
        } else {
//            System.out.println("NO IMAGE");
        }
    }
    private void drawImage(Graphics g, Image backgroundImage2, int i, int j, int k, int l) {
		// TODO Auto-generated method stub
		
	}
	@Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        drawBackgroundImage(g);
        //drawGraph(g, gameState);
    }
    
}
