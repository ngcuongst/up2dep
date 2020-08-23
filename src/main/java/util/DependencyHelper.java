package util;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import model.gradle.TransitiveLocalGradleCoordinate;
import model.libInfo.OnlineLibInfo;
import model.libInfo.TransLibInfo;
import model.libInfo.UsedLibInfo;
import model.gradle.GradleCoordinate;
import model.gradle.GradleInfo;
import model.gradle.LocalGradleCoordinate;
import com.google.common.base.Splitter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import model.*;
import notification.Messages;
import notification.WarningType;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import quickfix.CompatibleVersionQuickFix;
import quickfix.LatestVersionQuickFix;
import quickfix.ShowDependenciesQuickFix;
import util.localDb.SqliteHelper;

import java.util.*;


public class DependencyHelper {
    private static HashMap<String, TransitiveLocalGradleCoordinate> getDependingLibs(LocalGradleCoordinate currentDep, HashMap<String, Set<TransitiveLocalGradleCoordinate>> dependingLibs) {
        HashMap<String, TransitiveLocalGradleCoordinate> filteredDependingLibs = new HashMap<>();
        for (Map.Entry<String, Set<TransitiveLocalGradleCoordinate>> dependingLib : dependingLibs.entrySet()) {
            Set<TransitiveLocalGradleCoordinate> realDependingLibs = dependingLib.getValue();
            for (TransitiveLocalGradleCoordinate realDependingLib : realDependingLibs) {
                if (realDependingLib.getParentLib().getRevision().equals(currentDep.getRevision())) {
                    filteredDependingLibs.put(dependingLib.getKey(), realDependingLib);
                }
            }
        }

        return filteredDependingLibs;

    }

    public static GeneralOnlineLibInfo getOnlineLibInfoOnlyTransDep(LocalGradleCoordinate currentVersion, HashMap<String, Integer> mainLibVersionOders, HashMap<String, Set<TransitiveLocalGradleCoordinate>> dependingLibs,
                                                                    HashMap<String, UsedLibInfo> dependingLibsUsedInfo,
                                                                    WarningType warningType) {
        GeneralOnlineLibInfo generalOnlineLibInfo = null;

        SqliteHelper sqliteHelper = SqliteHelper.getInstance();


        try {
            //a set contains newer versions that have all compatible API (used by the current project)
//            HashSet<String> unitedVersions = null;

            // a set contains all used API that are not compatible to the latest version of the lib
            HashSet<String> incompatibleAPIs = new HashSet<>();

            String libName = currentVersion.getName();

            ArrayList<LocalGradleCoordinate> laterVersions = sqliteHelper.getLaterVersions(libName, currentVersion.getRevision());
            if (laterVersions.size() == 0) {
                return null;
            }
            Collections.sort(laterVersions);
            LocalGradleCoordinate latestVersion = laterVersions.get(laterVersions.size() - 1);
            if (mainLibVersionOders.get(latestVersion.getRevision()) <= mainLibVersionOders.get(currentVersion.getRevision())) {
                return null;
            }
            generalOnlineLibInfo = new GeneralOnlineLibInfo();

            generalOnlineLibInfo.setTransLibInfo(new TransLibInfo(currentVersion, dependingLibsUsedInfo));

            //now we have list of compatible version for the current lib.
            HashSet<String> unitedVersionTrans = new HashSet<>();
            for (LocalGradleCoordinate nextCurrentVersion : laterVersions) {

                //get transitive dependencies of this version
                HashMap<String, TransitiveLocalGradleCoordinate> nextDependingLibs = getDependingLibs(nextCurrentVersion, dependingLibs);


                //check if the used transitive dep of the currentVersion is still available
                //if not available, next
                if (!nextDependingLibs.keySet().containsAll(dependingLibsUsedInfo.keySet())) {
                    continue;
                }

                //if available, check if APIs are still available
                boolean allAPIAvailable = true;
                for (Map.Entry<String, UsedLibInfo> dependingLib : dependingLibsUsedInfo.entrySet()) {
                    if (!dependingLibsUsedInfo.containsKey(dependingLib.getKey())) {
                        continue;
                    }
                    UsedLibInfo dependingLibUsedInfo = dependingLibsUsedInfo.get(dependingLib.getKey());

                    HashMap<String, CryptoIssue> usedAPIs = dependingLibUsedInfo.getUsedAPIs();
                    HashMap<String, HashSet<String>> dependingLibAvailableAPIs = sqliteHelper.getLibAvailableAPIs(dependingLib.getValue().getLibDetail());
                    for (String useAPI : usedAPIs.keySet()) {
//
//                        if (!dependingLibAvailableAPIs.containsKey(useAPI)) {
//                            continue;
//                        }
                        HashSet<String> dependingLibAvailableVersions = dependingLibAvailableAPIs.get(useAPI);
                        if (dependingLibAvailableVersions == null) {
                            allAPIAvailable = false;
                            break;
                        }
//                        GradleCoordinate nextDependingLib = nextDependingLibs.
                        String nextDependingLibVersion = nextDependingLibs.get(dependingLib.getKey()).getRevision();
                        if (!dependingLibAvailableVersions.contains(nextDependingLibVersion)) {
                            allAPIAvailable = false;
                            break;
                        }
                    }

                    if (!allAPIAvailable) {
                        break;
                    }

                }

                if (!allAPIAvailable) {
                    continue;
                }


                //reaching this step means that this version not only provides all API that app used, but its transitive dependency
                //also provides all API that app is using.
                unitedVersionTrans.add(nextCurrentVersion.getRevision());
            }

            if (unitedVersionTrans.size() > 0) {
                String compatibleVersionStr = getLatestVersion(unitedVersionTrans);
                LocalGradleCoordinate compatibleVersion = new LocalGradleCoordinate(currentVersion.getGroupId(), currentVersion.getArtifactId(), compatibleVersionStr);
                String vulnerabilityUrl = sqliteHelper.isVulnerableVersion(compatibleVersion);
                if (vulnerabilityUrl != null) {
                    compatibleVersion = null;
                }


                //if the latest version is the latest compatible version -> return either of them.
                if (compatibleVersion != null && latestVersion.getRevision().equals(compatibleVersion.getRevision())) {

                    //TODO set onlineLibInfo
                    generalOnlineLibInfo.setOnlineLibInfo(new OnlineLibInfo(libName, currentVersion, true, latestVersion, latestVersion, null, null, warningType, vulnerabilityUrl));


                } else {
                    // if the latest version is not the latest compatible version -> find alternative (if any) for incompatible APIs
                    HashMap<String, String> alternativeApis = getAlterNative(libName, incompatibleAPIs);
                    generalOnlineLibInfo.setOnlineLibInfo(new OnlineLibInfo(libName, currentVersion, false, compatibleVersion, latestVersion, alternativeApis, null, warningType, vulnerabilityUrl));
                }

            } else {
                //TODO need to find the version of transitive deps to enforce in the buidl.gradle files.
                generalOnlineLibInfo.setOnlineLibInfo(new OnlineLibInfo(libName, currentVersion, false, null, latestVersion, null, null, warningType, null));
            }

        } catch (Exception ex) {
            
        }


        return generalOnlineLibInfo;
    }

    static GeneralOnlineLibInfo getOnlineLibInfoWithTransDep(LocalGradleCoordinate currentVersion, HashMap<String, Integer> mainLibVersionOders, UsedLibInfo usedLibInfo, HashMap<String, Set<TransitiveLocalGradleCoordinate>> dependingLibs, HashMap<String, UsedLibInfo> dependingLibsUsedAPI, WarningType warningType) {
        SqliteHelper sqliteHelper = SqliteHelper.getInstance();

        GeneralOnlineLibInfo generalOnlineLibInfo = null;

        HashMap<String, HashSet<String>> libAvailableAPIs = sqliteHelper.getLibAvailableAPIs(currentVersion);
        HashMap<String, HashSet<String>> allAvailableAPIs = new HashMap<>();
        for (Map.Entry<String, HashSet<String>> entry : libAvailableAPIs.entrySet()) {
            String api = entry.getKey();
            HashSet<String> versions = entry.getValue();
            if (allAvailableAPIs.containsKey(api)) {
                allAvailableAPIs.get(api).addAll(versions);
            } else {
                allAvailableAPIs.put(api, versions);
            }
        }


        try {
            //a set contains newer versions that have all compatible API (used by the current project)
            HashSet<String> unitedVersions = null;

            // a set contains all used API that are not compatible to the latest version of the lib
            HashSet<String> incompatibleAPIs = new HashSet<>();

            String libName = currentVersion.getName();

            LocalGradleCoordinate latestVersion = sqliteHelper.getLatestVersion(libName, currentVersion.getRevision());

            if (latestVersion != null && mainLibVersionOders.get(latestVersion.getRevision()) < mainLibVersionOders.get(currentVersion.getRevision())) {
                return null;
            }


            for (HashMap.Entry<PsiFile, HashSet<String>> entry : usedLibInfo.getFileAppUsedAPI().entrySet()) {
                HashSet<String> usedAPIList = entry.getValue();
                // iterate over all used API of each java code file (represents by PsiFile)
                for (String usedApi : usedAPIList) {
                    // get all versions that has the used API

                    HashSet<String> filteredAvailabelInVersions = allAvailableAPIs.get(usedApi);

                    HashSet<String> availabelInVersions = new HashSet<>();
                    if (filteredAvailabelInVersions != null) {
                        for (String version : filteredAvailabelInVersions) {
                            if (mainLibVersionOders.get(version) >= mainLibVersionOders.get(currentVersion.getRevision())) {
                                availabelInVersions.add(version);
                            }
                        }
                    }
                    // if the used API is not available in any newer version -> the used API is considered incompatible
                    if (availabelInVersions.size() == 0 || !availabelInVersions.contains(latestVersion.getRevision())) {
                        incompatibleAPIs.add(usedApi);
                    }

                    if (unitedVersions == null) {
                        unitedVersions = availabelInVersions;
                        continue;
                    }

//                    if (availabelInVersions != null) {
                        /* find the intersection between the previous version set and current version set
                        doing this to find a newer version (compatible version) that contain all used APIs
                        */
                    unitedVersions.retainAll(availabelInVersions);
//                    }
                }
            }

            //now we have list of compatible version for the current lib.
            HashSet<String> unitedVersionTrans = new HashSet<>();

            for (String unitedVersion : unitedVersions) {
                //get transitive dependencies of this version
                LocalGradleCoordinate nextCurrentVersion = new LocalGradleCoordinate(currentVersion.getGroupId(), currentVersion.getArtifactId(), unitedVersion);
                HashMap<String, TransitiveLocalGradleCoordinate> nextDependingLibs = getDependingLibs(nextCurrentVersion, dependingLibs);


                //check if the used transitive dep of the currentVersion is still available
                //if not available, next
                if (!nextDependingLibs.keySet().containsAll(dependingLibs.keySet())) {
                    continue;
                }

                //if available, check if APIs are still available
                boolean allAPIAvailable = true;
                for (Map.Entry<String, TransitiveLocalGradleCoordinate> dependingLib : nextDependingLibs.entrySet()) {
                    UsedLibInfo dependingLibUsedInfo = dependingLibsUsedAPI.get(dependingLib.getValue().getName());
                    //TODO null exception
                    HashMap<String, CryptoIssue> usedAPIs = dependingLibUsedInfo.getUsedAPIs();
                    HashMap<String, HashSet<String>> dependingLibAvailableAPIs = sqliteHelper.getLibAvailableAPIs(dependingLib.getValue());
                    for (String useAPI : usedAPIs.keySet()) {

                        if (!dependingLibAvailableAPIs.containsKey(useAPI)) {
                            continue;
                        }
                        HashSet<String> dependingLibAvailableVersions = dependingLibAvailableAPIs.get(useAPI);
                        if (dependingLibAvailableVersions == null) {
                            allAPIAvailable = false;
                            break;
                        }
                        String nextDependingLibVersion = null;
                        if (nextDependingLibs.get(dependingLib.getKey()).getParentLib().getRevision().equals(unitedVersion)) {
                            nextDependingLibVersion = nextDependingLibs.get(dependingLib.getKey()).getRevision();
                        }
                        if (!dependingLibAvailableVersions.contains(nextDependingLibVersion)) {
                            allAPIAvailable = false;
                            break;
                        }
                    }

                    if (!allAPIAvailable) {
                        break;
                    }

                }

                if (!allAPIAvailable) {
                    continue;
                }


                //reaching this step means that this version not only provides all API that app used, but its transitive dependency
                //also provides all API that app is using.
                unitedVersionTrans.add(unitedVersion);
            }
            generalOnlineLibInfo = new GeneralOnlineLibInfo();
            generalOnlineLibInfo.setTransLibInfo(new TransLibInfo(currentVersion, dependingLibsUsedAPI));

            if (unitedVersionTrans.size() > 0) {
                String compatibleVersionStr = getLatestVersion(unitedVersionTrans);
                LocalGradleCoordinate compatibleVersion = new LocalGradleCoordinate(currentVersion.getGroupId(), currentVersion.getArtifactId(), compatibleVersionStr);
                String vulnerabilityUrl = sqliteHelper.isVulnerableVersion(compatibleVersion);
                if (vulnerabilityUrl != null) {
                    compatibleVersion = null;
                }

                //if the latest version is the latest compatible version -> return either of them.
                if (compatibleVersion != null && latestVersion.getRevision().equals(compatibleVersion.getRevision())) {
                    generalOnlineLibInfo.setOnlineLibInfo(new OnlineLibInfo(libName, currentVersion, true, latestVersion, latestVersion, null, usedLibInfo, warningType, vulnerabilityUrl));


                } else {
                    // if the latest version is not the latest compatible version -> find alternative (if any) for incompatible APIs
                    HashMap<String, String> alternativeApis = getAlterNative(libName, incompatibleAPIs);
                    generalOnlineLibInfo.setOnlineLibInfo(new OnlineLibInfo(libName, currentVersion, false, compatibleVersion, latestVersion, alternativeApis, usedLibInfo, warningType, vulnerabilityUrl));
                }

            } else {
                //TODO need to find the version of transitive deps to enforce in the buidl.gradle files.
                HashMap<String, String> alternativeApis = getAlterNative(libName, incompatibleAPIs);
                generalOnlineLibInfo.setOnlineLibInfo(new OnlineLibInfo(libName, currentVersion, false, null, latestVersion, alternativeApis, usedLibInfo, warningType, null));
            }


        } catch (Exception ex) {
            
        }


        return generalOnlineLibInfo;
    }


    static OnlineLibInfo getOnlineLibInfo(LocalGradleCoordinate currentVersion, HashMap<String, Integer> mainLibVersionOrders, HashMap<String, Set<TransitiveLocalGradleCoordinate>> dependingLibs, UsedLibInfo usedLibInfo, WarningType warningType) {
        OnlineLibInfo onlineLibInfo = null;
        SqliteHelper sqliteHelper = SqliteHelper.getInstance();
        HashMap<String, HashSet<String>> libAvailableAPIs = sqliteHelper.getLibAvailableAPIs(currentVersion);
        //HashMap<String, HashSet<String>> = <API, <versions>>
        //TODO put all here will overwrite old value that exist in allAvailableAPIs, add more but not overwrite
        HashMap<String, HashSet<String>> allAvailableAPIs = new HashMap<>();
        for (Map.Entry<String, HashSet<String>> entry : libAvailableAPIs.entrySet()) {
            String api = entry.getKey();
            HashSet<String> versions = entry.getValue();
            if (allAvailableAPIs.containsKey(api)) {
                allAvailableAPIs.get(api).addAll(versions);
            } else {
                allAvailableAPIs.put(api, versions);
            }
        }


        try {
            //a set contains newer versions that have all compatible API (used by the current project)
            HashSet<String> unitedVersions = null;

            // a set contains all used API that are not compatible to the latest version of the lib
            HashSet<String> incompatibleAPIs = new HashSet<>();

            String libName = currentVersion.getName();

            LocalGradleCoordinate latestVersion = sqliteHelper.getLatestVersion(libName, currentVersion.getRevision());
            if (latestVersion != null && mainLibVersionOrders.get(latestVersion.getRevision()) <= mainLibVersionOrders.get(currentVersion.getRevision())) {
                return null;
            }


            for (HashMap.Entry<PsiFile, HashSet<String>> entry : usedLibInfo.getFileAppUsedAPI().entrySet()) {
                HashSet<String> usedAPIList = entry.getValue();
                // iterate over all used API of each java code file (represents by PsiFile)
                for (String usedApi : usedAPIList) {
                    // get all versions that has the used API
                    HashSet<String> availabelInVersions = allAvailableAPIs.get(usedApi);
                    // if the used API is not available in any newer version -> the used API is considered incompatible
                    if (availabelInVersions == null || !availabelInVersions.contains(latestVersion.getRevision())) {
                        incompatibleAPIs.add(usedApi);
                    }

                    if (unitedVersions == null) {
                        unitedVersions = availabelInVersions;
                        continue;
                    }

                    if (availabelInVersions != null) {
                            /* find the intersection between the previous version set and current version set
                            doing this to find a newer version (compatible version) that contain all used APIs
                            */
                        unitedVersions.retainAll(availabelInVersions);
                    }
                }
            }

            //if compatible version(s) is found, process to check additionally information such as latest version, and alternative APIs
            if (unitedVersions != null) {
                //TODO here also needs to find next transitive dependencies to solve potential conflict
                for (String unitedVersion : unitedVersions) {
                    LocalGradleCoordinate nextCurrentVersion = new LocalGradleCoordinate(currentVersion.getGroupId(), currentVersion.getArtifactId(), unitedVersion);
                    HashMap<String, TransitiveLocalGradleCoordinate> nextDependingLibs = getDependingLibs(nextCurrentVersion, dependingLibs);

                    String a = "b";
                }
                //get latest version from the set of compatible versions
                String compatibleVersionStr = getLatestVersion(unitedVersions);
                if (compatibleVersionStr != null) {

                    LocalGradleCoordinate compatibleVersion = new LocalGradleCoordinate(currentVersion.getGroupId(), currentVersion.getArtifactId(), compatibleVersionStr);
                    String vulnerabilityUrl = sqliteHelper.isVulnerableVersion(compatibleVersion);
                    if (vulnerabilityUrl != null) {
                        compatibleVersion = null;
                    }

                    //if the latest version is the latest compatible version -> return either of them.
                    if (compatibleVersion != null && latestVersion.getRevision().equals(compatibleVersion.getRevision())) {


                        onlineLibInfo = new OnlineLibInfo(libName, currentVersion, true, latestVersion, latestVersion, null, null, warningType, vulnerabilityUrl);

                    } else {
                        // if the latest version is not the latest compatible version -> find alternative (if any) for incompatible APIs
                        HashMap<String, String> alternativeApis = getAlterNative(libName, incompatibleAPIs);
                        onlineLibInfo = new OnlineLibInfo(libName, currentVersion, false, compatibleVersion, latestVersion, alternativeApis, usedLibInfo, warningType, vulnerabilityUrl);
                    }


                } else {
                    // if the latest version is not the latest compatible version -> find alternative (if any) for incompatible APIs
                    HashMap<String, String> alternativeApis = getAlterNative(libName, incompatibleAPIs);
                    onlineLibInfo = new OnlineLibInfo(libName, currentVersion, false, null, latestVersion,
                            alternativeApis, usedLibInfo, warningType, null);

                }

            } else {
                // in this case, no compatible version is found. This means all the used APIs are not available in any of the newer versions
                // find alternative APIs for the used APIs OR no APIs is used in the code
                HashMap<String, String> alternativeApis = getAlterNative(libName, incompatibleAPIs);
                //test here
                onlineLibInfo = new OnlineLibInfo(libName, currentVersion, false, null, latestVersion, alternativeApis,
                        usedLibInfo, warningType, null);

            }

        } catch (Exception ex) {
            
        }

        return onlineLibInfo;
    }


    public static HashMap<String, String> check2ndDeps(Project project, String firstLibName, String version, HashSet<String> usedAPIs, Set<String> otherLibs) {
        SqliteHelper sqliteHelper = SqliteHelper.getInstance();
        HashMap<String, String> tobeUpdated = new HashMap<>();
        //key: lib name, values: list of versions associated with the dependee API
        HashMap<String, HashSet<String>> libVersions = new HashMap<>();
        if (usedAPIs != null) {
            for (String api : usedAPIs) {
                HashSet<String> dependees = sqliteHelper.getDependees(firstLibName, version, api);
                if (dependees != null) {
                    for (String dependee : dependees) {
                        ApiInfo apiInfo = sqliteHelper.getApiInfo(project, otherLibs, dependee);
                        if (apiInfo != null) {
                            HashSet<String> versions = apiInfo.getVersions();
                            if (libVersions.containsKey(apiInfo.getLibName())) {
                                HashSet<String> currentVersions = libVersions.get(apiInfo.getLibName());
                                currentVersions.retainAll(versions);
                                libVersions.put(apiInfo.getLibName(), currentVersions);
                            } else {
                                libVersions.put(apiInfo.getLibName(), versions);
                            }

                        }
                    }
                }
            }
        }

        //check if compatible version
        for (Map.Entry<String, HashSet<String>> entry : libVersions.entrySet()) {
            String secondDepLibName = entry.getKey();
            HashSet<String> secondDepLibVersions = entry.getValue();
            LocalGradleCoordinate currentGradleDep = LibraryDependenciesBuilder.getInstance(project).getLibDetail(secondDepLibName);
            if (secondDepLibVersions.contains(currentGradleDep.getRevision())) {
                //this 2nd dependency is still compatible -> pass
                continue;
            }

            //the api that this library (libName) depends on belongs to a newer version of the 2nd dep (secondDepLibName)
            //-> 2nd dep needs to be upgraded too
            String secondDepLatestVersion = getLatestVersion(secondDepLibVersions);
            tobeUpdated.put(secondDepLibName, secondDepLatestVersion);

        }

        return tobeUpdated;
    }

    private static HashMap<String, String> getAlterNative(String libName, HashSet<String> usedAPIs) {
        HashMap<String, String> alternatives = null;
        try {
            SqliteHelper sqliteHelper = SqliteHelper.getInstance();
            alternatives = new HashMap<>();
            for (String usedAPI : usedAPIs) {
                String alternativeAPI = sqliteHelper.getAlternativeAPI(libName, usedAPI);
//                if (alternativeAPI != null) {
                alternatives.put(usedAPI, alternativeAPI);
//                }
            }
        } catch (Exception ex) {
            
        }
        return alternatives;
    }

    private static String getLatestVersion(HashSet<String> allVersions) {
        DefaultArtifactVersion[] artifactVersions = new DefaultArtifactVersion[allVersions.size()];
        int index = 0;
        for (String version : allVersions) {
            DefaultArtifactVersion defaultArtifactVersion = new DefaultArtifactVersion(version);
            artifactVersions[index] = defaultArtifactVersion;
            index += 1;
        }
        if (artifactVersions.length > 0) {
            DefaultArtifactVersion max = Collections.max(Arrays.asList(artifactVersions));
            return max.toString();
        } else {
            return null;
        }
    }

    public static LibraryOrderEntry getLibraryForFile(ProjectFileIndex fileIndex, VirtualFile virtualFile) {
        if (virtualFile == null) return null;
        List<OrderEntry> orders = fileIndex.getOrderEntriesForFile(virtualFile);
        for (OrderEntry order : orders) {
            if (order instanceof LibraryOrderEntry) return (LibraryOrderEntry) order;
        }
        return null;
    }

    public static LocalGradleCoordinate checkDependencies(@NonNull String value) {
        if (value.startsWith("files(")) {
            //local library
        } else {
            String dependency = getStringLiteralValue(value);
            if (dependency == null) {
                dependency = getNamedDependency(value);
            }
            // If the dependency is a GString (i.e. it uses Groovy variable substitution,
            // with a $variable_name syntax) then don't try to parse it.
            if (dependency != null) {
                LocalGradleCoordinate gc = LocalGradleCoordinate.parseCoordinateString(dependency);
                return gc;
            }

        }
        return null;
    }


    private static String getNamedDependency(@NonNull String expression) {
        String result = null;
        try {


            if (expression.indexOf(',') != -1 && expression.contains("version:")) {
                String artifact = null;
                String group = null;
                String version = null;
                Splitter splitter = Splitter.on(',').omitEmptyStrings().trimResults();
                for (String property : splitter.split(expression)) {
                    int colon = property.indexOf(':');
                    if (colon == -1) {
                        return null;
                    }
                    char quote = '\'';
                    int valueStart = property.indexOf(quote, colon + 1);
                    if (valueStart == -1) {
                        quote = '"';
                        valueStart = property.indexOf(quote, colon + 1);
                    }
                    if (valueStart == -1) {
                        // For example, "transitive: false"
                        continue;
                    }
                    valueStart++;
                    int valueEnd = property.indexOf(quote, valueStart);
                    if (valueEnd == -1) {
                        return null;
                    }
                    String value = property.substring(valueStart, valueEnd);
                    if (property.startsWith("group:")) {
                        group = value;
                    } else if (property.startsWith("name:")) {
                        artifact = value;
                    } else if (property.startsWith("version:")) {
                        version = value;
                    }
                }

                if (artifact != null && group != null && version != null) {
                    result = group + ':' + artifact + ':' + version;
                }
            }

        } catch (Exception ex) {
            
        }
        return result;
    }


    @Nullable
    private static String getStringLiteralValue(@NonNull String value) {
        if (value.length() > 2 && (value.startsWith("'") && value.endsWith("'") ||
                value.startsWith("\"") && value.endsWith("\""))) {
            return value.substring(1, value.length() - 1);
        }

        return null;
    }

    private static HashMap<String, GradleInfo> findCorrespondingDependencies(PsiElement element, HashMap<String, String> extraProperties) {
        HashMap<String, GradleInfo> dependencyElements = null;
        PsiElement parent = element.getParent();
        if (parent != null) {
            PsiElement grantParent = parent.getParent();
            if (grantParent != null && grantParent instanceof GrClosableBlock) {
                GrClosableBlock closableBlock = (GrClosableBlock) grantParent;
                dependencyElements = new HashMap<>();
                for (PsiElement currentElement : closableBlock.getChildren()) {
                    GradleInfo gradleInfo = GradleHelper.extractCoordinate(currentElement, extraProperties);
                    if (gradleInfo != null) {
                        dependencyElements.put(gradleInfo.getGradleCoordinate().toString(), gradleInfo);
                    }
                }
            }
        }

        return dependencyElements;
    }

//    //todo check for compatibility too
//    public static void checkForSecondDep(OnlineLibInfo libInfo, PsiElement currentElement, HashMap<String, LocalGradleCoordinate> libDetails, HashMap<String, String> extraProperties) {
//        Set<String> libNames = libDetails.keySet();
//        HashMap<String, String> libsToBeUpdated = DependencyHelper.check2ndDeps(currentElement.getProject(), libInfo.getLibName(), libInfo.getLatestVersion().getRevision(), libInfo.getUsedAPIs(), libNames);
//        if (libsToBeUpdated.size() > 0) {
//            StringBuilder message = new StringBuilder(String.format("The following dependency also need(s) to be updated as %s depends on it. Would you like to update it too?\n", libInfo.getLibName()));
//            ArrayList<PsiDependency> tobeUpdatedElements = new ArrayList<>();
//            HashMap<String, GradleInfo> correspondingDependencies = findCorrespondingDependencies(currentElement, extraProperties);
//            if (correspondingDependencies != null) {
//                for (Map.Entry<String, String> entry : libsToBeUpdated.entrySet()) {
//                    String libName = entry.getKey();
//                    String libVersion = entry.getValue();
//                    String currentLine = String.format("%s:%s\n", libName, libVersion);
//                    message.append(currentLine);
//                    LocalGradleCoordinate coordinates = libDetails.get(libName);
//                    if (correspondingDependencies.containsKey(coordinates.toString())) {
//                        GradleInfo gradleInfo = correspondingDependencies.get(coordinates.toString());
//                        PsiElement element = gradleInfo.getDependencyElement();
//                        PsiDependency dependency = new PsiDependency(element, gradleInfo.getGradleCoordinate(), libVersion);
//                        tobeUpdatedElements.add(dependency);
//                    }
//
//                }
//
//
//                ApplicationManager.getApplication().invokeLater(() -> {
//                    int reply = JOptionPane.showConfirmDialog(new JFrame(),
//                            message, "Secondary dependencies", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, AllIcons.General.QuestionDialog);
//                    if (reply == JOptionPane.YES_OPTION) {
//                        //perform updating 2nd dependencies
//                        upgradeVersions(currentElement.getProject(), tobeUpdatedElements);
//
//                    }
//                });
//            }
//        }
//    }


    private static void upgradeVersions(Project project, ArrayList<PsiDependency> tobeUpdatedElements) {
        ApplicationManager.getApplication().invokeLater(() -> WriteCommandAction.runWriteCommandAction(project, () -> {
            Document document = FileDocumentManager.getInstance().getDocument(tobeUpdatedElements.get(0).getElement().getContainingFile().getVirtualFile());
            final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
            documentManager.doPostponedOperationsAndUnblockDocument(document);
            documentManager.commitDocument(document);
            GroovyPsiElementFactory elementFactory = GroovyPsiElementFactory.getInstance(project);
            for (PsiDependency psiDependency : tobeUpdatedElements) {
                PsiElement psiElement = psiDependency.getElement();
                LocalGradleCoordinate coordinate = psiDependency.getCoordinate();
                String newerDependency = String.format("'%s:%s:%s'", coordinate.getGroupId(), coordinate.getArtifactId(), psiDependency.getNewerVersion());

                PsiElement literalExpression = elementFactory.createExpressionFromText(newerDependency, null);

                psiElement.replace(literalExpression);
            }
        }));

    }


    public static LocalQuickFix[] generateQuickfixes(GradleInfo gradleInfo, GeneralOnlineLibInfo generalOnlineLibInfo) {
        LocalQuickFix[] finalQuickFixes = null;
        ArrayList<LocalQuickFix> quickFixes = new ArrayList<>();
        OnlineLibInfo currentOnlineLibInfo = generalOnlineLibInfo.getOnlineLibInfo();

        if (currentOnlineLibInfo != null) {


            //if the latest version is also the compatible version
            if (currentOnlineLibInfo.isLatest()) {
                if (currentOnlineLibInfo.getWarningType() == WarningType.INSECURE_API_USED) {
                    quickFixes.add(new ShowDependenciesQuickFix(generalOnlineLibInfo));
                }
                quickFixes.add(new LatestVersionQuickFix(generalOnlineLibInfo.getOnlineLibInfo()));

            } else {
                if (currentOnlineLibInfo.getCompatibleVersion() != null && !gradleInfo.getGradleCoordinate().equals(currentOnlineLibInfo.getCompatibleVersion())) { //if the latest version is not compatible with current API usages
                    quickFixes.add(new ShowDependenciesQuickFix(generalOnlineLibInfo));
                    quickFixes.add(new CompatibleVersionQuickFix(currentOnlineLibInfo));
                    quickFixes.add(new LatestVersionQuickFix(currentOnlineLibInfo));

                } else {
                    //if no compatible version is found
                    quickFixes.add(new ShowDependenciesQuickFix(generalOnlineLibInfo));
                    quickFixes.add(new LatestVersionQuickFix(currentOnlineLibInfo));

                }
            }

            //add feedback option to the last position, Intellij shows the list of quick-fixes in a reverse-order



//            ProblemHighlightType problemHighlightType = ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
//
//            if (currentOnlineLibInfo.getWarningType() == WarningType.VULNERABLE_OUTDATED) {
//                problemHighlightType = ProblemHighlightType.ERROR;
//            } else if (currentOnlineLibInfo.getWarningType() == WarningType.INSECURE_API_USED) {
//                problemHighlightType = ProblemHighlightType.ERROR;
//            }

//            String message = Messages.getMessage(gradleInfo.getGradleCoordinate().getName(), currentOnlineLibInfo.getWarningType(), currentOnlineLibInfo.getLatestVersion().getRevision(), onlineLibInfo.getVulnerabilityUrl());
            finalQuickFixes = new LocalQuickFix[quickFixes.size()];
            int index = 0;
            for (LocalQuickFix quickFix : quickFixes) {
                finalQuickFixes[index] = quickFix;
                index++;
            }

        }

        return finalQuickFixes;
    }

//    private ArrayList<LocalQuickFix> generateQuickfixes(GradleInfo gradleInfo,
//                                                        HashMap<String, OnlineLibInfo> dependingOnlineLibInfo) {
//        ArrayList<LocalQuickFix> quickFixes = new ArrayList<>();
//
//
//
//        if (currentOnlineLibInfo != null) {
//
//
//            //if the latest version is also the compatible version
//            if (currentOnlineLibInfo.isLatest()) {
//                if (currentOnlineLibInfo.getWarningType() == WarningType.INSECURE_API_USED) {
//                    quickFixes.add(new ShowDependenciesQuickFix(currentOnlineLibInfo, dependingOnlineLibInfo));
//                }
//                quickFixes.add(new LatestVersionQuickFix(currentOnlineLibInfo));
//
//            } else {
//                if (currentOnlineLibInfo.getCompatibleVersion() != null && !gradleInfo.getGradleCoordinate().equals(currentOnlineLibInfo.getCompatibleVersion())) { //if the latest version is not compatible with current API usages
//                    quickFixes.add(new ShowDependenciesQuickFix(currentOnlineLibInfo, dependingOnlineLibInfo));
//                    quickFixes.add(new CompatibleVersionQuickFix(currentOnlineLibInfo));
//                    quickFixes.add(new LatestVersionQuickFix(currentOnlineLibInfo));
//
//                } else {
//                    //if no compatible version is found
//                    quickFixes.add(new ShowDependenciesQuickFix(currentOnlineLibInfo, dependingOnlineLibInfo));
//                    quickFixes.add(new LatestVersionQuickFix(currentOnlineLibInfo));
//
//                }
//            }
//
//            //add feedback option to the last position, Intellij shows the list of quick-fixes in a reverse-order
//            quickFixes.add(0, new SendFeedback(gradleInfo.getGradleCoordinate().toString()));
//
//
//            ProblemHighlightType problemHighlightType = ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
//
//            if (onlineLibInfo.getWarningType() == WarningType.VULNERABLE_OUTDATED) {
//                problemHighlightType = ProblemHighlightType.ERROR;
//            } else if (onlineLibInfo.getWarningType() == WarningType.INSECURE_API_USED) {
//                problemHighlightType = ProblemHighlightType.ERROR;
//            }
//
//            String message = Messages.getMessage(gradleInfo.getGradleCoordinate().getName(), onlineLibInfo.getWarningType(), onlineLibInfo.getLatestVersion().getRevision(), onlineLibInfo.getVulnerabilityUrl());
//            LocalQuickFix[] finalQuickFixes = new LocalQuickFix[quickFixes.size()];
//            int index = 0;
//            for (LocalQuickFix quickFix : quickFixes) {
//                finalQuickFixes[index] = quickFix;
//                index++;
//            }
//            registerError(gradleInfo.getDependencyElement(), message, finalQuickFixes, problemHighlightType);
//        }
//
//        return quickFixes;
//    }

//    private ArrayList<LocalQuickFix> generateQuickfixes(GradleInfo gradleInfo, OnlineLibInfo currentOnlineLibInfo,
//                                                        HashMap<String, OnlineLibInfo> dependingOnlineLibInfos) {
//        ArrayList<LocalQuickFix> quickFixes = new ArrayList<>();
//
//
//
//        if (currentOnlineLibInfo != null) {
//
//
//            //if the latest version is also the compatible version
//            if (currentOnlineLibInfo.isLatest()) {
//                if (currentOnlineLibInfo.getWarningType() == WarningType.INSECURE_API_USED) {
//                    quickFixes.add(new ShowDependenciesQuickFix(currentOnlineLibInfo));
//                }
//                quickFixes.add(new LatestVersionQuickFix(currentOnlineLibInfo));
//
//            } else {
//                //if the latest version is not compatible with current API usages
//                LocalGradleCoordinate compatibleVersion = currentOnlineLibInfo.getCompatibleVersion();
//                if ( compatibleVersion != null && !gradleInfo.getGradleCoordinate().equals(compatibleVersion)) {
//                    quickFixes.add(new ShowDependenciesQuickFix(currentOnlineLibInfo, dependingOnlineLibInfos));
//                    quickFixes.add(new CompatibleVersionQuickFix(currentOnlineLibInfo));
//                    quickFixes.add(new LatestVersionQuickFix(currentOnlineLibInfo));
//
//                } else {
//                    //if no compatible version is found
//                    quickFixes.add(new ShowDependenciesQuickFix(currentOnlineLibInfo, dependingOnlineLibInfos));
//                    quickFixes.add(new LatestVersionQuickFix(currentOnlineLibInfo));
//
//                }
//            }
//
//            //add feedback option to the last position, Intellij shows the list of quick-fixes in a reverse-order
//            quickFixes.add(0, new SendFeedback(gradleInfo.getGradleCoordinate().toString()));
//
//
//
//
//
//            LocalQuickFix[] finalQuickFixes = new LocalQuickFix[quickFixes.size()];
//            int index = 0;
//            for (LocalQuickFix quickFix : quickFixes) {
//                finalQuickFixes[index] = quickFix;
//                index++;
//            }
//
//        }
//
//        return quickFixes;
//    }

    public static String getWarningMessage(GradleInfo gradleInfo, GeneralOnlineLibInfo generalOnlineLibInfo) {
        OnlineLibInfo onlineLibInfo = generalOnlineLibInfo.getOnlineLibInfo();
        //TODO check for url of trans dep, and customize message w.r.t trans deps
        if (onlineLibInfo != null) {
            return Messages.getMessage(gradleInfo.getGradleCoordinate().getName(), onlineLibInfo.getWarningType(), onlineLibInfo.getLatestVersion().getRevision(), onlineLibInfo.getVulnerabilityUrl());
        } else {
            return null;
        }
    }

    public static ProblemHighlightType getHighlightType(GeneralOnlineLibInfo generalOnlineLibInfo) {
        ProblemHighlightType problemHighlightType = ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
        OnlineLibInfo currentOnlineLibInfo = generalOnlineLibInfo.getOnlineLibInfo();

        if (currentOnlineLibInfo.getWarningType() == WarningType.VULNERABLE_OUTDATED) {
            problemHighlightType = ProblemHighlightType.ERROR;
        } else if (currentOnlineLibInfo.getWarningType() == WarningType.INSECURE_API_USED) {
            problemHighlightType = ProblemHighlightType.ERROR;
        }
        //TODO check warning type for trans Dep
//        TransLibInfo  transLibInfo = generalOnlineLibInfo.getTransLibInfo();
//        if(transLibInfo != null) {
//            for (OnlineLibInfo dependingOnlineLibInfo : transLibInfo.getLibUsedInfos()) {
//                if (dependingOnlineLibInfo.getWarningType() == WarningType.VULNERABLE_OUTDATED) {
//                    problemHighlightType = ProblemHighlightType.ERROR;
//                } else if (dependingOnlineLibInfo.getWarningType() == WarningType.INSECURE_API_USED) {
//                    problemHighlightType = ProblemHighlightType.ERROR;
//                }
//            }
//        }
        return problemHighlightType;
    }
}
