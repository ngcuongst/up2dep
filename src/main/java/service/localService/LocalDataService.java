package service.localService;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@State(
        name = "LibraryUpdateLocalDataService",
        storages = {
                @Storage(
                        file = "$APP_CONFIG$/LibraryUpdateLocalDataService.xml")
        })
public class LocalDataService implements PersistentStateComponent<LocalDataService> {
    private String pluginId;
    private String dataFolder;
    private int countQuickFix = 0;

    private String logFolder;
    private Set<String> excludedProjects = new HashSet<>();
    private Date lastAlreadySent;
    private Date lastAsked;
    private boolean showedWelcome = false;
    private int dataVersion;


    @Nullable
    @Override
    public LocalDataService getState() {
        return this;
    }

    @Override
    public void loadState(LocalDataService state) {
        XmlSerializerUtil.copyBean(state, this);
    }


    public void setDataFolder(String dataFolder) {
        this.dataFolder = dataFolder;
    }

    public void setLogFolder(String logFolder) {
        this.logFolder = logFolder;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getDataFolder() {
        return dataFolder;
    }

    public String getLogFolder() {
        return logFolder;
    }

    public Date getLastAlreadySent() {
        return lastAlreadySent;
    }

    public void setLastAlreadySent(Date lastAlreadySent) {
        this.lastAlreadySent = lastAlreadySent;
    }

    public void setExcludedProjects(Set<String> excludedProjects) {
        this.excludedProjects = excludedProjects;
    }

    public void setCountQuickFix(int countQuickFix){
        this.countQuickFix = countQuickFix;
    }

    public int getCountQuickFix(){
        return this.countQuickFix;
    }


    public void setLastAsked(Date date){
        this.lastAsked = date;
    }

    public Date getLastAsked(){
        return this.lastAsked;
    }

    public void setShowedWelcome(boolean showedWelcome){
        this.showedWelcome = showedWelcome;
    }

    public boolean getShowedWelcome(){
        return this.showedWelcome;
    }


    public Set<String> getExcludedProjects() {
        return excludedProjects;
    }


    public int getDataVersion(){
        return this.dataVersion;
    }

    public void setDataVersion(int version){
        this.dataVersion = version;
    }
}
