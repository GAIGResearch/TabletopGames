package games.wonders7;

import games.wonders7.actions.PlayCard;
import games.wonders7.cards.Wonder7Board;
import games.wonders7.cards.Wonder7Card;
import org.junit.Before;
import org.junit.Test;

import static games.wonders7.Wonders7Constants.Resource.*;
import static games.wonders7.cards.Wonder7Card.CardType.*;
import static org.junit.Assert.*;

public class ScoringTests {

    Wonders7ForwardModel fm = new Wonders7ForwardModel();
    Wonders7GameParameters params;
    Wonders7GameState state;

    @Before
    public void setup() {
        params = new Wonders7GameParameters();
        params.setRandomSeed(4909);
        state = new Wonders7GameState(params, 4);
        fm.setup(state);
    }

    @Test
    public void checkGameScoreHasNoSideEffects() {

        for (int i = 0; i < 4; i++) {
            // 3 coins in starting resources
            assertEquals(1.0, state.getGameScore(i), 0.001);
        }
        state.getPlayerResources(0).put(Shield, 2);
        state.getPlayerResources(1).put(Shield, 3);
        state.getPlayerResources(2).put(Shield, 0);
        state.getPlayerResources(3).put(Shield, 2);

        // this should have no effect (unfortunately in the initial implementation it did)
        for (int i = 0; i < 4; i++) {
            assertEquals(1.0, state.getGameScore(i), 0.001);
        }
    }

    @Test
    public void militaryScoresAtEndOfAge() {

        state.getPlayerResources(0).put(Shield, 2);
        state.getPlayerResources(1).put(Shield, 3);
        state.getPlayerResources(2).put(Shield, 0);
        state.getPlayerResources(3).put(Shield, 2);

        fm.checkAgeEnd(state);
        // should do nothing yet
        for (int i = 0; i < 4; i++) {
            assertEquals(1.0, state.getGameScore(i), 0.001);
        }

        for (int i = 0; i < 4; i++) {
            state.getPlayerHand(i).clear();
            state.getPlayerHand(i).add(Wonder7Card.factory(Wonder7Card.CardType.LumberYard));
        }

        fm.checkAgeEnd(state);
        assertEquals(0, state.getGameScore(0), 0.001);
        assertEquals(3, state.getGameScore(1), 0.001);
        assertEquals(-1, state.getGameScore(2), 0.001);
        assertEquals(2, state.getGameScore(3), 0.001);

    }

    @Test
    public void scienceScoringBasic() {

        state.getPlayerResources(0).put(Cog, 2);
        state.getPlayerResources(1).put(Compass, 1);
        state.getPlayerResources(2).put(Tablet, 3);
        state.getPlayerResources(3).put(Coin, 0);

        assertEquals(5, state.getGameScore(0), 0.001);
        assertEquals(2, state.getGameScore(1), 0.001);
        assertEquals(10, state.getGameScore(2), 0.001);
        assertEquals(0, state.getGameScore(3), 0.001);

        state.getPlayerResources(1).put(Cog, 1);
        state.getPlayerResources(1).put(Tablet, 1);
        state.getPlayerResources(2).put(Compass, 3);

        assertEquals(5, state.getGameScore(0), 0.001);
        assertEquals(11, state.getGameScore(1), 0.001);
        assertEquals(19, state.getGameScore(2), 0.001);
        assertEquals(0, state.getGameScore(3), 0.001);

        state.getPlayerResources(2).put(Cog, 2);

        assertEquals(37, state.getGameScore(2), 0.001);
    }

    @Test
    public void scienceScoringWithWilds() {
        state.getPlayerResources(0).put(Cog, 2);
        state.getPlayerResources(1).put(ScienceWild, 1);
        state.getPlayerResources(2).put(Tablet, 3);
        state.getPlayerResources(3).put(Coin, 0);

        assertEquals(5, state.getGameScore(0), 0.001);
        assertEquals(2, state.getGameScore(1), 0.001);
        assertEquals(10, state.getGameScore(2), 0.001);
        assertEquals(0, state.getGameScore(3), 0.001);

        state.getPlayerResources(0).put(ScienceWild, 1);
        state.getPlayerResources(1).put(ScienceWild, 2);

        assertEquals(10, state.getGameScore(0), 0.001);
        assertEquals(5, state.getGameScore(1), 0.001);

        state.getPlayerResources(1).put(ScienceWild, 3);

        assertEquals(11, state.getGameScore(1), 0.001);
    }

    @Test
    public void workersGuild(){
        state.getPlayedCards(0).add(Wonder7Card.factory(WorkersGuild));
        state.getPlayedCards(0).add(Wonder7Card.factory(LumberYard));
        state.getPlayedCards(1).add(Wonder7Card.factory(LumberYard));
        state.getPlayedCards(1).add(Wonder7Card.factory(Apothecary));
        state.getPlayedCards(2).add(Wonder7Card.factory(LumberYard));
        state.getPlayedCards(3).add(Wonder7Card.factory(LumberYard));

        assertEquals(3, state.getGameScore(0), 0.001);

        state.getPlayedCards(2).remove(0);
        state.getPlayedCards(3).add(Wonder7Card.factory(LumberYard));
        assertEquals(4, state.getGameScore(0), 0.001);
    }

    @Test
    public void craftsmenGuild(){
        state.getPlayedCards(1).add(Wonder7Card.factory(CraftsmenGuild));
        state.getPlayedCards(0).add(Wonder7Card.factory(LumberYard));
        state.getPlayedCards(0).add(Wonder7Card.factory(Loom));
        state.getPlayedCards(1).add(Wonder7Card.factory(Loom));
        state.getPlayedCards(1).add(Wonder7Card.factory(Loom));
        assertEquals(3, state.getGameScore(1), 0.001);
        state.getPlayedCards(0).add(Wonder7Card.factory(Press));
        assertEquals(5, state.getGameScore(1), 0.001);
    }

    @Test
    public void magistratesGuild(){
        state.getPlayedCards(2).add(Wonder7Card.factory(MagistratesGuild));
        state.getPlayedCards(0).add(Wonder7Card.factory(Baths));
        state.getPlayedCards(0).add(Wonder7Card.factory(Baths));
        state.getPlayedCards(1).add(Wonder7Card.factory(Baths));
        state.getPlayedCards(1).add(Wonder7Card.factory(Baths));
        state.getPlayedCards(2).add(Wonder7Card.factory(Baths));

        assertEquals(3, state.getGameScore(2), 0.001);
    }

    @Test
    public void tradersGuild(){
        state.getPlayedCards(3).add(Wonder7Card.factory(TradersGuild));
        state.getPlayedCards(0).add(Wonder7Card.factory(Tavern));
        state.getPlayedCards(1).add(Wonder7Card.factory(Tavern));
        state.getPlayedCards(2).add(Wonder7Card.factory(Tavern));
        assertEquals(3, state.getGameScore(3), 0.001);
        state.getPlayedCards(0).remove(0);
        assertEquals(2, state.getGameScore(3), 0.001);
    }

    @Test
    public void philosophersGuild(){
        state.playedCards.get(0).add(Wonder7Card.factory(PhilosophersGuild));
        state.playedCards.get(0).add(Wonder7Card.factory(Apothecary));

        assertEquals(1, state.getGameScore(0), 0.001);
        state.playedCards.get(1).add(Wonder7Card.factory(Apothecary));
        assertEquals(2, state.getGameScore(0), 0.001);
        assertEquals(1, state.getGameScore(1), 0.001);
        assertEquals(1, state.getGameScore(3), 0.001);
    }

    @Test
    public void spiesGuild(){
        state.playedCards.get(0).add(Wonder7Card.factory(SpiesGuild));
        state.playedCards.get(1).add(Wonder7Card.factory(Stockade));
        assertEquals(2, state.getGameScore(0), 0.001);
        state.getPlayedCards(0).remove(0);
        assertEquals(1, state.getGameScore(0), 0.001);
    }


    @Test
    public void buildersGuild(){
        state.playedCards.get(0).add(Wonder7Card.factory(BuildersGuild));
        assertEquals(1, state.getGameScore(0), 0.001);
        state.getPlayerWonderBoard(0).changeStage();
        assertEquals(2, state.getGameScore(0), 0.001);
        state.getPlayerWonderBoard(0).changeStage();
        assertEquals(3, state.getGameScore(0), 0.001);

        state.getPlayerWonderBoard(1).changeStage();
        assertEquals(4, state.getGameScore(0), 0.001);
        state.getPlayerWonderBoard(3).changeStage();
        assertEquals(5, state.getGameScore(0), 0.001);
    }

    @Test
    public void shipownersGuild(){
        assertEquals(1, state.getGameScore(1), 0.001);
        state.getPlayedCards(1).add(Wonder7Card.factory(ShipownersGuild));
        state.getPlayedCards(0).add(Wonder7Card.factory(LumberYard));
        state.getPlayedCards(0).add(Wonder7Card.factory(Glassworks));
        state.getPlayedCards(0).add(Wonder7Card.factory(ScientistsGuild));
        assertEquals(2, state.getGameScore(1), 0.001);
        state.getPlayedCards(1).add(Wonder7Card.factory(StonePit));
        state.getPlayedCards(1).add(Wonder7Card.factory(Loom));
        state.getPlayedCards(1).add(Wonder7Card.factory(PhilosophersGuild));
        state.getPlayedCards(1).add(Wonder7Card.factory(University));
        assertEquals(5, state.getGameScore(1), 0.001);
    }


    @Test
    public void decoratorsGuild(){
        state.getPlayedCards(2).add(Wonder7Card.factory(DecoratorsGuild));
        Wonder7Board board = state.getPlayerWonderBoard(2);
        for (int i = 1; i <= board.totalWonderStages; i++){
            assertEquals(1, state.getGameScore(2), 0.001);
            board.changeStage();
        }
        assertEquals(8, state.getGameScore(2), 0.001);

        for (int i = 1; i <= state.getPlayerWonderBoard(3).totalWonderStages; i++){
            state.getPlayerWonderBoard(3).changeStage();
        }
        assertEquals(8, state.getGameScore(2), 0.001);
    }

    @Test
    public void scientistsGuild(){
        state.getPlayerHand(3).add(Wonder7Card.factory(ScientistsGuild));
        fm.next(state, new PlayCard(3, ScientistsGuild, true));
        assertEquals(1, state.getPlayerResources(3).get(ScienceWild), 0.001);
        assertEquals(2, state.getGameScore(3), 0.001);
    }


    @Test
    public void playingLighthouseGivesCoinAndVPForItself() {
        assertEquals(3, state.getPlayerResources(1).get(Coin), 0.001);
        state.getPlayerHand(1).add(Wonder7Card.factory(Lighthouse));
        state.getPlayedCards(1).add(Wonder7Card.factory(Tavern));
        fm.next(state, new PlayCard(1, Lighthouse, true));
        assertEquals(5, state.getPlayerResources(1).get(Coin), 0.001);
        assertEquals(3, state.getGameScore(1), 0.001);
        state.getPlayedCards(1).add(Wonder7Card.factory(Forum));
        assertEquals(4, state.getGameScore(1), 0.001);
        assertEquals(5, state.getPlayerResources(1).get(Coin), 0.001);
    }

    @Test
    public void vineyardGivesMoney() {
        assertEquals(1, state.getGameScore(1), 0.001);
        state.getPlayerHand(1).add(Wonder7Card.factory(Vineyard));
        state.getPlayedCards(0).add(Wonder7Card.factory(LumberYard));
        state.getPlayedCards(1).add(Wonder7Card.factory(LumberYard));
        state.getPlayedCards(2).add(Wonder7Card.factory(LumberYard));
        state.getPlayedCards(0).add(Wonder7Card.factory(Apothecary));
        fm.next(state, new PlayCard(1, Vineyard, true));
        assertEquals(2, state.getGameScore(1), 0.001);
        assertEquals(6, state.getPlayerResources(1).get(Coin), 0.001);
    }

    @Test
    public void bazaarDoesNotIncludeResourcesOnWonder() {
        state.playerWonderBoard[1] = new Wonder7Board(Wonder7Board.Wonder.TheLighthouseOfAlexandria, 0);  // Mfg goods
        state.playerWonderBoard[2] = new Wonder7Board(Wonder7Board.Wonder.TheTempleOfArtemisInEphesus, 0);  // Raw materials
        state.getPlayerHand(1).add(Wonder7Card.factory(Bazaar));
        state.getPlayedCards(1).add(Wonder7Card.factory(Press));
        fm.next(state, new PlayCard(1, Bazaar, true));
        assertEquals(5, state.getPlayerResources(1).get(Coin), 0.001);
    }

    @Test
    public void arenaGivesMoneyAndPointsIndependently() {
        state.getPlayerHand(2).add(Wonder7Card.factory(Arena));
        state.getPlayerWonderBoard(2).changeStage();
        fm.next(state, new PlayCard(2, Arena, true));
        assertEquals(6, state.getPlayerResources(2).get(Coin), 0.001);
        assertEquals(3, state.getGameScore(2), 0.001);

        state.getPlayerWonderBoard(2).changeStage();
        state.getPlayerWonderBoard(2).changeStage();
        assertEquals(6, state.getPlayerResources(2).get(Coin), 0.001);
        assertEquals(5, state.getGameScore(2), 0.001);
    }
}
