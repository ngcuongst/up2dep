package util;

import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import model.GeneralOnlineLibInfo;
import model.gradle.GradleCoordinate;
import model.libInfo.OnlineLibInfo;
import ui.dependency.LibraryDependenciesPanel;
import ui.dependency.LibraryDependenciesWindow;

import java.util.HashMap;

public class LibraryUsageHelper {
    private static final String WINDOW_PREFIX = "Usages of ";

    public static void showDependencyWindow(Project project, GeneralOnlineLibInfo generalOnlineLibInfo, boolean showIncompatibility) {
        if(showIncompatibility){
            showDependencyWindow(project, generalOnlineLibInfo);
        }

    }
    public static void showDependencyWindow(Project project, GeneralOnlineLibInfo generalOnlineLibInfo) {
        LibraryDependenciesPanel libDepPanel = new LibraryDependenciesPanel(project, generalOnlineLibInfo);
        OnlineLibInfo onlineLibInfo = generalOnlineLibInfo.getOnlineLibInfo();
        String windowsName = WINDOW_PREFIX + onlineLibInfo.getLibName();
        Content content = ContentFactory.SERVICE.getInstance().createContent(libDepPanel, windowsName, false);

        content.setDisposer(libDepPanel);
        libDepPanel.setContent(content);

        LibraryDependenciesWindow libDepWindow = LibraryDependenciesWindow.getInstance(project);

        libDepWindow.addContent(content);
    }
}
