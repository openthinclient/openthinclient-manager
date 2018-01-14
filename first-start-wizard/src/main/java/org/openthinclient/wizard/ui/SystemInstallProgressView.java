package org.openthinclient.wizard.ui;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import static org.openthinclient.wizard.FirstStartWizardMessages.*;

public class SystemInstallProgressView extends VerticalLayout implements SystemInstallProgressPresenter.View {

  private final List<InstallItemViewImpl> statusLabels;
  private final Label titleLabel;
  private final Label descriptionLabel;
  private final Button restartButton;

  protected IMessageConveyor mc;
  
  public SystemInstallProgressView() {

    mc = new MessageConveyor(UI.getCurrent().getLocale());
    
    setSpacing(true);

    this.titleLabel = createH1Label("");
    addComponent(this.titleLabel);
    this.descriptionLabel = createLargeLabel("");
    addComponent(this.descriptionLabel);

    this.restartButton = new Button(mc.getMessage(UI_FIRSTSTART_INSTALL_BUTTON_RESTART));
    this.restartButton.setStyleName(ValoTheme.BUTTON_HUGE);
    this.restartButton.addStyleName(ValoTheme.BUTTON_FRIENDLY);
    this.restartButton.setVisible(false);
    addComponent(this.restartButton);

    statusLabels = new ArrayList<>();
  }

  private Label createH1Label(String label) {
    final Label l = new Label(label);
    l.setStyleName(ValoTheme.LABEL_H1);
    return l;
  }

  private Label createLargeLabel(String label) {
    final Label l = new Label(label);
    l.setStyleName(ValoTheme.LABEL_LARGE);
    return l;
  }

  @Override
  public void enableRestartButton(Runnable onButtonClicked) {
    if (!restartButton.isVisible()) {

      restartButton.setVisible(true);
      restartButton.setEnabled(true);
      restartButton.addClickListener((e) -> onButtonClicked.run());
    }
  }

  @Override
  public void setDescription(String description) {
    descriptionLabel.setValue(description);
  }

  @Override
  public void setTitle(String title) {
    titleLabel.setValue(title);
  }

  @Override
  public SystemInstallProgressPresenter.InstallItemView addItemView() {
    final InstallItemViewImpl itemView = new InstallItemViewImpl();
    addComponent(itemView);
    statusLabels.add(itemView);

    return itemView;
  }

  protected final class InstallItemViewImpl extends Panel implements SystemInstallProgressPresenter.InstallItemView {
    private final VerticalLayout content;
    private NumberFormat defaultFormat;

    public InstallItemViewImpl() {
      content = new VerticalLayout();
      content.setMargin(true);
      content.setSpacing(true);
      setContent(content);
      defaultFormat = NumberFormat.getPercentInstance();
      defaultFormat.setMinimumFractionDigits(1);
    }

    @Override
    public void setTitle(String title) {
      setCaption(title);
    }

    @Override
    public void setPending() {
      // FIXME
      content.removeAllComponents();
      content.addComponent(new Label(mc.getMessage(UI_FIRSTSTART_INSTALL_STATE_PENDING)));
      setIcon(FontAwesome.PAUSE);
    }

    @Override
    public void setRunning() {
      content.removeAllComponents();
      final HorizontalLayout hl = new HorizontalLayout();
      hl.setSpacing(true);
      final ProgressBar progressBar = new ProgressBar();
      progressBar.setIndeterminate(true);

      hl.addComponent(progressBar);
      hl.addComponent(new Label(mc.getMessage(UI_FIRSTSTART_INSTALL_STATE_EXECUTING)));

      content.addComponent(hl);
    }

    @Override
    public void setFailed() {
      content.removeAllComponents();
      setIcon(FontAwesome.TIMES);

      final Label caption = new Label(mc.getMessage(UI_FIRSTSTART_INSTALL_STATE_FAILED));
      caption.setStyleName(ValoTheme.LABEL_FAILURE);
      final Label message = new Label(mc.getMessage(UI_FIRSTSTART_INSTALL_STATE_FAILED_DESCRIPTION));
      content.addComponents(caption, message);
    }

    @Override
    public void setFinished() {
      content.removeAllComponents();

      final Label caption = new Label(mc.getMessage(UI_FIRSTSTART_INSTALL_STATE_SUCCEED));
      caption.setStyleName(ValoTheme.LABEL_SUCCESS);
      content.addComponent(caption);

      setIcon(FontAwesome.CHECK);
    }

    @Override
    public void remove() {
      statusLabels.remove(this);
      SystemInstallProgressView.this.removeComponent(this);
    }

    @Override
    public void setProgress(double progress) {
      content.removeAllComponents();
      final HorizontalLayout hl = new HorizontalLayout();
      hl.setSpacing(true);
      hl.addComponent(new Label(mc.getMessage(UI_FIRSTSTART_INSTALL_STATE_EXECUTING) + ": " + defaultFormat.format(progress)));
      content.addComponent(hl);
    }
  }
}
