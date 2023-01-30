package games.explodingkittens;

import core.AbstractGameState;
import core.CoreConstants;
import core.turnorders.ReactiveTurnOrder;
import core.turnorders.TurnOrder;
import games.explodingkittens.cards.ExplodingKittensCard;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;

import static games.explodingkittens.ExplodingKittensGameState.ExplodingKittensGamePhase.Nope;
import static core.CoreConstants.GameResult.GAME_ONGOING;

public class ExplodingKittensTurnOrder extends ReactiveTurnOrder {
    // Number of cards the player is required to draw
    int requiredDraws;

    public ExplodingKittensTurnOrder(int nPlayers){
        super(nPlayers);
        requiredDraws = 1;
    }

    @Override
    protected void _reset() {
        super._reset();
        requiredDraws = 1;
    }

    /**
     * Ends one sequence of actions, when the player draws a card. Possibly ending its turn if all steps have been done.
     * @param gameState - current game state.
     */
    public void endPlayerTurnStep(AbstractGameState gameState) {
        if (gameState.getGameStatus() != GAME_ONGOING) return;

        ArrayList<Integer> deadPlayers = new ArrayList<>();
        for (int i: reactivePlayers) {
            if (gameState.getPlayerResults()[i] != CoreConstants.GameResult.GAME_ONGOING) {
                deadPlayers.add(i);
            }
        }
        reactivePlayers.removeAll(deadPlayers);
        if (reactivePlayers.size() > 0) reactivePlayers.poll();
        else {
            requiredDraws -= 1;
            if (requiredDraws == 0 || gameState.getPlayerResults()[turnOwner] != CoreConstants.GameResult.GAME_ONGOING) {
                requiredDraws = 1;
                endPlayerTurn(gameState);
            }
        }
    }

    /**
     * If a nopeable action was played, players with a NOPE card can react.
     * @param gameState - current game state.
     */
    public void registerNopeableActionByPlayer(ExplodingKittensGameState gameState, int currentPlayer){
        reactivePlayers.clear();
        for (int i = 1; i <= gameState.getNPlayers(); i++) {
            int nopingPlayer = (currentPlayer + i) % gameState.getNPlayers();

            if (nopingPlayer == currentPlayer && !((ExplodingKittensParameters) gameState.getGameParameters()).nopeOwnCards)
                continue;

            for (ExplodingKittensCard ekp: gameState.getPlayerHandCards().get(nopingPlayer).getComponents()) {
                if (ekp.cardType == ExplodingKittensCard.CardType.NOPE) {
                    reactivePlayers.add(nopingPlayer);
                    break;
                }
            }
        }
        if (reactivePlayers.size() > 0) {
            gameState.setGamePhase(Nope);
        }
    }

    /**
     * If a favor action was played, player requested has to react and give a card.
     * @param player - player asked for a favor.
     */
    public void registerFavorAction(int player){
        reactivePlayers.clear();
        addReactivePlayer(player);
    }

    /**
     * If an attack action was played, turn immediately changes to attacked player who has to play two turns.
     * @param attackTarget - player attacked.
     */
    public void registerAttackAction(int attackTarget){
        requiredDraws = 2;
        turnOwner = attackTarget;
    }

    @Override
    protected TurnOrder _copy() {
        ExplodingKittensTurnOrder to = new ExplodingKittensTurnOrder(nPlayers);
        to.reactivePlayers = new LinkedList<>(reactivePlayers);
        to.requiredDraws = requiredDraws;
        return to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExplodingKittensTurnOrder)) return false;
        if (!super.equals(o)) return false;
        ExplodingKittensTurnOrder that = (ExplodingKittensTurnOrder) o;
        return requiredDraws == that.requiredDraws;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), requiredDraws);
    }
}
