package games.coltexpress.cards.roundcards;

import games.coltexpress.ColtExpressGameState;
import utilities.Utils;

public class EndCardMarshallsRevenge extends RoundCard{

    public EndCardMarshallsRevenge(){
        turnTypes = new TurnType[] {TurnType.NormalTurn, TurnType.NormalTurn,
        TurnType.HiddenTurn, TurnType.NormalTurn};
    }

    @Override
    public void endTurnEvent(ColtExpressGameState gameState) {
        //todo  Marshall's Revenge - All bandits on the roof of the Marshall's car drop their least valuable purse.
        gameState.setGameStatus(Utils.GameResult.GAME_END);
    }
}
