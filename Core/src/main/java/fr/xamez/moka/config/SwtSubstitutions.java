package fr.xamez.moka.config;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.eclipse.swt.internal.Library")
final class SwtSubstitutions {
    @Substitute
    static boolean isLoadable() {
        return true;
    }
}