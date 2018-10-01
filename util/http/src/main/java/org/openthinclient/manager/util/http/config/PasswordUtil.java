/*
 * Created on 24.05.2013
 *
 * Copyright(c) 1995 - 2013 T-Systems Multimedia Solutions GmbH
 * Riesaer Str. 5, 01129 Dresden
 * All rights reserved.
 */
package org.openthinclient.manager.util.http.config;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

/**
 * Utility class containing password encryption and decryption facilities.
 */
public class PasswordUtil {

    /** The Constant LOG. */
    private static final Logger LOG = Logger.getLogger(PasswordUtil.class.getName());

    /** The Constant KEY_PASS. */
    private static final char[] KEY_PASS = new char[] { 'O', 'T', 'S', 'P', 'P', 'a', 's', 's' };

    /** The Constant ALGORITHM. */
    private static final String ALGORITHM = "PBEWithMD5AndDES";

    /** The Constant SALT. */
    private static final byte[] SALT = { (byte) 0xc7, (byte) 0x73, (byte) 0x21, (byte) 0x8c, (byte) 0x7e, (byte) 0xc8,
            (byte) 0xee, (byte) 0x99 };

    /** The Constant ITERATIONS. */
    private static final int ITERATIONS = 20;

    /**
     * The main method.
     * 
     * @param args
     *            the arguments
     * @throws Exception
     *             the exception
     */
    public static void main(String[] args) throws Exception {
        if (args != null && args.length > 0) {
            String input = args[0];
            String output = "";
            String check = "";

            output = encryptDES(input);
            System.out.println("DES value for input '" + input + "' is : " + output);

            check = decryptDES(output);
            System.out.println("Plain-text value for DES input '" + output + "' is : " + check);

        } else {
            System.out.println("No input value provided for hashing");
        }
    }

    /**
     * Decrypt the given input String using DES.
     * 
     * @param input
     *            the input
     * @return the string
     */
    public static String decryptDES(String input) {
        try {
            PBEKeySpec pbeKeySpec = new PBEKeySpec(KEY_PASS);
            PBEParameterSpec pbeParamSpec = new PBEParameterSpec(SALT, ITERATIONS);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
            SecretKey key = keyFactory.generateSecret(pbeKeySpec);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, pbeParamSpec);

            byte[] cipherText = Hex.decodeHex(input.toCharArray());
            return new String(cipher.doFinal(cipherText));

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error decrypting DES token", e);
        }
        return null;
    }

    /**
     * Encrypt the given input String using DES.
     * 
     * @param input
     *            the input
     * @return the string
     */
    public static String encryptDES(String input) {
        try {
            PBEKeySpec pbeKeySpec = new PBEKeySpec(KEY_PASS);
            PBEParameterSpec pbeParamSpec = new PBEParameterSpec(SALT, ITERATIONS);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
            SecretKey key = keyFactory.generateSecret(pbeKeySpec);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, pbeParamSpec);

            byte[] cipherText = cipher.doFinal(input.getBytes());
            return new String(Hex.encodeHex(cipherText));

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error decrypting DES token", e);
        }
        return null;
    }

    /**
     * Encrypt the given input String using Base64.
     * 
     * @param input
     *            the input
     * @return the string
     */
    public static String encryptBase64(String input) {
        return new String(Base64.encodeBase64(input.getBytes()));
    }

    /**
     * Decrypt the given input String using Base64.
     * 
     * @param input
     *            the input
     * @return the string
     */
    public static String decryptBase64(String input) {
        return new String(Base64.decodeBase64(input.getBytes()));
    }
}
