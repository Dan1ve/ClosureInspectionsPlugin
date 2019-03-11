package de.veihelmann.closureplugin.dependency_recognizers;

import com.intellij.lang.javascript.psi.JSArgumentList;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.lang.javascript.psi.JSStatement;
import com.intellij.lang.javascript.psi.impl.JSVariableImpl;
import com.intellij.psi.PsiElement;
import de.veihelmann.closureplugin.utils.ListMap;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class GoogRequireOrProvideRecognizer extends DependencyRecognizerBase<JSCallExpression> {

    private final Map<String, PsiElement> googRequires;

    /**
     * In case of s.th. like
     * <code>const foo = goog.require('x.y')</code>,
     * contains a mapping from 'x.y' -> 'foo', so the import 'shortcuts'.
     */
    public final ListMap<String, PsiElement> duplicateGoogRequires = new ListMap<>();

    private final Map<String, JSStatement> googProvides;

    private final Map<String, JSStatement> googModules;

    public final ListMap<String, PsiElement> duplicateGoogProvides = new ListMap<>();

    public GoogRequireOrProvideRecognizer(Map<String, PsiElement> googRequires, Map<String, JSStatement> googProvides, Map<String, JSStatement> googModules, Map<String, String> fullNamespacesToImports) {
        super(fullNamespacesToImports);
        this.googRequires = googRequires;
        this.googProvides = googProvides;
        this.googModules = googModules;
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

        Map<String, String> fullNamespacesToImportedOne = new HashMap<>();
        JSArgumentList argumentList = (JSArgumentList) callElement.getChildren()[1];
        calledMethod = resolveAndNormalizeNamespace(calledMethod);

        if (argumentList.getArguments().length != 1 || !(calledMethod.equals("goog.require") || calledMethod.equals("goog.provide") || calledMethod.equals("goog.module"))) {
            return false;
        }

        String targetNamespace = argumentList.getArguments()[0].getText().replaceAll("[\"']", "");

        PsiElement parent = targetMethod.getParent();
        Pattern namespaceShortcutAssignmentPattern = Pattern.compile("(\\w+)\\s*=\\s*\\w+");
        if (parent != null && parent.getParent() != null && namespaceShortcutAssignmentPattern.matcher(parent.getParent().getText()).find()) {
            if (parent.getParent() instanceof JSVariableImpl) {
                String namespaceShortcut = ((JSVariableImpl) parent.getParent()).getName();
                if (namespaceShortcut != null) {
                    fullNamespacesToImportedOne.put(targetNamespace, namespaceShortcut);
                }
            }
        }

        collectGoogRequireOrProvide(callElement, calledMethod, targetNamespace, fullNamespacesToImportedOne);
        return true;

    }

    private void collectGoogRequireOrProvide(JSCallExpression callElement, String calledMethod, String targetNamespace, Map<String, String> fullNamespacesToImportedOne) {
        switch (calledMethod) {
            case "goog.require":
                if (googRequires.containsKey(targetNamespace)) {
                    duplicateGoogRequires.put(targetNamespace, getParentStatement(callElement));
                } else {
                    googRequires.put(targetNamespace, getParentStatement(callElement));
                    if (fullNamespacesToImportedOne.containsKey(targetNamespace)) {
                        registerImportShortName(targetNamespace, fullNamespacesToImportedOne.get(targetNamespace));
                    }
                }
                break;
            case "goog.provide":
                if (googProvides.containsKey(targetNamespace)) {
                    duplicateGoogProvides.put(targetNamespace, getParentStatement(callElement));
                } else {
                    googProvides.put(targetNamespace, getParentStatement(callElement));
                }
                break;
            case "goog.module":
                googModules.put(targetNamespace, getParentStatement(callElement));
                break;
            default:
                throw new UnsupportedOperationException("Unexpected method at this point: " + calledMethod);
        }
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