package org.openthinclient.wizard.ui;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;

import java.util.function.Consumer;

import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_CHECK_NETWORK;

public class CheckingProgressWindow extends Window implements CheckingProgressPresenter.View {
  private final Button okButton = new Button("OK");
  private final Button cancelButton = new Button("Cancel");
  private final ProgressBar progressBar = new ProgressBar();
  private final Label resultLabel = new Label();
  private Consumer<CheckingProgressPresenter.View> okHandler;
  private Consumer<CheckingProgressPresenter.View> cancelHandler;

  protected IMessageConveyor mc;
  
  public CheckingProgressWindow() {
    
    mc = new MessageConveyor(UI.getCurrent().getLocale());

    setContent(createCheckingWindowContent());
    setModal(true);
    setResizable(false);
    setClosable(false);

    resultLabel.setVisible(false);

    okButton.addClickListener(this::onOkClicked);
    cancelButton.addClickListener(this::onCancelClicked);

  }

  private void onOkClicked(Button.ClickEvent clickEvent) {
    if (okHandler != null) {
      okHandler.accept(this);
    }
  }

  private void onCancelClicked(Button.ClickEvent clickEvent) {
    if (cancelHandler != null) {
      cancelHandler.accept(this);
    }
  }

  private VerticalLayout createCheckingWindowContent() {
    final VerticalLayout components = new VerticalLayout();
    components.setSpacing(true);
    components.setMargin(true);

    final Label label = new Label(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_CHECK_NETWORK));
    label.setStyleName(ValoTheme.LABEL_LARGE);
    components.addComponent(label);
    components.addComponent(progressBar);
    components.addComponent(resultLabel);

    final HorizontalLayout footer = new HorizontalLayout();
    footer.setWidth(100, Unit.PERCENTAGE);
    footer.setStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR);
    footer.addComponent(okButton);
    footer.addComponent(cancelButton);
    components.addComponent(footer);

    return components;
  }

  @Override
  public void accessUI(Consumer<CheckingProgressPresenter.View> consumer) {
    getUI().access(() -> consumer.accept(this));
  }

  @Override
  public void setInProgress() {
    progressBar.setIndeterminate(true);
//    progressBar.setImmediate(true);
    progressBar.setVisible(true);

    okButton.setEnabled(false);
    resultLabel.setVisible(false);
  }

  @Override
  public void setSuccess(String message) {
    okButton.setEnabled(true);
    progressBar.setValue(1f);
    progressBar.setVisible(false);

    resultLabel.setStyleName(ValoTheme.LABEL_SUCCESS);
    resultLabel.setValue(message);
    resultLabel.setVisible(true);
  }

  @Override
  public void setError(String message) {
    okButton.setEnabled(false);
    progressBar.setValue(0f);
    progressBar.setVisible(false);

    resultLabel.setStyleName(ValoTheme.LABEL_FAILURE);
    resultLabel.setValue(message);
    resultLabel.setVisible(true);
  }

  @Override
  public void setOnOkHandler(Consumer<CheckingProgressPresenter.View> handler) {
    this.okHandler = handler;
  }

  @Override
  public void setOnCancelHandler(Consumer<CheckingProgressPresenter.View> handler) {
    this.cancelHandler = handler;
  }


}
