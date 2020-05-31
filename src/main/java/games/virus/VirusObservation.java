package games.virus;

import core.components.Deck;
import core.interfaces.IObservation;
import core.interfaces.IPrintable;
import games.virus.cards.VirusCard;
import games.virus.components.VirusBody;

import java.util.List;

public class VirusObservation implements IPrintable, IObservation {
    String[] stringStr;

    public VirusObservation(List<VirusBody> playerBodies, Deck<VirusCard> playerHand) {
        int nPlayers = playerBodies.size();
        stringStr = new String[nPlayers+3];

        stringStr[0] = "----------------------------------------------------";

        for (int i=0; i<nPlayers; i++)
            stringStr[i+1] = "Player " + i + "    -> Body: " + playerBodies.get(i).toString();

        StringBuilder sb = new StringBuilder();
        sb.append("Player Hand -> ");

        for (VirusCard card : playerHand.getComponents()) {
            sb.append(card.toString());
            sb.append(" ");
        }
        stringStr[nPlayers+1] = sb.toString();
        stringStr[nPlayers+2] = "----------------------------------------------------";
    }

    @Override
    public void printToConsole() {
        for (String s : stringStr){
            System.out.println(s);
        }
    }
}
