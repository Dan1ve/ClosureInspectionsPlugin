package de.veihelmann.closureplugin.dependency_recognizers;

import com.intellij.lang.javascript.psi.JSArgumentList;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.lang.javascript.psi.JSStatement;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class GoogRequireOrProvideRecognizer extends DependencyRecognizerBase<JSCallExpression> {

    private final Map<String, PsiElement> googRequires;

    private final Map<String, JSStatement> googProvides;

    public GoogRequireOrProvideRecognizer(Map<String, PsiElement> googRequires, Map<String, JSStatement> googProvides) {
        this.googRequires = googRequires;
        this.googProvides = googProvides;
    }


    @Override
    protected boolean doConsumeElement(JSCallExpression callElement) {

        // Goog.require / goog.provides:
        // JSCall Expression
        // [0] JSReferenceExpression -> "goog.provide"
        // [1] JS Arcgumentlist
        // [1] [2] JSLiteralExpression -> stringValue  "ts.my.namespace"


        if (!(childCount(callElement) > 1 && childType(callElement, 0, JSReferenceExpression.class) && childType(callElement, 1, JSArgumentList.class))) {
            return false;
        }

        JSReferenceExpression targetMethod = (JSReferenceExpression) callElement.getFirstChild();
        String calledMethod = targetMethod.getText();

        JSArgumentList argumentList = (JSArgumentList) callElement.getChildren()[1];
        calledMethod = normalizeNamespace(calledMethod);
        boolean isGoogRequire = calledMethod.equals("goog.require");
        if (argumentList.getArguments().length != 1 || !(isGoogRequire || calledMethod.equals("goog.provide"))) {
            return false;
        }

        String targetNamespace = argumentList.getArguments()[0].getText().replaceAll("[\"']", "");
        if (isGoogRequire) {
            googRequires.put(targetNamespace, getParentStatement(callElement));
        } else {
            // We checked for goog.provide above, so we are safe in the 'else' clause here.
            googProvides.put(targetNamespace, getParentStatement(callElement));
        }
        return true;

    }

    private @NotNull
    JSStatement getParentStatement(JSCallExpression callElement) {
        PsiElement currentParent = callElement.getParent();
        while (!(currentParent instanceof JSStatement)) {
            currentParent = currentParent.getParent();
            if (currentParent == null) {
                throw new UnsupportedOperationException("No parent JS statement found for sub-element " + callElement);
            }
        }
        return (JSStatement) currentParent;
    }
}