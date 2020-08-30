package ui.telemetry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class FeedbackJDialog extends JDialog {
    private static final String TITLE = "Help Us Improve Up2Dep";

    public FeedbackJDialog(Frame frame, String check) {
        super(frame, TITLE);
        JPanel panel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.setLayout(layout);

        Button button = new Button("Useful");
        button.addActionListener(new FeedbackButtonListener("useful", check, this));
        constraints.gridx = 0;
        constraints.gridy = 0;

        layout.setConstraints(button, constraints);
        panel.add(button);

        Label correct = new Label("This check is correct, and you find it useful");
        constraints.gridx = 1;
        constraints.gridy = 0;
        layout.setConstraints(correct, constraints);
        panel.add(correct);

        Button falsePositive = new Button("False positive");
        constraints.gridx = 0;
        constraints.gridy = 1;
        falsePositive.addActionListener(new FeedbackButtonListener("falsePositive", check, this));
        layout.setConstraints(falsePositive, constraints);

        panel.add(falsePositive);

        Label incorrect = new Label("This check is incorrect");
        constraints.gridx = 1;
        constraints.gridy = 1;
        layout.setConstraints(incorrect, constraints);
        panel.add(incorrect);


        Button moreInfo = new Button("I don't get it");
        constraints.gridx = 0;
        constraints.gridy = 2;
        moreInfo.addActionListener(new FeedbackButtonListener("moreInfo", check, this));
        layout.setConstraints(moreInfo, constraints);

        panel.add(moreInfo);

        Label moreInfoMsg = new Label("The message does not convey enough information");
        constraints.gridx = 1;
        constraints.gridy = 2;
        layout.setConstraints(moreInfoMsg, constraints);
        panel.add(moreInfoMsg);

        Button btnOther = new Button("Other");
        constraints.gridx = 0;
        constraints.gridy = 3;
        btnOther.addActionListener(new FeedbackButtonListener("other", check, this));
        layout.setConstraints(btnOther, constraints);
        panel.add(btnOther);
        getContentPane().add(panel);
        this.setLocationRelativeTo(frame);
        getRootPane().registerKeyboardAction(e -> setVisible(false),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
    }

    private static class FeedbackButtonListener implements ActionListener {
        private String feedback;
        private String dependency;
        private JDialog dialog;

        public FeedbackButtonListener(String feedback, String dependency, JDialog dialog) {
            this.feedback = feedback;
            this.dependency = dependency;
            this.dialog = dialog;

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            
            dialog.setVisible(false);
        }
    }
}
