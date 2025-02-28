package games.tickettoride;

import utilities.Hash;

public class TicketToRideConstants {

    public final static String[] cardColors = new String[] {"Red", "Green", "Blue", "Black", "White", "Pink", "Yellow", "Orange", "Locomotive"};


    public final static int ticketToRideBoardHash = Hash.GetInstance().hash("ticketToRideBoard");
    public final static int trainCardDeckHash = Hash.GetInstance().hash("trainCardDeck");
    public final static int trainCardDeckDiscardHash = Hash.GetInstance().hash("trainCardDeckDiscard");
    public final static int trainCarsHash = Hash.GetInstance().hash("trainCars");


    public final static int trainCardsRequiredHash = Hash.GetInstance().hash("trainCardsRequired");

    public final static int destinationCardDeckHash = Hash.GetInstance().hash("destinationCardDeck");
    public final static int playerDestinationHandHash = Hash.GetInstance().hash("playerDestinationHand");

    public final static int destinationHash = Hash.GetInstance().hash("destination");

    public final static int edgeHash = Hash.GetInstance().hash("edge");

    public final static int pointsHash = Hash.GetInstance().hash("points");

    public final static int claimedByPlayerRoute1Hash = Hash.GetInstance().hash("claimedByPlayerRoute1");
    public final static int claimedByPlayerRoute2Hash = Hash.GetInstance().hash("claimedByPlayerRoute2");
    public final static int routeClaimedHash = Hash.GetInstance().hash("routeClaimed");
    public final static int nodesHash = Hash.GetInstance().hash("nodes");

    public final static int location1Hash = Hash.GetInstance().hash("location1");
    public final static int location2Hash = Hash.GetInstance().hash("location2");

}
