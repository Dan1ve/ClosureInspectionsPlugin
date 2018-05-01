package de.veihelmann.closureplugin;

import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.javascript.psi.JSDefinitionExpression;
import com.intellij.lang.javascript.refactoring.convertToClass.JSConvertToClassProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.debugger.PsiVisitors;

public class ConvertToES6ClassInspection extends LocalInspectionTool {

    @NotNull
    public String getDisplayName() {
        return "Check if a class can be converted into an  ES6 class";
    }

    @NotNull
    public String getGroupDisplayName() {
        return GroupNames.CLASS_LAYOUT_GROUP_NAME;
    }

    @NotNull
    public String getShortName() {
        return "ES6Conversion";
    }


    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new ValidateRequirementsPsiRecursiveElementVisitor(holder);
    }

    public boolean isEnabledByDefault() {
        return true;
    }

    private class ValidateRequirementsPsiRecursiveElementVisitor extends PsiElementVisitor {

        private final ProblemsHolder problemsHolder;

        ValidateRequirementsPsiRecursiveElementVisitor(ProblemsHolder holder) {
            this.problemsHolder = holder;
        }

        @Override
        public void visitFile(PsiFile file) {
            super.visitFile(file);

            // new MyVisitor(problemsHolder).visitFile(file);

        }

    }

    class MyVisitor extends PsiVisitors.FilteringPsiRecursiveElementWalkingVisitor {

        private ProblemsHolder problemsHolder;

        public MyVisitor(ProblemsHolder problemsHolder) {
            this.problemsHolder = problemsHolder;
        }

        @Override
        public void visitElement(PsiElement psiElement) {
            super.visitElement(psiElement);


            if (psiElement instanceof JSDefinitionExpression) {
                problemsHolder.registerProblem(psiElement, "Convert to ES6 class", new LocalQuickFix() {
                    @Nls
                    @NotNull
                    @Override
                    public String getFamilyName() {
                        return "ES6";
                    }

                    @Override
                    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {


                        final PsiDocumentManager psiManager = PsiDocumentManager.getInstance(project);
                        Document document = psiManager.getDocument(problemDescriptor.getPsiElement().getContainingFile());

                        psiElement.toString();

                        String text = document.getText();
                        text = text.replaceAll("(^|\\s)ts\\.test\\.MyFile\\.", "MyFile.");
                        text = text.replaceAll("(^|\\s)ts\\.test\\.MyFile\\s", "MyFile ");
                        document.setText(text);

                        if (false) {
                            new JSConvertToClassProcessor(project, EmptyRunnable.getInstance(), null).generateTheWholeTree();
                        }
                    }
                });
            }
        }
    }
}
