package de.veihelmann.closureplugin;

import com.intellij.codeInspection.InspectionToolProvider;
import org.jetbrains.annotations.NotNull;


/**
 * Provides the inspections offered by this plugin.
 */
public class ClosureInspectionsProvider implements InspectionToolProvider {
    @NotNull
    public Class[] getInspectionClasses() {
        return new Class[]{MissingOrObsoleteGoogRequiresInspection.class,

                // TODO: Disabled (for now):
                // ConvertToES6ClassInspection.class
        };
    }
}
