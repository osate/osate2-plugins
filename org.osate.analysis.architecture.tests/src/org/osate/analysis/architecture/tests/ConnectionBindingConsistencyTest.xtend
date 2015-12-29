package org.osate.analysis.architecture.tests

import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.core.runtime.Path
import org.eclipse.emf.common.util.URI
import org.eclipse.xtext.junit4.InjectWith
import org.eclipselabs.xtext.utils.unittesting.XtextRunner2
import org.junit.Test
import org.junit.runner.RunWith
import org.osate.aadl2.AadlPackage
import org.osate.aadl2.SystemImplementation
import org.osate.aadl2.instance.SystemInstance
import org.osate.analysis.architecture.actions.CheckConnectionBindingConsistency
import org.osate.analysis.architecture.fptest.CheckConnectionBindingConsistencyJava8
import org.osate.analysis.architecture.fptest.CheckConnectionBindingConsistencyXtend
import org.osate.core.test.Aadl2UiInjectorProvider
import org.osate.core.test.OsateTest

import static extension org.eclipse.xtext.util.Files.readStreamIntoString
import static extension org.junit.Assert.assertEquals
import static extension org.osate.aadl2.instantiation.InstantiateModel.buildInstanceModelFile

@RunWith(XtextRunner2)
@InjectWith(Aadl2UiInjectorProvider)
class ConnectionBindingConsistencyTest extends OsateTest {
	override getProjectName() {
		"Connection_Binding_Consistency_Test"
	}
	
	@Test
	def void testOriginal() {
		testAnalysis[new CheckConnectionBindingConsistency().invoke(new NullProgressMonitor, it)]
	}
	
	@Test
	def void testXtend() {
		testAnalysis[new CheckConnectionBindingConsistencyXtend().invoke(new NullProgressMonitor, it)]
	}
	
	@Test
	def void testJava8() {
		testAnalysis[new CheckConnectionBindingConsistencyJava8().invoke(new NullProgressMonitor, it)]
	}
	
	@Test
	def void testScala() {
		testAnalysis[XtendScalaBridge.invokeScala(it)]
	}
	
	def void testAnalysis((SystemInstance)=>void analysisInvoker) {
		val connectionBindingFileName = "Connection_Binding.aadl"
		createFiles(connectionBindingFileName -> '''
			package Connection_Binding
			public
				thread t1
					features
						op: out data port;
				end t1;
				
				thread t2
					features
						ip: in data port;
				end t2;
				
				process ps1
				end ps1;
				
				process implementation ps1.impl
					subcomponents
						t1: thread t1;
						t2: thread t2;
					connections
						conn1: port t1.op -> t2.ip;
				end ps1.impl;
				
				processor proc1
					features
						ba: requires bus access bus1;
				end proc1;
				
				bus bus1
				end bus1;
				
				system s1
				end s1;
				
				system implementation s1.impl
					subcomponents
						ps1: process ps1.impl;
						ps2: process ps1.impl;
						ps3: process ps1.impl;
						proc1: processor proc1;
						proc2: processor proc1;
						bus1: bus bus1;
						bus2: bus bus1;
					connections
						proc1_ba: bus access proc1.ba -> bus1;
						proc2_ba: bus access proc2.ba -> bus1;
					properties
						Actual_Processor_Binding => (reference (proc1)) applies to ps1.t1, ps2.t1;
						Actual_Processor_Binding => (reference (proc2)) applies to ps1.t2, ps2.t2;
						Actual_Connection_Binding => (reference (bus1)) applies to ps1.conn1;
						Actual_Connection_Binding => (reference (bus2)) applies to ps2.conn1;
				end s1.impl;
			end Connection_Binding;
		''')
		suppressSerialization
		testFile(connectionBindingFileName).resource.contents.head as AadlPackage => [
			"Connection_Binding".assertEquals(name)
			publicSection.ownedClassifiers.get(7) as SystemImplementation => [
				"s1.impl".assertEquals(name)
				buildInstanceModelFile => [
					"s1_impl_Instance".assertEquals(name)
					analysisInvoker.apply(it)
					val uri = URI.createURI(resourceRoot + "/instances/reports/ConnectionBindingConsistency/Connection_Binding_s1_impl_Instance__ConnectionBindingConsistency.csv")
					val actual = workspaceRoot.getFile(new Path(uri.toPlatformString(true))).contents.readStreamIntoString.trim
					'''
						Check connection binding consistency Report
						
						Warning! t1.op -> t2.ip: Connection ps2.t1.op -> t2.ip source bound hardware is not connected to the first bus in the actual binding
						Warning! t1.op -> t2.ip: Connection ps2.t1.op -> t2.ip destination bound hardware is not connected to the last bus in the actual binding
						Warning! t1.op -> t2.ip: Connection ps3.t1.op -> t2.ip source or destination is not bound to hardware
						Warning! t1.op -> t2.ip: Connection ps3.t1.op -> t2.ip has no actual connection binding to hardware
						ERROR:  t1.op -> t2.ip: Hardware (processor or device) of connection ps3.t1.op -> t2.ip source and destination are not physically connected
					'''.toString.trim.assertEquals(actual)
				]
			]
		]
	}
}