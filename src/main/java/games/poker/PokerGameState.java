package games.poker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.ArrayList;
import java.util.List;

import core.AbstractGameState;
import core.AbstractParameters;
import core.CoreConstants;
import core.components.Component;
import core.components.Deck;
import core.components.FrenchCard;
import core.interfaces.IPrintable;
import core.turnorders.AlternatingTurnOrder;
import games.GameType;

import static core.components.FrenchCard.FrenchCardType.*;

public class PokerGameState extends AbstractGameState implements IPrintable {
    List<Deck<FrenchCard>>  playerDecks;
    Deck<FrenchCard>        drawDeck;
    Deck<FrenchCard>        discardDeck;
    FrenchCard              currentCard;
    FrenchCard[]            communityCards;
    String                  currentSuite;
    int                     smallBlind;
    int                     bigBlind;
    int                     previousBet;
    boolean[]               playerCheck = new boolean[getNPlayers()];
    boolean                 blindsFinished;
    int                     totalPotMoney;
    boolean                 checkBets;
    int[]                   playerHand;
    int[]                   currentMoney;
    int                     turnNumber;
    boolean                 equalBets;
    int                     totalPot;


    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers      - number of players for this game.
     */

    public PokerGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new AlternatingTurnOrder(nPlayers), GameType.Poker);
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            addAll(playerDecks);
            add(drawDeck);
            add(discardDeck);
            add(currentCard);
        }};
    }

    boolean isJackCard(FrenchCard card) {
        return card.type == Jack;
    }

    boolean isQueenCard(FrenchCard card) {
        return card.type == Queen;
    }

    boolean isKingCard(FrenchCard card) {
        return card.type == King;
    }

    boolean isAceCard(FrenchCard card) {
        return card.type == Ace;
    }

    public void updateCurrentCard(FrenchCard card) {
        currentCard = card;
    }

    public void updateBlindsFinished() {
        blindsFinished = true;
    }

    public int[] getPlayersMoney() {
        return currentMoney;
    }

    public int getPlayerMoney(int playerId) {
        return currentMoney[playerId];
    }

    public void setCheckToFalse(int players) {
        //System.out.println(Arrays.toString(playerCheck));

        for (int i = 0; i < players; i++) {
            playerCheck[i] = false;
        }
    }

    public void updatePlayerMoney(int playerID, int money) {
        currentMoney[playerID] = currentMoney[playerID] + money;
    }

    public void updateTotalPot(int money) {
        totalPotMoney = totalPotMoney + money;
    }

    public int getTotalPot() {
        return totalPotMoney;
    }

    public Deck<FrenchCard> getCommunityCards() {
        Deck<FrenchCard> temp = new Deck<FrenchCard>("CommunityCards", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
       //if (communityCards[0] == )
        if (getTurnOrder().getRoundCounter() == 2) {
            temp.add(communityCards[0]);
            temp.add(communityCards[1]);
            temp.add(communityCards[2]);
        }
        else if (getTurnOrder().getRoundCounter() == 3) {
            temp.add(communityCards[0]);
            temp.add(communityCards[1]);
            temp.add(communityCards[2]);
            temp.add(communityCards[3]);
        }
        else if (getTurnOrder().getRoundCounter() >= 4) {
            temp.add(communityCards[0]);
            temp.add(communityCards[1]);
            temp.add(communityCards[2]);
            temp.add(communityCards[3]);
            temp.add(communityCards[4]);
        }
        else {
            temp.add(communityCards[0]);
        }

        /*temp.add(communityCards[0]);
        temp.add(communityCards[1]);
        temp.add(communityCards[2]);
        temp.add(communityCards[3]);
        temp.add(communityCards[4]);*/
        //communityCards;
        return temp;
    }

    public void replacePlayerMoney(int playerID, int money) {
        currentMoney[playerID] = money;
    }

    public void updateCurrentCard(FrenchCard card, String suite) {
        currentCard  = card;
        currentSuite = card.suite;
    }

    public boolean isEqualBets() {
        for (int i = 0; i < getNPlayers(); i++) {
            if (currentMoney[0] != currentMoney[i]) {
                return false;
            }
        }
        return true;
    }

    public Deck<FrenchCard> getDrawDeck() {
        return drawDeck;
    }

    public Deck<FrenchCard> getDiscardDeck() {
        return discardDeck;
    }

    public List<Deck<FrenchCard>> getPlayerDecks() {
        return playerDecks;
    }

    public FrenchCard getCurrentCard() {
        return currentCard;
    }

    public String getCurrentSuite() {
        return currentSuite;
    }

    public int getPreviousBet() {
        return previousBet;
    }

    public void setPreviousBet(int bet) {
        previousBet = bet;
    }

    public boolean getCheck(int player) {
        return playerCheck[player];
    }

    public void setCheck(int player, boolean check) {
        playerCheck[player] = check;
    }

    public boolean isCheckEqual() {
        for (int i = 0; i < getNPlayers(); i++) {
            if (getCheck(0) != getCheck(i)) {
                return false;
            }
        }
        return true;
    }

    public int[] getPlayerHand() {
        return playerHand;
    }

    public int[] calculatePlayerHand() {
        PokerGameParameters pgp = (PokerGameParameters) getGameParameters();
        List<Integer> playerHands = new ArrayList<>();

        for (int players = 0; players < getNPlayers(); players++) {
            List<String> currentPlayerCards = new ArrayList<>();
            String[] cardToInt = new String[7];
            List<FrenchCard> localPlayerHand = playerDecks.get(players).getComponents();
            List<FrenchCard> localCommunityCards = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                localCommunityCards.add(communityCards[i]);
            }

            for (int i = 0; i < 7; i++) {
                if (i < 2) {
                    switch (localPlayerHand.get(i).type) {
                        case Number:
                            currentPlayerCards.add(localPlayerHand.get(i).number + "-" + localPlayerHand.get(i).suite);
                            break;
                        case Jack:
                        case Queen:
                        case King:
                        case Ace:
                            currentPlayerCards.add(localPlayerHand.get(i).type.name() + "-" + localPlayerHand.get(i).suite);
                            break;
                    }
                }
                else {
                    switch (localCommunityCards.get(i-2).type) {
                        case Number:
                            currentPlayerCards.add(localCommunityCards.get(i-2).number + "-" + localCommunityCards.get(i-2).suite);
                            break;
                        case Jack:
                        case Queen:
                        case King:
                        case Ace:
                            currentPlayerCards.add(localCommunityCards.get(i-2).type.name() + "-" + localCommunityCards.get(i-2).suite);
                            break;
                    }
                }/*

                else {
                    //FrenchCard.FrenchCardType community = communityCards.get(i-2).type;
                    switch (communityCards[i-2].type) { //should be the player's card instead instead of card.type, replace with the actual card from the player
                        case Number:
                            currentPlayerCards.add(Integer.toString(communityCards[i-2].drawN) + "-" + communityCards[i-2].suite);
                            break;
                        case Jack:
                            currentPlayerCards.add("Jack-" + communityCards[i-2].suite);
                            break;
                        case Queen:
                            currentPlayerCards.add("Queen-" + communityCards[i-2].suite);
                            break;
                        case King:
                            currentPlayerCards.add("King-" + communityCards[i-2].suite);
                            break;
                        case Ace:
                            currentPlayerCards.add("Ace-" + communityCards[i-2].suite);
                            break;
                    }
                }*/
                // Check which hands the player has and then append to the playerHands List
                // which is then used to find who has the highest hand and
                // returns the playerID with the highest hand
                //System.out.println(currentPlayerCards);
                cardToInt = cardsToInteger(currentPlayerCards);

            }
            //System.out.println("cardToInt: " + Arrays.toString(cardToInt));
                try {
                    playerHands.add(Integer.parseInt(handStrengthCalculator(cardToInt)));
                    //System.out.println(Arrays.toString(playerHand));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //playerHands.add(convertedCurrentPlayerCards);
            }

        int[] myArray = new int[playerHands.size()];

        for (int i = 0; i < playerHands.size(); i++) {
            myArray[i] = playerHands.get(i);
        }
        //System.out.println(myArray);
        return myArray;
    }

    public String handStrengthCalculator(String[] pokerHand) throws IOException {
        /*
        Making use of the Poker Hand evaluator created by kennethshackleton
        (https://github.com/kennethshackleton/SKPokerEval)
         */
        String[] temp = pokerHand;
        //System.out.println(Arrays.toString(pokerHand));
        //System.out.println(Arrays.toString(temp));
        String[] ConvertedPokerHand = new String[8];
        String output = "";

        ConvertedPokerHand[0] = "PokerHandCalculator.exe";

        for (int i = 0; i < 7; i++) {

            ConvertedPokerHand[i+1] = temp[i];

        }
        //System.out.println(Arrays.toString(ConvertedPokerHand));

        Process p = Runtime.getRuntime().exec(ConvertedPokerHand);

        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line;
        while ((line = input.readLine()) != null) {
            output = line;
        }

        input.close();
        return output;
    }

    public String[] cardsToInteger (List<String> pokerHand) {
        /*
              #    spacer,  A,  K,  Q,  J, 10, 9,  8,  7,  6,  5,  4,  3,  2
               spade : [ 0, 0,  4,  8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48 ],
                heart: [ 0, 1,  5, 9,  13, 17, 21, 25, 29, 33, 37, 41, 45, 49 ],
              diamond: [ 0, 2,  6, 10, 14, 18, 22, 26, 30, 34, 38, 42, 46, 50 ],
                 club: [ 0, 3,  7, 11, 15, 19, 23, 27, 31, 35, 39, 43, 47, 51 ],
         */

        List<String> cardNumber = new ArrayList<>();
        List<String> cardSuite = new ArrayList<>();
        List<String> convertedCards = new ArrayList<>();

        for (int i = 0; i < pokerHand.size(); i++) {
            String[] temp = pokerHand.get(i).split("-");
            cardNumber.add(temp[0]);
            cardSuite.add(temp[1]);

            if (cardNumber.get(i).equals("Jack")) {
                switch (cardSuite.get(i)) {
                    case "Clubs":
                        convertedCards.add("15");
                        break;

                    case "Diamonds":
                        convertedCards.add("14");
                        break;

                    case "Hearts":
                        convertedCards.add("13");
                        break;

                    case "Spades":
                        convertedCards.add("12");
                        break;
                }
            }
            else if (cardNumber.get(i).equals("Queen")) {
                switch (cardSuite.get(i)) {
                    case "Clubs":
                        convertedCards.add("11");
                        break;

                    case "Diamonds":
                        convertedCards.add("10");
                        break;

                    case "Hearts":
                        convertedCards.add("9");
                        break;

                    case "Spades":
                        convertedCards.add("8");
                        break;
                }

            }
            else if (cardNumber.get(i).equals("King")) {
                switch (cardSuite.get(i)) {
                    case "Clubs":
                        convertedCards.add("7");
                        break;

                    case "Diamonds":
                        convertedCards.add("6");
                        break;

                    case "Hearts":
                        convertedCards.add("5");
                        break;

                    case "Spades":
                        convertedCards.add("4");
                        break;
                }

            }
            else if (cardNumber.get(i).equals("Ace")) {
                switch (cardSuite.get(i)) {
                    case "Clubs":
                        convertedCards.add("3");
                        break;

                    case "Diamonds":
                        convertedCards.add("2");
                        break;

                    case "Hearts":
                        convertedCards.add("1");
                        break;

                    case "Spades":
                        convertedCards.add("0");
                        break;
                }

            }
            else {
                switch (cardNumber.get(i)) {
                    case "2":
                        if (cardSuite.get(i).equals("Clubs")) {
                            convertedCards.add("51");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Diamonds")) {
                            convertedCards.add("50");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Hearts")) {
                            convertedCards.add("49");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Spades")) {
                            convertedCards.add("48");
                            break;
                        }

                    case "3":
                        if (cardSuite.get(i).equals("Clubs")) {
                            convertedCards.add("47");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Diamonds")) {
                            convertedCards.add("46");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Hearts")) {
                            convertedCards.add("45");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Spades")) {
                            convertedCards.add("44");
                            break;
                        }

                    case "4":
                        if (cardSuite.get(i).equals("Clubs")) {
                            convertedCards.add("43");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Diamonds")) {
                            convertedCards.add("42");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Hearts")) {
                            convertedCards.add("41");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Spades")) {
                            convertedCards.add("40");
                            break;
                        }

                    case "5":
                        if (cardSuite.get(i).equals("Clubs")) {
                            convertedCards.add("39");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Diamonds")) {
                            convertedCards.add("38");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Hearts")) {
                            convertedCards.add("37");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Spades")) {
                            convertedCards.add("36");
                            break;
                        }

                    case "6":
                        if (cardSuite.get(i).equals("Clubs")) {
                            convertedCards.add("35");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Diamonds")) {
                            convertedCards.add("34");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Hearts")) {
                            convertedCards.add("33");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Spades")) {
                            convertedCards.add("32");
                            break;
                        }

                    case "7":
                        if (cardSuite.get(i).equals("Clubs")) {
                            convertedCards.add("31");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Diamonds")) {
                            convertedCards.add("30");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Hearts")) {
                            convertedCards.add("29");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Spades")) {
                            convertedCards.add("28");
                            break;
                        }

                    case "8":
                        if (cardSuite.get(i).equals("Clubs")) {
                            convertedCards.add("27");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Diamonds")) {
                            convertedCards.add("26");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Hearts")) {
                            convertedCards.add("25");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Spades")) {
                            convertedCards.add("24");
                            break;
                        }

                    case "9":
                        if (cardSuite.get(i).equals("Clubs")) {
                            convertedCards.add("23");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Diamonds")) {
                            convertedCards.add("22");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Hearts")) {
                            convertedCards.add("21");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Spades")) {
                            convertedCards.add("20");
                            break;
                        }

                    case "10":
                        if (cardSuite.get(i).equals("Clubs")) {
                            convertedCards.add("19");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Diamonds")) {
                            convertedCards.add("18");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Hearts")) {
                            convertedCards.add("17");
                            break;
                        }

                        else if (cardSuite.get(i).equals("Spades")) {
                            convertedCards.add("16");
                            break;
                        }
                }
            }
        }

        String[] myArray = new String[convertedCards.size()];


        //System.out.println("test: " + Arrays.toString(convertedCards.toArray(myArray)));
        return convertedCards.toArray(myArray);
    } //need to call


    @Override
    protected AbstractGameState _copy(int playerId) {
        PokerGameState copy = new PokerGameState(gameParameters.copy(), getNPlayers());
        copy.playerDecks = new ArrayList<>();

        for (Deck<FrenchCard> d : playerDecks) {
            copy.playerDecks.add(d.copy());
        }
        copy.drawDeck = drawDeck.copy();
        copy.discardDeck = discardDeck.copy();
        copy.currentCard = (FrenchCard) currentCard.copy();
        copy.currentSuite = currentSuite;
        copy.currentMoney = currentMoney;
        copy.communityCards = communityCards;
        copy.playerHand = playerHand.clone();
        copy.playerCheck = playerCheck.clone();
        copy.blindsFinished = blindsFinished;
        copy.equalBets = equalBets;
        copy.totalPotMoney = totalPotMoney;
        copy.totalPot = totalPot;
        copy.checkBets = checkBets;
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return new PokerHeuristic().evaluateState(this, playerId);
    }

    @Override
    public double getGameScore(int playerId) {
        return 0;
    }

    /*@Override
    protected double _getScore(int playerId) {

    }*/

    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        return new ArrayList<Integer>() {{
            add(drawDeck.getComponentID());
            for (Component c: drawDeck.getComponents()) {
                add(c.getComponentID());
            }
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    add(playerDecks.get(i).getComponentID());
                    for (Component c: playerDecks.get(i).getComponents()) {
                        add(c.getComponentID());
                    }
                }
            }
        }};
    }

    @Override
    protected void _reset() {
        playerDecks = new ArrayList<>();
        drawDeck = null;
        discardDeck = null;
        currentCard = null;
        currentSuite = null;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PokerGameState)) return false;
        PokerGameState that = (PokerGameState) o;
        return Objects.equals(playerDecks, that.playerDecks) &&
                Objects.equals(drawDeck, that.drawDeck) &&
                Objects.equals(discardDeck, that.discardDeck) &&
                Objects.equals(currentCard, that.currentCard) &&
                Objects.equals(currentSuite, that.currentSuite) &&
                Objects.equals(currentMoney, that.currentMoney) &&
                Objects.equals(playerHand, that.playerHand) &&
                Arrays.equals(playerCheck, that.playerCheck) &&
                Objects.equals(blindsFinished, that.blindsFinished) &&
                Objects.equals(equalBets, that.equalBets) &&
                Objects.equals(checkBets, that.checkBets) &&
                Objects.equals(totalPotMoney, that.totalPotMoney) &&
                Objects.equals(totalPot, that.totalPot) &&
                Arrays.equals(playerHand, that.playerHand);
    }
    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), playerDecks, drawDeck, discardDeck, currentCard, currentSuite);
        result = 31 * result + Arrays.hashCode(playerHand);
        return result;
    }

    @Override
    public void printToConsole() {
        String[] strings = new String[7];


        for (int i = 0; i < getNPlayers(); i++) {
            totalPot = totalPot + getPlayerMoney(i);
        }

        strings[0] = "\nTotal Pot: "+String.valueOf(getTotalPot());
        strings[1] = "Current money: " + currentMoney[getCurrentPlayer()] + " Current player: " + (getCurrentPlayer() + 1);
        strings[2] = "----------------------------------------------------";

        if (turnOrder.getRoundCounter() == 2) {
            strings[3] = "Current community cards: " + communityCards[0] + " " + communityCards[1] + " " + communityCards[2];
        }
        else if (turnOrder.getRoundCounter() == 3) {
            strings[3] = "Current community cards: " + communityCards[0] + " " + communityCards[1] + " " + communityCards[2] + " " + communityCards[3];
        }
        else if (turnOrder.getRoundCounter() >= 4) {
            strings[3] = "Current community cards: " + communityCards[0] + " " + communityCards[1] + " " + communityCards[2] + " " + communityCards[3] + " " + communityCards[4];
        }
        else {
            strings[3] = "Current community cards: ";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Player Hand : ");

        for (FrenchCard card : playerDecks.get(getCurrentPlayer()).getComponents()) {
            sb.append(card.toString());
            sb.append(" ");
        }
        strings[4] = sb.toString();
        strings[5] = "----------------------------------------------------";
        strings[6] = "Actions that can be taken:";

        for (String s : strings){
            System.out.println(s);
        }
    }

}
