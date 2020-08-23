package quickfix;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import model.libInfo.OnlineLibInfo;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import util.PsiElementHelper;

public class LatestVersionQuickFix extends GeneralQuickFix {
    private final String NAME = "Update to %s";
    private final OnlineLibInfo libInfo;


    public LatestVersionQuickFix(OnlineLibInfo libInfo) {
        this.libInfo = libInfo;
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return "Update to the latest version";
    }

    @Nls
    @NotNull
    @Override
    public String getName() {
        String name = String.format(NAME, libInfo.getLatestVersion());
        if (!libInfo.isLatest()) {
            name += " (code changes needed)";
        }

        return name;
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
        super.applyFix(project, problemDescriptor);
        PsiElement startElement = problemDescriptor.getStartElement();
        PsiElement endElement = problemDescriptor.getEndElement();

        String newerversion = String.format("'%s'", libInfo.getLatestVersion().toString());
        Document document = FileDocumentManager.getInstance().getDocument(startElement.getContainingFile().getVirtualFile());

        TextRange range = PsiElementHelper.getRange(startElement, endElement);
        if (range != null) {
            ApplicationManager.getApplication().runWriteAction(() -> {
                final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
                documentManager.doPostponedOperationsAndUnblockDocument(document);
                document.replaceString(range.getStartOffset(), range.getEndOffset(), newerversion);
                documentManager.commitDocument(document);
            });
        }

//        DependencyHelper.checkForSecondDep(libInfo, problemDescriptor.getPsiElement(), libDetails, extraProperties);
    }

}
