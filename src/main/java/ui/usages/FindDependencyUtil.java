package ui.usages;

import com.intellij.analysis.AnalysisScopeBundle;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packageDependencies.DependenciesBuilder;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtCallExpression;
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import util.LibraryDependenciesBuilder;

import java.util.*;

public class FindDependencyUtil {
    private FindDependencyUtil() {
    }

    public static UsageInfo[] findDependencies(@Nullable final LibraryDependenciesBuilder builder, Set<PsiFile> searchIn, Set<PsiFile> searchFor) {
        final List<UsageInfo> usages = new ArrayList<>();
        ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
        int totalCount = searchIn.size();
        int count = 0;

        nextFile:
        for (final PsiFile psiFile : searchIn) {
            count = updateIndicator(indicator, totalCount, count, psiFile);

            if (!psiFile.isValid())
                continue;

            final Set<PsiFile> precomputedDeps;
            if (builder != null) {
                final Set<PsiFile> depsByFile = new HashSet<>();
//                for (DependenciesBuilder builder : builders) {
                final Set<PsiFile> deps = builder.getDependencies().get(psiFile);
                if (deps != null) {
                    depsByFile.addAll(deps);
                }
//                }
                precomputedDeps = new HashSet<>(depsByFile);
                precomputedDeps.retainAll(searchFor);
                if (precomputedDeps.isEmpty())
                    continue nextFile;
            } else {
                precomputedDeps = Collections.unmodifiableSet(searchFor);
            }

            analyzeFileDependencies(psiFile, precomputedDeps, usages);
        }

        return usages.toArray(UsageInfo.EMPTY_ARRAY);
    }

    public static UsageInfo[] findBackwardDependencies(final List<DependenciesBuilder> builders, final Set<PsiFile> searchIn, final Set<PsiFile> searchFor) {
        final List<UsageInfo> usages = new ArrayList<>();
        ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();


        final Set<PsiFile> deps = new HashSet<>();
        for (PsiFile psiFile : searchFor) {
            for (DependenciesBuilder builder : builders) {
                final Set<PsiFile> depsByBuilder = builder.getDependencies().get(psiFile);
                if (depsByBuilder != null) {
                    deps.addAll(depsByBuilder);
                }
            }
        }
        deps.retainAll(searchIn);
        if (deps.isEmpty()) return UsageInfo.EMPTY_ARRAY;

        int totalCount = deps.size();
        int count = 0;
        for (final PsiFile psiFile : deps) {
            count = updateIndicator(indicator, totalCount, count, psiFile);

            analyzeFileDependencies(psiFile, searchFor, usages);
        }

        return usages.toArray(UsageInfo.EMPTY_ARRAY);
    }

    //TODO consider to merge code with LibraryUsageHelper.java
    private static void analyzeFileDependencies(PsiFile psiFile, final Set<PsiFile> searchFor, final List<UsageInfo> result) {
        if (psiFile instanceof PsiJavaFile) {
            Collection<PsiElement> methodCallExpressions = PsiTreeUtil.findChildrenOfAnyType(psiFile, PsiMethodCallExpression.class, PsiConstructorCall.class);
            for (PsiElement element : methodCallExpressions) {
                PsiMethod psiMethod = null;

                if(element instanceof PsiMethodCallExpression) {
                    psiMethod = ((PsiMethodCallExpression)element).resolveMethod();
                } else if (element instanceof PsiConstructorCall){
                    psiMethod = ((PsiConstructorCall)element).resolveMethod();
                }
                if (psiMethod == null) {
                    continue;
                }
                PsiFile methodContainingFile = psiMethod.getContainingFile();
                if (searchFor.contains(methodContainingFile)) {
                    result.add(new UsageInfo(element));
                }
            }
        } else if (psiFile instanceof KtFile) {
            Collection<PsiElement> methodCallExpressions = PsiTreeUtil.findChildrenOfAnyType(psiFile, KtCallExpression.class, KtDotQualifiedExpression.class);
            for (PsiElement element : methodCallExpressions) {
                PsiReference[] references = null;
                if(element instanceof KtCallExpression){
                    KtExpression calleeExpression = ((KtCallExpression)element).getCalleeExpression();
                    if (calleeExpression == null) {
                        continue;
                    }
                    references = calleeExpression.getReferences();

                } else if(element instanceof KtDotQualifiedExpression){
                    PsiElement psiElement = element.getLastChild();
                    references = psiElement.getReferences();
                }

                for (PsiReference reference : references) {
                    PsiElement resolve = reference.resolve();
                    if (!(resolve instanceof PsiMethod)) {
                        continue;
                    }
                    PsiFile ktMethodContainingFile = resolve.getContainingFile();
                    if (searchFor.contains(ktMethodContainingFile)) {
                        result.add(new UsageInfo(element));
                    }
                }
            }
        }

    }
//        DependenciesBuilder.analyzeFileDependencies(psiFile, new DependenciesBuilder.DependencyProcessor() {
//            @Override
//            public void process(PsiElement place, PsiElement dependency) {
//                PsiFile dependencyFile = dependency.getContainingFile();
//                if (dependencyFile != null) {
//                    final PsiElement navigationElement = dependencyFile.getNavigationElement();
//                    if (navigationElement instanceof PsiFile) {
//                        dependencyFile = (PsiFile)navigationElement;
//                    }
//                }
//                if (searchFor.contains(dependencyFile)) {
//                    result.add(new UsageInfo(place));
//                }
//            }
//        });
//    }

    private static int updateIndicator(final ProgressIndicator indicator, final int totalCount, int count, final PsiFile psiFile) {
        if (indicator != null) {
            if (indicator.isCanceled()) throw new ProcessCanceledException();
            indicator.setFraction(((double) ++count) / totalCount);
            final VirtualFile virtualFile = psiFile.getVirtualFile();
            if (virtualFile != null) {
                indicator.setText(AnalysisScopeBundle.message("find.dependencies.progress.text", virtualFile.getPresentableUrl()));
            }
        }
        return count;
    }


    public static UsageInfo[] findBackwardDependencies(final DependenciesBuilder builder, final Set<PsiFile> searchIn, final Set<PsiFile> searchFor) {
        return findBackwardDependencies(Collections.singletonList(builder), searchIn, searchFor);
    }
}
