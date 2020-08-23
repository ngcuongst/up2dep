package listener;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFilePropertyEvent;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import util.FileHelper;
import util.LibraryDependenciesBuilder;
import util.LibraryUsageHelper;

public class FileChangeListener implements VirtualFileListener {
    private Project project;

    public FileChangeListener(Project project) {
        this.project = project;
    }

    @Override
    public void contentsChanged(@NotNull VirtualFileEvent event) {
        if (FileHelper.isKotlinOrJavaFile(event)) {
            updateFileDependency(event.getFile());
        }
    }

    @Override
    public void propertyChanged(@NotNull VirtualFilePropertyEvent event) {
        if (FileHelper.isKotlinOrJavaFile(event)) {
            updateFileDependency(event.getFile());
        }
    }


    private void updateFileDependency(VirtualFile file) {
        PsiFile foundFile = PsiManager.getInstance(project).findFile(file);
        if (foundFile != null && foundFile.isValid() && FileHelper.isKotlinOrJavaFile(foundFile)) {
            DumbService dumpService = DumbService.getInstance(project);
            dumpService.smartInvokeLater(() -> LibraryDependenciesBuilder.getInstance(project).updateDependency(project, foundFile));
        }
    }
}
