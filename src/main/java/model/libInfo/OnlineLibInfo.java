package model.libInfo;

import com.intellij.psi.PsiFile;
import model.CryptoIssue;
import model.gradle.GradleCoordinate;
import model.gradle.LocalGradleCoordinate;
import notification.WarningType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OnlineLibInfo {
    private boolean isLatest;
    private final LocalGradleCoordinate compatibleVersion;
    private LocalGradleCoordinate latestVersion;
    private HashMap<String, String> alternatives;
    private HashMap<PsiFile, HashSet<String>> fileUsedAPi;
    private Set<PsiFile> usedLibFiles;
    private final GradleCoordinate currentVersion;
    private  HashMap<String, CryptoIssue> usedAPIs = null;
    private final String libName;
    private final WarningType warningType;
    private final String vulnerabilityUrl;


    public OnlineLibInfo(String libName, GradleCoordinate currentVersion, boolean isLatest, LocalGradleCoordinate compatibleVersion,
                         HashMap<String, String> alternatives, UsedLibInfo usedLibInfo,  WarningType warningType, String vulnerabilityUrl) {
        this.currentVersion = currentVersion;
        this.isLatest = isLatest;
        this.compatibleVersion = compatibleVersion;
        if (isLatest) {
            this.latestVersion = this.compatibleVersion;
        }
        this.alternatives = alternatives;
        if( usedLibInfo != null) {
            this.usedAPIs = usedLibInfo.getUsedAPIs();
        }
        this.libName = libName;
        this.warningType = getWarningType(warningType);
        this.vulnerabilityUrl = vulnerabilityUrl;
    }

    public OnlineLibInfo(String libName, GradleCoordinate currentVersion, boolean isLatest, LocalGradleCoordinate compatibleVersion,
                         LocalGradleCoordinate latestVersion, HashMap<String, String> alternatives, UsedLibInfo usedLibInfo, WarningType warningType, String vulnerabilityUrl) {
        this.isLatest = isLatest;
        this.compatibleVersion = compatibleVersion;
        this.latestVersion = latestVersion;
        this.alternatives = alternatives;
        if(usedLibInfo != null) {
            this.fileUsedAPi = usedLibInfo.getFileAppUsedAPI();
            this.usedLibFiles = usedLibInfo.getUsedLibFiles();
            this.usedAPIs = usedLibInfo.getUsedAPIs();
        }
        this.currentVersion = currentVersion;

        this.libName = libName;
        this.warningType = getWarningType(warningType);
        this.vulnerabilityUrl = vulnerabilityUrl;
    }

    private WarningType getWarningType(WarningType preWarningType) {
        WarningType adjustedWarningType = preWarningType;

        if(this.usedAPIs != null) {
            for (Map.Entry<String, CryptoIssue> usedAPI : this.usedAPIs.entrySet()) {
                if (usedAPI.getValue() != null) {
                    adjustedWarningType = WarningType.INSECURE_API_USED;
                }
            }
        }

        return adjustedWarningType;
    }


    public LocalGradleCoordinate getCompatibleVersion() {
        return this.compatibleVersion;
    }

    public HashMap<PsiFile, HashSet<String>> getFileUsedAPi() {
        return fileUsedAPi;
    }


    public Set<PsiFile> getUsedLibFiles() {
        return usedLibFiles;
    }

    public HashMap<String, String> getAlternatives() {
        return alternatives;
    }

    public boolean isLatest() {
        return isLatest;
    }

    public GradleCoordinate getLatestVersion() {
        return latestVersion;
    }

    public GradleCoordinate getCurrentVersion() {
        return currentVersion;
    }

    public HashMap<String, CryptoIssue> getUsedAPIs() {
        return usedAPIs;
    }

    public String getLibName() {
        return libName;
    }

    public WarningType getWarningType() {
        return warningType;
    }

    public String getVulnerabilityUrl() {
        return vulnerabilityUrl;
    }
}
