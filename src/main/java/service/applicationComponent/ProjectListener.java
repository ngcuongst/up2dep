package service.applicationComponent;

import com.android.tools.idea.project.AndroidProjectBuildNotifications;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import constants.Topics;
import listener.BuildListener;
import listener.FileChangeListener;
import listener.QuickFixListener;
import org.jetbrains.annotations.NotNull;

import util.LibraryDependenciesBuilder;
import util.remoteDb.RemoteDbHelper;

import java.util.Collection;

public class ProjectListener implements ApplicationComponent {
    private MessageBusConnection connection;

    String gradleDependencyInspectionName = "AndroidLintGradleDependency";
//    Collection<InspectionProfileImpl> profiles;

    @Override
    public void initComponent() {
        MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
        //listen for project opened event (if this is the first time, then perform lib dependency analysis
        connection = messageBus.connect();
        InspectionProfileManager.getInstance();
        connection.subscribe(Topics.QUICK_FIX_LISTENER_TOPIC, new QuickFixListener());
        RemoteDbHelper.checkDatabase();


        connection.subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {

            @Override
            public void projectOpened(@NotNull Project project) {
                try {                    

                    AndroidProjectBuildNotifications.subscribe(project, project, new BuildListener(project));

                    VirtualFileManager.getInstance().addVirtualFileListener(new FileChangeListener(project), project);

                } catch (Exception ex) {
                    
                }
            }

            @Override
            public void projectClosing(Project project) {                                

                LibraryDependenciesBuilder.getInstance(project).delete();
            }
        });
        
    }

}

