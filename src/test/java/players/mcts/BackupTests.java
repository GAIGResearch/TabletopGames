package players.mcts;

import core.actions.AbstractAction;
import org.junit.Test;
import utilities.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class BackupTests {


    LMRForwardModel fm = new LMRForwardModel();
    LMRGame game = new LMRGame(new LMTParameters(302));
    MCTSParams params = new MCTSParams();
    TestMCTSPlayer player;
    Random rnd = new Random(303897);
    STNWithTestInstrumentation root;

    List<AbstractAction> baseActions = List.of(new LMRAction("Left"), new LMRAction("Middle"), new LMRAction("Right"));
    List<SingleTreeNode> nodeTrajectory001;
    List<Pair<Integer, AbstractAction>> actionTrajectory001;
    SingleTreeNode lastNode;


    public void setupPlayer() {
        fm.setup(game);
        player = new TestMCTSPlayer(params, STNWithTestInstrumentation::new);
        player.setForwardModel(fm);
        root = (STNWithTestInstrumentation) SingleTreeNode.createRootNode(player, game, rnd, STNWithTestInstrumentation::new);

        // we construct a known tree on which we will then apply some backups
        // This assumes we will be back-propagating a trajectory of Middle, Left, Right, Middle

        for (int i = 0; i < 50; i++) {
            root.backUpSingleNode(new LMRAction("Middle"), new double[]{1.0});
        }
        for (int i = 0; i < 25; i++) {
            root.backUpSingleNode(new LMRAction("Left"), new double[]{0.0});
        }
        for (int i = 0; i < 25; i++) {
            root.backUpSingleNode(new LMRAction("Right"), new double[]{0.5});
        }
        // We now have a root node with 100 visits; with values of 1.0 for Middle, 0.0 for Left, 0.5 for Right

        // level 1
        for (int i = 0; i < 3; i++) {
            root.expandNode(baseActions.get(i), game);
        }
        SingleTreeNode levelOneMiddle = root.getChildren().get(baseActions.get(1))[0];
        for (int i = 0; i < 25; i++) {
            levelOneMiddle.backUpSingleNode(new LMRAction("Left"), new double[]{0.9});
        }
        for (int i = 0; i < 15; i++) {
            levelOneMiddle.backUpSingleNode(new LMRAction("Middle"), new double[]{0.0});
        }
        for (int i = 0; i < 10; i++) {
            levelOneMiddle.backUpSingleNode(new LMRAction("Right"), new double[]{1.0});
        }
        // We now have a level 1 node with 50 visits; with values of 0.9 for Left, 0.0 for Middle, 1.0 for Right

        // level 2
        for (int i = 0; i < 3; i++) {
            levelOneMiddle.expandNode(baseActions.get(i), game);
        }
        SingleTreeNode levelTwoLeft = levelOneMiddle.getChildren().get(baseActions.get(0))[0];
        for (int i = 0; i < 10; i++) {
            levelTwoLeft.backUpSingleNode(new LMRAction("Left"), new double[]{1.0});
        }
        for (int i = 0; i < 10; i++) {
            levelTwoLeft.backUpSingleNode(new LMRAction("Middle"), new double[]{1.0});
        }
        for (int i = 0; i < 5; i++) {
            levelTwoLeft.backUpSingleNode(new LMRAction("Right"), new double[]{0.0});
        }
        // We now have a level 2 node with 25 visits; with values of 1.0 for Left, 1.0 for Middle, 0.0 for Right

        // level 3
        for (int i = 0; i < 3; i++) {
            levelTwoLeft.expandNode(baseActions.get(i), game);
        }
        SingleTreeNode levelThreeRight = levelTwoLeft.getChildren().get(baseActions.get(2))[0];
        for (int i = 0; i < 2; i++) {
            levelThreeRight.backUpSingleNode(new LMRAction("Left"), new double[]{1.0});
        }
        for (int i = 0; i < 2; i++) {
            levelThreeRight.backUpSingleNode(new LMRAction("Middle"), new double[]{0.7});
        }
        for (int i = 0; i < 1; i++) {
            levelThreeRight.backUpSingleNode(new LMRAction("Right"), new double[]{0.2});
        }

        for (int i = 0; i < 3; i++) {
            levelThreeRight.expandNode(baseActions.get(i), game);
        }
        SingleTreeNode levelFourLeaf = levelThreeRight.getChildren().get(baseActions.get(1))[0];

        assertEquals(100, root.getVisits());
        assertEquals(50, levelOneMiddle.getVisits());
        assertEquals(25, levelTwoLeft.getVisits());
        assertEquals(5, levelThreeRight.getVisits());
        assertEquals(0, levelFourLeaf.getVisits());

        nodeTrajectory001 = List.of(root, levelOneMiddle, levelTwoLeft, levelThreeRight);
        actionTrajectory001 = List.of(new Pair<>(0, new LMRAction("Middle")),
                new Pair<>(0, new LMRAction("Left")),
                new Pair<>(0, new LMRAction("Right")),
                new Pair<>(0, new LMRAction("Middle")));
        root.currentNodeTrajectory = nodeTrajectory001;
        root.actionsInTree = actionTrajectory001;
        root.actionsInRollout = new ArrayList<>();
        lastNode = levelFourLeaf;
    }

    @Test
    public void standardBackup() {
        setupPlayer();
        lastNode.backUp(new double[]{0.5});

        assertEquals(101, root.getVisits());
        assertEquals(51, nodeTrajectory001.get(1).getVisits());
        assertEquals(26, nodeTrajectory001.get(2).getVisits());
        assertEquals(6, nodeTrajectory001.get(3).getVisits());
        assertEquals(0, lastNode.getVisits());

        assertEquals(51, root.getActionStats(new LMRAction("Middle")).nVisits);
        assertEquals(50.5, root.getActionStats(new LMRAction("Middle")).totValue[0], 0.0001);

        assertEquals(26, nodeTrajectory001.get(1).actionValues.get(new LMRAction("Left")).nVisits);
        assertEquals(25 * 0.9 + 0.5, nodeTrajectory001.get(1).actionValues.get(new LMRAction("Left")).totValue[0], 0.0001);

        assertEquals(6, nodeTrajectory001.get(2).actionValues.get(new LMRAction("Right")).nVisits);
        assertEquals(0.5, nodeTrajectory001.get(2).actionValues.get(new LMRAction("Right")).totValue[0], 0.0001);

        assertEquals(3, nodeTrajectory001.get(3).actionValues.get(new LMRAction("Middle")).nVisits);
        assertEquals(0.7 + 0.7 + 0.5, nodeTrajectory001.get(3).actionValues.get(new LMRAction("Middle")).totValue[0], 0.0001);
    }


    @Test
    public void maxBackup001() {
        // This will just affect the root and first level nodes
        // But, since the best action is already taken from the root, it will not affect the final result
        // It will affect the level 1 node
        setupPlayer();
        params.maxBackupThreshold = 30; // we set this now to avoid interfering with the tree construction
        lastNode.backUp(new double[]{0.5});

        assertEquals(101, root.getVisits());
        assertEquals(51, nodeTrajectory001.get(1).getVisits());
        assertEquals(26, nodeTrajectory001.get(2).getVisits());
        assertEquals(6, nodeTrajectory001.get(3).getVisits());
        assertEquals(0, lastNode.getVisits());

        assertEquals(51, root.getActionStats(new LMRAction("Middle")).nVisits);
        assertEquals(50.5, root.getActionStats(new LMRAction("Middle")).totValue[0], 0.0001);

        double update = 30.0/81.0 * 0.5 + 51.0 / 81.0 * 1.0;
        assertEquals(26, nodeTrajectory001.get(1).actionValues.get(new LMRAction("Left")).nVisits);
        assertEquals(25 * 0.9 + update, nodeTrajectory001.get(1).actionValues.get(new LMRAction("Left")).totValue[0], 0.00001);

        assertEquals(6, nodeTrajectory001.get(2).actionValues.get(new LMRAction("Right")).nVisits);
        assertEquals(0.5, nodeTrajectory001.get(2).actionValues.get(new LMRAction("Right")).totValue[0], 0.0001);

        assertEquals(3, nodeTrajectory001.get(3).actionValues.get(new LMRAction("Middle")).nVisits);
        assertEquals(0.7 + 0.7 + 0.5, nodeTrajectory001.get(3).actionValues.get(new LMRAction("Middle")).totValue[0], 0.0001);
    }
}
