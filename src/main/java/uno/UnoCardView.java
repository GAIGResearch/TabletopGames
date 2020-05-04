package uno;

import components.Card;
import content.Property;
import content.PropertyString;
import utilities.Hash;
import utilities.ImageIO.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class UnoCardView  extends JComponent {
    private Image  image;
    private String path = "data/uno/";
    private Card   card;
    private int    width = 107;
    private int    height = 150;

    public UnoCardView(Card c)  {
        this.card = c;
        // load image depending on the card
        // TODO USE path propierty
        Property type   = card.getProperty(Hash.GetInstance().hash("type"));
        Property number = card.getProperty(Hash.GetInstance().hash("number"));
        Property color  = card.getProperty(Hash.GetInstance().hash("color"));

        LoadImage( ((PropertyString) type).value, ((PropertyString) color).value, ((PropertyString) number).value);

    }

    private void LoadImage(String type, String color, String number) {
        String imageFileName = "";

        switch (type) {
            case "Ordinary":
                imageFileName = path + color + number + ".png";
                break;
            case "Draw2":
                imageFileName = path + color + "Draw2.png";
                break;
            case "SkipTurn":
                imageFileName = path + color + "SkipTurn.png";
                break;
            case "ChangeDirection":
                imageFileName = path + color + "ChangeDirection.png";
                break;
            case "ChangeColor":
                imageFileName = path + "ChangeColor.png";
                break;
            case "ChangeColor4":
                imageFileName = path + "ChangeColor4.png";
                break;
        }
        try {
            image = ImageIO.read(new File(imageFileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawCard((Graphics2D) g, width, height, card, 0, 0);
    }

    private static void drawCard(Graphics2D g, int width, int height, Card card, int x, int y) {
        // TODO Draw card
    }

    public void update(Card c) {
        this.card = c;
        // load image depending on the card
        Property type   = card.getProperty(Hash.GetInstance().hash("type"));
        Property number = card.getProperty(Hash.GetInstance().hash("number"));
        Property color  = card.getProperty(Hash.GetInstance().hash("color"));

        LoadImage( ((PropertyString) type).value, ((PropertyString) color).value, ((PropertyString) number).value);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }
}
