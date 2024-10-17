package games.blackjack;

import core.AbstractGameState;
import core.AbstractParameters;
import core.CoreConstants;
import core.components.Component;
import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.GameType;

import java.util.ArrayList;
import java.util.*;

public class BlackjackGameState extends AbstractGameState implements IPrintable {
    List<PartialObservableDeck<FrenchCard>> playerDecks;
    Deck<FrenchCard> drawDeck;
    int dealerPlayer;

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players for this game.
     */
    public BlackjackGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Blackjack;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            addAll(playerDecks);
            add(drawDeck);
        }};
    }


    public Deck<FrenchCard> getDrawDeck() {
        return drawDeck;
    }

    public List<PartialObservableDeck<FrenchCard>> getPlayerDecks() {
        return playerDecks;
    }

    public int getDealerPlayer() {
        return dealerPlayer;
    }

    public int calculatePoints(int playerID) {
        BlackjackParameters params = (BlackjackParameters) gameParameters;
        int points = 0;
        int aces = 0;

        for (FrenchCard card : playerDecks.get(playerID).getComponents()) {
            switch (card.type) {
                case Number:
                    points += card.number;
                    break;
                case Jack:
                    points += params.jackCard;
                    break;
                case Queen:
                    points += params.queenCard;
                    break;
                case King:
                    points += params.kingCard;
                    break;
                case Ace:
                    aces++;
                    break;
            }
        }
        if (aces > 0) {
            int nAcePointMin = aces * params.aceCardBelowThreshold;
            int nAcePointMax = aces * params.aceCardAboveThreshold;
            if (points + nAcePointMax > params.winScore) {
                points += nAcePointMin;
            } else if (points + nAcePointMax >= params.dealerStand) {
                points += nAcePointMax;
            } else {
                points += nAcePointMin;
            }
        }

        return points;
    }


    @Override
    protected AbstractGameState _copy(int playerId) {
        BlackjackGameState copy = new BlackjackGameState(gameParameters.copy(), getNPlayers());
        copy.playerDecks = new ArrayList<>();
        for (PartialObservableDeck<FrenchCard> d : playerDecks) {
            copy.playerDecks.add(d.copy());
        }
        copy.drawDeck = drawDeck.copy();
        if (getCoreGameParameters().partialObservable && playerId != -1) {
            // some cards in dealer's deck are hidden
            for (int i = 0; i < copy.playerDecks.get(dealerPlayer).getSize(); i++) {
                if (!copy.playerDecks.get(dealerPlayer).getVisibilityForPlayer(i, playerId)) {
                    copy.drawDeck.add(copy.playerDecks.get(dealerPlayer).pick(i));
                }
            }
            copy.drawDeck.shuffle(new Random(copy.gameParameters.getRandomSeed()));
            for (int i = 0; i < copy.playerDecks.get(dealerPlayer).getSize(); i++) {
                if (!copy.playerDecks.get(dealerPlayer).getVisibilityForPlayer(i, playerId)) {
                    copy.playerDecks.get(dealerPlayer).add(copy.drawDeck.draw());
                }
            }
        }
        copy.dealerPlayer = dealerPlayer;
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        CoreConstants.GameResult playerResult = getPlayerResults()[playerId];
        if (playerResult == CoreConstants.GameResult.LOSE_GAME)
            return -1;
        if (playerResult == CoreConstants.GameResult.WIN_GAME)
            return 1;

        // if game not over, return the score scaled by the maximum score possible
        return getGameScore(playerId) / ((BlackjackParameters) gameParameters).winScore;
    }

    @Override
    public double getGameScore(int playerId) {
        int rawPoints = calculatePoints(playerId);
        if (rawPoints > ((BlackjackParameters) gameParameters).winScore)
            return 0;
        return rawPoints;
    }

    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        return new ArrayList<Integer>() {{
            add(drawDeck.getComponentID());
            for (Component c : drawDeck.getComponents()) {
                add(c.getComponentID());
            }
            for (int i = 0; i < playerDecks.get(dealerPlayer).getSize(); i++) {
                if (!playerDecks.get(dealerPlayer).isComponentVisible(i, playerId)) {
                    add(playerDecks.get(dealerPlayer).get(i).getComponentID());
                }
            }
        }};
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlackjackGameState)) return false;
        if (!super.equals(o)) return false;
        BlackjackGameState that = (BlackjackGameState) o;
        return dealerPlayer == that.dealerPlayer && Objects.equals(playerDecks, that.playerDecks) && Objects.equals(drawDeck, that.drawDeck);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerDecks, drawDeck, dealerPlayer);
    }

    @Override
    public void printToConsole() {
        String[] strings = new String[4];

        strings[0] = "Player      : " + getCurrentPlayer();
        strings[1] = "Points      : " + calculatePoints(getCurrentPlayer());
        StringBuilder sb = new StringBuilder();
        sb.append("Player Hand : ");


        for (FrenchCard card : playerDecks.get(getCurrentPlayer()).getComponents()) {
            sb.append(card.toString());
            sb.append(" ");
        }
        strings[2] = sb.toString();
        strings[3] = "----------------------------------------------------";

        for (String s : strings) {
            System.out.println(s);
        }
    }

}
