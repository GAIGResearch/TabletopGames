package explodingkittens;

import actions.*;
import components.*;
import core.Area;
import core.Game;
import core.GameState;
import explodingkittens.actions.*;
import utilities.Hash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ExplodingKittensGameState extends GameState {

    public static int playerHandHash = Hash.GetInstance().hash("playerHand");
    public static int cardTypeHash = Hash.GetInstance().hash("cardTypeHash");
    public static int drawPileHash = Hash.GetInstance().hash("drawPileHash");
    public static int discardPileHash = Hash.GetInstance().hash("discardPileHash");

    public int numAvailableActions = 0;
    public int remainingDraws = 1;

    public int playerAskingForFavorID;

    public boolean[] playerActive;
    public int nPlayersActive;

    public ExplodingKittensGamePhase gamePhase = ExplodingKittensGamePhase.PlayerMove;

    HashMap<ExplodingKittenCard, Integer> cardCounts = new HashMap<ExplodingKittenCard, Integer>() {{
        put(ExplodingKittenCard.ATTACK, 4);
        put(ExplodingKittenCard.SKIP, 4);
        put(ExplodingKittenCard.FAVOR, 4);
        put(ExplodingKittenCard.SHUFFLE, 4);
        put(ExplodingKittenCard.SEETHEFUTURE, 5);
        put(ExplodingKittenCard.TACOCAT, 4);
        put(ExplodingKittenCard.MELONCAT, 4);
        put(ExplodingKittenCard.BEARDCAT, 4);
        put(ExplodingKittenCard.RAINBOWCAT, 4);
        put(ExplodingKittenCard.FURRYCAT, 4);
        put(ExplodingKittenCard.NOPE, 5);
        put(ExplodingKittenCard.DEFUSE, 6);
        put(ExplodingKittenCard.EXPLODING_KITTEN, nPlayers-1);
    }};

    public ExplodingKittensGameState() {
    }

    public void killPlayer(int playerID){
        playerActive[playerID] = false;
        nPlayersActive -= 1;
        remainingDraws = 1;
    }

    public void setup(Game game)
    {
        playerActive = new boolean[nPlayers];
        for (int i = 0; i < nPlayers; i++) playerActive[i] = true;
        nPlayersActive = nPlayers;

        Deck deck = new Deck("DrawDeck");
        // add all cards and distribute 7 random cards to each player
        for (HashMap.Entry<ExplodingKittenCard, Integer> entry : cardCounts.entrySet()) {
            if (entry.getKey() == ExplodingKittenCard.DEFUSE || entry.getKey() == ExplodingKittenCard.EXPLODING_KITTEN)
                continue;
            for (int i = 0; i < entry.getValue(); i++) {
                Card card = new Card();
                card.addProperty(cardTypeHash, new ExplodingKittensCardTypeProperty(entry.getKey()));
                deck.add(card);
            }
        }
        deck.shuffle();

        // For each player, initialize their own areas: they get a player hand and a player card
        // give each player a defuse card and seven random cards from the deck
        for (int i = 0; i < nPlayers; i++) {
            Area playerArea = new Area();
            playerArea.setOwner(i);
            String deckname = "Player" + i + "HandCards";
            Deck playerCards = new Deck(deckname);
            game.addDeckToList(playerCards);

            //add defuse card
            Card defuse =  new Card();
            defuse.addProperty(cardTypeHash, new ExplodingKittensCardTypeProperty(ExplodingKittenCard.DEFUSE));
            playerCards.add(defuse);

            // add 7 random cards from the deck
            for (int j = 0; j < 7; j++)
                playerCards.add(deck.draw());

            playerArea.addComponent(playerHandHash, playerCards); // there is not hand card limit
            areas.put(i, playerArea);
        }

        // add remaining defuse cards and exploding kitten cards to the deck and shuffle again
        for (int i = nPlayers; i < 6; i++){
            Card defuse = new Card();
            defuse.addProperty(cardTypeHash, new ExplodingKittensCardTypeProperty(ExplodingKittenCard.DEFUSE));
            deck.add(defuse);
        }
        for (int i = 0; i < nPlayers-1; i++){
            Card explodingKitten = new Card();
            explodingKitten.addProperty(cardTypeHash, new ExplodingKittensCardTypeProperty(ExplodingKittenCard.EXPLODING_KITTEN));
            deck.add(explodingKitten);
        }
        deck.shuffle();

        // setup drawPile area
        Area drawPile = new Area();
        drawPile.setOwner(-1);
        drawPile.addComponent(drawPileHash, deck);
        areas.put(drawPileHash, drawPile);

        // setup discardPile area
        Area discardPile = new Area();
        discardPile.setOwner(-1);
        Deck discardDeck = new Deck("DiscardDeck");
        discardPile.addComponent(discardPileHash, discardDeck);
        areas.put(discardPileHash, discardPile);

        game.addDeckToList(deck);
        game.addDeckToList(discardDeck);

        // add them to the list of decks, so they are accessible by the game.findDeck() function
        //game.addDeckToList(playerDeck);
        //game.addDeckToList(infDiscard);
        //game.addDeckToList(playerDiscard);
    }

    @Override
    public GameState copy() {
        //TODO: copy exploding kitten game state
        return this;
    }

    @Override
    public int nInputActions() {
        return 1;  // Exploding Kittens allows the player to repeatedly do actions until he played a card
     }

    @Override
    public int nPossibleActions() {
        return this.numAvailableActions;
    }

    private ArrayList<Action> defuseActions(){
        ArrayList<Action> actions = new ArrayList<>();
        Deck playerDeck = findDeck("Player"+activePlayer+"HandCards");
        Deck drawDeck = findDeck("DrawDeck");

        for (int i = 0; i <= drawDeck.getCards().size(); i++){
            actions.add(new PlaceExplodingKittenAction(activePlayer, playerDeck.peek(), i));
        }
        return actions;
    }

    private ArrayList<Action> nopeActions(){
        ArrayList<Action> actions = new ArrayList<>();
        Deck playerDeck = findDeck("Player"+activePlayer+"HandCards");
        for (Card card : playerDeck.getCards()) {
            if (((ExplodingKittensCardTypeProperty) card.getProperty(cardTypeHash)).value == ExplodingKittenCard.NOPE) {
                actions.add(new NopeAction(activePlayer, card));
            }
            break;
        }
        actions.add(new PassAction(activePlayer));
        return actions;
    }

    private ArrayList<Action> favorActions(){
        ArrayList<Action> actions = new ArrayList<>();
        Deck playerDeck = findDeck("Player"+activePlayer+"HandCards");
        for (Card card : playerDeck.getCards()) {
            actions.add(new GiveCardAction(card, activePlayer, playerAskingForFavorID));
        }
        return actions;
    }

    private ArrayList<Action> playerActions(){
        ArrayList<Action> actions = new ArrayList<>();
        Deck playerDeck = findDeck("Player"+activePlayer+"HandCards");

        // todo: only add unique actions
        for (Card card : playerDeck.getCards()) {
            switch (((ExplodingKittensCardTypeProperty) card.getProperty(cardTypeHash)).value) {
                case DEFUSE:
                    break;
                case EXPLODING_KITTEN:
                    break;
                case NOPE:
                    break;
                case SKIP:
                    actions.add(new SkipAction(activePlayer, card,
                            nextPlayerToDraw(activePlayer)));
                    break;
                case FAVOR:
                    for (int player = 0; player < nPlayers; player++) {
                        if (player == activePlayer)
                            continue;
                        if (findDeck("Player"+player+"HandCards").getCards().size() > 0)
                            actions.add(new FavorAction(activePlayer, card, player));
                    }
                    break;
                case ATTACK:
                    for (int player = 0; player < nPlayers; player++) {

                        if (player == activePlayer || !playerActive[player])
                            continue;

                        actions.add(new AttackAction(activePlayer, card, player));
                    }
                    break;
                case SHUFFLE:
                    actions.add(new ShuffleAction(activePlayer, card));
                    break;
                case TACOCAT:
                    break;
                case BEARDCAT:
                    break;
                case FURRYCAT:
                    break;
                case MELONCAT:
                    break;
                case RAINBOWCAT:
                    break;
                case SEETHEFUTURE:
                    actions.add(new SeeTheFutureAction(activePlayer, card));
                    break;
                default:
                    System.out.println("No actions known for cardtype" +
                            ((ExplodingKittensCardTypeProperty) card.getProperty(cardTypeHash)).value.toString());
            }
        }
        /* todo add special combos
        // can take any card from anyone
        for (int i = 0; i < nPlayers; i++){
            if (i != activePlayer){
                Deck otherDeck = (Deck)this.areas.get(activePlayer).getComponent(playerHandHash);
                for (Card card: otherDeck.getCards()){
                    actions.add(new TakeCard(card, i));
                }
            }
        }*/

        // add end turn by drawing a card
        actions.add(new DrawExplodingKittenCard(activePlayer, findDeck("DrawDeck"), playerDeck));
        return actions;
    }

    @Override
    public List<Action> possibleActions() {
        ArrayList<Action> actions;
        // todo the actions per player do not change a lot in between two turns
        // i would strongly recommend to update an existing list instead of generating a new list everytime we query this function
        switch (gamePhase){
            case PlayerMove:
                actions = playerActions();
                break;
            case DefusePhase:
                actions = defuseActions();
                break;
            case NopePhase:
                actions = nopeActions();
                break;
            case FavorPhase:
                actions = favorActions();
                break;
            default:
                actions = new ArrayList<>();
                break;
        }

        this.numAvailableActions = actions.size();
        return actions;
    }

    public int nextPlayer(int currentPlayer){
        int nextPlayer = currentPlayer;
        for (int i = 0; i < nPlayers; i++)
        {
            nextPlayer = (nextPlayer + 1) % nPlayers;
            if (playerActive[nextPlayer])
                return nextPlayer;
        }
        return currentPlayer;
    }

    public int nextPlayerToDraw(int currentPlayer){
        int nextPlayer = currentPlayer;
        if (remainingDraws > 1)
            return nextPlayer;
        for (int i = 0; i < nPlayers; i++)
        {
            nextPlayer = (nextPlayer + 1) % nPlayers;
            if (playerActive[nextPlayer])
                return nextPlayer;
        }
        return currentPlayer;
    }


    public void setActivePlayer(int nextActivePlayer) {
        this.activePlayer = nextActivePlayer;
    }

    public void print() {
        System.out.println("Exploding Kittens Game-State");
        System.out.println("============================");
        for (int i = 0; i < nPlayers; i++){
            System.out.print("Player " + i + ":");
            Deck playerDeck = ((Deck)this.areas.get(i).getComponent(playerHandHash));
            printDeck(playerDeck);
        }

        System.out.print("DrawPile" + ":");
        Deck drawPile = (Deck)this.areas.get(drawPileHash).getComponent(drawPileHash);
        printDeck(drawPile);

        System.out.print("DiscardPile" + ":");
        Deck discardPile = (Deck)this.areas.get(discardPileHash).getComponent(discardPileHash);
        printDeck(discardPile);
        System.out.println("" + remainingDraws + " remaining draws");
    }

    public void printDeck(Deck deck){
        StringBuilder sb = new StringBuilder();
        for (Card card : deck.getCards()){
            sb.append(((ExplodingKittensCardTypeProperty)card.getProperty(cardTypeHash)).value.toString());
            sb.append(",");
        }
        if (sb.length() > 0) sb.deleteCharAt(sb.length()-1);
        System.out.println(sb.toString());
        //System.out.println();
    }

    public boolean isGameOver(){
        int activePlayers = 0;
        for (int i = 0; i < nPlayers; i++)
            if (this.playerActive[i]) activePlayers += 1;
        return activePlayers <= 1;
    }
}
