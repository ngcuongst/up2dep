package listener;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import service.localService.LocalDataService;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

public class QuickFixListener implements IQuickFixListener {



    @Override
    public void applied(PsiElement element, String name) {
        String content = element.getText() + "\t" + name;
        Project project = element.getProject();
        String projectName = project.getName();
        final LocalDataService service = ServiceManager.getService(LocalDataService.class);
        Set<String> excludedProjects = service.getExcludedProjects();
        if (excludedProjects.contains(projectName)) {
            return;
        }
        


    }
}
