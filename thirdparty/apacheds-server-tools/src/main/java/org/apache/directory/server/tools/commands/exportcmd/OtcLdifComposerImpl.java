package org.apache.directory.server.tools.commands.exportcmd;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.directory.shared.ldap.util.Base64;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;

/**
 * This is a copy of org.apache.directory.shared.ldap.ldif.LdifComposerImpl but with MultiValueMap
 */
public class OtcLdifComposerImpl
{
    /**
     * Generates an LDIF from a multi map.
     * 
     * @param attrHash the multi map of single and multivalued attributes.
     * @return the LDIF as a String.
     */
    public String compose( MultiValueMap attrHash )
    {
        Object val = null;
        String key = null;
        Iterator keys = attrHash.keySet().iterator();
        Iterator values = null;
        Collection valueCol = null;
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter( sw );

        while ( keys.hasNext() )
        {
            key = ( String ) keys.next();
            valueCol = ( Collection ) attrHash.get( key );
            values = valueCol.iterator();

            if ( valueCol.isEmpty() )
            {
                continue;
            }
            else if ( valueCol.size() == 1 )
            {
                out.print( key );
                out.print( ':' );
                val = values.next();

                if ( val.getClass().isArray() )
                {
                    out.print( ": " );
                    out.println( base64encode( ( byte[] ) val ) );
                }
                else
                {
                    out.print( ' ' );
                    out.println( val );
                }
                continue;
            }

            while ( values.hasNext() )
            {
                out.print( key );
                out.print( ':' );
                val = values.next();

                if ( val.getClass().isArray() )
                {
                    out.print( ": " );
                    out.println( base64encode( ( byte[] ) val ) );
                }
                else
                {
                    out.print( ' ' );
                    out.println( val );
                }
            }
        }

        return sw.getBuffer().toString();
    }


    /**
     * Encodes an binary data into a base64 String.
     * 
     * @param byteArray
     *            the value of a binary attribute.
     * @return the encoded binary data as a char array.
     */
    public char[] base64encode( byte[] byteArray )
    {
        return Base64.encode( byteArray );
    }
}
