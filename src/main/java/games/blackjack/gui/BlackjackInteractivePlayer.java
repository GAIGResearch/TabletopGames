package games.blackjack.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import games.blackjack.BlackjackGameState;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Stand;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class BlackjackInteractivePlayer extends AbstractPlayer {
    private JFrame frame;
    private JButton hitButton;
    private JButton standButton;
    private BlackjackGameState gameState;
   

    public BlackjackInteractivePlayer() {
        createGUI();
    }

    private void createGUI() {
        frame = new JFrame("Blackjack Interactive Player");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 100);
        frame.setLayout(new FlowLayout());

        hitButton = new JButton("Hit");
        standButton = new JButton("Stand");

        frame.add(hitButton);
        frame.add(standButton);

        hitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setChosenAction(new Hit(playerID));
            }
        });

        standButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setChosenAction(new Stand());
            }
        });

        frame.setVisible(true);
    }

    @Override
    public void initializePlayer(AbstractGameState initialState) {
        this.gameState = (BlackjackGameState) initialState;
    }

    private volatile AbstractAction chosenAction = null; // 确保可见性和线程安全

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        synchronized (this) {
            while (chosenAction == null) { // 只有在没有选择动作时才等待
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // 重新设置中断状态
                    return null; // 或其他错误处理
                }
            }
        }
        AbstractAction action = chosenAction;
        chosenAction = null; // 重置以便下次使用
        return action;
    }

    public void setChosenAction(AbstractAction action) {
        synchronized (this) {
            this.chosenAction = action;
            this.notifyAll(); // 唤醒等待的线程
        }
    }
    @Override
    public AbstractPlayer copy() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'copy'");
    }

}
