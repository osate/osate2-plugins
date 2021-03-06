/*
 * Copyright 2013 Carnegie Mellon University

 * Any opinions, findings and conclusions or recommendations expressed in this 
 * Material are those of the author(s) and do not necessarily reflect the 
 * views of the United States Department of Defense. 

 * NO WARRANTY. THIS CARNEGIE MELLON UNIVERSITY AND SOFTWARE ENGINEERING 
 * INSTITUTE MATERIAL IS FURNISHED ON AN �AS-IS� BASIS. CARNEGIE MELLON 
 * UNIVERSITY MAKES NO WARRANTIES OF ANY KIND, EITHER EXPRESSED OR IMPLIED, 
 * AS TO ANY MATTER INCLUDING, BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR 
 * PURPOSE OR MERCHANTABILITY, EXCLUSIVITY, OR RESULTS OBTAINED FROM USE OF 
 * THE MATERIAL. CARNEGIE MELLON UNIVERSITY DOES NOT MAKE ANY WARRANTY OF 
 * ANY KIND WITH RESPECT TO FREEDOM FROM PATENT, TRADEMARK, OR COPYRIGHT 
 * INFRINGEMENT.
 * 
 * This Material has been approved for public release and unlimited 
 * distribution except as restricted below. 
 * 
 * This Material is provided to you under the terms and conditions of the 
 * Eclipse Public License Version 1.0 ("EPL"). A copy of the EPL is 
 * provided with this Content and is also available at 
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Carnegie Mellon� is registered in the U.S. Patent and Trademark 
 * Office by Carnegie Mellon University. 
 * 
 */

package org.osate.importer.properties;

import org.osate.aadl2.BooleanLiteral;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.Property;
import org.osate.aadl2.PropertyExpression;
import org.osate.aadl2.properties.PropertyLookupException;
import org.osate.contribution.sei.names.SEI;
import org.osate.xtext.aadl2.properties.util.GetProperties;

public class InspectProperty {

	private static Property inspectProperty = null;

	public static boolean shallInspect(final NamedElement ph) {
		PropertyExpression pv;
		boolean val;

		if (inspectProperty == null) {
			inspectProperty = GetProperties.lookupPropertyDefinition(ph, SEI._NAME, SEI.VDID_INSPECT);
		}

		val = false;

		try {
			pv = ph.getSimplePropertyValue(inspectProperty);

			if (pv instanceof BooleanLiteral) {
				val = ((BooleanLiteral) pv).getValue();
			}
		} catch (PropertyLookupException e) {
			return false;
		}
		System.out.println("return inspect: " + val + " on comp " + ph);
		return val;
	}

}
