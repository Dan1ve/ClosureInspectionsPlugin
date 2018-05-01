package de.veihelmann.closureplugin.dependency_recognizers;

import com.intellij.lang.javascript.psi.JSNewExpression;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.psi.PsiElement;
import de.veihelmann.closureplugin.utils.ListMap;

/**
 * Collects constructor dependencies, e.g.  'new x.y.MyNamespace()'.
 */
public class ConstructorDependencyRecognizer extends DependencyRecognizerBase<JSNewExpression> {

    private final ListMap<String, PsiElement> constructors;

    public ConstructorDependencyRecognizer(ListMap<String, PsiElement> constructors) {
        this.constructors = constructors;
    }


    @Override
    protected boolean doConsumeElement(JSNewExpression newCallElement) {

        // new ts.commons.Bla('test');
        // JSNewExpression
        // [~2] JSReferenceExpression  text: "ts.ccmmons.Something"

        for (PsiElement child : newCallElement.getChildren()) {
            if (!(child instanceof JSReferenceExpression)) {
                continue;
            }
            String namespace = child.getText();
            if (isInvalidDependency(namespace)) {
                return false;
            }
            constructors.put(namespace, child);
            return true;
        }

        return false;
    }
}