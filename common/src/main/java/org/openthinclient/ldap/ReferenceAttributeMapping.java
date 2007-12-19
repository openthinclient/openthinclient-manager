package org.openthinclient.ldap;

/**
 * Abstract superclass for {@link AttributeMapping}s dealing with attributes
 * which reference objects.
 */
abstract class ReferenceAttributeMapping extends AttributeMapping {
	public ReferenceAttributeMapping(String fieldName, String fieldType)
			throws ClassNotFoundException {
		super(fieldName, fieldType);
	}

	abstract Cardinality getCardinality();
}
