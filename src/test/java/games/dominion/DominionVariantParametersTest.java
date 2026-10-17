package games.dominion;

import core.AbstractParameters;
import games.GameType;
import games.dominion.cards.CardType;
import org.junit.Test;

import java.util.List;

import static games.dominion.cards.CardType.*;
import static org.junit.Assert.*;

public class DominionVariantParametersTest {

    static final List<CardType> FIRST_GAME = List.of(CELLAR, MARKET, MERCHANT, MILITIA, MINE, MOAT, REMODEL, SMITHY, VILLAGE, WORKSHOP);
    static final List<CardType> SIZE_DISTORTION = List.of(ARTISAN, BANDIT, BUREAUCRAT, CHAPEL, FESTIVAL, GARDENS, SENTRY, THRONE_ROOM, WITCH, CURSE, WORKSHOP);
    static final List<CardType> IMPROVEMENTS = List.of(ARTISAN, CELLAR, MARKET, MERCHANT, MINE, MOAT, MONEYLENDER, POACHER, REMODEL, WITCH, CURSE);

    private List<CardType> cards(GameType gameType) {
        return ((DominionParameters) gameType.createParameters(1)).cardsUsed;
    }

    @Test
    public void variantsUseOnlyTheirOwnKingdom() {
        assertEquals(FIRST_GAME, cards(GameType.Dominion));
        assertEquals(FIRST_GAME, cards(GameType.DominionFG));
        assertEquals(SIZE_DISTORTION, cards(GameType.DominionSizeDistortion));
        assertEquals(IMPROVEMENTS, cards(GameType.DominionImprovements));
    }

    @Test
    public void jsonFilesDefineTheSameKingdoms() {
        assertEquals(SIZE_DISTORTION, ((DominionParameters) AbstractParameters.createFromFile(GameType.Dominion, "data/dominion/SizeDistortion.json")).cardsUsed);
        assertEquals(IMPROVEMENTS, ((DominionParameters) AbstractParameters.createFromFile(GameType.Dominion, "data/dominion/Improvements.json")).cardsUsed);
    }

    @Test
    public void copyKeepsTheKingdom() {
        DominionParameters fromJson = (DominionParameters) AbstractParameters.createFromFile(GameType.Dominion, "data/dominion/SizeDistortion.json");
        assertEquals(SIZE_DISTORTION, ((DominionParameters) fromJson.copy()).cardsUsed);
        assertEquals(IMPROVEMENTS, ((DominionParameters) new DominionIParameters().copy()).cardsUsed);
    }

    @Test
    public void changingAnotherParameterKeepsTheKingdom() {
        // setParameterValue calls _reset(), which rebuilds cardsUsed from the CARDS parameter
        DominionParameters params = new DominionSDParameters();
        params.setParameterValue("HAND_SIZE", 7);
        assertEquals(SIZE_DISTORTION, params.cardsUsed);
        assertEquals(7, params.HAND_SIZE);
    }
}
