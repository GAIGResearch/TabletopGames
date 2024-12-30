package games.wonders7;

import games.wonders7.actions.PlayCard;
import games.wonders7.cards.Wonder7Card;
import games.wonders7.cards.Wonder7Guilds;
import org.junit.Before;
import org.junit.Test;

import static games.wonders7.Wonders7Constants.Resource.*;
import static games.wonders7.Wonders7Constants.createCardHash;
import static games.wonders7.cards.Wonder7Card.Type.*;
import static org.junit.Assert.*;

public class scoring {

    Wonders7ForwardModel fm = new Wonders7ForwardModel();
    Wonders7GameParameters params;
    Wonders7GameState state;
    Wonder7Guilds guilds = new Wonder7Guilds();


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
            state.getPlayerHand(i).add(new Wonder7Card("Lumber Yard", RawMaterials, createCardHash(Wood)));
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
        state.getPlayedCards(0).add(guilds.workersGuild);
        state.getPlayedCards(0).add(new Wonder7Card("Lumber Yard", RawMaterials, createCardHash(Wood)));
        state.getPlayedCards(1).add(new Wonder7Card("Lumber Yard", RawMaterials, createCardHash(Wood)));
        state.getPlayedCards(1).add(new Wonder7Card("Lumber Yard", ScientificStructures, createCardHash(Wood)));
        state.getPlayedCards(2).add(new Wonder7Card("Lumber Yard", RawMaterials, createCardHash(Wood)));
        state.getPlayedCards(3).add(new Wonder7Card("Lumber Yard", RawMaterials, createCardHash(Wood)));

        assertEquals(3, state.getGameScore(0), 0.001);

        state.getPlayedCards(2).remove(0);
        state.getPlayedCards(3).add(new Wonder7Card("Lumber Yard", RawMaterials, createCardHash(Wood)));
        assertEquals(4, state.getGameScore(0), 0.001);
    }

    @Test
    public void craftsmenGuild(){
        state.getPlayedCards(1).add(guilds.craftsmenGuild);
        state.getPlayedCards(0).add(new Wonder7Card("Lumber Yard", RawMaterials, createCardHash(Wood)));
        state.getPlayedCards(0).add(new Wonder7Card("Lumber Yard", ManufacturedGoods, createCardHash(Wood)));
        state.getPlayedCards(1).add(new Wonder7Card("Lumber Yard", ManufacturedGoods, createCardHash(Wood)));
        state.getPlayedCards(1).add(new Wonder7Card("Lumber Yard", ManufacturedGoods, createCardHash(Wood)));

        assertEquals(3, state.getGameScore(1), 0.001);
        state.getPlayedCards(0).add(new Wonder7Card("Lumber Yard", ManufacturedGoods, createCardHash(Wood)));
        assertEquals(5, state.getGameScore(1), 0.001);
    }

    @Test
    public void magistratesGuild(){
        state.getPlayedCards(2).add(guilds.magistratesGuild);
        state.getPlayedCards(0).add(new Wonder7Card("Lumber Yard", CivilianStructures, createCardHash(Wood)));
        state.getPlayedCards(0).add(new Wonder7Card("Lumber Yard", CivilianStructures, createCardHash(Wood)));
        state.getPlayedCards(1).add(new Wonder7Card("Lumber Yard", CivilianStructures, createCardHash(Wood)));
        state.getPlayedCards(1).add(new Wonder7Card("Lumber Yard", CivilianStructures, createCardHash(Wood)));
        state.getPlayedCards(2).add(new Wonder7Card("Lumber Yard", CivilianStructures, createCardHash(Wood)));

        assertEquals(3, state.getGameScore(2), 0.001);
    }

    @Test
    public void tradersGuild(){
        state.getPlayedCards(3).add(guilds.tradersGuild);
        state.getPlayedCards(0).add(new Wonder7Card("Lumber Yard", CommercialStructures, createCardHash(Wood)));
        state.getPlayedCards(1).add(new Wonder7Card("Lumber Yard", CommercialStructures, createCardHash(Wood)));
        state.getPlayedCards(2).add(new Wonder7Card("Lumber Yard", CommercialStructures, createCardHash(Wood)));
        assertEquals(3, state.getGameScore(3), 0.001);
        state.getPlayedCards(0).remove(0);
        assertEquals(2, state.getGameScore(3), 0.001);
    }

    @Test
    public void philosophersGuild(){
        state.playedCards.get(0).add(guilds.philosophersGuild);
        state.playedCards.get(0).add(new Wonder7Card("Lumber Yard", ScientificStructures, createCardHash(Wood)));

        assertEquals(1, state.getGameScore(0), 0.001);
        state.playedCards.get(1).add(new Wonder7Card("Lumber Yard", ScientificStructures, createCardHash(Wood)));
        assertEquals(2, state.getGameScore(0), 0.001);
        assertEquals(1, state.getGameScore(1), 0.001);
        assertEquals(1, state.getGameScore(3), 0.001);
    }

    @Test
    public void spiesGuild(){
        state.playedCards.get(0).add(guilds.spiesGuild);
        state.playedCards.get(1).add(new Wonder7Card("Lumber Yard", MilitaryStructures, createCardHash(Wood)));
        assertEquals(2, state.getGameScore(0), 0.001);
        state.getPlayedCards(0).remove(0);
        assertEquals(1, state.getGameScore(0), 0.001);
    }


    @Test
    public void buildersGuild(){
        state.playedCards.get(0).add(guilds.buildersGuild);
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
        state.getPlayedCards(1).add(guilds.shipownersGuild);
        state.getPlayedCards(0).add(new Wonder7Card("Lumber Yard", RawMaterials, createCardHash(Wood)));
        state.getPlayedCards(0).add(new Wonder7Card("Lumber Yard", ManufacturedGoods, createCardHash(Wood)));
        state.getPlayedCards(0).add(new Wonder7Card("Lumber Yard", Guilds, createCardHash(Wood)));
        assertEquals(2, state.getGameScore(1), 0.001);
        state.getPlayedCards(1).add(new Wonder7Card("Lumber Yard", RawMaterials, createCardHash(Wood)));
        state.getPlayedCards(1).add(new Wonder7Card("Lumber Yard", ManufacturedGoods, createCardHash(Wood)));
        state.getPlayedCards(1).add(new Wonder7Card("Lumber Yard", Guilds, createCardHash(Wood)));
        assertEquals(5, state.getGameScore(1), 0.001);
    }


    @Test
    public void decoratorsGuild(){
        state.getPlayedCards(2).add(guilds.decoratorsGuild);
        for (int i = 1; i <= state.getPlayerWonderBoard(2).type.wonderStages; i++){
            assertEquals(1, state.getGameScore(2), 0.001);
            state.getPlayerWonderBoard(2).changeStage();
        }
        assertEquals(8, state.getGameScore(2), 0.001);

        for (int i = 1; i <= state.getPlayerWonderBoard(3).type.wonderStages; i++){
            state.getPlayerWonderBoard(3).changeStage();
        }
        assertEquals(8, state.getGameScore(2), 0.001);
    }

    @Test
    public void scientistsGuild(){
        state.getPlayerHand(3).add(guilds.scientistsGuild);
        fm.next(state, new PlayCard(3, "Scientists Guild", true));
        assertEquals(1, state.getPlayerResources(3).get(ScienceWild), 0.001);
        assertEquals(2, state.getGameScore(3), 0.001);
    }
}
