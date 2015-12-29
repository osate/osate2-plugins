package org.osate.analysis.architecture.tests;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.analysis.architecture.fptest.CheckConnectionBindingConsistencyScala;

//For some reason, the Xtend compiler doesn't like calling Scala code, so it is called here from Java.
class XtendScalaBridge {
	static void invokeScala(SystemInstance si) {
		new CheckConnectionBindingConsistencyScala().invoke(new NullProgressMonitor(), si);
	}
}