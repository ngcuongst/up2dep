package util;

import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName;
import org.jetbrains.kotlin.psi.*;

public class MethodHelper {
    public static String getSignatureString(PsiElement method) {
        String signature = null;
        PsiMethod psiMethod = null;
        if(method instanceof PsiMethodCallExpression) {
            psiMethod = ((PsiMethodCallExpression)method).resolveMethod();
        }else{
            psiMethod = ((PsiConstructorCall)method).resolveMethod();
        }

        if (psiMethod != null) {
            signature = getSignatureString(psiMethod);
        }
        return signature;
    }

    public static String getSignatureString(KtCallExpression ktCallExpression) {
        KtExpression calleeExpression = ktCallExpression.getCalleeExpression();
        if (calleeExpression == null) {
            return null;
        }
        String signature = null;

        PsiReference[] references = calleeExpression.getReferences();

        for (PsiReference reference : references) {
            PsiElement resolve = reference.resolve();
            if (resolve != null & resolve instanceof PsiMethod) {
                PsiMethod method = (PsiMethod) resolve;
                signature = getSignatureString(method);
                break;
            }
        }
        return signature;
    }

    /**
     * Get signature for the Kotlin functions apart from the constructors
     *
     * @param dotQualifiedExpression
     * @return function signature
     */
    public static String getSignatureString(KtDotQualifiedExpression dotQualifiedExpression) {
        PsiElement psiElement = dotQualifiedExpression.getLastChild();
        PsiReference psiReferences[] = psiElement.getReferences();
        PsiMethod method = null;
        for (int i = 0; i < psiReferences.length; i++) {
            PsiElement element = psiReferences[i].resolve();
            if (element != null && element instanceof PsiMethod) {
                method = (PsiMethod) element;
                break;
            }
        }
        if (method != null) {
            return getSignatureString(method);
        }
        return null;
    }

    private static String getTypeString(PsiType type) {
        String typeStr = type.getCanonicalText();
        if (typeStr.equals("T")) {
            PsiType[] superTypes = type.getSuperTypes();
            if (superTypes.length > 0) {
                typeStr = superTypes[0].getCanonicalText();
            }
        }

        return typeStr;
    }

    private static String getSignatureString(PsiMethod method) {
        if (method == null) {
            return null;
        }
        StringBuilder methodSignature = new StringBuilder();
        String returnType = "";
        PsiType methodReturnType = method.getReturnType();
        if (methodReturnType != null) {
            if(methodReturnType instanceof PsiClassReferenceType){
                PsiClass resolvedReturnType = ((PsiClassReferenceType) methodReturnType).resolve();
                if(resolvedReturnType != null) {
                    returnType = getTypeString(resolvedReturnType);
                }
            }else {
                returnType = getTypeString(methodReturnType);
            }
        }
        PsiClass containingClass = method.getContainingClass();

        String className = getTypeString(containingClass);

        methodSignature.append(className);
        methodSignature.append(".");
        if (method.isConstructor()) {
            methodSignature.append("<init>");
        } else {
            methodSignature.append(method.getName());
        }
        methodSignature.append("(");

        PsiParameterList parameterList = method.getParameterList();
        PsiParameter[] parameters = parameterList.getParameters();

        if (parameters.length > 0) {
            for (PsiParameter parameter : parameters) {
                String parameterTypeString;
                PsiType parameterType = parameter.getType();
                if (parameterType instanceof PsiClassType) {
                    PsiClass parameterClassType = ((PsiClassType) parameterType).resolve();
                    parameterTypeString = getTypeString(parameterClassType);
                } else {
                    parameterTypeString = getTypeString(parameterType);
                }

                methodSignature.append(CodeUtil.convertToDexBytecodeNotation(parameterTypeString));
            }
        }
        methodSignature.append(")");
        if (method.isConstructor()) {
            methodSignature.append("V");
        } else {
            methodSignature.append(CodeUtil.convertToDexBytecodeNotation(returnType));
        }

        return methodSignature.toString();
    }

    private static String getTypeString(PsiClass psiClass) {
        PsiFile containingFile = psiClass.getContainingFile();
        if (!(containingFile instanceof PsiClassOwner)) {
            return null;
        }
        String className = psiClass.getName();
        if (className != null && className.equals("T")) {
            return "java/lang/Object";
        }
        String qualifiedName = psiClass.getQualifiedName();
        PsiClassOwner classOwner = (PsiClassOwner) containingFile;
        String packageName = classOwner.getPackageName();

        if (packageName.length() > 0) {
            className = packageName + ".";
            if (qualifiedName != null) {
                className += qualifiedName.substring(packageName.length() + 1).replace('.', '$');
            } else {
                className += psiClass.getName();
            }
        } else {
            if (qualifiedName != null) {
                className = qualifiedName.replace('.', '$');
            } else {
                className = psiClass.getName();
            }
        }

        return className;
    }
}
