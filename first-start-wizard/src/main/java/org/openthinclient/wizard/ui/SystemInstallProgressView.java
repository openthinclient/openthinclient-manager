package org.openthinclient.wizard.ui;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import java.util.ArrayList;
import java.util.List;

public class SystemInstallProgressView extends VerticalLayout implements SystemInstallProgressPresenter.View {

  private final List<InstallItemViewImpl> statusLabels;
  private final Label titleLabel;
  private final Label descriptionLabel;

  public SystemInstallProgressView() {

    setSpacing(true);

    this.titleLabel = createH1Label("");
    addComponent(this.titleLabel);
    this.descriptionLabel = createLargeLabel("");
    addComponent(this.descriptionLabel);


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


    public InstallItemViewImpl() {
      content = new VerticalLayout();
      content.setMargin(true);
      content.setSpacing(true);
      setContent(content);

    }

    @Override
    public void setTitle(String title) {
      setCaption(title);
    }

    @Override
    public void setPending() {
      // FIXME
      content.removeAllComponents();
      content.addComponent(new Label("Pending execution..."));
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
      hl.addComponent(new Label("Currently executing"));

      content.addComponent(hl);
    }

    @Override
    public void setFailed() {
      content.removeAllComponents();
      setIcon(FontAwesome.TIMES);

      final Label caption = new Label("This installation step failed.");
      caption.setStyleName(ValoTheme.LABEL_FAILURE);
      final Label message = new Label("Please refer to the server logfile for further details");
      content.addComponents(caption, message);
    }

    @Override
    public void setFinished() {
      content.removeAllComponents();

      final Label caption = new Label("Successfully executed");
      caption.setStyleName(ValoTheme.LABEL_SUCCESS);
      content.addComponent(caption);

      setIcon(FontAwesome.CHECK);
    }

    @Override
    public void remove() {
      statusLabels.remove(this);
      SystemInstallProgressView.this.removeComponent(this);
    }
  }
}
