package org.openthinclient.cron;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Date;

public class WakeUp implements Runnable {

	public void run() {

		System.out.println(new Date());

		final int port = 9;
		final String macAddress = "00:E0:C5:4E:7F:CF";
		final String broadcast = "10.1.4.0";

		try {
			final byte[] macBytes = getMacBytes(macAddress);
			final byte[] bytes = new byte[6 + 16 * macBytes.length];
			for (int i = 0; i < 6; i++)
				bytes[i] = (byte) 0xff;
			for (int i = 6; i < bytes.length; i += macBytes.length)
				System.arraycopy(macBytes, 0, bytes, i, macBytes.length);

			final InetAddress address = InetAddress.getByName(broadcast);
			final DatagramPacket packet = new DatagramPacket(bytes, bytes.length,
					address, port);
			final DatagramSocket socket = new DatagramSocket();
			socket.send(packet);
			socket.close();
			// eventl thread um mehrmals zu schicken - um sicher zu gehen....
			System.out.println("Wake-On-LAN packet was sent to " + address + " - "
					+ macAddress);
		} catch (final Exception e) {
			System.out.println("Failed to send Wake-on-LAN packet: + e");
		}
	}

	private static byte[] getMacBytes(String macStr)
			throws IllegalArgumentException {
		final byte[] bytes = new byte[6];
		final String[] hex = macStr.split("(\\:|\\-)");
		if (hex.length != 6)
			throw new IllegalArgumentException("Invalid MAC address.");
		try {
			for (int i = 0; i < 6; i++)
				bytes[i] = (byte) Integer.parseInt(hex[i], 16);
		} catch (final NumberFormatException e) {
			throw new IllegalArgumentException("Invalid hex digit in MAC address.");
		}
		return bytes;
	}
}
