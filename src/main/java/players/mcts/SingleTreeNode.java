package players.mcts;

import core.AbstractGameState;
import core.actions.AbstractAction;
import utilities.ElapsedCpuTimer;
import java.util.List;

class SingleTreeNode
{
    private SingleTreeNode root;
    private SingleTreeNode parent;
    private SingleTreeNode[] children;
    private double totValue;
    private int nVisits;
    private int m_depth;
    private double[] bounds = new double[]{Double.MAX_VALUE, -Double.MAX_VALUE};
    private int fmCallsCount;
    private MCTSPlayer player;

    private AbstractGameState state;
    private AbstractGameState rootState;

    //From MCTSPlayer
    SingleTreeNode(MCTSPlayer player, int num_actions) {
        this(player, num_actions, null, null, null);
    }


    private SingleTreeNode(MCTSPlayer player, int num_actions, SingleTreeNode parent,
                           SingleTreeNode root, AbstractGameState state) {
        this.player = player;
        this.fmCallsCount = 0;
        this.parent = parent;
        this.root = root;
        children = new SingleTreeNode[num_actions];
        totValue = 0.0;
        this.state = state;
        if(parent != null) {
            m_depth = parent.m_depth + 1;
        }
        else {
            m_depth = 0;
        }

    }

    void setRootGameState(SingleTreeNode root, AbstractGameState gs)
    {
        this.state = gs;
        this.root = root;
        this.rootState = gs;
    }


    void mctsSearch() {

        double avgTimeTaken;
        double acumTimeTaken = 0;
        long remaining;
        int numIters = 0;

        int remainingLimit = 5;
        boolean stop = false;

        ElapsedCpuTimer elapsedTimer = new ElapsedCpuTimer();

        if(player.params.stop_type == player.params.STOP_TIME) {
            elapsedTimer.setMaxTimeMillis(player.params.num_time);
        }

        while(!stop){
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
            SingleTreeNode selected = treePolicy();
            double delta = selected.rollOut();
            backUp(selected, delta);
            numIters++;

            //Stopping condition
            if(player.params.stop_type == player.params.STOP_TIME) {
                acumTimeTaken += (elapsedTimerIteration.elapsedMillis()) ;
                avgTimeTaken  = acumTimeTaken/numIters;
                remaining = elapsedTimer.remainingTimeMillis();
                stop = remaining <= 2 * avgTimeTaken || remaining <= remainingLimit;
            }else if(player.params.stop_type == player.params.STOP_ITERATIONS) {
                stop = numIters >= player.params.num_iterations;
            }else if(player.params.stop_type == player.params.STOP_FMCALLS)
            {
                stop = fmCallsCount > player.params.num_fmcalls;
            }
        }
    }

    private SingleTreeNode treePolicy() {

        SingleTreeNode cur = this;

        while (cur.state.isNotTerminal() && cur.m_depth < player.params.ROLLOUT_LENGTH)
        {
            if (cur.notFullyExpanded()) {
                return cur.expand();

            } else {
                cur = cur.uct();
            }
        }

        return cur;
    }

    private SingleTreeNode expand() {

        int bestAction = -1;
        double bestValue = -1;

        for (int i = 0; i < children.length; i++) {
            double x = player.m_rnd.nextDouble();
            if (x > bestValue && children[i] == null) {
                bestAction = i;
                bestValue = x;
            }
        }

        //Roll the state, create a new node and assign it.
        AbstractGameState nextState = state.copy();
        List<AbstractAction> availableActions = nextState.getActions();
        List<AbstractAction> nextActions = advance(nextState, availableActions.get(bestAction));
        SingleTreeNode tn = new SingleTreeNode(player, nextActions.size(), this.root,
                this.m_depth == 0 ? this : this.root, nextState);
        children[bestAction] = tn;
        return tn;
    }



    private List<AbstractAction> advance(AbstractGameState gs, AbstractAction act)
    {
        player.getForwardModel().next(gs, act);
        root.fmCallsCount++;
        return player.getForwardModel().computeAvailableActions(gs);
    }


    private SingleTreeNode uct() {

        SingleTreeNode selected;
        boolean IamMoving = (state.getCurrentPlayer() == player.getPlayerID());

        double[] vals = new double[this.children.length];
        for(int i = 0; i < this.children.length; ++i)
        {
            SingleTreeNode child = children[i];

            double hvVal = child.totValue;
            double childValue =  hvVal / (child.nVisits + player.params.epsilon);
            childValue = normalise(childValue, bounds[0], bounds[1]);

            double uctValue = childValue +
                    player.params.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + player.params.epsilon));

            uctValue = noise(uctValue, player.params.epsilon, player.m_rnd.nextDouble());     //break ties randomly
            vals[i] = uctValue;
        }

        int which = -1;
        double bestValue = IamMoving ? -Double.MAX_VALUE : Double.MAX_VALUE;
        for(int i = 0; i < vals.length; ++i) {
            if ((IamMoving && vals[i] > bestValue) || (!IamMoving && vals[i] < bestValue)){
                which = i;
                bestValue = vals[i];
            }
        }
        selected = children[which];

        //Roll the state. This is closed loop.
        //advance(state, actions.get(selected.childIdx), true);

        root.fmCallsCount++;

        return selected;
    }

    private double rollOut()
    {
        if(player.params.ROLOUTS_ENABLED) {
            AbstractGameState rolloutState = state.copy();
            int thisDepth = this.m_depth;
            while (!finishRollout(rolloutState, thisDepth)) {
                int nActions = rolloutState.getActions().size();
                AbstractAction next = rolloutState.getActions().get(player.m_rnd.nextInt(nActions));
                advance(rolloutState, next);
                thisDepth++;
            }
            double score = rolloutState.getScore(player.getPlayerID());
//            double score = player.params.gameHeuristic.evaluateState(rolloutState, player.getPlayerID());
            return normalise(score, 0, 1);
        }

        double score = state.getScore(player.getPlayerID());
//        double score = player.params.gameHeuristic.evaluateState(state, player.getPlayerID());
        return normalise(score, 0, 1);
    }

    private boolean finishRollout(AbstractGameState rollerState, int depth)
    {
        if (depth >= player.params.ROLLOUT_LENGTH)      //rollout end condition.
            return true;

        //end of game
        return !rollerState.isNotTerminal();
    }


    private void backUp(SingleTreeNode node, double result)
    {
        SingleTreeNode n = node;
        while(n != null)
        {
            n.nVisits++;
            n.totValue += result;
            if (result < n.bounds[0]) {
                n.bounds[0] = result;
            }
            if (result > n.bounds[1]) {
                n.bounds[1] = result;
            }
            n = n.parent;
        }
    }


    int mostVisitedAction() {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;
        boolean allEqual = true;
        double first = -1;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null)
            {
                if(first == -1)
                    first = children[i].nVisits;
                else if(first != children[i].nVisits)
                {
                    allEqual = false;
                }

                double childValue = children[i].nVisits;
                childValue = noise(childValue, player.params.epsilon, player.m_rnd.nextDouble());     //break ties randomly
                if (childValue > bestValue) {
                    bestValue = childValue;
                    selected = i;
                }
            }
        }

        if (selected == -1)
        {
            selected = 0;
        }else if(allEqual)
        {
            //If all are equal, we opt to choose for the one with the best Q.
            selected = bestAction();
        }

        return selected;
    }

    private int bestAction()
    {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null) {
                double childValue = children[i].totValue / (children[i].nVisits + player.params.epsilon);
                childValue = noise(childValue, player.params.epsilon, player.m_rnd.nextDouble());     //break ties randomly
                if (childValue > bestValue) {
                    bestValue = childValue;
                    selected = i;
                }
            }
        }

        if (selected == -1)
        {
            System.out.println("Unexpected selection!");
            selected = 0;
        }

        return selected;
    }


    private boolean notFullyExpanded() {
        for (SingleTreeNode tn : children) {
            if (tn == null) {
                return true;
            }
        }

        return false;
    }

    private double normalise(double a_value, double a_min, double a_max)
    {
        if(a_min < a_max)
            return (a_value - a_min)/(a_max - a_min);
        else    // if bounds are invalid, then return same value
            return a_value;
    }

    private double noise(double input, double epsilon, double random)
    {
        return (input + epsilon) * (1.0 + epsilon * (random - 0.5));
    }

}
