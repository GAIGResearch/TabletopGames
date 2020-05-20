package games.explodingkittens;

import core.gamephase.GamePhase;
import core.gamephase.DefaultGamePhase;
import core.ForwardModel;
import core.actions.IAction;
import core.components.Deck;
import core.components.IDeck;
import core.AbstractGameState;
import core.components.PartialObservableDeck;
import games.explodingkittens.cards.ExplodingKittenCard;
import games.explodingkittens.actions.*;
import core.observations.IObservation;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ExplodingKittensGameState extends AbstractGameState {

    public enum ExplodingKittensGamePhase implements GamePhase {
        Nope,
        Defuse,
        Favor,
        SeeTheFuture
    }

    private List<PartialObservableDeck<ExplodingKittenCard>> playerHandCards;
    private PartialObservableDeck<ExplodingKittenCard> drawPile;
    private Deck<ExplodingKittenCard> discardPile;
    private int playerGettingAFavor = -1;

    public static boolean PARTIAL_OBSERVABLE = false;

    public int getPlayerGettingAFavor() {
        return playerGettingAFavor;
    }

    public void setPlayerGettingAFavor(int playerGettingAFavor) {
        this.playerGettingAFavor = playerGettingAFavor;
    }

    public Deck<ExplodingKittenCard> getDiscardPile() {
        return discardPile;
    }

    public ExplodingKittensGameState(ExplodingKittenParameters gameParameters, ForwardModel model, int nPlayers) {
        super(gameParameters, model, nPlayers, new ExplodingKittenTurnOrder(nPlayers));
        gamePhase = DefaultGamePhase.Main;
    }

    public void killPlayer(int playerID){
        setPlayerResult(Utils.GameResult.GAME_LOSE, playerID);
        int nPlayersActive = 0;
        for (int i = 0; i < getNPlayers(); i++) {
            if (playerResults[i] == Utils.GameResult.GAME_ONGOING) nPlayersActive++;
        }
        if (nPlayersActive == 1) {
            this.gameStatus = Utils.GameResult.GAME_END;
        }
    }

    public void setComponents() {
        ExplodingKittenParameters ekp = (ExplodingKittenParameters)gameParameters;
        drawPile = new PartialObservableDeck<>("Draw Pile", getNPlayers());

        // add all cards and distribute 7 random cards to each player
        for (HashMap.Entry<ExplodingKittenCard.CardType, Integer> entry : ekp.cardCounts.entrySet()) {
            if (entry.getKey() == ExplodingKittenCard.CardType.DEFUSE || entry.getKey() == ExplodingKittenCard.CardType.EXPLODING_KITTEN)
                continue;
            for (int i = 0; i < entry.getValue(); i++) {
                ExplodingKittenCard card = new ExplodingKittenCard(entry.getKey());
                drawPile.add(card);
            }
        }
        drawPile.shuffle();

        // For each player, initialize their own areas: they get a player hand and a player card
        // give each player a defuse card and seven random cards from the deck
        playerHandCards = new ArrayList<>(getNPlayers());
        for (int i = 0; i < getNPlayers(); i++) {
            boolean[] visibility = new boolean[getNPlayers()];
            Arrays.fill(visibility, !PARTIAL_OBSERVABLE);
            visibility[i] = true;

            PartialObservableDeck<ExplodingKittenCard> playerCards =
                    new PartialObservableDeck<>("Player Cards", visibility);
            playerHandCards.add(playerCards);

            //add defuse card
            ExplodingKittenCard defuse =  new ExplodingKittenCard(ExplodingKittenCard.CardType.DEFUSE);
            playerCards.add(defuse);

            // add 7 random cards from the deck
            for (int j = 0; j < 7; j++)
                playerCards.add(drawPile.draw());
        }

        // add remaining defuse cards and exploding kitten cards to the deck and shuffle again
        for (int i = getNPlayers(); i < 6; i++){
            ExplodingKittenCard defuse =  new ExplodingKittenCard(ExplodingKittenCard.CardType.DEFUSE);
            drawPile.add(defuse);
        }
        for (int i = 0; i < getNPlayers()-1; i++){
            ExplodingKittenCard explodingKitten = new ExplodingKittenCard(ExplodingKittenCard.CardType.EXPLODING_KITTEN);
            drawPile.add(explodingKitten);
        }
        drawPile.shuffle();

        // setup discardPile area
        discardPile = new Deck<>("Discard Pile");
    }

    private ArrayList<IAction> defuseActions(int playerID){
        ArrayList<IAction> actions = new ArrayList<>();
        Deck<ExplodingKittenCard> playerDeck = playerHandCards.get(playerID);
        ExplodingKittenCard kitten = playerDeck.peek();
        for (int i = 0; i <= drawPile.getSize(); i++){
            actions.add(new PlaceExplodingKittenAction<>(kitten, playerDeck, drawPile, i));
        }
        return actions;
    }

    private ArrayList<IAction> nopeActions(int playerID){
        ArrayList<IAction> actions = new ArrayList<>();
        Deck<ExplodingKittenCard> playerDeck = playerHandCards.get(playerID);
        for (ExplodingKittenCard card : playerDeck.getCards()) {
            if (card.cardType == ExplodingKittenCard.CardType.NOPE) {
                actions.add(new NopeAction<>(card, playerDeck, discardPile, playerID));
            }
            break;
        }
        actions.add(new PassAction(playerID));
        return actions;
    }

    private ArrayList<IAction> favorActions(int playerID){
        ArrayList<IAction> actions = new ArrayList<>();
        Deck<ExplodingKittenCard> playerDeck = playerHandCards.get(playerID);
        Deck<ExplodingKittenCard> receiverDeck = playerHandCards.get(playerGettingAFavor);
        for (ExplodingKittenCard card : playerDeck.getCards()) {
            actions.add(new GiveCardAction(card, playerDeck, receiverDeck));
        }
        return actions;
    }

    private ArrayList<IAction> seeTheFutureActions(int playerID){
        ArrayList<IAction> actions = new ArrayList<>();
        ArrayList<ExplodingKittenCard> cards = drawPile.getCards();
        int numberOfCards = drawPile.getSize();
        actions.add(new ChooseSeeTheFutureOrder(drawPile, 1 >= numberOfCards ? null : cards.get(1),
                2 >= numberOfCards ? null : cards.get(2), 3 >= numberOfCards? null : cards.get(3), playerID));
        actions.add(new ChooseSeeTheFutureOrder(drawPile, 1 >=numberOfCards ? null : cards.get(1),
                3 >= numberOfCards ? null : cards.get(3), 2 >= numberOfCards? null : cards.get(2), playerID));
        actions.add(new ChooseSeeTheFutureOrder(drawPile, 2 >=numberOfCards ? null : cards.get(2),
                1 >= numberOfCards ? null : cards.get(1), 3 >= numberOfCards? null : cards.get(3), playerID));
        actions.add(new ChooseSeeTheFutureOrder(drawPile, 2 >=numberOfCards ? null : cards.get(2),
                3 >= numberOfCards ? null : cards.get(3), 1 >= numberOfCards? null : cards.get(1), playerID));
        actions.add(new ChooseSeeTheFutureOrder(drawPile, 3 >=numberOfCards ? null : cards.get(3),
                1 >= numberOfCards ? null : cards.get(1), 2 >= numberOfCards? null : cards.get(2), playerID));
        actions.add(new ChooseSeeTheFutureOrder(drawPile, 3 >=numberOfCards ? null : cards.get(3),
                2 >= numberOfCards ? null : cards.get(2), 1 >= numberOfCards? null : cards.get(1), playerID));

        return actions;
    }

    private ArrayList<IAction> playerActions(int playerID){
        ArrayList<IAction> actions = new ArrayList<>();
        Deck<ExplodingKittenCard> playerDeck = playerHandCards.get(playerID);

        // todo: only add unique actions
        for (ExplodingKittenCard card : playerDeck.getCards()) {
            switch (card.cardType) {
                case DEFUSE:
                case MELONCAT:
                case RAINBOWCAT:
                case FURRYCAT:
                case BEARDCAT:
                case TACOCAT:
                case NOPE:
                case EXPLODING_KITTEN:
                    break;
                case SKIP:
                    actions.add(new SkipAction<>(card, playerDeck, discardPile));
                    break;
                case FAVOR:
                    for (int player = 0; player < getNPlayers(); player++) {
                        if (player == playerID)
                            continue;
                        if (playerHandCards.get(player).getSize() > 0)
                            actions.add(new FavorAction<>(card, playerDeck, discardPile, player, playerID));
                    }
                    break;
                case ATTACK:
                    for (int targetPlayer = 0; targetPlayer < getNPlayers(); targetPlayer++) {

                        if (targetPlayer == playerID || playerResults[targetPlayer] != Utils.GameResult.GAME_ONGOING)
                            continue;

                        actions.add(new AttackAction<>(card, playerDeck, discardPile, targetPlayer));
                    }
                    break;
                case SHUFFLE:
                    actions.add(new ShuffleAction<>(card, playerDeck, discardPile, drawPile));
                    break;
                case SEETHEFUTURE:
                    actions.add(new SeeTheFutureAction<>(card, playerDeck, discardPile, playerID, drawPile));
                    break;
                default:
                    System.out.println("No core.actions known for cardtype: " + card.cardType.toString());
            }
        }
        /* todo add special combos
        // can take any card from anyone
        for (int i = 0; i < nPlayers; i++){
            if (i != activePlayer){
                Deck otherDeck = (Deck)this.areas.get(activePlayer).getComponent(playerHandHash);
                for (Card card: otherDeck.getCards()){
                    core.actions.add(new TakeCard(card, i));
                }
            }
        }*/

        // add end turn by drawing a card
        actions.add(new DrawExplodingKittenCard(playerID, drawPile, playerDeck));
        return actions;
    }

    @Override
    public IObservation getObservation(int player) {
        return new ExplodingKittenObservation(playerHandCards, drawPile, discardPile, player);
    }

    @Override
    public void endGame() {
        this.gameStatus = Utils.GameResult.GAME_END;
        for (int i = 0; i < getNPlayers(); i++){
            if (playerResults[i] == Utils.GameResult.GAME_ONGOING)
                playerResults[i] = Utils.GameResult.GAME_WIN;
        }
    }

    @Override
    public List<IAction> computeAvailableActions() {

        ArrayList<IAction> actions;
        // todo the actions per player do not change a lot in between two turns
        // i would strongly recommend to update an existing list instead of generating a new list everytime we query this function
        int player = getTurnOrder().getCurrentPlayer(this);
        if (DefaultGamePhase.Main.equals(gamePhase)) {
            actions = playerActions(player);
        } else if (ExplodingKittensGamePhase.Defuse.equals(gamePhase)) {
            actions = defuseActions(player);
        } else if (ExplodingKittensGamePhase.Nope.equals(gamePhase)) {
            actions = nopeActions(player);
        } else if (ExplodingKittensGamePhase.Favor.equals(gamePhase)) {
            actions = favorActions(player);
        } else if (ExplodingKittensGamePhase.SeeTheFuture.equals(gamePhase)) {
            actions = seeTheFutureActions(player);
        } else {
            actions = new ArrayList<>();
        }

        return actions;
    }


    public void print(ExplodingKittenTurnOrder turnOrder) {
        System.out.println("Exploding Kittens Game-State");
        System.out.println("============================");

        int currentPlayer = turnOrder.getCurrentPlayer(this);

        for (int i = 0; i < getNPlayers(); i++){
            if (currentPlayer == i)
                System.out.print(">>> Player " + i + ":");
            else
                System.out.print("Player " + i + ":");
            printDeck(playerHandCards.get(i));
        }

        System.out.print("DrawPile" + ":");
        printDeck(drawPile);

        System.out.print("DiscardPile" + ":");
        printDeck(discardPile);

        System.out.println("Current GamePhase: " + gamePhase);
        System.out.println("Missing Draws: " + turnOrder.requiredDraws);
    }

    public void printDeck(IDeck<ExplodingKittenCard> deck){
        StringBuilder sb = new StringBuilder();
        for (ExplodingKittenCard card : deck.getCards()){
            sb.append(card.cardType.toString());
            sb.append(",");
        }
        if (sb.length() > 0) sb.deleteCharAt(sb.length()-1);
        System.out.println(sb.toString());
        //System.out.println();
    }
}
