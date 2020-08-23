package listener;

import com.android.tools.idea.project.AndroidProjectBuildNotifications;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbModeTask;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import util.LibraryDependenciesBuilder;
import util.LibraryUsageHelper;

public class BuildListener implements AndroidProjectBuildNotifications.AndroidProjectBuildListener {
    private Project project;

    public BuildListener(Project project) {
        this.project = project;
    }

    @Override
    public void buildComplete(@NotNull AndroidProjectBuildNotifications.BuildContext buildContext) {
        DumbService dumpService = DumbService.getInstance(project);
        dumpService.runWhenSmart(() -> LibraryDependenciesBuilder.getInstance(project).findAllDependencies());
    }
}
