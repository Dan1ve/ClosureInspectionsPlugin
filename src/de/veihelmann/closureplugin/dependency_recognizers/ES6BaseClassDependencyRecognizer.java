package de.veihelmann.closureplugin.dependency_recognizers;

import com.intellij.lang.ecmascript6.psi.ES6ClassExpression;
import com.intellij.lang.javascript.psi.ecma6.ES6ReferenceList;
import com.intellij.psi.PsiElement;
import de.veihelmann.closureplugin.utils.ListMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ES6BaseClassDependencyRecognizer extends DependencyRecognizerBase<ES6ClassExpression> {

    private final ListMap<String, PsiElement> dependencyMap;

    private final static Pattern EXTENDS_PATTERN = Pattern.compile("extends\\s+((\\w+\\.)+\\w+)");

    public ES6BaseClassDependencyRecognizer(ListMap<String, PsiElement> dependencyMap) {
        this.dependencyMap = dependencyMap;
    }


    @Override
    protected boolean doConsumeElement(ES6ClassExpression classExpression) {

        for (PsiElement child : classExpression.getChildren()) {
            if (!(child instanceof ES6ReferenceList)) {
                continue;
            }
            Matcher matcher = EXTENDS_PATTERN.matcher(child.getText());
            if (!matcher.find()) {
                continue;
            }
            String dependency = matcher.group(1);
            if (!isInvalidDependency(dependency)) {
                dependencyMap.put(dependency, child);
                return true;
            }
        }

        return false;
    }
}