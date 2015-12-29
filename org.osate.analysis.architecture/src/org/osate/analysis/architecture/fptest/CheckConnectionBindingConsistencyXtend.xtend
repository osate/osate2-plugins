package org.osate.analysis.architecture.fptest

import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.OperationCanceledException
import org.osate.aadl2.Element
import org.osate.aadl2.NamedElement
import org.osate.aadl2.instance.ComponentInstance
import org.osate.aadl2.instance.ConnectionInstance
import org.osate.aadl2.instance.ConnectionKind
import org.osate.aadl2.instance.SystemInstance
import org.osate.analysis.architecture.ArchitecturePlugin
import org.osate.ui.actions.AaxlReadOnlyActionAsJob
import org.osate.ui.dialogs.Dialog
import org.osate.xtext.aadl2.properties.util.InstanceModelUtil

final class CheckConnectionBindingConsistencyXtend extends AaxlReadOnlyActionAsJob {
	override protected getBundle() {
		ArchitecturePlugin.^default.bundle
	}
	
	override protected getMarkerType() {
		"org.osate.analysis.architecture.ConnectionBindingConsistencyObjectMarker"
	}
	
	override protected initializeAction(NamedElement object) {
		setCSVLog("ConnectionBindingConsistency", object)
		true
	}
	
	override protected doAaxlAction(IProgressMonitor monitor, Element root) {
		if (root instanceof ComponentInstance) {
			monitor.beginTask(actionName, IProgressMonitor.UNKNOWN)
			
			//Adapted from ConnectionBindingConsistency.initSwitches()
			root.systemInstance?.eAllContents?.takeWhile[!monitor.canceled]?.filter(ConnectionInstance)?.filter[kind == ConnectionKind.PORT_CONNECTION]?.forEach[
				val srcHW = InstanceModelUtil.getHardwareComponent(source)
				val dstHW = InstanceModelUtil.getHardwareComponent(destination)
				if (srcHW == null || dstHW == null) {
					warning('''Connection «componentInstancePath» source or destination is not bound to hardware''')
				}
				val bindings = InstanceModelUtil.getPhysicalConnectionBinding(it)
				if (bindings.empty) {
					warning('''Connection «componentInstancePath» has no actual connection binding to hardware''')
					if (InstanceModelUtil.connectedByBus(srcHW, dstHW).empty) {
						error('''Hardware (processor or device) of connection «componentInstancePath» source and destination are not physically connected''')
					}
				} else {
					if (srcHW != null && !InstanceModelUtil.connectedToBus(srcHW, bindings.head)) {
						warning('''Connection «componentInstancePath» source bound hardware is not connected to the first bus in the actual binding''')
					}
					if (dstHW != null && !InstanceModelUtil.connectedToBus(srcHW, bindings.last)) {
						warning('''Connection «componentInstancePath» destination bound hardware is not connected to the last bus in the actual binding''')
					}
				}
			]
			
			if (monitor.canceled) {
				throw new OperationCanceledException
			} else {
				monitor.done
			}
		} else {
			Dialog.showWarning(actionName, "Please invoke command on an instance model")
		}
	}
	
	override protected getActionName() {
		"Check connection binding consistency"
	}
	
	def void invoke(IProgressMonitor monitor, SystemInstance root) {
		actionBody(monitor, root)
	}
}