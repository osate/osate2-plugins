package org.osate.analysis.architecture.fptest

import java.util.Collections

import scala.annotation.tailrec
import scala.collection.JavaConverters

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

final class CheckConnectionBindingConsistencyScala extends AaxlReadOnlyActionAsJob {
	protected override def getBundle() = ArchitecturePlugin.getDefault.getBundle

	protected override def getMarkerType() = "org.osate.analysis.architecture.ConnectionBindingConsistencyObjectMarker"

	protected override def initializeAction(obj: NamedElement) = {
		setCSVLog("ConnectionBindingConsistency", obj)
		true
	}

	protected def doAaxlAction(monitor: IProgressMonitor, root: Element) = {
		root match {
			case comp: ComponentInstance => {
				monitor.beginTask(getActionName, IProgressMonitor.UNKNOWN)

				//Adapted from ConnectionBindingConsistency.initSwitches()
				val iter = Option(comp.getSystemInstance).map(_.eAllContents).getOrElse(Collections.emptyIterator)
				val stream = JavaConverters.asScalaIteratorConverter(iter).asScala.toStream
				val connections = stream.takeWhile(conni => !monitor.isCanceled).collect{case conni: ConnectionInstance => conni}
				connections.filter(_.getKind == ConnectionKind.PORT_CONNECTION).foreach{conni =>
					val srcHW = InstanceModelUtil.getHardwareComponent(conni.getSource)
					val dstHW = InstanceModelUtil.getHardwareComponent(conni.getDestination)
					if (srcHW == null || dstHW == null) {
						warning(conni, "Connection " + conni.getComponentInstancePath + " source or destination is not bound to hardware")
					}
					val bindings = InstanceModelUtil.getPhysicalConnectionBinding(conni)
					if (bindings.isEmpty) {
						warning(conni, "Connection " + conni.getComponentInstancePath + " has no actual connection binding to hardware")
						if (InstanceModelUtil.connectedByBus(srcHW, dstHW).isEmpty) {
							error(conni, "Hardware (processor or device) of connection " + conni.getComponentInstancePath + " source and destination are not physically connected")
						}
					} else {
						if (srcHW != null && !InstanceModelUtil.connectedToBus(srcHW, bindings.get(0))) {
							warning(conni, "Connection " + conni.getComponentInstancePath + " source bound hardware is not connected to the first bus in the actual binding")
						}
						if (dstHW != null && !InstanceModelUtil.connectedToBus(srcHW, bindings.get(bindings.size - 1))) {
							warning(conni, "Connection " + conni.getComponentInstancePath + " destination bound hardware is not connected to the last bus in the actual binding")
						}
					}
				}
				
				if (monitor.isCanceled) {
					throw new OperationCanceledException
				} else {
					monitor.done
				}
			}
			case _ => Dialog.showWarning(getActionName, "Please invoke command on an instance model")
		}
	}

	protected def getActionName() = "Check connection binding consistency"
	
	def invoke(monitor: IProgressMonitor, root: SystemInstance): Unit = actionBody(monitor, root)
}