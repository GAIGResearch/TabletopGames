package groupM.players.mcts;

import java.util.Random;

import core.AbstractGameState;

public class TreeNodeFactory {
    MCTSEnums.ExplorationStrategy exporationStrategy;
    public TreeNodeFactory(MCTSEnums.ExplorationStrategy exporationStrategy){
        this.exporationStrategy= exporationStrategy;
    }

    public TreeNode createNode(GroupMMCTSPlayer player, TreeNode parent, AbstractGameState state, Random rnd){
        switch(exporationStrategy){
            case UCB1: 
                return new UCB1TreeNode(player, parent, state, rnd);
            case Thompson:
                return new ThompsonTreeNode(player, parent, state, rnd);
            default:
                return new UCB1TreeNode(player, parent, state, rnd);
        }

    }
}

