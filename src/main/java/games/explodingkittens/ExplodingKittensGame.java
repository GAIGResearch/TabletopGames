package updated_core.games.explodingkittens;

import updated_core.ForwardModel;
import updated_core.Game;
import updated_core.actions.IAction;
import updated_core.actions.IPrintable;
import updated_core.games.explodingkittens.cards.ExplodingKittenCard;
import updated_core.observations.Observation;
import updated_core.players.AbstractPlayer;
import updated_core.players.HumanConsolePlayer;
import updated_core.players.RandomAIPlayer;
import updated_core.turn_order.TurnOrder;

import java.util.*;

public class ExplodingKittensGame extends Game {

    TurnOrder turnOrder;
    ForwardModel forwardModel = new ExplodingKittensForwardModel();

    public ExplodingKittensGame(List<AbstractPlayer> agents)
    {
        turnOrder = new ExplodingKittenTurnOrder(agents);

        HashMap<ExplodingKittenCard.CardType, Integer> cardCounts = new HashMap<ExplodingKittenCard.CardType, Integer>() {{
            put(ExplodingKittenCard.CardType.ATTACK, 4);
            put(ExplodingKittenCard.CardType.SKIP, 4);
            put(ExplodingKittenCard.CardType.FAVOR, 4);
            put(ExplodingKittenCard.CardType.SHUFFLE, 4);
            put(ExplodingKittenCard.CardType.SEETHEFUTURE, 5);
            put(ExplodingKittenCard.CardType.TACOCAT, 4);
            put(ExplodingKittenCard.CardType.MELONCAT, 4);
            put(ExplodingKittenCard.CardType.BEARDCAT, 4);
            put(ExplodingKittenCard.CardType.RAINBOWCAT, 4);
            put(ExplodingKittenCard.CardType.FURRYCAT, 4);
            put(ExplodingKittenCard.CardType.NOPE, 5);
            put(ExplodingKittenCard.CardType.DEFUSE, 6);
            put(ExplodingKittenCard.CardType.EXPLODING_KITTEN, agents.size()-1);
        }};

        ExplodingKittenParameters params = new ExplodingKittenParameters(agents.size(), cardCounts);

        gameState = new ExplodingKittensPartialObservableGameState(params);
    }

    @Override
    public void run() {
        while (!isEnded()){
            //((ExplodingKittensGameState) gameState).print((ExplodingKittenTurnOrder) turnOrder);
            AbstractPlayer currentPlayer = turnOrder.getCurrentPlayer(gameState);
            List<IAction> actions = Collections.unmodifiableList(gameState.getActions(currentPlayer));
            Observation observation = gameState.getObservation(currentPlayer);
            if (observation != null)
                ((IPrintable) observation).PrintToConsole();
            int actionIdx = currentPlayer.getAction(observation, actions);
            forwardModel.next(gameState, turnOrder, actions.get(actionIdx));
            System.out.println();
        }

        ((ExplodingKittensGameState) gameState).print((ExplodingKittenTurnOrder) turnOrder);
        // ((IPrintable) gameState.getObservation(null)).PrintToConsole();
        System.out.println(Arrays.toString(gameState.getPlayerResults()));

        System.out.println("Game Over");
        for (int i = 0; i < gameState.getNPlayers(); i++){
            if (((ExplodingKittensGameState) gameState).isPlayerAlive[i])
                System.out.println("Player " + i + " won");
        }
    }

    @Override
    public boolean isEnded() {
        return ((ExplodingKittensGameState) gameState).isGameOver();
    }

    @Override
    public HashSet<Integer> winners() {
        HashSet<Integer> winners = new HashSet<>();
        // TODO: all or nothing, check gamestate
        return winners;
    }

    public static void main(String[] args){
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new RandomAIPlayer(0));
        agents.add(new RandomAIPlayer(1));
        agents.add(new RandomAIPlayer(2));
        agents.add(new RandomAIPlayer(3));

        Game game = new ExplodingKittensGame(agents);
        game.run();
    }
}
