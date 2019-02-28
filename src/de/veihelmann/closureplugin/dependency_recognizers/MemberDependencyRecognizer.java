package de.veihelmann.closureplugin.dependency_recognizers;

import com.intellij.lang.ecmascript6.psi.ES6ClassExpression;
import com.intellij.lang.javascript.psi.JSNewExpression;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.psi.PsiElement;
import de.veihelmann.closureplugin.utils.ListMap;

import java.util.Map;

public class MemberDependencyRecognizer extends DependencyRecognizerBase<JSReferenceExpression> {

    private final ListMap<String, PsiElement> dependencies;

    private static final String NAMESPACE_WITH_MEMBER_PATTERN = "(\\w+\\.)+[a-z][\\w_]*";

    private static final String NAMESPACE_WITH_CONSTANT_AND_OPTIONAL_SUFFIX = "(\\w+\\.)+[A-Z_]{2,}(\\..*)?";

    /**
     * The passed map will be filled in-place, meaning it changes.
     */
    public MemberDependencyRecognizer(ListMap<String, PsiElement> constructors, Map<String, String> fullNamespacesToImports) {
        super(fullNamespacesToImports);
        this.dependencies = constructors;
    }


    @Override
    protected boolean doConsumeElement(JSReferenceExpression memberCallElement) {

        // ts.commons.Constants.MY_CONSTANT
        // JSReferenceExprerssion
        // [0] JSReferenceExpression   text()
        // [1] PsiElement [DOT]
        // [2] PsiElement (IDENTIFIER)

        if (memberCallElement.getParent() instanceof JSNewExpression || memberCallElement.getParent() instanceof JSReferenceExpression
                || memberCallElement.getText().startsWith("this.") || childCount(memberCallElement) < 3
                || !childType(memberCallElement, 0, JSReferenceExpression.class)
                || !memberCallElement.getChildren()[1].getText().equals(".")) {
            return false;
        }

        if (isPartOfES6ClassDefinition(memberCallElement)) {
            return false;
        }

        String namespace = memberCallElement.getFirstChild().getText();
        if (memberCallElement.getText().matches(NAMESPACE_WITH_CONSTANT_AND_OPTIONAL_SUFFIX)) {
            namespace = memberCallElement.getText();
        }

        if (namespace.equals("goog.provide") || namespace.equals("goog.require")) {
            // Not handled in this class
            return false;
        }

        boolean removedConstant = false;
        while (namespace.matches(NAMESPACE_WITH_CONSTANT_AND_OPTIONAL_SUFFIX)) {
            // Cut of any potential constants appended to the namespace
            namespace = namespace.substring(0, namespace.lastIndexOf("."));
            removedConstant = true;
        }

        if (!removedConstant && namespace.matches(NAMESPACE_WITH_MEMBER_PATTERN)) {
            // e.g. myvar.length (no Type or constant in reference (if following default Closure naming conventions)
            return false;
        }

        namespace = resolveAndNormalizeNamespace(namespace);

        if (isInvalidDependency(namespace)) {
            return false;
        }
        dependencies.put(namespace, memberCallElement);
        return true;
    }

    private boolean isPartOfES6ClassDefinition(JSReferenceExpression memberCallElement) {
        PsiElement parent = memberCallElement.getParent();

        // Do not check full parent hierachy (can be long), but use a heuristic
        final int maxParentIterations = 3;
        for (int i = 0; i < maxParentIterations; i++) {
            if (parent instanceof ES6ClassExpression) {
                // Probably a 'extends <baseCLass>' statement, which is handled separately.
                return true;
            }
            parent = parent.getParent();
            if (parent == null) {
                return true;
            }
        }
        return false;
    }
}