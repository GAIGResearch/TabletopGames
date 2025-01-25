package games.tickettoride;

import utilities.Hash;

public class TicketToRideConstants {

    public enum cardColors {
        Red, Green, Blue, Black, White, Purple, Yellow, Orange, Locomotive
    }


    public final static int ticketToRideBoardHash = Hash.GetInstance().hash("ticketToRideBoard");
    public final static int trainCardDeckHash = Hash.GetInstance().hash("trainCardDeck");
    public final static int playerDeckDiscardHash = Hash.GetInstance().hash("Player Deck Discard");
    public final static int trainCarsHash = Hash.GetInstance().hash("trainCars");

    public final static int destinationCardDeckHash = Hash.GetInstance().hash("destinationCardDeck");
    public final static int playerDestinationHandHash = Hash.GetInstance().hash("playerDestinationHand");

    public final static int destinationHash = Hash.GetInstance().hash("destination");

    public final static int edgeHash = Hash.GetInstance().hash("edge");

    public final static int pointsHash = Hash.GetInstance().hash("points");

    public final static int claimedByHash = Hash.GetInstance().hash("claimedByPlayer");
    public final static int routeClaimedHash = Hash.GetInstance().hash("routeClaimed");
    public final static int nodesHash = Hash.GetInstance().hash("nodes");

    public final static int location1Hash = Hash.GetInstance().hash("location1");
    public final static int location2Hash = Hash.GetInstance().hash("location2");

}
