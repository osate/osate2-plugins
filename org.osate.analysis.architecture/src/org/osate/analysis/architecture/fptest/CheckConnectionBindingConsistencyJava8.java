package org.osate.analysis.architecture.fptest;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.osate.aadl2.Element;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.ConnectionInstance;
import org.osate.aadl2.instance.ConnectionKind;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.analysis.architecture.ArchitecturePlugin;
import org.osate.ui.actions.AaxlReadOnlyActionAsJob;
import org.osate.ui.dialogs.Dialog;
import org.osate.xtext.aadl2.properties.util.InstanceModelUtil;
import org.osgi.framework.Bundle;

public final class CheckConnectionBindingConsistencyJava8 extends AaxlReadOnlyActionAsJob {
	@Override
	protected Bundle getBundle() {
		return ArchitecturePlugin.getDefault().getBundle();
	}
	
	@Override
	protected String getMarkerType() {
		return "org.osate.analysis.architecture.ConnectionBindingConsistencyObjectMarker";
	}
	
	@Override
	protected boolean initializeAction(NamedElement object) {
		setCSVLog("ConnectionBindingConsistency", object);
		return true;
	}
	
	@Override
	protected void doAaxlAction(IProgressMonitor monitor, Element root) {
		if (root instanceof ComponentInstance) {
			monitor.beginTask(getActionName(), IProgressMonitor.UNKNOWN);
			
			//Adapted from ConnectionBindingConsistency.initSwitches()
			SystemInstance si = ((ComponentInstance)root).getSystemInstance();
			if (si != null) {
				Stream<EObject> stream = StreamSupport.stream(new Spliterators.AbstractSpliterator<EObject>(Long.MAX_VALUE, Spliterator.ORDERED) {
					private final Iterator<EObject> allContentsIter = si.eAllContents();
					
					@Override
					public boolean tryAdvance(Consumer<? super EObject> action) {
						if (!monitor.isCanceled() && allContentsIter.hasNext()) {
							action.accept(allContentsIter.next());
							return true;
						} else {
							return false;
						}
					}
				}, false);
				Stream<ConnectionInstance> connections = stream.filter(conni -> conni instanceof ConnectionInstance).map(conni -> (ConnectionInstance)conni);
				Stream<ConnectionInstance> portConnections = connections.filter(conni -> conni.getKind().equals(ConnectionKind.PORT_CONNECTION));
				portConnections.forEach(conni -> {
					ComponentInstance srcHW = InstanceModelUtil.getHardwareComponent(conni.getSource());
					ComponentInstance dstHW = InstanceModelUtil.getHardwareComponent(conni.getDestination());
					if (srcHW == null || dstHW == null) {
						warning(conni, "Connection " + conni.getComponentInstancePath() + " source or destination is not bound to hardware");
					}
					EList<ComponentInstance> bindings = InstanceModelUtil.getPhysicalConnectionBinding(conni);
					if (bindings.isEmpty()) {
						warning(conni, "Connection " + conni.getComponentInstancePath() + " has no actual connection binding to hardware");
						if (InstanceModelUtil.connectedByBus(srcHW, dstHW).isEmpty()) {
							error(conni, "Hardware (processor or device) of connection " + conni.getComponentInstancePath() + " source and destination are not physically connected");
						}
					} else {
						if (srcHW != null && !InstanceModelUtil.connectedToBus(srcHW, bindings.get(0))) {
							warning(conni, "Connection " + conni.getComponentInstancePath() + " source bound hardware is not connected to the first bus in the actual binding");
						}
						if (dstHW != null && !InstanceModelUtil.connectedToBus(srcHW, bindings.get(bindings.size() - 1))) {
							warning(conni, "Connection " + conni.getComponentInstancePath() + " destination bound hardware is not connected to the last bus in the actual binding");
						}
					}
				});
			}
			
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			} else {
				monitor.done();
			}
		} else {
			Dialog.showWarning(getActionName(), "Please invoke command on an instance model");
		}
	}
	
	@Override
	protected String getActionName() {
		return "Check connection binding consistency";
	}
	
	public void invoke(IProgressMonitor monitor, SystemInstance root) {
		actionBody(monitor, root);
	}
}