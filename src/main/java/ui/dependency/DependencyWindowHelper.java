package ui.dependency;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

import java.util.logging.Level;

public class DependencyWindowHelper {
    public static boolean isLibWindowVisible(Project project){
        boolean isActive = false;
        try {
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
            ToolWindow toolWindow = toolWindowManager.getToolWindow(Constants.DEPENDENCIES_WINDOW_ID);
            isActive = toolWindow.isActive();
        }catch (Exception ex){
            
        }

        return isActive;
    }
}
