package de.veihelmann.closureplugin.dependency_recognizers;

import com.intellij.lang.javascript.psi.JSArgumentList;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.psi.PsiElement;
import de.veihelmann.closureplugin.utils.ListMap;

public class GoogInheritsDependencyRecognizer extends StaticMethodOrConstantDependencyRecognizer {

    public GoogInheritsDependencyRecognizer(ListMap<String, PsiElement> dependencyMap) {
        super(dependencyMap);
    }


    @Override
    protected boolean doConsumeElement(JSCallExpression callElement) {

        // goog.inherits(ts.test.SimpleFile, ts.test.BaseClass)
        // JSCallExpression
        // [0] JSReferenceExpression  text: goog.inherits
        // [1] JSArgumentsList
        // [1] JSReferenceExpression : nth-of-type(2) : text

        if (!isStaticMethodCall(callElement) || !callElement.getFirstChild().getText().equals("goog.inherits")) {
            return false;
        }

        JSArgumentList argumentElement = (JSArgumentList) callElement.getChildren()[1];

        // We need the second JSReference inside the goog.inherits call, which determines the base class.
        boolean alreadySawOneReference = false;
        for (PsiElement argument : argumentElement.getArguments()) {
            if (!(argument instanceof JSReferenceExpression)) {
                continue;
            }

            if (alreadySawOneReference) {
                dependencies.put(argument.getText(), argument);
                return true;
            }
            alreadySawOneReference = true;
        }

        return false;
    }
}