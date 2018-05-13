package de.veihelmann.closureplugin;

import com.intellij.lang.javascript.psi.JSStatement;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import de.veihelmann.closureplugin.dependency_recognizers.*;
import de.veihelmann.closureplugin.utils.LanguageUtils;
import de.veihelmann.closureplugin.utils.ListMap;
import org.jetbrains.debugger.PsiVisitors.FilteringPsiRecursiveElementWalkingVisitor;

import java.util.*;

import static java.util.Arrays.asList;

/**
 * Collects (Google Closure-) dependencies of a single file to other namespaces. Found goog.requires, goog.provides and
 * dependencies in the rest of the code will be collected in the corresponding fields, e.g. {@link #googRequires}.
 */
public class ClosureDependenciesExtractor {

    /**
     * Namespaces provided (via goog.provide('x.y.Z') ) in the current file, with their corresponding PSI element.
     */
    public final SortedMap<String, PsiElement> googRequires = new TreeMap<>(Comparator.naturalOrder());

    /**
     * Namespaces required (via goog.require('x.y.Z') ) by the current file, with their corresponding PSI element.
     */
    public final SortedMap<String, JSStatement> googProvides = new TreeMap<>(Comparator.naturalOrder());

    /**
     * Actual dependencies to other namespaces in the current file (e.g. new x.y.Z(); ) ), with their corresponding PSI element.
     */
    public final ListMap<String, PsiElement> dependencies = new ListMap<>();

    /**
     * The raw types from Closure type comments, e..g 'Array<Object<ts.my.Namespace>>' or 'string'. We store them to
     * identify requires that are optional, meaning that  the Closure compiler does not require them, but it will also
     * not report them as 'obsolete'.
     */
    public final Set<String> rawTypesInComments = new HashSet<>();

    private final List<DependencyRecognizerBase> dependencyRecognizers = asList( //
            new GoogRequireOrProvideRecognizer(googRequires, googProvides), //
            new GoogInheritsDependencyRecognizer(dependencies), //
            new ConstructorDependencyRecognizer(dependencies), //
            new ES6BaseClassDependencyRecognizer(dependencies), //
            new StaticMethodOrConstantDependencyRecognizer(dependencies), //
            new MemberDependencyRecognizer(dependencies));

    private final CommentDependencyCollector commentDependencyRecognizer = new CommentDependencyCollector(rawTypesInComments);


    /**
     * Extracts dependencies of the given file by filling the respective fields of this class (e.g. {@link #dependencies}.
     */
    public void extractDependencies(PsiFile file) {

        if (!file.getFileType().getDefaultExtension().equals("js")) {
            return;
        }

        if (LanguageUtils.JAVASCRIPT == null) {
            throw new UnsupportedOperationException("JavaScript is not available as language in your IntelliJ distribution.");
        }

        new RecursiveElementVisitor().visitElement(file);
    }

    class RecursiveElementVisitor extends FilteringPsiRecursiveElementWalkingVisitor {

        @Override
        public void visitElement(PsiElement element) {
            super.visitElement(element);

            for (DependencyRecognizerBase consumer : dependencyRecognizers) {
                if (consumer.consumeElement(element)) {
                    return;
                }
            }
        }

        @Override
        public void visitComment(PsiComment psiComment) {
            super.visitComment(psiComment);
            commentDependencyRecognizer.collectTypeDependenciesFromComment(psiComment);
        }
    }
}
