package players.reinforcement;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import core.interfaces.IStateHeuristic;
import games.GameType;
import games.blackjack.BlackjackForwardModel;
import games.blackjack.BlackjackGameState;
import games.blackjack.BlackjackParameters;
import net.jpountz.util.Utils;
import players.PlayerType;
import players.simple.RandomPlayer;
import utilities.ElapsedCpuTimer;
import core.CoreConstants;
import utilities.Pair;
import static games.GameType.Blackjack;

import java.awt.SystemColor;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.AbstractGameState;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Stand;

import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.AbstractGameState;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.avro.hadoop.file.SortedKeyValueFile.Reader;

public class RL_player extends AbstractPlayer {
    private double alpha; // learning rate
    private double gamma; // discount rate
    private double epsilon; // epsilon rate
    private double final_epsilon; // final epsilon
    private double epsilon_decay; // epsilon decay rate
    public final Map<String, Double> qTable; // state-action Q-value
    private final Random random;
    private static int totalIterations = 20000;

    public RL_player(double alpha, double gamma, double epsilon, long seed) {
        this.alpha = alpha;
        this.gamma = gamma;
        this.epsilon = epsilon;
        this.qTable = new HashMap<>();
        this.random = new Random(seed);
        this.final_epsilon = 0.01;
        this.epsilon_decay = epsilon / (totalIterations / 2); //
    }

    public void setPlayerID(int id) {
        this.playerID = id;
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        epsilon = Math.max(final_epsilon, epsilon - epsilon_decay);
        // System.out.println("epsilon is" + epsilon);
        if (random.nextDouble() < epsilon) {
         
            return possibleActions.get(random.nextInt(possibleActions.size()));
        } else {
            
            return getBestAction((BlackjackGameState) gameState, possibleActions);
        }
    }

    private AbstractAction getBestAction(BlackjackGameState gameState, List<AbstractAction> possibleActions) {
        String state = encodeState(gameState);
        AbstractAction bestAction = null;
        double maxQ = Double.NEGATIVE_INFINITY;

        for (AbstractAction action : possibleActions) {
            String actionKey = state + action.toString();
            double qValue = qTable.getOrDefault(actionKey, 0.0);
            if (qValue > maxQ) {
                maxQ = qValue;
                bestAction = action;
            }
        }
        return bestAction != null ? bestAction : possibleActions.get(random.nextInt(possibleActions.size()));
    }

    public void updateQTable(String state, AbstractAction action, double reward, String nextState, boolean done) {
        String actionKey = state + ":" + action.toString();
        double q = qTable.getOrDefault(actionKey, 0.0); // get the current q-value
        double maxQNext = done ? 0 : getMaxQ(nextState); // calculate the next q-value

        // Temporal Difference (TD) error
        double tdError = reward + gamma * maxQNext - q;

        // in order to update the q value, use the learning rate
        double newQ = q + alpha * tdError; // TD error in use as well
        qTable.put(actionKey, newQ); // keep the new q-value in the hashtable
    }

    private double getMaxQ(String nextState) {
        return qTable.entrySet().stream()
                .filter(e -> e.getKey().startsWith(nextState))
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getValue)
                .orElse(0.0);
    }

    public String encodeState(BlackjackGameState gameState) {
        int playerPoints = gameState.calculatePoints(getPlayerID());
        // get the one visuable dealer card
        FrenchCard dealerVisibleCard = (FrenchCard) gameState.getDrawDeck().peek();
        //
        int dealerVisibleValue = dealerVisibleCard != null ? dealerVisibleCard.number : -1;

        return playerPoints + ":" + dealerVisibleValue;
    }

    public double calculateReward(BlackjackGameState gameState, int playerId) {
        CoreConstants.GameResult playerResult = gameState.getPlayerResults()[playerId];
        int playerPoints = gameState.calculatePoints(playerId); 
        double reward = 0.0;

        switch (playerResult) {
            case WIN_GAME:
                if (playerPoints == 21) {
                    reward = 2.0; // Additional reward for hitting exactly 21 points
                } else {
                    reward = 1.0;
                }
                break;
            case LOSE_GAME:
                reward = -1.0;
                break;
            case DRAW_GAME:
                reward = 0; // Or any other appropriate value as per game design
                break;
            default:
                reward = 0.0; // Game is still ongoing
                break;
        }
        // Uncomment below line to debug and track reward values
        // System.out.println("Reward calculated: " + reward);
        return reward;
    }


    @Override
    public AbstractPlayer copy() {
        RL_player clone = new RL_player(alpha, gamma, epsilon, random.nextLong());
        clone.qTable.putAll(this.qTable);
        return clone;
    }


    public static void main(String[] args) {
        double win = 0.0;
        double lose = 0.0;
        double draw = 0.0;
        RL_player rlPlayer = new RL_player(0.1, 0.80, 1, System.currentTimeMillis());
        // init forword model
        QTableVisualizer table = new QTableVisualizer();
        BlackjackForwardModel model = new BlackjackForwardModel();
        RewardChart chart = new RewardChart("RL Training Reward Progress", totalIterations);



        for (int i = 0; i < totalIterations; i++) {
            long seed = System.currentTimeMillis(); 
            BlackjackParameters params = new BlackjackParameters(seed);

            

            BlackjackGameState gameState = new BlackjackGameState(params, 2); // two player, one rl one dealer
            model.setup(gameState); 
            while (!gameState.isGameOver()) {
                List<AbstractAction> possibleActions = model.computeAvailableActions(gameState);
                AbstractAction chosenAction = rlPlayer.getAction(gameState, possibleActions);
                model.performAction(gameState, chosenAction);
                if (gameState.isGameOver()) {
                    double reward = rlPlayer.calculateReward(gameState, rlPlayer.getPlayerID());
                    if(reward == -1.0){lose ++;}
                    else if(reward == 1.0){win ++;}
                    else{draw ++;}
                    String nextState = rlPlayer.encodeState(gameState);
                    rlPlayer.updateQTable(rlPlayer.encodeState(gameState), chosenAction, reward, nextState,
                            gameState.isGameOver());
                    chart.updateChart(i, reward);
                    table.updateQTable(rlPlayer.qTable, i+1);
                    break;
                }
            }
        }
        System.out.println("Training completed.");
       // double win_rate = (win/totalIterations)*100;
       // double lose_rate = (lose/totalIterations)*100;
        System.out.println(draw);
       // System.out.println(win_rate + "%");
       // System.out.println(lose_rate + "%");
        chart.displayChart();
    }

}