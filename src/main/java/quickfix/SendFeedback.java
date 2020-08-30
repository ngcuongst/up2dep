package quickfix;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import ui.telemetry.FeedbackDialog;
import ui.telemetry.FeedbackJDialog;

import javax.swing.*;

public class SendFeedback extends GeneralQuickFix{
    private String check;

    public SendFeedback(String check){
        this.check = check;
    }
    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return "Send feedback";
    }
    @Nls
    @NotNull
    @Override
    public String getName() {
        return "Give feedback on this check";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
        super.applyFix(project, problemDescriptor);
        SwingUtilities.invokeLater(() -> {
            JFrame frame = WindowManager.getInstance().getFrame(project);
            FeedbackJDialog feedbackDialog = new FeedbackJDialog(frame, check);
            feedbackDialog.setVisible(true);
//            feedbackDialog.show();
        });
    }
}
