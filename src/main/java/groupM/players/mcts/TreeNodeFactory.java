package groupM.players.mcts;

import java.util.Random;

import core.AbstractGameState;

public class TreeNodeFactory {
    GroupMMCTSParams params;
    public TreeNodeFactory(GroupMMCTSParams params){
        this.params= params;
    }

    public TreeNode createNode(GroupMMCTSPlayer player, TreeNode parent, AbstractGameState state, Random rnd){
        boolean amaf = this.params.amaf;
        
        switch(this.params.exporationStrategy){
            case UCB1: 
                return amaf ? new UCB1RaveTreeNode(player, parent, state, rnd) : new UCB1TreeNode(player, parent, state, rnd);
            case Thompson:
                return amaf? new RaveThompsonTreeNode(player, parent, state, rnd) : new ThompsonTreeNode(player, parent, state, rnd);
            default:
                return new UCB1TreeNode(player, parent, state, rnd);
        }
    }
}


