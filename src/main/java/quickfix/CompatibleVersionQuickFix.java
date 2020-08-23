package quickfix;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import model.gradle.LocalGradleCoordinate;
import model.libInfo.OnlineLibInfo;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import util.PsiElementHelper;

public class CompatibleVersionQuickFix extends GeneralQuickFix {
    private OnlineLibInfo onlineLibInfo;
//    private final HashMap<String, LocalGradleCoordinate> libDetails;
//    private final HashMap<String, String> extraProperties;
    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return "Update to the compatible version";
    }

    public CompatibleVersionQuickFix(OnlineLibInfo onlineLibInfo) {
        this.onlineLibInfo = onlineLibInfo;
//        this.libDetails = libDetails;
//        this.extraProperties = extraProperties;
    }

    @Nls
    @NotNull
    @Override
    public String getName() {
        return String.format("Change to %s (latest compatible version)", onlineLibInfo.getCompatibleVersion());
    }


    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
        super.applyFix(project, problemDescriptor);
        PsiElement startElement = problemDescriptor.getStartElement();
        PsiElement endElement = problemDescriptor.getEndElement();
        LocalGradleCoordinate newerVersion = onlineLibInfo.getCompatibleVersion();
        String newerversion = String.format("'%s'", newerVersion.toString());
        Document document = FileDocumentManager.getInstance().getDocument(startElement.getContainingFile().getVirtualFile());


        TextRange range = PsiElementHelper.getRange(startElement, endElement);
        if (range != null) {
            final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
            documentManager.doPostponedOperationsAndUnblockDocument(document);
            document.replaceString(range.getStartOffset(), range.getEndOffset(), newerversion);
            documentManager.commitDocument(document);
        }

//        DependencyHelper.checkForSecondDep(onlineLibInfo, problemDescriptor.getPsiElement(), libDetails, extraProperties);
    }


}
