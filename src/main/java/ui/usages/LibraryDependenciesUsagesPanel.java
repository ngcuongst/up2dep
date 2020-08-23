package ui.usages;


import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.packageDependencies.ui.PanelProgressIndicator;
import com.intellij.psi.PsiConstructorCall;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.usageView.UsageInfo;
import model.libInfo.OnlineLibInfo;
import org.jetbrains.kotlin.psi.KtCallExpression;
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression;
import util.LibraryDependenciesBuilder;
import util.MethodHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class LibraryDependenciesUsagesPanel extends UsagesPanel {
    private final LibraryDependenciesBuilder builder;

    public LibraryDependenciesUsagesPanel(Project project, final LibraryDependenciesBuilder builder) {
        super(project);
        this.builder = builder;
        setToInitialPosition();
    }

    @Override
    public String getInitialPositionText() {
        return builder.getInitialUsagesPosition();
    }


    @Override
    public String getCodeUsagesString() {
        return builder.getRootNodeNameInUsageView();
    }

    public void findUsages(final Set<PsiFile> searchIn, OnlineLibInfo onlineLibInfo) {
        cancelCurrentFindRequest();

        myAlarm.cancelAllRequests();
        myAlarm.addRequest(() -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final ProgressIndicator progress = new PanelProgressIndicator(component -> setToComponent(component));
            myCurrentProgress = progress;
            ProgressManager.getInstance().runProcess(() -> {
                ApplicationManager.getApplication().runReadAction(() -> {
                    ArrayList<UsageInfo> usages = new ArrayList<>();
                    Set<PsiFile> elementsToSearch = null;
                    UsageInfo[] temUsages;
                    try {
                        if (builder.isBackward()) {
                            elementsToSearch = searchIn;
                            temUsages = FindDependencyUtil.findBackwardDependencies(builder, onlineLibInfo.getUsedLibFiles(), searchIn);
                        } else {
                            elementsToSearch = onlineLibInfo.getUsedLibFiles();
                            temUsages = FindDependencyUtil.findDependencies(builder, searchIn, elementsToSearch);
                        }

                        for (UsageInfo uinfo : temUsages) {
                            PsiElement element = uinfo.getElement();
                            String methodSignature = null;
                            if (element instanceof PsiMethodCallExpression || element instanceof PsiConstructorCall) {
                                methodSignature = MethodHelper.getSignatureString(element);
                            } else if (element instanceof KtCallExpression) {
                                methodSignature = MethodHelper.getSignatureString((KtCallExpression) element);
                            } else if (element instanceof KtDotQualifiedExpression) {
                                methodSignature = MethodHelper.getSignatureString((KtDotQualifiedExpression) element);
                            }

                            if (methodSignature == null)
                                continue;
                            if(onlineLibInfo.getAlternatives() != null && onlineLibInfo.getAlternatives().size() > 0){
                                if(!onlineLibInfo.getAlternatives().containsKey(methodSignature)){
                                    continue;
                                }
                            }

                            usages.add(uinfo);

                        }

                        assert !new HashSet<>(elementsToSearch).contains(null);
                    } catch (Exception e) {
                        LOG.error(e);
                    }

                    if (!progress.isCanceled()) {
                        final UsageInfo[] finalUsages = usages.toArray(new UsageInfo[usages.size()]);
                        final PsiElement[] _elementsToSearch =
                                elementsToSearch != null ? PsiUtilCore.toPsiElementArray(elementsToSearch) : PsiElement.EMPTY_ARRAY;
                        ApplicationManager.getApplication().invokeLater(() -> showUsages(_elementsToSearch, finalUsages), ModalityState.stateForComponent(
                                this));
                    }
                });
                myCurrentProgress = null;
            }, progress);
        }), 300);
    }

}