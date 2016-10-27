package org.openthinclient.common.test.ads;

public class Menu {

	// Settings - Creation
	private final static AllocateGroupsModus modus = AllocateGroupsModus.onlyLowerToUpper;

	private final static int quantityUsers = 0;
	private final static int normal = 0;
	private final static int upper = 0;

	private final static String ouNameNet = "net";
	private final static String ouNameTree = "tree";
	private final static String ouNameDefault = "default";
	private final static String ouNameMixed = "mixed";

	// only for AdsCreateAOUTree
	private final static int heightOfTheTree = 0;

	// only for AdsCreateAOuNet
	private final static int quantityOUs = 0;
	// Attention: Quantity of created objects: quantityUsers*quantityOU +
	// (normal+upper)*quantityOUs

	// ............................................................................

	// Settings - Connection

	private final static String baseDN = "";
	private final static short portNumber = (short) 389;
	private final static String hostname = "";
	private final static String username = "";
	private final static String password = "";

	// ............................................................................

	public enum AllocateGroupsModus {
		onlyLowerToUpper, anyoneWithAny;
	}

	public static AllocateGroupsModus getModus() {
		return modus;
	}

	public static int getQuantityUsers() {
		return quantityUsers;
	}

	public static int getNormal() {
		return normal;
	}

	public static int getUpper() {
		return upper;
	}

	public static int getHeightOfTheTree() {
		return heightOfTheTree;
	}

	public static int getQuantityOUs() {
		return quantityOUs;
	}

	public static String getBaseDN() {
		return baseDN;
	}

	public static short getPortNumber() {
		return portNumber;
	}

	public static String getHostname() {
		return hostname;
	}

	public static String getUsername() {
		return username;
	}

	public static String getPassword() {
		return password;
	}

	public static String getName(Class yourclass) {
		if (yourclass == AdsCreateAOuNet.class)
			return ouNameNet;
		if (yourclass == AdsCreateAOuTree.class)
			return ouNameTree;
		if (yourclass == AdsMixedOU.class)
			return ouNameMixed;
		if (yourclass == AdsCreateADefaultTree.class)
			return ouNameDefault;
		return "";
	}
}
