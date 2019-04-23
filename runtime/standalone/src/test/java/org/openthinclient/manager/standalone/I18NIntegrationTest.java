package org.openthinclient.manager.standalone;

import org.junit.Test;
import org.openthinclient.advisor.AdvisorMessages;
//import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.wizard.FirstStartWizardMessages;

import java.util.List;

import ch.qos.cal10n.verifier.Cal10nError;
import ch.qos.cal10n.verifier.IMessageKeyVerifier;
import ch.qos.cal10n.verifier.MessageKeyVerifier;

import static org.junit.Assert.assertEquals;

public class I18NIntegrationTest {

  @Test
  public void testFirstStartWizardMessages() {
    assertMessagesCorrect(FirstStartWizardMessages.class);
  }

  // TODO: wtmflow enable test again
//  @Test
//  public void testConsoleWebMessages() throws Exception {
//    assertMessagesCorrect(ConsoleWebMessages.class);
//  }

  @Test
  public void testAdvisorMessages() throws Exception {
    assertMessagesCorrect(AdvisorMessages.class);
  }

  private void assertMessagesCorrect(Class<? extends Enum<?>> clazz) {
    IMessageKeyVerifier mkv = new MessageKeyVerifier(clazz);
    List<Cal10nError> errorList = mkv.verifyAllLocales();
    for (Cal10nError error : errorList) {
      System.out.println(error);
    }
    assertEquals(0, errorList.size());
  }
}
