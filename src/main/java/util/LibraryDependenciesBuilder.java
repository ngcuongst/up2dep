package util;

import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.AnalysisScopeBundle;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.GeneratedSourcesFilter;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packageDependencies.DependenciesBuilder;
import com.android.builder.model.AndroidBundle;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.util.PsiTreeUtil;
import model.*;
import model.gradle.TransitiveLocalGradleCoordinate;
import model.libInfo.OnlineLibInfo;
import model.libInfo.UsedLibInfo;
import model.gradle.GradleCoordinate;
import model.gradle.LocalGradleCoordinate;
import notification.WarningType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.*;

import util.localDb.SqliteHelper;

import java.util.*;

//TODO combine this one with LibraryUsageHelper.java
public class LibraryDependenciesBuilder extends DependenciesBuilder {
    
    private final Map<PsiFile, Set<PsiFile>> myDirectDependencies = new HashMap<>();
    private final Map<PsiFile, Set<PsiFile>> dependencies = new HashMap<>();
    private HashMap<String, ArrayList<PsiElement>> libDependencies = null;
    private HashMap<String, LocalGradleCoordinate> libVersions = null;


    private HashMap<String, Set<PsiFile>> usedLibFiles = null;
    private final Project mproject;
    private OnlineLibInfo onlineLibInfo;
    private String libraryName;
    //    private HashMap<String, LocalGradleCoordinate> libDetails = new HashMap<>();
    private static SqliteHelper sqliteHelper = SqliteHelper.getInstance();
    private static HashMap<Project, LibraryDependenciesBuilder> instances = new HashMap<>();
    private static HashSet<VirtualFile> incompatibleDependencies = null;

    public static LibraryDependenciesBuilder getInstance(Project project) {
        if (instances.containsKey(project)) {
            return instances.get(project);
        } else {
            LibraryDependenciesBuilder instance = new LibraryDependenciesBuilder(project);
            instances.put(project, instance);
            return instance;
        }
    }

    public void delete() {
        instances.remove(mproject);
    }

    public LibraryDependenciesBuilder(Project project) {
        super(project, new AnalysisScope(GlobalSearchScopesCore.projectProductionScope(project), project));
        this.mproject = project;
        this.libraryName = null;
    }

    public HashMap<String, String> getAlternativeAPI() {
        if (onlineLibInfo != null) {
            return onlineLibInfo.getAlternatives();
        } else {
            return null;
        }
    }

    public HashMap<String, CryptoIssue> getUsedAPIs() {
        if (onlineLibInfo != null) {
            return onlineLibInfo.getUsedAPIs();
        } else {
            return null;
        }
    }

    @Override
    public String getRootNodeNameInUsageView() {
        return "Usage of the dependency tree selection in the above tree";
    }

    @Override
    public String getInitialUsagesPosition() {
        return String.format("Select a scope to find usages of the %s", this.libraryName);
    }

    @Override
    public boolean isBackward() {
        return false;
    }

    public void analyzeLibDependency(GeneralOnlineLibInfo generalOnlineLibInfo, String libName) {
        this.onlineLibInfo = generalOnlineLibInfo.getOnlineLibInfo();
        this.libraryName = libName;
        final PsiManager psiManager = PsiManager.getInstance(getProject());
        psiManager.startBatchFilesProcessingMode();
        final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(getProject()).getFileIndex();
        try {
            getScope().accept(new PsiRecursiveElementVisitor() {
                @Override
                public void visitFile(final PsiFile file) {
                    if (onlineLibInfo.getFileUsedAPi().containsKey(file)) {
                        visit(file, fileIndex, psiManager);
                    }
                }
            });
        } catch (Exception ex) {
            
        } finally {
            psiManager.finishBatchFilesProcessingMode();
        }
    }

    @Override
    public void analyze() {
        final PsiManager psiManager = PsiManager.getInstance(getProject());
        psiManager.startBatchFilesProcessingMode();
        final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(getProject()).getFileIndex();
        try {
            getScope().accept(new PsiRecursiveElementVisitor() {
                @Override
                public void visitFile(final PsiFile file) {
                    if (onlineLibInfo.getFileUsedAPi().containsKey(file)) {
                        visit(file, fileIndex, psiManager);
                    }
                }
            });
        } catch (Exception ex) {
            
        } finally {
            psiManager.finishBatchFilesProcessingMode();
        }
    }

    @Override
    public Map<PsiFile, Set<PsiFile>> getDependencies() {
        return dependencies;
    }

    private void analyzeFileDependencies(@NotNull PsiFile file, ProjectFileIndex fileIndex, Set<PsiFile> found) {
        Collection<PsiElement> methodCallExpressions = null;
        boolean isJavaFile = true;
        if (file instanceof PsiJavaFile) {
            methodCallExpressions = PsiTreeUtil.findChildrenOfAnyType(file, PsiMethodCallExpression.class, PsiConstructorCall.class);
        } else if (file instanceof KtFile) {
            methodCallExpressions = (PsiTreeUtil.findChildrenOfAnyType(file, KtDotQualifiedExpression.class, KtCallExpression.class));
            isJavaFile = false;
        } else {
            return;
        }

        for (PsiElement element : methodCallExpressions) {
            PsiFile methodContainingFile = null;
            if (isJavaFile) {
                PsiMethod psiMethod = null;
                if (element instanceof PsiMethodCallExpression) {
                    psiMethod = ((PsiMethodCallExpression) element).resolveMethod();
                } else {
                    psiMethod = ((PsiConstructorCall) element).resolveMethod();
                }
                if (psiMethod == null) {
                    continue;
                }

                methodContainingFile = psiMethod.getContainingFile();
            } else {
                if (element instanceof KtCallExpression) {
                    KtCallExpression ktCallExpression = (KtCallExpression) element;
                    KtExpression calleeExpression = ktCallExpression.getCalleeExpression();
                    if (calleeExpression == null) {
                        continue;
                    }
                    PsiReference[] ktCallExpressionReferences = calleeExpression.getReferences();
                    if (ktCallExpressionReferences.length > 0) {
                        PsiElement resolve = null;
                        for (PsiReference reference : ktCallExpressionReferences) {
                            resolve = reference.resolve();
                            if (resolve != null) {
                                methodContainingFile = resolve.getContainingFile();
                                break;
                            }
                        }
                    }
                } else if (element instanceof KtDotQualifiedExpression) {
                    KtDotQualifiedExpression dotQualifiedExpression = (KtDotQualifiedExpression) element;
                    PsiElement psiElement = dotQualifiedExpression.getLastChild();
                    PsiReference psiReferences[] = psiElement.getReferences();
                    if (psiReferences.length > 0) {
                        PsiElement resolve = null;
                        for (PsiReference reference : psiReferences) {
                            resolve = reference.resolve();
                            if (resolve != null && resolve instanceof PsiMethod) {
                                methodContainingFile = resolve.getContainingFile();
                                break;
                            }
                        }
                    }
                }

            }

            if (methodContainingFile != null && methodContainingFile != file) {
                //this is external method e.g, library
                final VirtualFile libraryFile = methodContainingFile.getVirtualFile();
                if (libraryFile != null &&
                        (fileIndex.isInContent(libraryFile) ||
                                fileIndex.isInLibraryClasses(libraryFile) ||
                                fileIndex.isInLibrarySource(libraryFile))) {
                    found.add(methodContainingFile);
                    LibraryOrderEntry libraryForFile = DependencyHelper.getLibraryForFile(fileIndex, libraryFile);
                    if (libraryForFile != null) {
                        LocalGradleCoordinate gradleCoordinate = extractLibraryDetails(libraryForFile.getLibraryName());
                        //TODO check if current lib is transitive or not
                        if (gradleCoordinate != null) {
                            String gradleLibName = gradleCoordinate.getName();
                            libVersions.put(gradleLibName, gradleCoordinate);
                            if (libDependencies.containsKey(gradleLibName)) {
                                libDependencies.get(gradleLibName).add(element);
                            } else {
                                ArrayList<PsiElement> temp = new ArrayList<>();
                                temp.add(element);
                                libDependencies.put(gradleLibName, temp);
                            }


                            if (usedLibFiles.containsKey(gradleLibName)) {
                                usedLibFiles.get(gradleLibName).add(methodContainingFile);
                            } else {
                                Set<PsiFile> tempSet = new HashSet<>();
                                tempSet.add(methodContainingFile);
                                usedLibFiles.put(gradleLibName, tempSet);
                            }
                        }
                    }
                }
            }
        }
    }

    private LocalGradleCoordinate extractLibraryDetails(String fullName) {
        LocalGradleCoordinate result = null;
        String GRADLE_PREFIX = "Gradle: ";
        try {
            if (fullName.startsWith(GRADLE_PREFIX)) {
                String[] nameParts = fullName.split(":");
                if(nameParts.length != 4){
                    return null;
                }
                String artifactId;
                String version;
                // first index is "Gradle"
                String groupId = nameParts[1].trim();
                artifactId = nameParts[2];
                char AT_JAR_SUFFIX = '@';
                int AT_INDEX = nameParts[3].indexOf(AT_JAR_SUFFIX);
                version = nameParts[3].substring(0, AT_INDEX);
                result = new LocalGradleCoordinate(groupId, artifactId, version);

            }
        }catch (Exception ex){
            
        }
        return result;
    }

    public void updateDependency(Project project, PsiFile file) {
        try {
            ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
            PsiManager psiManager = PsiManager.getInstance(project);
            //remove dependency of PsiElements that belong to the currently being changed PsiFile
            Set<String> tobeRemovedKeys = new HashSet<>();
            if (libDependencies == null) {
                findAllDependencies();
            }

            for (Map.Entry<String, ArrayList<PsiElement>> entry : libDependencies.entrySet()) {
                ArrayList<PsiElement> elements = entry.getValue();
                ArrayList<PsiElement> filteredElements = new ArrayList<>();
                for (PsiElement element : elements) {
                    if (!element.isValid())
                        continue;

                    PsiFile containingFile = element.getContainingFile();
                    if (containingFile != null && containingFile != file) {
                        // only add back the element which doesn't belong to the currently being changed file
                        filteredElements.add(element);
                    }
                }

                //update the ArrayList
                if (filteredElements.size() > 0) {
                    entry.setValue(filteredElements);
                } else {
                    tobeRemovedKeys.add(entry.getKey());
                }
            }

            libDependencies.keySet().removeAll(tobeRemovedKeys);

            //TODO remove used LibFile of PsiElements that belong to the currently being changed PsiFile

            //3. update the dependency of this file
            visit(file, fileIndex, psiManager);
        } catch (Exception ex) {
            
        }
    }


    public static String getMethodSignature(PsiElement psiElement) {
        String signature = null;
        if (psiElement.isValid()) {
            PsiFile containingFile = psiElement.getContainingFile();
            if (containingFile != null && containingFile.isValid()) {
                if (containingFile.getName().endsWith(".java")) {
                    signature = MethodHelper.getSignatureString(psiElement);
                } else if (containingFile.getName().endsWith(".kt")) {
                    if (psiElement instanceof KtCallExpression) {
                        KtCallExpression ktCallExpression = (KtCallExpression) psiElement;
                        signature = MethodHelper.getSignatureString(ktCallExpression);
                    } else if (psiElement instanceof KtDotQualifiedExpression) {
                        KtDotQualifiedExpression ktDotQualifiedExpression = (KtDotQualifiedExpression) psiElement;
                        signature = MethodHelper.getSignatureString(ktDotQualifiedExpression);
                    }
                }
            }
        }
        return signature;
    }

    private void visit(final PsiFile file, final ProjectFileIndex fileIndex, final PsiManager psiManager) {
        try {
            final FileViewProvider viewProvider = file.getViewProvider();
            if (viewProvider.getBaseLanguage() != file.getLanguage()) return;

            if (getScopeOfInterest() != null && !getScopeOfInterest().contains(file)) return;

            ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            final VirtualFile virtualFile = file.getVirtualFile();
            if (GeneratedSourcesFilter.isGeneratedSourceByAnyFilter(virtualFile, mproject)) {
                return;
            }

            if (!FileHelper.isKotlinOrJavaFile(file)) {
                return;
            }
            if (indicator != null) {
                if (indicator.isCanceled()) {
                    throw new ProcessCanceledException();
                }
                indicator.setText(AnalysisScopeBundle.message("package.dependencies.progress.text"));

                indicator.setText2(getRelativeToProjectPath(virtualFile));

                if (myTotalFileCount > 0) {
                    indicator.setFraction(((double) ++myFileCount) / myTotalFileCount);
                }
            }

            final boolean isInLibrary = fileIndex.isInLibrarySource(virtualFile) || fileIndex.isInLibraryClasses(virtualFile);
            if (isInLibrary) {
                return;
            }
            final Set<PsiFile> collectedDeps = new HashSet<>();
            final HashSet<PsiFile> processed = new HashSet<>();
            collectedDeps.add(file);
            do {
                for (PsiFile psiFile : new HashSet<>(collectedDeps)) {
                    final VirtualFile vFile = psiFile.getVirtualFile();
                    if (vFile != null) {

                        if (indicator != null) {
                            indicator.setText2(getRelativeToProjectPath(vFile));
                        }

                        if (fileIndex.isInLibraryClasses(vFile) || fileIndex.isInLibrarySource(vFile)) {
                            processed.add(psiFile);
                        }
                    }
                    final Set<PsiFile> found = new HashSet<>();
                    if (!processed.contains(psiFile)) {
                        processed.add(psiFile);
//                        analyzeFileDependencies(psiFile, fileIndex, found);
                        analyzeFileDependencies(psiFile, fileIndex, found);
                        Set<PsiFile> deps = getDependencies().get(file);
                        if (deps == null) {
                            deps = new HashSet<>();

                        }
                        deps.addAll(found);

                        getDirectDependencies().put(psiFile, new HashSet<>(found));

                        dependencies.put(file, deps);

                        collectedDeps.addAll(found);

                        psiManager.dropResolveCaches();
                        InjectedLanguageManager.getInstance(file.getProject()).dropFileCaches(file);
                    }
                }
                collectedDeps.removeAll(processed);
            }
            while (isTransitive() && !collectedDeps.isEmpty());
        } catch (Exception ex) {
            
            reset();
        }
    }

    private void reset() {
        libDependencies = null;
        libVersions = null;
        usedLibFiles = null;
    }

    public void findAllDependencies() {
        try {
            libDependencies = new HashMap<>();
            libVersions = new HashMap();
            usedLibFiles = new HashMap<>();
            final PsiManager psiManager = PsiManager.getInstance(mproject);
            psiManager.startBatchFilesProcessingMode();
            final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(mproject).getFileIndex();
            AnalysisScope analysisScope = new AnalysisScope(GlobalSearchScopesCore.projectProductionScope(mproject), mproject);
            try {
                analysisScope.accept(new PsiRecursiveElementVisitor() {
                    @Override
                    public void visitFile(final PsiFile file) {
                        visit(file, fileIndex, psiManager);
                    }
                });
            } finally {
                psiManager.finishBatchFilesProcessingMode();
            }
        } catch (Exception ex) {
            
            reset();
        }
    }

    private UsedLibInfo getUsedAPI(GradleCoordinate libDetail) {
        UsedLibInfo usedLibInfo = null;
        HashMap<String, CryptoIssue> cryptoIssues = sqliteHelper.getIssues(libDetail);
        try {
            if (shouldFindDependencies()) {
                findAllDependencies();
            }

            if (libDependencies == null) {
                return null;
            }

            HashMap<PsiFile, HashSet<String>> apis = new HashMap<>();
//            if (!libDependencies.containsKey(libDetail.getName())) {
//                return null;
//            }
            ArrayList<PsiElement> psiElements = libDependencies.get(libDetail.getName());
            if (psiElements != null) {
                HashSet<String> usedAPIs = new HashSet<>();
                for (PsiElement psiElement : psiElements) {
                    String methodSignature = getMethodSignature(psiElement);
                    if (methodSignature != null) {
                        PsiFile containingFile = psiElement.getContainingFile();
                        if (apis.containsKey(containingFile)) {
                            apis.get(containingFile).add(methodSignature);
                        } else {
                            HashSet<String> firstSignature = new HashSet<>();
                            firstSignature.add(methodSignature);
                            apis.put(containingFile, firstSignature);
                        }
                        usedAPIs.add(methodSignature);
                    }

                }

                usedLibInfo = new UsedLibInfo(libDetail, apis, usedLibFiles.get(libDetail.getName()), usedAPIs, cryptoIssues);

            }
        } catch (Exception ex) {
            
        }

        return usedLibInfo;
    }

    private HashMap<String, UsedLibInfo> getUsedAPI(HashMap<String, ?> dependingLibs) {
        HashMap<String, UsedLibInfo> usedLibInfos = new HashMap<>();
        for (Map.Entry dependingLib : dependingLibs.entrySet()) {
            GradleCoordinate gradleCoordinate = (GradleCoordinate) dependingLib.getValue();
            UsedLibInfo usedLibInfo = getUsedAPI(gradleCoordinate);
            if (usedLibInfo != null) {
                usedLibInfos.put(gradleCoordinate.getName(), usedLibInfo);
            }

        }
        return usedLibInfos;
    }


    public GeneralOnlineLibInfo getLibUsage(LocalGradleCoordinate libDetail) {
        GeneralOnlineLibInfo generalOnlineLibInfo = new GeneralOnlineLibInfo();
        OnlineLibInfo onlineLibInfo = null;
        if (shouldFindDependencies()) {
            findAllDependencies();
        }
        if (libDependencies == null) {
            return null;
        }
        HashMap<String, Integer> mainLibVersionOrders = sqliteHelper.getVersionOrders(libDetail.getName());
        if (mainLibVersionOrders.size() == 0) {
            return null;
        }
        //update libDetails mapping
        HashMap<String, Set<TransitiveLocalGradleCoordinate>> dependingLibs = sqliteHelper.getDependingLibs(libDetail);


        WarningType warningType = WarningType.OUTDATED;
        String vulnerabilityUrl = null;
        boolean hasTransitiveDep = dependingLibs.size() > 0;
        boolean isTransitiveUsed = false;
        boolean isUsedAtAll = false;
        sqliteHelper.isVulnerableVersion(libDetail);
        vulnerabilityUrl = sqliteHelper.isVulnerableVersion(libDetail);
        if(vulnerabilityUrl != null) {
            warningType = WarningType.VULNERABLE_OUTDATED;
        }
        HashMap<String, Set<TransitiveLocalGradleCoordinate>> usedTransDeps = new HashMap<>();
        for (Map.Entry<String,Set<TransitiveLocalGradleCoordinate>> entry: dependingLibs.entrySet()) {
            for(TransitiveLocalGradleCoordinate dependingLibDetail: entry.getValue()) {
                if (dependingLibDetail.getParentLib().getRevision().equals(libDetail.getRevision()) && libDependencies.containsKey(dependingLibDetail.getName())) {
                    dependingLibDetail.setIsUsed(true);
                    usedTransDeps.put(dependingLibDetail.getName(), entry.getValue());
                    isTransitiveUsed = true;
                    break;

                }


                if (vulnerabilityUrl == null) {
                    vulnerabilityUrl = sqliteHelper.isVulnerableVersion(dependingLibDetail);
                    if(vulnerabilityUrl != null) {
                        warningType = WarningType.VULNERABLE_OUTDATED;
                    }
                }
            }

        }


        //get used API (app used) of the current dependency
        if(libDependencies.containsKey(libDetail.getName())){
            isUsedAtAll = true;
            libDetail.setIsUsed(true);
        }


        //case 1: if the current lib is not used and (it doesn't have any dependencies or its dependencies are also not used)
        if (!isUsedAtAll && (!hasTransitiveDep || !isTransitiveUsed)) {
            LocalGradleCoordinate latestVersion = sqliteHelper.getLatestVersion(libDetail.getName(), libDetail.getRevision());

            if (mainLibVersionOrders.get(libDetail.getRevision()) < mainLibVersionOrders.get(latestVersion.getRevision())) {
                generalOnlineLibInfo.setOnlineLibInfo(new OnlineLibInfo(libDetail.getName(), libDetail, true, latestVersion, null, null, warningType, vulnerabilityUrl));
            }

            return generalOnlineLibInfo;
        }
        //case 2: if the current lib is not used but at least one of its trans dependencies is used by app
        if (!isUsedAtAll && isTransitiveUsed) {
            HashMap<String, UsedLibInfo> dependingLibsUsedInfo = getUsedAPI(usedTransDeps);
            generalOnlineLibInfo = DependencyHelper.getOnlineLibInfoOnlyTransDep(libDetail, mainLibVersionOrders, dependingLibs, dependingLibsUsedInfo, warningType);
            return generalOnlineLibInfo;
        }

        //case 3: if the current lib is used but non of its transitive libs is used
        if (isUsedAtAll && (!hasTransitiveDep || !isTransitiveUsed)) {
            UsedLibInfo currentUsedLibInfo = getUsedAPI(libDetail);
            generalOnlineLibInfo.setOnlineLibInfo(DependencyHelper.getOnlineLibInfo(libDetail,  mainLibVersionOrders, dependingLibs, currentUsedLibInfo, warningType));
            return generalOnlineLibInfo;
        }

        // case 4: if the current lib is used, and also at least one of its transitive libs is used by app
        if (isUsedAtAll && isTransitiveUsed) {
            UsedLibInfo currentUsedLibInfo = getUsedAPI(libDetail);
            HashMap<String, UsedLibInfo> dependingLibsUsedAPI = getUsedAPI(usedTransDeps);
            generalOnlineLibInfo = DependencyHelper.getOnlineLibInfoWithTransDep(libDetail, mainLibVersionOrders, currentUsedLibInfo, usedTransDeps, dependingLibsUsedAPI, warningType);

        }

//        //TODO also consider current lib is used, and its dependency is used by other libs declared in the gradle files.
//        if (hasTransitiveDep) {
//            HashMap<String, UsedLibInfo> transitiveUsedLibInfos = getUsedAPI(dependingLibs);
//            if (transitiveUsedLibInfos != null && transitiveUsedLibInfos.size() > 0) {
//                    /*
//                        app does use API of transitive dependencies of the current dependency
//                        therefore, when suggesting newer version of the current lib, Up2Dep needs to consider the version
//                        that not only provides API that the app is using, but also has the transitive dependencies that
//                        provide API that the app is using.
//                     */
//
//                HashMap<String, HashMap<String, HashSet<String>>> transitiveLibAvailableAPIs = sqliteHelper.getLibAvailableAPIs(dependingLibs);
//                /**
//                 *TODO now we have info for the current lib and also infos of its transitive dependencies,
//                 * need to find if there is any conflict with the project's dependency
//                 */
//
//            } else {
//                //app doesn't use any transitive dependencies of the current dependency
//                onlineLibInfo = DependencyHelper.getOnlineLibInfo(libDetail, currentUsedLibInfo, warningType);
//                generalOnlineLibInfo.setOnlineLibInfo(onlineLibInfo);
//            }
//
//        } else {
//            generalOnlineLibInfo.setOnlineLibInfo(DependencyHelper.getOnlineLibInfo(libDetail, currentUsedLibInfo, warningType));
//
//        }

//        } else {
//            //TODO check transitive dependency too
////            onlineLibInfos = new HashMap<>();
//            HashMap<String, LocalGradleCoordinate> latestVersions = sqliteHelper.getLatestVersion(libDetail, dependingLibs);
//
//            for (Map.Entry<String, LocalGradleCoordinate> entry : latestVersions.entrySet()) {
//                LocalGradleCoordinate latestVersion = entry.getValue();
//                if (latestVersion != null) {
//                    String lName = entry.getKey();
//                    int greater = sqliteHelper.compareVersions(lName, latestVersion.getRevision(), libDetail.getRevision());
//                    if (greater > 0) {
//                        OnlineLibInfo latestVersionPossible = new OnlineLibInfo(lName, libDetail, true, latestVersion, null, null, warningType, vulnerabilityUrl);
//                        onlineLibInfo = latestVersionPossible;
//                    }
//                }
//            }
//        }
        return generalOnlineLibInfo;
    }

    public OnlineLibInfo getLatestPossibleVersion(GradleCoordinate libDetail, Set<GradleCoordinate> dependingLibs, WarningType warningType, String vulnerabilityUrl) {
        HashMap<String, Integer> versionOrders = sqliteHelper.getVersionOrders(libDetail.getName());
        HashMap<String, LocalGradleCoordinate> latestVersions = sqliteHelper.getLatestVersion(libDetail, dependingLibs);
        OnlineLibInfo onlineLibInfo = null;
        for (Map.Entry<String, LocalGradleCoordinate> entry : latestVersions.entrySet()) {
            LocalGradleCoordinate latestVersion = entry.getValue();
            if (latestVersion != null) {
                String lName = entry.getKey();
                if (versionOrders.get(latestVersion.getRevision()) > versionOrders.get(libDetail.getRevision())) {
                    onlineLibInfo = new OnlineLibInfo(lName, libDetail, true, latestVersion, null, null, warningType, vulnerabilityUrl);
//                    onlineLibInfo = latestVersionPossible;
                }
            }
        }

        return onlineLibInfo;
    }


    public LocalGradleCoordinate getLibDetail(String libName) {
        LocalGradleCoordinate detail = null;
        if (libVersions != null && libVersions.containsKey(libName)) {
            detail = libVersions.get(libName);
        }

        return detail;
    }

    public boolean isLibIncluded(String libName) {
        boolean isIncluded = false;
        if (libVersions != null && libVersions.containsKey(libName)) {
            isIncluded = true;
        }
        return isIncluded;
    }


    private boolean shouldFindDependencies() {
        boolean shouldRerun = false;
        if (libDependencies == null) {
            shouldRerun = true;
        } else {
            for (Map.Entry<String, Set<PsiFile>> usedLibFile : usedLibFiles.entrySet()) {
                if (!usedLibFile.getValue().iterator().next().isValid()) {
                    shouldRerun = true;
                    break;
                }
            }
        }

        return shouldRerun;
    }

    @Override
    public Map<PsiFile, Set<PsiFile>> getDirectDependencies() {
        return myDirectDependencies;
    }
}
