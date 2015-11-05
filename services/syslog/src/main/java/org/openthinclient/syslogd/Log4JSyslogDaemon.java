/*******************************************************************************
 * openthinclient.org ThinClient suite
 * 
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 * 
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *******************************************************************************/
package org.openthinclient.syslogd;

import java.net.InetAddress;
import java.net.SocketException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.ext.EventData;
import org.slf4j.ext.EventLogger;

/**
 * @author levigo
 */
public class Log4JSyslogDaemon extends SyslogDaemon {
	
  private static final Logger logger = LoggerFactory.getLogger(Log4JSyslogDaemon.class);

  private Map<Facility, Logger> loggerMap = new HashMap<Facility, Logger>();
  private final String prefix;

  public Log4JSyslogDaemon() throws SocketException {
    super();
    this.prefix = "syslog";
  }

  public Log4JSyslogDaemon(int port) throws SocketException {
    this(port, "syslog");
  }

  public Log4JSyslogDaemon(int port, String prefix) throws SocketException {
    super(port);
    this.prefix = prefix;
  }

  @Override
  protected void handleMessage(InetAddress source, String hostname, Priority prio, Facility facility, Date timestamp, String message) {
	  
	Logger logger = loggerMap.get(facility);
	if (null == logger) {
		logger = LoggerFactory.getLogger(prefix + "." + facility.getFullName());
		loggerMap.put(facility, logger);
	}

	
	// LoggingEvent le = new LoggingEvent("foo", logger, timestamp.getTime(), prio.getL4jPriority(), message, null);
	// JavaDoc:	//	public LoggingEvent(String fqnOfCategoryClass,
			//            Category logger,
			//            Priority level,
			//            Object message,
			//            Throwable throwable)
			//
			//Instantiate a LoggingEvent from the supplied parameters. 
	
	EventData data = new EventData();
	data.setEventId("foo");
	data.setEventDateTime(timestamp);
	data.setMessage(message);
	data.setEventType(prio.getFullName());
	
	MDC.put("hostname", hostname != null ? hostname : "-");
	MDC.put("peer", null != source ? source.getHostAddress() : "-");

	//    logger.callAppenders(le);
    EventLogger.logEvent(data);
    
  }

  /*
   * @see org.openthinclient.syslogd.SyslogDaemon#handleError(java.lang.String,
   *      java.lang.Throwable)
   */
  @Override
  protected void handleError(String message, Throwable t) {
    logger.error(message, t);
  }
}
