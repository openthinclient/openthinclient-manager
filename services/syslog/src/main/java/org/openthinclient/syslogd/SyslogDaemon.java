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
 ******************************************************************************/
package org.openthinclient.syslogd;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.levigo.util.collections.IntHashtable;

public abstract class SyslogDaemon implements Runnable {
	private static final int IN_BUF_SZ = (8 * 1024);
	public static final int SYSLOG_PORT = 514;

	public enum Facility {
		LOG_KERN(0, "kernel"), // kernel messages
		LOG_USER(1, "user"), // random user-level messages
		LOG_MAIL(2, "mail"), // mail system
		LOG_DAEMON(3, "daemon"), // system daemons
		LOG_AUTH(4, "authentication"), // security/authorization messages
		LOG_SYSLOG(5, "syslog"), // messages generated internally by syslogd

		LOG_LPR(6, "lpr"), // line printer subsystem
		LOG_NEWS(7, "news"), // network news subsystem
		LOG_UUCP(8, "uucp"), // UUCP subsystem
		LOG_CRON(9, "cron"), // clock daemon

		// other codes through 15 reserved for system use
		LOG_LOCAL0(16, "local0"), // reserved for local use
		LOG_LOCAL1(17, "local1"), // reserved for local use
		LOG_LOCAL2(18, "local2"), // reserved for local use
		LOG_LOCAL3(19, "local3"), // reserved for local use
		LOG_LOCAL4(20, "local4"), // reserved for local use
		LOG_LOCAL5(21, "local5"), // reserved for local use
		LOG_LOCAL6(22, "local6"), // reserved for local use
		LOG_LOCAL7(23, "local7"); // reserved for local use

		private static IntHashtable byValue;

		private final int code;
		private final String fullName;

		private Facility(int code, String fullName) {
			this.code = code;
			this.fullName = fullName;
		}

		public String getFullName() {
			return fullName;
		}

		// mask to extract facility part
		private static final int LOG_FACMASK = 0x03F8;

		public static Facility fromCode(int code) {
			if (null == Facility.byValue) {
				Facility.byValue = new IntHashtable();
				for (Facility f : values())
					Facility.byValue.put(f.code, f);
			}

			return (Facility) byValue.get(getCode(code));
		}

		/**
		 * @param code
		 * @return
		 */
		public static int getCode(int code) {
			return (code & LOG_FACMASK) >> 3;
		}
	}

	public static enum Priority {
		// system is unusable
		LOG_EMERG(0, "emergency"),
		// action must be taken immediately
		LOG_ALERT(1, "alert"),
		// critical conditions
		LOG_CRIT(2, "critical"),
		// error conditions
		LOG_ERR(3, "error"),
		// warning conditions
		LOG_WARNING(4, "warning"),
		// normal but significant condition
		LOG_NOTICE(5, "notice"),
		// informational
		LOG_INFO(6, "info"),
		// debug-level messages
		LOG_DEBUG(7, "debug"),
		// '*' in config, all levels
		LOG_ALL(8, "all");

		private static IntHashtable byValue;

		private final String fullName;
		private final int code;

		private Priority(int code, String name) {
			this.code = code;
			this.fullName = name;
		}

		public String getFullName() {
			return fullName;
		}

		// mask to extract priority part
		private static final int LOG_PRIMASK = 0x07;

		public static Priority fromCode(int code) {
			if (null == Priority.byValue) {
				Priority.byValue = new IntHashtable();
				for (Priority p : values())
					Priority.byValue.put(p.code, p);
			}

			return (Priority) byValue.get(getCode(code));
		}

		/**
		 * @param code
		 * @return
		 */
		public static int getCode(int code) {
			return code & LOG_PRIMASK;
		}
	}

	private final int port;
	private final DatagramSocket socket;
	private boolean shutdownRequested;

	public SyslogDaemon() throws SocketException {
		this(SYSLOG_PORT);
	}

	public SyslogDaemon(int port) throws SocketException {
		super();
		this.port = port;
		socket = new DatagramSocket(this.port);
	}

	@Override
	public void finalize() {
		this.shutdown();
	}

	public void shutdown() {
		this.shutdownRequested = true;
		if (null != socket && !socket.isClosed())
			socket.close();
	}

	public void run() {
		byte[] buffer = new byte[IN_BUF_SZ];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

		while (!shutdownRequested) {
			try {
				socket.receive(packet);
			} catch (IOException e) {
				handleError("Error receiving message: ", e);
				break;
			}

			try {
				this.processMessage(packet);
			} catch (Exception e) {
				handleError("Exception processing message: ", e);
			}
		}
	}

	public void processMessage(DatagramPacket packet) {
		try {
			String message = new String(packet.getData(), 0, packet.getLength(),
					"UTF-8");

			try {
				Pattern messagePattern = Pattern
						.compile("^<(\\d{1,3})>(\\p{ASCII}{3} \\d{2} \\d{2}:\\d{2}:\\d{2})\\s+"
								+ "(\\p{XDigit}{2}:\\p{XDigit}{2}:\\p{XDigit}{2}:\\p{XDigit}{2}:\\p{XDigit}{2}:\\p{XDigit}{2})\\s*"
								+ "(.*)$");

				Matcher m = messagePattern.matcher(message);
				if (m.matches()) {
					Priority priority;
					Facility facility;

					String priStr = m.group(1);
					if (priStr.length() == 0) {
						priority = Priority.LOG_INFO;
						facility = Facility.LOG_USER;
					} else {
						try {
							int code = Integer.parseInt(priStr);
							priority = Priority.fromCode(code);
							if (null == priority) {
								handleError("No Priority for code " + Priority.getCode(code),
										null);
								priority = Priority.LOG_INFO;
							}

							facility = Facility.fromCode(code);
							if (null == facility) {
								handleError("No Facility for code " + Facility.getCode(code),
										null);
								facility = Facility.LOG_USER;
							}
						} catch (NumberFormatException e) {
							// ignore, skip parsing facility
							priority = Priority.LOG_INFO;
							facility = Facility.LOG_USER;
						}
					}

					Date timestamp = new Date();
					try {
						timestamp = TimestampFormat.getInstance().parse(m.group(2));
					} catch (ParseException e) {
						handleError("Can't parse timestamp " + message.substring(0, 15),
								null);
						timestamp = new Date();
					}

					handleMessage(packet.getAddress(), m.group(3), priority, facility,
							timestamp, m.group(4));
				} else
					handleMessage(packet.getAddress(), null, Priority.LOG_INFO,
							Facility.LOG_USER, new Date(), message);
			} catch (Throwable t) {
				handleError("Can't parse message: " + message, t);
			}
		} catch (UnsupportedEncodingException e) {
			// doesn't happen
		}
	}

	/**
	 * @param message
	 * @param t
	 */
	protected abstract void handleError(String message, Throwable t);

	protected abstract void handleMessage(InetAddress source, String hostname,
			Priority prio, Facility facility, Date timestamp, String message);
}
