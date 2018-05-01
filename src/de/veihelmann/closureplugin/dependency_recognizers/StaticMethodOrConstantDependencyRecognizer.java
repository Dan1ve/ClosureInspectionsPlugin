package de.veihelmann.closureplugin.dependency_recognizers;

import com.intellij.lang.javascript.psi.JSArgumentList;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSNewExpression;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.psi.PsiElement;
import de.veihelmann.closureplugin.utils.ListMap;

public class StaticMethodOrConstantDependencyRecognizer extends DependencyRecognizerBase<JSCallExpression> {

    private static final String NAMESPACE_WITH_CONSTANT_PATTERN = ".+\\.[A-Z_]+$";

    protected final ListMap<String, PsiElement> dependencies;

    public StaticMethodOrConstantDependencyRecognizer(ListMap<String, PsiElement> constructors) {
        this.dependencies = constructors;
    }


    @Override
    protected boolean doConsumeElement(JSCallExpression callElement) {

        // goog.array.contains()
        // JSCallExpression
        // [0] JSReference  -> text: "goog.array.contains"
        // [1] JSArgumentList

        if (!isStaticMethodCall(callElement)) {
            return false;
        }

        String namespaceWithMethod = callElement.getFirstChild().getText();
        if (namespaceWithMethod.contains("(")) {

            // Special case: 'Double' static method call, e.g. my.namespace.getCurrent().get, so we need to extract the actual namespace before 'getCurrent()'
            namespaceWithMethod = namespaceWithMethod.substring(0, namespaceWithMethod.indexOf("("));
        }

        if (!namespaceWithMethod.contains(".")) {
            // e.g. 'setTimeout()', so no Closure dependency
            return false;
        }

        String namespace = namespaceWithMethod.substring(0, namespaceWithMethod.lastIndexOf("."));


        if (namespace.matches(NAMESPACE_WITH_CONSTANT_PATTERN)) {
            namespace = namespace.substring(0, namespace.lastIndexOf("."));
        }


        if (isInvalidDependency(namespace)) {
            return false;
        }

        PsiElement markerElement = callElement.getFirstChild();
        while (markerElement.getChildren().length > 0 && markerElement.getFirstChild() instanceof JSReferenceExpression && markerElement.getText().matches("\\w+\\.\\w+\\.\\w+")) {
            // Use deepest reference of first children, which only marks the actual namespace and no (potential) additional method calls
            markerElement = markerElement.getFirstChild();
        }

        dependencies.put(namespace, markerElement);
        return true;
    }

    protected boolean isStaticMethodCall(JSCallExpression callElement) {
        return childCount(callElement) > 1 && childType(callElement, 0, JSReferenceExpression.class) && childType(callElement, 1, JSArgumentList.class) && !callElement.getText().startsWith("this.") && !(callElement.getChildren()[0].getChildren()[0] instanceof JSNewExpression);
    }
}