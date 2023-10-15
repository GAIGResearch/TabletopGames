package groupM.players.mcts;

import java.util.Random;

import core.AbstractGameState;

public class TreeNodeFactory {
    MCTSEnums.ExplorationStrategy exporationStrategy;
    public TreeNodeFactory(MCTSEnums.ExplorationStrategy exporationStrategy){
        this.exporationStrategy= exporationStrategy;
    }

    public TreeNode createNode(MCTSPlayer player, TreeNode parent, AbstractGameState state, Random rnd){
        if(exporationStrategy == MCTSEnums.ExplorationStrategy.UCB1){
            return new UCB1TreeNode(player, parent, state, rnd);
        }
        return new UCB1TreeNode(player, parent, state, rnd);
    }
}

