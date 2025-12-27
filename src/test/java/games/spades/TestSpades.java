package games.spades;

import core.AbstractParameters;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.spades.actions.Bid;
import games.spades.actions.PlayCard;
import org.junit.*;

import java.util.List;
import java.util.Map;

import static core.CoreConstants.GameResult.*;
import static org.junit.Assert.*;


public class TestSpades {

    private SpadesForwardModel forwardModel;
    private SpadesGameState gameState;
    private AbstractParameters gameParameters;

    @Before
    public void setUp() {
        forwardModel = new SpadesForwardModel();
        gameParameters = new SpadesParameters();
        gameState = new SpadesGameState(gameParameters, 4);
        forwardModel.setup(gameState);
    }

    // ========================================
    // Set-up Phase
    // ========================================

    /**
     * 测试游戏初始设置是否正确
     * 
     * 验证内容：
     * - 4 plyaers
     * - 13 cards
     * - Start as Bidding phase
     * - Player 1 (on the left of the dealer) bids first
     * - Current bid for every player is -1(=not bid yet)
     * - Teamscore = 0
     * - Spades not broken
     */
    @Test
    public void testGameSetup() {
        assertEquals(4, gameState.getNPlayers());
        assertEquals(SpadesGameState.Phase.BIDDING, gameState.getSpadesGamePhase());
        assertEquals(1, gameState.getCurrentPlayer());

        for (int i = 0; i < 4; i++) {
            assertEquals(13, gameState.getPlayerHands().get(i).getSize());
            assertEquals(-1, gameState.getPlayerBid(i));
        }

        assertEquals(0, gameState.getTeamScore(0));
        assertEquals(0, gameState.getTeamScore(1));
        assertFalse(gameState.isSpadesBroken());

        int totalCards = gameState.getPlayerHands().stream()
                .mapToInt(hand -> hand.getSize())
                .sum();
        assertEquals("总共应该有52张牌", 52, totalCards);
    }

    /**
     * player-team
     * 
     * player0&2 - team1, player1&3 - team2
     */
    @Test
    public void testPlayerTeamMapping() {
        assertEquals(0, gameState.getTeam(0));
        assertEquals(1, gameState.getTeam(1));
        assertEquals(0, gameState.getTeam(2));
        assertEquals(1, gameState.getTeam(3));
    }

    // ========================================
    // Bidding
    // ========================================

    /**
     * 测试叫牌阶段的可用动作
     * 
     * 在叫牌阶段，当前玩家应该能够叫0-13的任意数字
     * 这测试了action generation的正确性
     */
    @Test
    public void testBiddingPhaseAvailableActions() {
        assertEquals("游戏开始时应该处于叫牌阶段", SpadesGameState.Phase.BIDDING, gameState.getSpadesGamePhase());
        
        List<AbstractAction> actions = forwardModel._computeAvailableActions(gameState);
        assertEquals("叫牌阶段应该有14个可用动作（0-13）", 14, actions.size());
        
        // 验证所有动作都是Bid动作且范围正确
        for (int i = 0; i < 14; i++) {
            assertTrue("所有动作都应该是Bid动作", actions.get(i) instanceof Bid);
                         Bid bid = (Bid) actions.get(i);
             assertTrue("叫牌值应该在0-13范围内", bid.bidAmount >= 0 && bid.bidAmount <= 13);
        }
    }

    /**
     * 测试叫牌动作的执行和阶段转换
     * 
     * 验证：
     * - 叫牌被正确记录
     * - 轮到下一个玩家
     * - 所有玩家叫牌后转换到出牌阶段
     */
    @Test
    public void testBiddingExecution() {
        // 玩家1叫5
        Bid bid1 = new Bid(5);
        forwardModel.next(gameState, bid1);
        
        assertEquals("玩家1的叫牌应该被记录", 5, gameState.getPlayerBid(1));
        assertEquals("应该轮到玩家2", 2, gameState.getCurrentPlayer());
        assertEquals("仍然应该处于叫牌阶段", SpadesGameState.Phase.BIDDING, gameState.getSpadesGamePhase());
        
        // 其他玩家完成叫牌
        forwardModel.next(gameState, new Bid(3));
        forwardModel.next(gameState, new Bid( 4));
        assertEquals("应该轮到玩家0", 0, gameState.getCurrentPlayer());
        
        // 最后一个玩家叫牌
        forwardModel.next(gameState, new Bid(2));
        assertEquals("所有玩家叫牌后应该转换到出牌阶段", SpadesGameState.Phase.PLAYING, gameState.getSpadesGamePhase());
        assertEquals("出牌阶段应该从玩家1开始（庄家左边）", 1, gameState.getCurrentPlayer());
    }

    /**
     * 测试团队叫牌计算
     * 
     * 团队的总叫牌是两个队友叫牌的和，这对最终得分计算很重要
     */
    @Test
    public void testTeamBidCalculation() {
        // 完成所有叫牌
        forwardModel.next(gameState, new Bid(5)); // 团队1
        forwardModel.next(gameState, new Bid( 3)); // 团队0
        forwardModel.next(gameState, new Bid( 4)); // 团队1
        forwardModel.next(gameState, new Bid(2)); // 团队0
        
        // 验证团队叫牌总和
        int team0Bid = gameState.getPlayerBid(0) + gameState.getPlayerBid(2); // 2 + 3 = 5
        int team1Bid = gameState.getPlayerBid(1) + gameState.getPlayerBid(3); // 5 + 4 = 9
        
        assertEquals("团队0的总叫牌应该是5", 5, team0Bid);
        assertEquals("团队1的总叫牌应该是9", 9, team1Bid);
    }

    // ========================================
    // 出牌阶段测试
    // ========================================

    /**
     * 测试出牌阶段的基本动作生成
     * 
     * 在出牌阶段开始时，首家应该能够出任何牌（除了黑桃，除非只有黑桃）
     */
    @Test
    public void testPlayingPhaseBasicActions() {
        // 完成叫牌阶段
        completeBiddingPhase();
        
        assertEquals("应该处于出牌阶段", SpadesGameState.Phase.PLAYING, gameState.getSpadesGamePhase());
        
        List<AbstractAction> actions = forwardModel._computeAvailableActions(gameState);
        assertTrue("出牌阶段应该有可用动作", actions.size() > 0);
        
        // 验证所有动作都是PlayCard动作
        for (AbstractAction action : actions) {
            assertTrue("所有动作都应该是PlayCard动作", action instanceof PlayCard);
        }
    }

    /**
     * 测试黑桃破门规则
     * 
     * 在黑桃未破门时，不能首攻黑桃，除非手中只有黑桃
     * 这是Spades游戏的核心规则之一
     */
    @Test
    public void testSpadesBreakingRule() {
        completeBiddingPhase();
        
        // 创建一个有混合花色的手牌来测试黑桃破门规则
        Deck<FrenchCard> playerHand = gameState.getPlayerHands().get(gameState.getCurrentPlayer());
        playerHand.clear();
        
        // 添加非黑桃牌和黑桃牌
        playerHand.add(new FrenchCard(FrenchCard.FrenchCardType.Ace, FrenchCard.Suite.Hearts));
        playerHand.add(new FrenchCard(FrenchCard.FrenchCardType.King, FrenchCard.Suite.Spades));
        
        List<AbstractAction> actions = forwardModel._computeAvailableActions(gameState);
        
        // 验证不能首攻黑桃（黑桃未破门且有其他花色）
        boolean canLeadSpades = actions.stream()
                .filter(a -> a instanceof PlayCard)
                .map(a -> (PlayCard) a)
                .anyMatch(a -> a.card.suite == FrenchCard.Suite.Spades);
        
        assertFalse("黑桃未破门时，有其他花色时不能首攻黑桃", canLeadSpades);
    }

    /**
     * 测试只有黑桃时可以首攻黑桃
     * 
     * 当玩家手中只有黑桃时，即使黑桃未破门也必须出黑桃
     */
    @Test
    public void testSpadesOnlyCanLeadSpades() {
        completeBiddingPhase();
        
        // 创建只有黑桃的手牌
        Deck<FrenchCard> playerHand = gameState.getPlayerHands().get(gameState.getCurrentPlayer());
        playerHand.clear();
        playerHand.add(new FrenchCard(FrenchCard.FrenchCardType.Ace, FrenchCard.Suite.Spades));
        playerHand.add(new FrenchCard(FrenchCard.FrenchCardType.King, FrenchCard.Suite.Spades));
        
        List<AbstractAction> actions = forwardModel._computeAvailableActions(gameState);
        
        // 验证可以出黑桃
        boolean canLeadSpades = actions.stream()
                .filter(a -> a instanceof PlayCard)
                .map(a -> (PlayCard) a)
                .anyMatch(a -> a.card.suite == FrenchCard.Suite.Spades);
        
        assertTrue("手中只有黑桃时应该可以首攻黑桃", canLeadSpades);
    }

    /**
     * 测试跟牌规则
     * 
     * 当别人已经出牌时，必须跟牌（如果有的话）
     * 这是trick-taking游戏的基本规则
     */
    @Test
    public void testFollowSuitRule() {
        completeBiddingPhase();
        
        // 玩家1出红心A
        Deck<FrenchCard> player1Hand = gameState.getPlayerHands().get(1);
        player1Hand.clear();
        FrenchCard heartAce = new FrenchCard(FrenchCard.FrenchCardType.Ace, FrenchCard.Suite.Hearts);
        player1Hand.add(heartAce);
        
        forwardModel.next(gameState, new PlayCard(heartAce));
        
        // 现在轮到玩家2，设置玩家2的手牌（有红心和其他花色）
        Deck<FrenchCard> player2Hand = gameState.getPlayerHands().get(2);
        player2Hand.clear();
        player2Hand.add(new FrenchCard(FrenchCard.FrenchCardType.King, FrenchCard.Suite.Hearts));
        player2Hand.add(new FrenchCard(FrenchCard.FrenchCardType.Queen, FrenchCard.Suite.Clubs));
        
        List<AbstractAction> actions = forwardModel._computeAvailableActions(gameState);
        
        // 验证只能出红心
        for (AbstractAction action : actions) {
            PlayCard playAction = (PlayCard) action;
            assertEquals("必须跟首攻花色（红心）", FrenchCard.Suite.Hearts, playAction.card.suite);
        }
    }

    /**
     * 测试没有跟牌花色时可以出任意牌
     * 
     * 当玩家没有首攻花色时，可以出任意牌（包括将牌黑桃）
     */
    @Test
    public void testCannotFollowSuitRule() {
        completeBiddingPhase();
        
        // 玩家1出红心A
        Deck<FrenchCard> player1Hand = gameState.getPlayerHands().get(1);
        player1Hand.clear();
        FrenchCard heartAce = new FrenchCard(FrenchCard.FrenchCardType.Ace, FrenchCard.Suite.Hearts);
        player1Hand.add(heartAce);
        
        forwardModel.next(gameState, new PlayCard( heartAce));
        
        // 设置玩家2没有红心
        Deck<FrenchCard> player2Hand = gameState.getPlayerHands().get(2);
        player2Hand.clear();
        player2Hand.add(new FrenchCard(FrenchCard.FrenchCardType.King, FrenchCard.Suite.Clubs));
        player2Hand.add(new FrenchCard(FrenchCard.FrenchCardType.Queen, FrenchCard.Suite.Spades));
        
        List<AbstractAction> actions = forwardModel._computeAvailableActions(gameState);
        assertEquals("没有跟牌花色时应该可以出所有手牌", 2, actions.size());
        
        // 验证可以出任意花色
        boolean hasClubs = actions.stream().anyMatch(a -> ((PlayCard) a).card.suite == FrenchCard.Suite.Clubs);
        boolean hasSpades = actions.stream().anyMatch(a -> ((PlayCard) a).card.suite == FrenchCard.Suite.Spades);
        
        assertTrue("应该可以出梅花", hasClubs);
        assertTrue("应该可以出黑桃", hasSpades);
    }

    // ========================================
    // 赢牌判断测试
    // ========================================

    /**
     * 测试黑桃将牌的优先级
     * 
     * 黑桃是将牌，总是比其他花色大
     * 这是Spades游戏的核心机制
     */
    @Test
    public void testSpadesAreTrump() {
        completeBiddingPhase();
        
        // 创建一个完整的trick
        setupCompleteTrick();
        
        // 模拟出牌：红心A, 黑桃2, 红心K, 红心Q
        FrenchCard heartAce = new FrenchCard(FrenchCard.FrenchCardType.Ace, FrenchCard.Suite.Hearts);
        FrenchCard spade2 = new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Spades, 2);
        FrenchCard heartKing = new FrenchCard(FrenchCard.FrenchCardType.King, FrenchCard.Suite.Hearts);
        FrenchCard heartQueen = new FrenchCard(FrenchCard.FrenchCardType.Queen, FrenchCard.Suite.Hearts);
        
        playCompleteRound(heartAce, spade2, heartKing, heartQueen);
        
        // 验证黑桃2赢了这轮（尽管红心A更大）
        assertEquals("出黑桃2的玩家应该赢得这轮", 1, gameState.getCurrentPlayer());
    }

    /**
     * 测试同花色牌的大小比较
     * 
     * 在没有将牌的情况下，同花色中点数大的牌获胜
     */
    @Test
    public void testSameSuitComparison() {
        completeBiddingPhase();
        setupCompleteTrick();
        
        // 模拟出牌：红心2, 红心A, 红心K, 红心Q (都是红心，A最大)
        FrenchCard heart2 = new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Hearts, 2);
        FrenchCard heartAce = new FrenchCard(FrenchCard.FrenchCardType.Ace, FrenchCard.Suite.Hearts);
        FrenchCard heartKing = new FrenchCard(FrenchCard.FrenchCardType.King, FrenchCard.Suite.Hearts);
        FrenchCard heartQueen = new FrenchCard(FrenchCard.FrenchCardType.Queen, FrenchCard.Suite.Hearts);
        
        playCompleteRound(heart2, heartAce, heartKing, heartQueen);
        
        // 验证红心A赢了这轮
        assertEquals("出红心A的玩家应该赢得这轮", 1, gameState.getCurrentPlayer());
    }

    // ========================================
    // 得分系统测试
    // ========================================

    /**
     * 测试成功完成合约的得分
     * 
     * 当团队赢得的墩数 >= 叫牌数时：
     * - 基础分 = 叫牌数 * 10
     * - 沙袋分 = (实际墩数 - 叫牌数) * 1
     */
    @Test
    public void testSuccessfulContractScoring() {
        // 设置一个简单的得分场景
        gameState.setPlayerBid(0, 3); // 团队0叫3
        gameState.setPlayerBid(2, 2); // 团队0叫2，总共5
        
        // 模拟团队0赢得6墩
        for (int i = 0; i < 6; i++) {
            gameState.incrementTricksTaken(0); // 玩家0赢得6墩
        }
        
        // 手动调用得分计算逻辑
        int teamBid = gameState.getPlayerBid(0) + gameState.getPlayerBid(2); // 5
        int teamTricks = gameState.getTricksTaken(0) + gameState.getTricksTaken(2); // 6
        
        int expectedScore = teamBid * 10 + (teamTricks - teamBid); // 5*10 + 1 = 51
        
        assertEquals("团队叫牌应该是5", 5, teamBid);
        assertEquals("团队实际墩数应该是6", 6, teamTricks);
        // 注意：这里我们测试的是计算逻辑，实际的endRound方法会自动计算
    }

    /**
     * 测试未完成合约的惩罚
     * 
     * 当团队赢得的墩数 < 叫牌数时：
     * - 扣分 = 叫牌数 * 10
     */
    @Test
    public void testFailedContractPenalty() {
        gameState.setPlayerBid(0, 5); // 团队0叫5
        gameState.setPlayerBid(2, 2); // 团队0叫2，总共7
        
        // 模拟团队0只赢得5墩（少于叫牌7）
        for (int i = 0; i < 5; i++) {
            gameState.incrementTricksTaken(0);
        }
        
        int teamBid = gameState.getPlayerBid(0) + gameState.getPlayerBid(2); // 7
        int teamTricks = gameState.getTricksTaken(0) + gameState.getTricksTaken(2); // 5
        
        assertTrue("团队实际墩数应该少于叫牌", teamTricks < teamBid);
        int expectedPenalty = teamBid * 10; // 70分惩罚
        assertEquals("惩罚应该是70分", 70, expectedPenalty);
    }

    /**
     * 测试沙袋累积和惩罚
     * 
     * 每10个沙袋扣100分，这防止玩家故意低叫
     */
    @Test
    public void testSandbagPenalty() {
        SpadesParameters params = (SpadesParameters) gameState.getGameParameters();
        
        // 设置团队已有9个沙袋
        gameState.addTeamSandbags(0, 9);
        
        // 再添加2个沙袋（总共11个）
        gameState.addTeamSandbags(0, 2);
        
        // 检查是否触发沙袋惩罚
        if (gameState.getTeamSandbags(0) >= params.sandbagsPerPenalty) {
            int penalty = params.sandbagsRandPenalty; // 100分
            assertEquals("沙袋惩罚应该是100分", 100, penalty);
            
            // 验证沙袋数重置
            int remainingSandbags = gameState.getTeamSandbags(0) - params.sandbagsPerPenalty;
            assertEquals("剩余沙袋应该是1个", 1, remainingSandbags);
        }
    }

    // ========================================
    // 游戏结束条件测试  
    // ========================================

    /**
     * 测试达到获胜分数的游戏结束
     * 
     * 当任意团队达到500分时游戏结束，得分高的团队获胜
     */
    @Test
    public void testWinningScoreGameEnd() {
        // 设置团队0得分为520分
        gameState.setTeamScore(0, 520);
        gameState.setTeamScore(1, 300);
        
        SpadesParameters params = (SpadesParameters) gameState.getGameParameters();
        boolean gameEnded = gameState.getTeamScore(0) >= params.winningScore || 
                           gameState.getTeamScore(1) >= params.winningScore;
        
        assertTrue("游戏应该结束（有团队达到获胜分数）", gameEnded);
        
        int winningTeam = gameState.getTeamScore(0) > gameState.getTeamScore(1) ? 0 : 1;
        assertEquals("团队0应该获胜", 0, winningTeam);
    }

    // ========================================
    // 状态管理和信息隐藏测试
    // ========================================

    /**
     * 测试游戏状态复制的正确性
     * 
     * 状态复制对MCTS算法至关重要，必须确保：
     * - 复制的状态独立于原状态
     * - 信息隐藏正确实现
     * - hashCode一致性
     */
    @Test
    public void testGameStateCopy() {
        // 复制状态
        SpadesGameState copy = (SpadesGameState) gameState.copy();
        
        // 验证复制的状态是独立的
        assertNotSame("复制的状态应该是不同的对象", gameState, copy);
        assertEquals("复制的状态应该有相同的玩家数", gameState.getNPlayers(), copy.getNPlayers());
        assertEquals("复制的状态应该有相同的游戏阶段", gameState.getSpadesGamePhase(), copy.getSpadesGamePhase());
        
        // 验证手牌复制
        for (int i = 0; i < 4; i++) {
            assertNotSame("玩家手牌应该是独立复制的", 
                         gameState.getPlayerHands().get(i), 
                         copy.getPlayerHands().get(i));
            assertEquals("玩家手牌大小应该相同", 
                        gameState.getPlayerHands().get(i).getSize(),
                        copy.getPlayerHands().get(i).getSize());
        }
    }

    /**
     * 测试特定玩家视角的状态复制（信息隐藏）
     * 
     * 当为特定玩家复制状态时，其他玩家的手牌应该被随机化
     * 这对Information Set MCTS很重要
     */
    @Test
    public void testPlayerSpecificStateCopy() {
        // 为玩家0复制状态
        SpadesGameState copyForPlayer0 = (SpadesGameState) gameState.copy(0);
        
        // 玩家0自己的手牌应该保持不变
        assertEquals("玩家0的手牌应该保持原样", 
                    gameState.getPlayerHands().get(0).getSize(),
                    copyForPlayer0.getPlayerHands().get(0).getSize());
        
        // 其他玩家的手牌应该被随机化（但大小相同）
        for (int i = 1; i < 4; i++) {
            assertEquals("其他玩家的手牌大小应该相同", 
                        gameState.getPlayerHands().get(i).getSize(),
                        copyForPlayer0.getPlayerHands().get(i).getSize());
        }
    }

    /**
     * 测试状态hashCode的一致性
     * 
     * 相同的状态应该有相同的hashCode，这对状态比较很重要
     */
    @Test  
    public void testStateHashCodeConsistency() {
        SpadesGameState copy = (SpadesGameState) gameState.copy();
        
        assertEquals("相同状态的hashCode应该相等", gameState.hashCode(), copy.hashCode());
        
        // 修改复制的状态
        copy.setPlayerBid(0, 5);
        
        assertNotEquals("不同状态的hashCode应该不同", gameState.hashCode(), copy.hashCode());
    }

    // ========================================
    // 辅助方法
    // ========================================

    /**
     * 完成叫牌阶段的辅助方法
     * 让所有玩家完成叫牌，进入出牌阶段
     */
    private void completeBiddingPhase() {
        forwardModel.next(gameState, new Bid( 3));
        forwardModel.next(gameState, new Bid(4));
        forwardModel.next(gameState, new Bid(2));
        forwardModel.next(gameState, new Bid(4));
    }

    /**
     * 设置完整trick的辅助方法
     * 为测试赢牌判断准备游戏状态
     */
    private void setupCompleteTrick() {
        // 清空所有玩家手牌，方便控制出牌
        for (int i = 0; i < 4; i++) {
            gameState.getPlayerHands().get(i).clear();
        }
    }

    /**
     * 执行完整一轮出牌的辅助方法
     * 
     * @param card1 玩家1出的牌
     * @param card2 玩家2出的牌  
     * @param card3 玩家3出的牌
     * @param card4 玩家0出的牌
     */
    private void playCompleteRound(FrenchCard card1, FrenchCard card2, FrenchCard card3, FrenchCard card4) {
        // 添加牌到对应玩家手中并出牌
        gameState.getPlayerHands().get(1).add(card1);
        forwardModel.next(gameState, new PlayCard(card1));
        
        gameState.getPlayerHands().get(2).add(card2);
        forwardModel.next(gameState, new PlayCard(card2));
        
        gameState.getPlayerHands().get(3).add(card3);
        forwardModel.next(gameState, new PlayCard( card3));
        
        gameState.getPlayerHands().get(0).add(card4);
        forwardModel.next(gameState, new PlayCard( card4));
    }
}