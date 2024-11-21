package games.tickettoride;

import utilities.Hash;

public class TicketToRideConstants {

    public enum cardColors {
        Red, Green, Blue, Black, White, Purple, Yellow, Orange, Locomotive
    }


    public final static int ticketToRideBoardHash = Hash.GetInstance().hash("ticketToRideBoard");
    public final static int trainCardDeckHash = Hash.GetInstance().hash("trainCardDeck");
    public final static int playerDeckDiscardHash = Hash.GetInstance().hash("Player Deck Discard");

    public final static int destinationHash = Hash.GetInstance().hash("destination");

    public final static int edgeHash = Hash.GetInstance().hash("edge");

}
