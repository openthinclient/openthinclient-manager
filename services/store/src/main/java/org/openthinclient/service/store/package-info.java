/**
 * This package / module provides very fast and memory efficient access
 * for the PXE boot services. This makes it possible to boot 1000+ clients
 * simultaneously.
 *
 * The embedded LDAP server is accessed directly (instead of being
 * connected via localhost). Most data will be cached for a short time
 * to reduce repeated LDAP searches. The code only reads necessary data
 * and only does what needs to be done.
 *
 * ClientBootData, the primary interface, give easy access to the
 * combined data from all relevant ldap entries and schemas.
 */

package org.openthinclient.service.store;
