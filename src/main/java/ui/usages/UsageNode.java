package ui.usages;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageView;
import org.jetbrains.annotations.NotNull;
import util.UsageInfo2UsageAdapter;

import java.util.Arrays;

public class UsageNode extends Node implements Comparable<UsageNode>, Navigatable {
    private  PsiElement psiElement;

    public UsageNode(Node parent, @NotNull Usage usage) {
        setUserObject(usage);
        setParent(parent);
        if(usage instanceof UsageInfo2UsageAdapter){
            psiElement = ((UsageInfo2UsageAdapter)usage).getElement();
        }
    }

    public String toString() {
        return getUsage().toString();
    }

    @Override
    public String tree2string(int indent, String lineSeparator) {
        StringBuffer result = new StringBuffer();
        StringUtil.repeatSymbol(result, ' ', indent);
        result.append(getUsage());
        return result.toString();
    }

    @Override
    public int compareTo(@NotNull UsageNode usageNode) {
        return LibUsageView.USAGE_COMPARATOR.compare(getUsage(), usageNode.getUsage());
    }

    @NotNull
    public Usage getUsage() {
        return (Usage) getUserObject();
    }

    @Override
    public void navigate(boolean requestFocus) {
        getUsage().navigate(requestFocus);
    }

    @Override
    public boolean canNavigate() {
        return getUsage().isValid() && getUsage().canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        return getUsage().isValid() && getUsage().canNavigate();
    }

    @Override
    protected boolean isDataValid() {
        return getUsage().isValid();
    }

    @Override
    protected boolean isDataReadOnly() {
        return getUsage().isReadOnly();
    }

    @Override
    protected boolean isDataExcluded() {
        return isExcluded();
    }

    @NotNull
    @Override
    protected String getText(@NotNull final UsageView view) {
        try {
            return getUsage().getPresentation().getPlainText();
        } catch (AbstractMethodError e) {
            return Arrays.asList(getUsage().getPresentation().getText()).toString();
        }
    }

    public PsiElement getPsiElement() {
        return psiElement;
    }
}
