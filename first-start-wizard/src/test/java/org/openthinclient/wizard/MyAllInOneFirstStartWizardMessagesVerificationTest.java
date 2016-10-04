package org.openthinclient.wizard;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import ch.qos.cal10n.verifier.Cal10nError;
import ch.qos.cal10n.verifier.IMessageKeyVerifier;
import ch.qos.cal10n.verifier.MessageKeyVerifier;

public class MyAllInOneFirstStartWizardMessagesVerificationTest {

  // verify all locales in one step
  @Test
//  @Ignore
  public void all() {
    IMessageKeyVerifier mkv = new MessageKeyVerifier(FirstStartWizardMessages.class);
    List<Cal10nError> errorList = mkv.verifyAllLocales();
    for(Cal10nError error: errorList) {
      System.out.println(error);
    }
    assertEquals(0, errorList.size());
  }
} 