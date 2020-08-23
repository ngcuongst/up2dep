package quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import constants.Topics;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public abstract class GeneralQuickFix implements LocalQuickFix {
    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
        MessageBus bus = ApplicationManager.getApplication().getMessageBus();
        bus.syncPublisher(Topics.QUICK_FIX_LISTENER_TOPIC).applied(problemDescriptor.getPsiElement(), getName());

    }
}
