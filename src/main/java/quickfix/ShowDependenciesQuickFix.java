package quickfix;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import model.GeneralOnlineLibInfo;
import model.libInfo.OnlineLibInfo;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import util.LibraryUsageHelper;

import java.util.HashMap;

public class ShowDependenciesQuickFix extends GeneralQuickFix {
    private GeneralOnlineLibInfo generalOnlineLibInfo;
    public ShowDependenciesQuickFix(GeneralOnlineLibInfo generalOnlineLibInfo) {
        this.generalOnlineLibInfo = generalOnlineLibInfo;

    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return "Show dependencies";
    }

    @Nls
    @NotNull
    @Override
    public String getName() {
        return "Show me dependencies";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
        super.applyFix(project, problemDescriptor);
        LibraryUsageHelper.showDependencyWindow(project, generalOnlineLibInfo);

    }
}