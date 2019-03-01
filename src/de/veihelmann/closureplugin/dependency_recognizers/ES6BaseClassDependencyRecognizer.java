package de.veihelmann.closureplugin.dependency_recognizers;

import com.intellij.lang.ecmascript6.psi.impl.ES6ClassImpl;
import com.intellij.psi.PsiElement;
import de.veihelmann.closureplugin.utils.ListMap;

import java.util.Map;

public class ES6BaseClassDependencyRecognizer extends DependencyRecognizerBase<ES6ClassImpl> {

    private final ListMap<String, PsiElement> dependencyMap;

    public ES6BaseClassDependencyRecognizer(ListMap<String, PsiElement> dependencyMap, Map<String, String> fullNamespacesToImports) {
        super(fullNamespacesToImports);
        this.dependencyMap = dependencyMap;
    }

    @Override
    protected boolean doConsumeElement(ES6ClassImpl es6Class) {

        if (es6Class.getExtendsList() == null || es6Class.getExtendsList().getMembers().length == 0) {
            return false;
        }

        String dependency = es6Class.getExtendsList().getMembers()[0].getText();
        dependency = resolveAndNormalizeNamespace(dependency);
        if (!isInvalidDependency(dependency)) {
            dependencyMap.put(dependency, es6Class.getExtendsList().getMembers()[0]);
            return true;
        }

        return false;
    }
}