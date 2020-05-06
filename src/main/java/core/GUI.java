package core;

import core.actions.IAction;
import players.AbstractPlayer;
import players.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

@SuppressWarnings("rawtypes")
public abstract class GUI extends JFrame {
    protected ActionButton[] actionButtons;
    protected int maxActionSpace;
    protected ActionController ac;

    public GUI(ActionController ac, int maxActionSpace) {
        this.ac = ac;
        this.maxActionSpace = maxActionSpace;
    }

    public final void update(AbstractPlayer player, AbstractGameState gameState){
        resetActionButtons();
        _update(player, gameState);
    }

    private void resetActionButtons() {
        for (int i = 0; i < maxActionSpace; i++) {
            actionButtons[i].setVisible(false);
        }
    }

    protected abstract void _update(AbstractPlayer player, AbstractGameState gameState);

    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState){}
    protected JComponent createActionPanel(Collection[] highlights, int width, int height) {
        JPanel actionPanel = new JPanel();
        actionPanel.setPreferredSize(new Dimension(width, height));

        actionButtons = new ActionButton[maxActionSpace];
        for (int i = 0; i < maxActionSpace; i++) {
            ActionButton ab = new ActionButton(ac, highlights);
            actionButtons[i] = ab;
            actionButtons[i].setVisible(false);
            actionPanel.add(actionButtons[i]);
        }

        return actionPanel;
    }

    @SuppressWarnings("rawtypes")
    protected static class ActionButton extends JButton {
        IAction action;

        public ActionButton(ActionController ac, Collection[] highlights) {
            addActionListener(e -> {
                ac.addAction(action);
                for (Collection c: highlights) {
                    c.clear();
                }
            });
        }

        public void setButtonAction(IAction action) {
            this.action = action;
            setText(action.toString());
        }

        public void setButtonAction(IAction action, String actionText) {
            this.action = action;
            setText(actionText);
        }
    }
}
