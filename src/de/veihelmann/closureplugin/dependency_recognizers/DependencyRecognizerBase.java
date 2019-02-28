package de.veihelmann.closureplugin.dependency_recognizers;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * Base class for dependency recognizers. A PSI element can be 'consumed' if a (Closure-) dependency could be
 * extracted.
 */
public abstract class DependencyRecognizerBase<T extends PsiElement> {

    private final Map<String, String> fullNamespacesToShortReferences;

    protected DependencyRecognizerBase(Map<String, String> fullNamespacesToShortReferences) {
        this.fullNamespacesToShortReferences = fullNamespacesToShortReferences;
    }

    protected void registerImportShortName(String fullNamespace, String shortReference) {
        this.fullNamespacesToShortReferences.put(fullNamespace, shortReference);
    }

    /**
     * @return <code>true</code> if the element was consumed, which means a dependency could be extracted,
     * <code>false</code> otherwise.
     */
    public final boolean consumeElement(PsiElement element) {
        try {
            if (element == null) {
                return false;
            }
            //noinspection unchecked
            return doConsumeElement((T) element);
        } catch (ClassCastException e) {
            // Expected if the target type does not match, so error handling not needed.
            return false;
        } catch (Exception e) {
            Logger.getInstance(this.getClass()).error("Error traversing " +
                    element.getContainingFile().getName() + " at element " + element.getText() + ": " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * @return true if a dependent namespace could be extracted, false otherwise.
     */
    protected abstract boolean doConsumeElement(T element);

    protected int childCount(PsiElement element) {
        return element.getChildren().length;
    }

    protected boolean childType(PsiElement element, int childIndex, Class<?> targetClass) {
        PsiElement[] children = element.getChildren();
        if (children.length <= childIndex) {
            return false;
        }
        return targetClass.isInstance(children[childIndex]);
    }

    /**
     * @return whether the given namespace is a valid dependent namespace (in Google Closure), meaning it e.g.
     * contains at least one dot (.).
     */
    protected boolean isInvalidDependency(String namespace) {
        return !namespace.contains(".") || namespace.startsWith("location.") || namespace.startsWith("$") || namespace.startsWith("document.") || containsAny(namespace, "(", "[", ".prototype.")
                || namespace.endsWith(".prototype") || namespace.equals("goog.module");
    }

    protected String resolveAndNormalizeNamespace(String namespace) {
        Optional<String> resolvedFullNamespace = fullNamespacesToShortReferences.keySet().stream().filter(
                key -> fullNamespacesToShortReferences.get(key).equals(namespace)).findFirst();
        if (resolvedFullNamespace.isPresent()) {
            return normalizeNamespace(resolvedFullNamespace.get());
        }
        return normalizeNamespace(namespace);
    }

    /* package */
    static String normalizeNamespace(String namespace) {
        return namespace.replaceAll("[\n\\s]", "");
    }

    private static boolean containsAny(String input, String... terms) {
        return Arrays.stream(terms).anyMatch(input::contains);
    }
}