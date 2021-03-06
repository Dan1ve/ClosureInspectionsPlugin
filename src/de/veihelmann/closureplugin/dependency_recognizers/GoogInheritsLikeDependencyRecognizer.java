package de.veihelmann.closureplugin.dependency_recognizers;

import com.intellij.lang.javascript.psi.JSArgumentList;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.psi.PsiElement;
import de.veihelmann.closureplugin.utils.ListMap;

import java.util.Map;

public class GoogInheritsLikeDependencyRecognizer extends StaticMethodOrConstantDependencyRecognizer {

    public GoogInheritsLikeDependencyRecognizer(ListMap<String, PsiElement> dependencyMap, Map<String, String> fullNamespacesToImports) {
        super(dependencyMap, fullNamespacesToImports);
    }


    @Override
    protected boolean doConsumeElement(JSCallExpression callElement) {

        // goog.inherits(ts.test.SimpleFile, ts.test.BaseClass)
        // JSCallExpression
        // [0] JSReferenceExpression  text: goog.inherits
        // [1] JSArgumentsList
        // [1] JSReferenceExpression : nth-of-type(2) : text

        if (!isStaticMethodCall(callElement)) {
            return false;
        }

        String fullMethod = resolveAndNormalizeNamespace(callElement.getFirstChild().getText());

        String alternativeInheritsMethod = "ts.fixInheritanceForEs6Class";
        if (!(fullMethod.equals("goog.inherits") || fullMethod.equals(alternativeInheritsMethod))) {
            return false;
        }

        if (fullMethod.equals(alternativeInheritsMethod)) {
            dependencies.put(alternativeInheritsMethod, callElement.getFirstChild());
        }

        JSArgumentList argumentElement = (JSArgumentList) callElement.getChildren()[1];

        // We need the second JSReference inside the goog.inherits call, which determines the base class.
        boolean alreadySawOneReference = false;
        for (PsiElement argument : argumentElement.getArguments()) {
            if (!(argument instanceof JSReferenceExpression)) {
                continue;
            }

            if (alreadySawOneReference) {
                dependencies.put(resolveAndNormalizeNamespace(argument.getText()), argument);
                return true;
            }
            alreadySawOneReference = true;
        }

        return false;
    }
}