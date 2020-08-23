package listener;

import com.intellij.psi.PsiElement;

public interface IQuickFixListener {
    void applied(PsiElement psiElement, String name);
}
