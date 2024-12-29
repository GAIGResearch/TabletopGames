package games.wonders7;

import games.wonders7.Wonders7Constants.TradeSource;
import games.wonders7.actions.PlayCard;
import games.wonders7.cards.Wonder7Board;
import games.wonders7.cards.Wonder7Card;
import org.junit.Before;
import org.junit.Test;
import utilities.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static games.wonders7.Wonders7Constants.Resource.*;
import static games.wonders7.Wonders7Constants.createCardHash;
import static games.wonders7.cards.Wonder7Card.Type.*;
import static org.junit.Assert.*;

public class buildCosts {

    Wonders7ForwardModel fm = new Wonders7ForwardModel();
    Wonders7GameParameters params;
    Wonders7GameState state;

    Wonder7Card lumberyard = new Wonder7Card("Lumber Yard", RawMaterials, createCardHash(Wood));
    Wonder7Card timberyard = new Wonder7Card("Timber Yard", RawMaterials, createCardHash(Coin), createCardHash(Wood));
    Wonder7Card apothecary = new Wonder7Card("Apothecary", ScientificStructures, createCardHash(Textile), createCardHash(Compass));
    Wonder7Card library = new Wonder7Card("Library", ScientificStructures, createCardHash(Stone, Stone, Textile), createCardHash(Tablet), "Scriptorium");
    Wonder7Card scriptorium = new Wonder7Card("Scriptorium", ScientificStructures, createCardHash(Stone, Stone, Textile), createCardHash(Tablet));
    Wonder7Card temple = new Wonder7Card("Temple", CivilianStructures, createCardHash(Wood, Clay, Glass), createCardHash(Victory, Victory, Victory), "Altar");


    @Before
    public void setup() {
        params = new Wonders7GameParameters();
        params.setRandomSeed(4902);
        state = new Wonders7GameState(params, 4);
        fm.setup(state);
    }

    @Test
    public void canBuildFreeCards() {
        state.getPlayerHand(0).add(lumberyard);
        state.getPlayerHand(0).add(timberyard);
        state.getPlayerResources(0).put(Coin, 0);

        assertEquals(new Pair<>(false, Collections.emptyList()), timberyard.isPlayable(0, state));
        assertEquals(new Pair<>(true, Collections.emptyList()), lumberyard.isPlayable(0, state));

        state.getPlayerResources(0).put(Coin, 1);

        assertEquals(new Pair<>(true, Collections.emptyList()), timberyard.isPlayable(0, state));
        assertEquals(new Pair<>(true, Collections.emptyList()), lumberyard.isPlayable(0, state));
    }

    @Test
    public void playingCardRemovesCoins() {
        state.getPlayerHand(0).add(timberyard);

        assertEquals(3, state.getPlayerResources(0).get(Coin).intValue());
        fm.next(state, new PlayCard(0, "Timber Yard", false));
        assertEquals(2, state.getPlayerResources(0).get(Coin).intValue());
    }

    @Test
    public void canBuildByBuyingFromNeighbour() {
        assertEquals(0, state.getPlayerResources(0).get(Textile).intValue());
        assertEquals(1, state.getPlayerResources(1).get(Textile).intValue());
        state.getPlayerHand(0).add(library);
        assertEquals(new Pair<>(false, Collections.emptyList()), library.isPlayable(0, state));
        state.getPlayerResources(0).put(Stone, 2);
        assertEquals(new Pair<>(true, List.of(new TradeSource(Textile, 2, 1))), library.isPlayable(0, state));

        // then building it should transfer money
        fm.next(state, new PlayCard(0, "Library", false));
        assertEquals(1, state.getPlayerResources(0).get(Coin).intValue());
        assertEquals(5, state.getPlayerResources(1).get(Coin).intValue());
        assertFalse(state.getPlayerHand(0).contains(library));
    }

    @Test
    public void canBuyWithWildCardOfRightTypeOnly() {
        state.getPlayerHand(0).add(library);
        assertEquals(new Pair<>(false, Collections.emptyList()), library.isPlayable(0, state));
        state.getPlayerResources(0).put(Stone, 1);

        assertEquals(new Pair<>(false, Collections.emptyList()), library.isPlayable(0, state));
        state.getPlayerResources(0).put(RareWild, 1);
        assertEquals(new Pair<>(false, Collections.emptyList()), library.isPlayable(0, state));
        state.getPlayerResources(0).put(BasicWild, 1);
        assertEquals(new Pair<>(true, Collections.emptyList()), library.isPlayable(0, state));

        // then building it should not transfer money
        fm.next(state, new PlayCard(0, "Library", false));
        assertEquals(3, state.getPlayerResources(0).get(Coin).intValue());
        assertEquals(3, state.getPlayerResources(1).get(Coin).intValue());
        assertFalse(state.getPlayerHand(0).contains(library));
    }

    @Test
    public void usesMarketplaceDiscount() {
        // We give each of the neighbours the required resources
        state.getPlayerHand(0).add(apothecary);
        state.getPlayerResources(1).put(Textile, 1);
        state.getPlayerResources(3).put(Textile, 1);

        state.playedCards.get(0).add(new Wonder7Card("Marketplace", CommercialStructures, createCardHash(Coin), new HashMap<>()));
        assertEquals(0, state.getPlayerResources(0).get(Textile).intValue());
        Pair<Boolean, List<TradeSource>> required = apothecary.isPlayable(0, state);

        assertTrue(required.a);
        assertEquals(1, required.b.size());
        assertEquals(1, required.b.get(0).cost());
    }

    @Test
    public void usesTradingPostDiscount() {
        // We give each of the neighbours the required resources
        state.getPlayerHand(0).add(library);
        state.getPlayerResources(1).put(Stone, 1);
        state.getPlayerResources(3).put(Stone, 1);
        state.getPlayerResources(0).put(Stone, 0);
        state.getPlayerResources(0).put(Textile, 1);
        state.getPlayerResources(0).put(Coin, 1);  // 1 coin is not enough to buy the card
        state.playedCards.get(0).add(new Wonder7Card("East Trading Post", CommercialStructures, createCardHash(Coin), new HashMap<>()));

        // P0 now has 1 Coin, 1 Textile and needs to buy 2 Stone
        assertEquals(new Pair<>(false, Collections.emptyList()), library.isPlayable(0, state));

        // We check a few options
        // i) give them a basic wildcard - they should now spend the coin to player 3
        state.getPlayerResources(0).put(BasicWild, 1);
        assertEquals(new Pair<>(true, List.of(new TradeSource(Stone, 1, 3))), library.isPlayable(0, state));

        // ii) give them lots of money - they should still spend the coin to player 3 (i.e. use the wildcard in preference)
        state.getPlayerResources(0).put(Coin, 10);
        assertEquals(new Pair<>(true, List.of(new TradeSource(Stone, 1, 3))), library.isPlayable(0, state));

        // iii) now flip to the West Trading Post - they should now spend the coin to player 1
        state.playedCards.get(0).clear();
        state.playedCards.get(0).add(new Wonder7Card("West Trading Post", CommercialStructures, createCardHash(Coin), new HashMap<>()));
        assertEquals(new Pair<>(true, List.of(new TradeSource(Stone, 1, 1))), library.isPlayable(0, state));

    }

    @Test
    public void canBuildForFreeWithPrerequisite() {
        state.getPlayerHand(0).add(library);
        assertEquals(new Pair<>(false, Collections.emptyList()), library.isPlayable(0, state));

        state.playedCards.get(0).add(scriptorium);
        assertEquals(new Pair<>(true, Collections.emptyList()), library.isPlayable(0, state));
    }


    @Test
    public void hasMoneyForEitherTradeOrBaseCost() {
        // technically this is redundant, as there are no cards in the base game that cost coins and resources
        Wonder7Card testCard = new Wonder7Card("Timber Yard", RawMaterials, createCardHash(Coin, Stone), createCardHash(Wood));

        state.getPlayerHand(0).add(testCard);
        state.getPlayerResources(0).put(Coin, 2);
        state.getPlayerResources(0).put(Stone, 0);

        state.getPlayerResources(1).put(Stone, 1);
        assertEquals(new Pair<>(false, Collections.emptyList()), testCard.isPlayable(0, state));
    }

    @Test
    public void cannotBuyWildCardResourcesFromNeighbours() {
        state.getPlayerHand(0).add(library);
        state.getPlayerResources(0).put(Stone, 1);
        state.getPlayerResources(0).put(RareWild, 1);

        state.getPlayerResources(1).put(BasicWild, 1);
        assertEquals(new Pair<>(false, Collections.emptyList()), library.isPlayable(0, state));

        state.getPlayerResources(1).put(Stone, 1);
        assertEquals(new Pair<>(true, List.of(new TradeSource(Stone, 2, 1))), library.isPlayable(0, state));
    }

    @Test
    public void canUseCompositeResourcesFromNeighbours() {
        state.getPlayerHand(0).add(library);
        state.getPlayerResources(0).put(Stone, 1);
        state.getPlayerResources(0).put(RareWild, 1);

        state.getPlayerResources(1).put(Wood_Stone, 1);
        assertEquals(new Pair<>(true, List.of(new TradeSource(Wood_Stone, 2, 1))), library.isPlayable(0, state));
    }

    @Test
    public void checkNoMultiplePurchaseOptions() {
        state.getPlayerHand(0).add(library);
        state.getPlayerResources(0).put(Stone, 1);
        state.getPlayerResources(0).put(RareWild, 1);

        state.getPlayerResources(1).put(Stone, 1);
        state.getPlayerResources(3).put(Wood_Stone, 1);

        List<List<TradeSource>> options = library.buildOptions(0, state);

        // Currently we only check options if we have a composite resource that could be allocated in multiple ways
        // If we have two non-composite resources at the same price, we just pick the first one
        // TODO: This would need to be changed to implement full functionality of providing a player choice on build options
        assertEquals(1, options.size());
    }

    @Test
    public void checkMultiplePurchaseOptions() {
        state.getPlayerHand(0).add(temple); // needs Wood, Clay, Glass
        state.getPlayerResources(0).put(Stone, 1);
        state.getPlayerResources(0).put(Wood, 0);
        state.getPlayerResources(0).put(Clay, 0);
        state.getPlayerResources(0).put(RareWild, 1);
        state.getPlayerResources(0).put(Glass, 1);

        state.getPlayerResources(1).put(Stone_Clay, 1);
        state.getPlayerResources(3).put(Wood_Stone, 1);

        List<List<TradeSource>> options = temple.buildOptions(0, state);
        assertEquals(0, options.size());

        // Compared to the previous test, we now have two different ways to pay for the card
        state.getPlayerResources(0).put(BasicWild, 1);
        options = temple.buildOptions(0, state);
        assertEquals(2, options.size());

        assertTrue(options.contains(List.of(
                new TradeSource(BasicWild, 0, -1),
                new TradeSource(Stone_Clay, 2, 1)))
        );
        assertTrue(options.contains(List.of(
                new TradeSource(BasicWild, 0, -1),
                new TradeSource(Wood_Stone, 2, 3)))
        );
    }
}
