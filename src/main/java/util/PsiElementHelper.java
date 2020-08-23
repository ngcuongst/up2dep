package util;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

public class PsiElementHelper {
    
    @Nullable
    public static TextRange getRange(PsiElement startElement, PsiElement endElement) {
        TextRange textRange = null;
        try {
            if (!startElement.isValid() || !endElement.isValid()) {
                return textRange;
            }
            int start = startElement.getTextOffset();
            int end = endElement.getTextOffset() + endElement.getTextLength();
            textRange =  new TextRange(start, end);
        }catch (Exception ex){
            
        }

        return textRange;
    }

    public static String getClosureName(GrClosableBlock closure) {
        String result = null;
        try {
            if (closure.getParent() instanceof GrMethodCall) {
                GrMethodCall parent = (GrMethodCall) closure.getParent();
                if (parent.getInvokedExpression() instanceof GrReferenceExpression) {
                    GrReferenceExpression invokedExpression = (GrReferenceExpression) (parent.getInvokedExpression());
                    if (invokedExpression.getDotToken() == null) {
                        result = invokedExpression.getReferenceName();
                    }
                }
            }
        } catch (Exception ex) {
            
        }
        return result;
    }
}
