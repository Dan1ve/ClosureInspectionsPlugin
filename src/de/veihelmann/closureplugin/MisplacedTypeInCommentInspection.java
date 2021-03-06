package de.veihelmann.closureplugin;

import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.javascript.psi.jsdoc.JSDocComment;
import com.intellij.lang.javascript.psi.jsdoc.JSDocTag;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiWhiteSpace;
import de.veihelmann.closureplugin.dependency_recognizers.CommentDependencyCollector;
import de.veihelmann.closureplugin.fixes.SwappedTypeAndParameterInCommentFix;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * The inspection for missing or superfluous goog.require statements in a file.
 */
public class MisplacedTypeInCommentInspection extends LocalInspectionTool {


    @NotNull
    public String getDisplayName() {
        return "Misplaced types in comments";
    }

    @NotNull
    public String getGroupDisplayName() {
        return GroupNames.COMPILER_ISSUES;
    }

    @NotNull
    public String getShortName() {
        return "MisplacedTypeInCommentInspection";
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitComment(PsiComment comment) {
                super.visitComment(comment);

                if (!(comment instanceof JSDocComment)) {
                    return;
                }
                List<JSDocTag> paramTags = CommentDependencyCollector.collectTagsFromComment((JSDocComment) comment, new HashSet<>(Arrays.asList("param")));
                paramTags.forEach(tag -> {
                    if (tag.getChildren().length <= 1) {
                        return;
                    }
                    PsiElement afterParamElement = tag.getChildren()[1];
                    if (afterParamElement instanceof PsiWhiteSpace) {
                        if (tag.getChildren().length > 2) {
                            afterParamElement = tag.getChildren()[2];
                        } else {
                            return;
                        }

                    }
                    if (afterParamElement.getText().startsWith("{")) {
                        // Type annotation present
                        return;
                    }
                    if (SwappedTypeAndParameterInCommentFix.canBeQuickFixed(tag)) {
                        holder.registerProblem(tag, "Type and parameter name are in wrong order", new SwappedTypeAndParameterInCommentFix(tag));
                    } else {
                        holder.registerProblem(tag, "Missing type after @param");
                    }
                });

            }
        };

    }

    public boolean isEnabledByDefault() {
        return true;
    }
}