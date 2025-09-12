package games.powergrid;

import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;

import core.components.Component;
import core.components.Deck;
import games.powergrid.components.PowerGridCard;
import games.powergrid.components.PowerGridGraphBoard;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
public class powergridtest {

    public static void main(String[] args) {
    	int nPlayers = 3;
    	Random rnd = new Random();
    	int x = rnd.nextInt(10);
    	//Random rnd = gameState.getRandomGenerator();
    	//int nPlayers = gameState.getNPlayers();
    	 Deck<PowerGridCard> finalDeck = setupDecks(nPlayers, rnd);

    	    // Print it out to check
    	    System.out.println("YAAW (top to bottom):");
    	    int idx = 0;
    	    for (Component c : finalDeck.getComponents()) {
    	        System.out.println((idx++) + ": " + c);
    	    }
        	
    }
    
    
    
    public static Deck<PowerGridCard> setupDecks(int nPlayers, Random rnd) {
        Deck<PowerGridCard> drawPile = new Deck<>("Draw", HIDDEN_TO_ALL);
        Deck<PowerGridCard> tempPile = new Deck<>("Temp", HIDDEN_TO_ALL);

        // Add all cards
        PowerGridParameters params = new PowerGridParameters();
        for (PowerGridCard card : params.plantsIncludedInGame) {
            drawPile.add(card);
        }
        drawPile.add(PowerGridCard.step3());

        // Separate step 3 + low-number plants
        PowerGridCard step3Card = null;
        List<PowerGridCard> snap = new ArrayList<>(drawPile.getComponents());
        for (PowerGridCard c : snap) {
            if (c.type == PowerGridCard.Type.STEP3) {
                step3Card = c;
                drawPile.remove(c);
            } else if (c.type == PowerGridCard.Type.PLANT && c.getNumber() <= 15) {
                drawPile.remove(c);
                tempPile.addToBottom(c);
            }
        }

        // Shuffle piles
        tempPile.shuffle(rnd);
        drawPile.shuffle(rnd);

        // Remove cards based on player count
        if (nPlayers == 2) {
            tempPile.draw();
            for (int k = 0; k < 5; k++) drawPile.draw();
        } else if (nPlayers == 3) {
            tempPile.draw();
            tempPile.draw();
            for (int k = 0; k < 6; k++) drawPile.draw();
        } else if (nPlayers == 4) {
            tempPile.draw();
            for (int k = 0; k < 3; k++) drawPile.draw();
        }

        // Recombine: tempPile on top of drawPile
        List<PowerGridCard> buf = new ArrayList<>();
        while (tempPile.getSize() > 0) {
            buf.add(tempPile.draw());
        }
        for (int i = buf.size() - 1; i >= 0; i--) {
            drawPile.add(buf.get(i));
        }

        // Add step 3 card to bottom
        if (step3Card != null) {
            drawPile.addToBottom(step3Card);
        }

        return drawPile;
    }

}

