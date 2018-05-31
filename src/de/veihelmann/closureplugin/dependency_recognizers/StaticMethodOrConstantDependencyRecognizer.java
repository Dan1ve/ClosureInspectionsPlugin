package de.veihelmann.closureplugin.dependency_recognizers;

import com.intellij.lang.javascript.psi.JSArgumentList;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSNewExpression;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.psi.PsiElement;
import de.veihelmann.closureplugin.utils.ListMap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class StaticMethodOrConstantDependencyRecognizer extends DependencyRecognizerBase<JSCallExpression> {

    private static final String NAMESPACE_WITH_CONSTANT_PATTERN = ".+\\.[A-Z_]+$";

    private final static Set<String> WHITELISTED_METHODS = new HashSet<>(Arrays.asList(
            "substring", "toString", "toLowerCase", "toUpperCase", "split", "slice", "splice", "toLocaleString",
            "push", "getBBox", "getBrowserEvent", "preventDefault", "concat", "localeCompare", "apply", "forEach", "map"
    ));

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

        int offsetBeforeMethodOrConstant = namespaceWithMethod.lastIndexOf(".");
        String namespace = namespaceWithMethod.substring(0, offsetBeforeMethodOrConstant);

        namespace = normalizeNamespace(namespace);

        if (namespace.matches(NAMESPACE_WITH_CONSTANT_PATTERN)) {
            namespace = namespace.substring(0, namespace.lastIndexOf("."));
        } else {
            String methodName = namespaceWithMethod.substring(offsetBeforeMethodOrConstant + 1);
            if (WHITELISTED_METHODS.contains(methodName)) {
                return false;
            }
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
        return childCount(callElement) > 1 && childType(callElement, 0, JSReferenceExpression.class) &&
                childType(callElement, 1, JSArgumentList.class) && !callElement.getText().startsWith("$") && !callElement.getText().startsWith("this.")
                && !(isConstructorCall(callElement));
    }

    /**
     * Checks the first childs (recursively) to determine if the statement starts with an constructor call ('new ...')
     */
    private boolean isConstructorCall(JSCallExpression callElement) {
        if (childCount(callElement) == 0) {
            return false;
        }
        PsiElement currentFirstChild = callElement.getFirstChild();
        while (currentFirstChild != null) {
            if (currentFirstChild instanceof JSNewExpression) {
                return true;
            }
            if (childCount(currentFirstChild) == 0) {
                return false;
            }
            currentFirstChild = currentFirstChild.getFirstChild();
        }
        return false;
    }
}