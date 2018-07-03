package de.veihelmann.closureplugin.dependency_recognizers;

import com.intellij.lang.javascript.psi.jsdoc.JSDocComment;
import com.intellij.lang.javascript.psi.jsdoc.JSDocTag;
import com.intellij.lang.javascript.psi.jsdoc.JSDocTagValue;
import com.intellij.psi.PsiComment;

import java.util.*;

import static de.veihelmann.closureplugin.dependency_recognizers.DependencyRecognizerBase.normalizeNamespace;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

/**
 * Collects raw types from JSDOc comments in a file. A 'raw type' string can contain additional symbols, like '!' or
 * 'Array<>'. This should work fine for our purposes and avoids having to of build a fully-fledged type parser for
 * Closure comments :)
 */
public class CommentDependencyCollector {

    private final Set<String> rawTypesInComments;

    private static final Set<String> RELEVANT_TAG_NAMES = new HashSet<>(asList("param", "return", "type", "typedef", "implements"));

    private static final String CLOSURE_TYPE_ANNOTATION_START = "{";

    private static final String CLOSURE_TYPE_ANNOTATION_END = "}";

    /**
     * The passed set will be filled in-place (meaning its contents can change)
     */
    public CommentDependencyCollector(Set<String> rawTypesInComments) {
        this.rawTypesInComments = rawTypesInComments;
    }

    /**
     * Collects the type strings in the given comment and stores them in {@link #rawTypesInComments}
     */
    public void collectTypeDependenciesFromComment(PsiComment psiComment) {
        if (!(psiComment instanceof JSDocComment)) {
            // All relevant comments in Closure are JSDoc comments
            return;
        }

        JSDocComment comment = (JSDocComment) psiComment;
        List<JSDocTag> relevantTags = collectTagsFromComment(comment, RELEVANT_TAG_NAMES);
        for (JSDocTag tag : relevantTags) {

            Optional<String> typeReferences = extractClosureTypeReference(tag.getValue());
            typeReferences.ifPresent(reference -> {
                reference = normalizeNamespace(reference);
                rawTypesInComments.add(reference);
            });
        }
    }

    public static List<JSDocTag> collectTagsFromComment(JSDocComment comment, Set<String> relevantTags) {
        return Arrays.stream(comment.getTags()).filter(tag -> relevantTags.contains(tag.getName())).collect(toList());
    }


    private Optional<String> extractClosureTypeReference(JSDocTagValue tagValue) {
        if (tagValue == null || tagValue.getChildren().length == 0 || tagValue.getFirstChild().getText() == null) {
            return Optional.empty();
        }
        String referenceWithBrackets = tagValue.getText();

        // Closure type parameters are always embraced in { }, e.g. @type{string}
        if (!referenceWithBrackets.startsWith(CLOSURE_TYPE_ANNOTATION_START) || !referenceWithBrackets.endsWith(CLOSURE_TYPE_ANNOTATION_END)) {
            // Malformated type reference
            return Optional.empty();
        }

        return Optional.of(referenceWithBrackets.substring(1, referenceWithBrackets.length() - 1));
    }
}
