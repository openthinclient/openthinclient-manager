package org.openthinclient.wizard.ui.steps;

import com.vaadin.event.UIEvents;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.wizard.model.CheckStatus;
import org.vaadin.teemu.wizards.Wizard;

import java.util.List;

public abstract class AbstractCheckExecutingStep extends AbstractStep {
  protected final Wizard wizard;
  private final UIEvents.PollListener pollListener = this::onPoll;

  public AbstractCheckExecutingStep(Wizard wizard) {
    this.wizard = wizard;
  }

  protected void updateStatusLabels() {
    getStatusLabels().forEach(CheckStatusLabel::update);
  }

  protected abstract List<CheckStatusLabel> getStatusLabels();

  protected void runChecks() {
    wizard.getUI().setPollInterval(100);
    wizard.getUI().addPollListener(pollListener);
    onRunChecks();
    updateStatusLabels();
  }

  protected abstract void onRunChecks();

  private void onPoll(UIEvents.PollEvent pollEvent) {

    updateStatusLabels();

    // have all checks been run?
    if (isChecksFinished()) {
      // remove the listener and reset the poll mode
      wizard.getUI().removePollListener(pollListener);
      wizard.getUI().setPollInterval(-1);

      onChecksFinished();
    }
  }

  protected abstract void onChecksFinished();

  protected abstract boolean isChecksFinished();

  protected static final class CheckStatusLabel extends Label {
    private final CheckStatus checkStatus;

    public CheckStatusLabel(CheckStatus checkStatus) {
      this.checkStatus = checkStatus;
      setValue(checkStatus.getCheck().getName());
    }

    public void update() {
      if (checkStatus.getResultType() != null)
        switch (checkStatus.getResultType()) {
          case SUCCESS:
            setStyleName(ValoTheme.LABEL_SUCCESS);
            break;
          case WARNING:
            // FIXME
            break;
          case FAILED:
            setStyleName(ValoTheme.LABEL_FAILURE);
            break;
        }
    }
  }
}
