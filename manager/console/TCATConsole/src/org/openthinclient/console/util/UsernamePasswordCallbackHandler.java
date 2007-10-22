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
package org.openthinclient.console.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.Repository;
import org.openthinclient.common.directory.CachingCallbackHandler;
import org.openthinclient.console.Messages;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A CallbackHandler which provides a fixed combination of username and
 * password.
 * 
 * @author levigo
 */
public class UsernamePasswordCallbackHandler
    implements
      CachingCallbackHandler,
      Serializable {
  private static final long serialVersionUID = 1L;

  private class PasswordEntryPanel extends JPanel {
    private JPasswordField passwordField;
    private JCheckBox savePasswordBox;
    private JTextField userField;

    PasswordEntryPanel(boolean askForUsername) throws IOException {
      final DefaultFormBuilder dfb = new DefaultFormBuilder(new FormLayout(
          "p, 3dlu, f:p:g"), Messages.getBundle(), this); //$NON-NLS-1$
      dfb.setDefaultDialogBorder();

      dfb.append(new JLabel(Messages.getString("UsernamePassword.subtitle")), //$NON-NLS-1$
          3);
      dfb.nextLine();

      JLabel pd = new JLabel(protectionDomain);
      dfb.append("", pd); //$NON-NLS-1$
      dfb.nextLine();

      dfb.appendUnrelatedComponentsGapRow();
      dfb.nextLine();

      userField = new JTextField((String) unscramble(username));
      userField.setEnabled(false);
      dfb.appendI15d("UsernamePassword.username", userField); //$NON-NLS-1$
      dfb.nextLine();

      passwordField = new JPasswordField();
      dfb.appendI15d("UsernamePassword.password", passwordField); //$NON-NLS-1$
      dfb.nextLine();

      savePasswordBox = new JCheckBox(Messages
          .getString("UsernamePassword.savePassword")); //$NON-NLS-1$
      dfb.append(savePasswordBox, 3);
    }

    char[] getPassword() {
      return passwordField.getPassword();
    }

    boolean getSavePassword() {
      return savePasswordBox.isSelected();
    }

    String getUsername() {
      return userField.getText();
    }
  }

  private String protectionDomain;
  private Key key;

  private transient SealedObject username;
  private transient SealedObject password;
  private String pwdFileName;
  private final boolean transientPassword;

  public UsernamePasswordCallbackHandler(String protectionDomain,
      String username, char password[], boolean transientPassword) {
    this.protectionDomain = protectionDomain;
    this.transientPassword = transientPassword;
    try {
      this.key = KeyGenerator.getInstance("DES").generateKey(); //$NON-NLS-1$

      this.username = scramble(username);
      this.password = scramble(password);

      saveCredentials(transientPassword);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Crypto prerequisites not met", e); //$NON-NLS-1$
    } catch (IOException e) {
      throw new RuntimeException("Can't scramble credentials", e); //$NON-NLS-1$
    }
  }

  /**
   * @param transientPassword
   * @throws NoSuchAlgorithmException
   * @throws NoSuchAlgorithmException
   * @throws IOException
   */
  private void saveCredentials(boolean transientPassword) throws IOException {
    setPwdFileName();

    FileObject folder = Repository.getDefault().getDefaultFileSystem()
        .getRoot().getFileObject("Credentials"); //$NON-NLS-1$

    try {
      FileObject fo = folder.getFileObject(pwdFileName, "ser"); //$NON-NLS-1$
      if (fo == null)
        fo = folder.createData(pwdFileName, "ser"); //$NON-NLS-1$

      FileLock lock = fo.lock();
      try {
        ObjectOutputStream str = new ObjectOutputStream(fo
            .getOutputStream(lock));
        try {
          str.writeObject(this.username);
          if (transientPassword)
            str.writeObject(null);
          else
            str.writeObject(this.password);
        } finally {
          str.close();
        }
      } finally {
        lock.releaseLock();
      }
    } catch (IOException ioe) {
      ErrorManager.getDefault().notify(ioe);
    }
  }

  /**
   * @throws NoSuchAlgorithmException
   * @throws IOException
   */
  private void setPwdFileName() throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
      digest.update(((String) unscramble(username)).getBytes());
      digest.update(protectionDomain.getBytes());
      byte[] bs = digest.digest();

      StringBuffer sb = new StringBuffer(bs.length * 2);
      for (int i = 0; i < bs.length; i++) {
        String s = Integer.toHexString(bs[i] & 0xff);
        if (s.length() < 2)
          sb.append('0');
        sb.append(s);
      }
      pwdFileName = sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IOException(
          "Can't save credentials: digest method MD5 unavailable."); //$NON-NLS-1$
    }
  }

  private void readObject(ObjectInputStream s) throws IOException,
      ClassNotFoundException {
    s.defaultReadObject();

    // try to load the password
    FileObject folder = Repository.getDefault().getDefaultFileSystem()
        .getRoot().getFileObject("Credentials"); //$NON-NLS-1$
    FileObject fo = folder.getFileObject(pwdFileName, "ser"); //$NON-NLS-1$
    if (fo != null) {
      try {
        FileLock lock = fo.lock();
        try {
          ObjectInputStream str = new ObjectInputStream(fo.getInputStream());
          try {
            this.username = (SealedObject) str.readObject();
            this.password = (SealedObject) str.readObject();
          } finally {
            str.close();
          }
        } finally {
          lock.releaseLock();
        }
      } catch (IOException ioe) {
        ErrorManager.getDefault().notify(ioe);
      }
    }
  }

  /*
   * @see javax.security.auth.callback.CallbackHandler#handle(javax.security.auth.callback.Callback[])
   */
  public void handle(Callback[] callbacks) throws IOException,
      UnsupportedCallbackException {
    for (int i = 0; i < callbacks.length; i++) {
      Callback callback = callbacks[i];
      if (callback instanceof NameCallback) {
        ((NameCallback) callback).setName((String) unscramble(username));
      } else if (callback instanceof PasswordCallback) {
        if (null == password)
          queryForPassword();

        ((PasswordCallback) callback)
            .setPassword((char[]) unscramble(password));
      }
    }
  }

  /**
   * @throws IOException
   * 
   */
  private void queryForPassword() throws IOException {
    PasswordEntryPanel pep = new PasswordEntryPanel(false);
    DialogDescriptor dd = new DialogDescriptor(pep, Messages
        .getString("UsernamePassword.titleEnterPassword"), true, null); //$NON-NLS-1$

    DialogDisplayer.getDefault().createDialog(dd).setVisible(true);

    if (dd.getValue() == DialogDescriptor.CANCEL_OPTION)
      throw new IOException("User cancelled password entry"); //$NON-NLS-1$

    password = scramble(pep.getPassword());

    if (pep.getSavePassword())
      saveCredentials(false);
  }

  /**
   * @throws IOException
   * 
   */
  public void purgeCache() throws IOException {
    PasswordEntryPanel pep = new PasswordEntryPanel(true);
    DialogDescriptor dd = new DialogDescriptor(pep, Messages
        .getString("UsernamePassword.titleLoginFailed"), true, null); //$NON-NLS-1$

    DialogDisplayer.getDefault().createDialog(dd).setVisible(true);

    if (dd.getValue() == DialogDescriptor.CANCEL_OPTION)
      throw new IOException("User cancelled password entry"); //$NON-NLS-1$

    username = scramble(pep.getUsername());
    password = scramble(pep.getPassword());

    saveCredentials(pep.getSavePassword());
  }

  /**
   * @param password2
   * @return
   * @throws NoSuchPaddingException
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeyException
   * @throws IOException
   * @throws IllegalBlockSizeException
   */
  private SealedObject scramble(Serializable clear) throws IOException {
    try {
      Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding"); //$NON-NLS-1$
      cipher.init(Cipher.ENCRYPT_MODE, key);
      return new SealedObject(clear, cipher);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException("Can't scramble: " + e); //$NON-NLS-1$
    }
  }

  /**
   * @param password2
   * @return
   * @throws ClassNotFoundException
   * @throws IOException
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeyException
   */
  private Object unscramble(SealedObject scrambled) throws IOException {
    try {
      return scrambled.getObject(key);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException("Can't unscramble: " + e); //$NON-NLS-1$
    }
  }

  public String getProtectionDomain() {
    return protectionDomain;
  }

  public void setProtectionDomain(String protectionDomain) throws IOException {
    this.protectionDomain = protectionDomain;
    saveCredentials(transientPassword);
  }
}
