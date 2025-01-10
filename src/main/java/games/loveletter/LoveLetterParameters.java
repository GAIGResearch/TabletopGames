package games.loveletter;

import core.AbstractParameters;
import core.Game;
import evaluation.optimisation.TunableParameters;
import games.GameType;
import games.loveletter.cards.CardType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

/** This class allows to modify the balance of the game. Each modification may result in several problems with the
 * game's logic. Please be very careful in modifying these values and check for possible consequences.
 */
public class LoveLetterParameters extends TunableParameters {

    String dataPath = "data/loveletter/";

    // Occurrence count for each card
    public HashMap<CardType, Integer> cardCounts = new HashMap<CardType, Integer>() {{
        put(CardType.Princess, 1);
        put(CardType.Countess, 1);
        put(CardType.King, 1);
        put(CardType.Prince, 2);
        put(CardType.Handmaid, 2);
        put(CardType.Baron, 2);
        put(CardType.Priest, 2);
        put(CardType.Guard, 5);
    }};

    // How many cards each player draws
    public int nCardsPerPlayer = 1;
    // How many cards are put on the side (visible) in minimum player games
    public int nCardsVisibleReserve = 3;
    // Affection tokens required to win, maps to number of players (2, 3, 4+)
    public int nTokensWin2 = 7;
    public int nTokensWin3 = 5;
    public int nTokensWin4 = 4;

    public LoveLetterParameters() {
        addTunableParameter("nCardsPerPlayer", 1, Arrays.asList(1,2,3));
        addTunableParameter("nCardsVisibleReserve", 3, Arrays.asList(1,2,3,4,5));
        addTunableParameter("nTokensWin2", 7, Arrays.asList(3,4,5,6,7,8,9,10));
        addTunableParameter("nTokensWin3", 5, Arrays.asList(3,4,5,6,7,8,9,10));
        addTunableParameter("nTokensWin4", 4, Arrays.asList(3,4,5,6,7,8,9,10));
        for (CardType c: cardCounts.keySet()) {
            addTunableParameter(c.name() + " count", cardCounts.get(c), Arrays.asList(1,2,3,4,5));
        }
        _reset();
    }

    @Override
    public void _reset() {
        nCardsPerPlayer = (int) getParameterValue("nCardsPerPlayer");
        nCardsVisibleReserve = (int) getParameterValue("nCardsVisibleReserve");
        nTokensWin2 = (int) getParameterValue("nTokensWin2");
        nTokensWin3 = (int) getParameterValue("nTokensWin3");
        nTokensWin4 = (int) getParameterValue("nTokensWin4");
        cardCounts.replaceAll((c, v) -> (Integer) getParameterValue(c.name() + " count"));
    }

    public String getDataPath() {
        return dataPath;
    }

    @Override
    protected AbstractParameters _copy() {
        return new LoveLetterParameters();
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoveLetterParameters that = (LoveLetterParameters) o;
        return nCardsPerPlayer == that.nCardsPerPlayer && nCardsVisibleReserve == that.nCardsVisibleReserve && nTokensWin2 == that.nTokensWin2 && nTokensWin3 == that.nTokensWin3 && nTokensWin4 == that.nTokensWin4 && Objects.equals(dataPath, that.dataPath) && Objects.equals(cardCounts, that.cardCounts);
    }

    @Override
    public Object instantiate() {
        return new Game(GameType.LoveLetter, new LoveLetterForwardModel(), new LoveLetterGameState(this, GameType.LoveLetter.getMinPlayers()));
    }
}
