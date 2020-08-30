package ui.telemetry;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;

public class FeedbackDialog extends DialogWrapper {
    JPanel panel = new JPanel();

    public FeedbackDialog(@Nullable Project project, String check) {
        super(project, true);

        setTitle("Help Us Improve Up2Dep");
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
        falsePositive.addActionListener(new FeedbackButtonListener("falsePositive",check,  this));
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
        btnOther.addActionListener(new FeedbackButtonListener("other",check,  this));
        layout.setConstraints(btnOther, constraints);
        panel.add(btnOther);

        //listen to escape keypressed
        getRootPane().registerKeyboardAction((actionEvent) -> {
            doCancelAction();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        init();
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{getCancelAction()};
    }

    private class FeedbackButtonListener implements ActionListener {
        private String feedback;
        private String dependency;
        private DialogWrapper dialogWrapper;

        public FeedbackButtonListener(String feedback, String dependency, DialogWrapper dialogWrapper) {
            this.feedback = feedback;
            this.dependency = dependency;
            this.dialogWrapper = dialogWrapper;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            
            dialogWrapper.close(200, true);
        }
    }


    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return panel;
    }
}
