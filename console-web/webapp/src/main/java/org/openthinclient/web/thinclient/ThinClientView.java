package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.BinderValidationStatus;
import com.vaadin.data.BindingValidationStatus;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Responsive;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

@SuppressWarnings("serial")
@SpringView(name = "thinclientview")
@SideBarItem(sectionId = DashboardSections.COMMON, caption="ThinClient", order = 99)
public final class ThinClientView extends Panel implements View {

   private static final Logger LOGGER = LoggerFactory.getLogger(ThinClientView.class);

   private final IMessageConveyor mc;
   private final VerticalLayout root;

   private List<File> visibleItems = new ArrayList<>();

   public ThinClientView() {

      mc = new MessageConveyor(UI.getCurrent().getLocale());
      
      addStyleName(ValoTheme.PANEL_BORDERLESS);
      setSizeFull();
      DashboardEventBus.register(this);

      root = new VerticalLayout();
      root.setSizeFull();
      root.setMargin(false);
      root.addStyleName("dashboard-view");
      setContent(root);
      Responsive.makeResponsive(root);

      root.addComponent(new ViewHeader("ThinClient"));

   }


   @Override
   public String getCaption() {
      return "ThinClient";
   }

   @PostConstruct
   private void init() {
      Component content = buildContent();
      root.addComponent(content);
      root.setExpandRatio(content, 1);
   }

   private Component buildContent() {

     FormLayout layout = new FormLayout();
     layout.addComponents(
         new BooleanPropertyPanel<>("AnOderAusPropertyCaption", new OtcBooleanProperty(true)),
         new TextPropertyPanel<>("Mein Text", new OtcTextProperty("Hallo Walther"))
     );

     // Button bar
     NativeButton save = new NativeButton("Save");
     NativeButton reset = new NativeButton("Reset");
     HorizontalLayout actions = new HorizontalLayout();
     actions.addComponents(save, reset);
     layout.addComponent(actions);

     // Click listeners for the buttons
     save.addClickListener(event -> {
       for (int i=0; i<layout.getComponentCount(); i++) {
         if (layout.getComponent(i) instanceof BinderComponent) {
           BinderComponent bc = (BinderComponent) layout.getComponent(i);
           if (bc.getBinder().writeBeanIfValid(bc.getBinder().getBean())) {
             Notification.show("Saved");
           } else {
             BinderValidationStatus<?> validate = bc.getBinder().validate();
             String errorText = validate.getFieldValidationStatuses()
                 .stream().filter(BindingValidationStatus::isError)
                 .map(BindingValidationStatus::getMessage)
                 .map(Optional::get).distinct()
                 .collect(Collectors.joining(", "));
             Notification.show("There are errors: " + errorText, Type.ERROR_MESSAGE);
           }
         }
       }


     });
     reset.addClickListener(event -> {
       // clear fields by setting null
       for (int i=0; i<layout.getComponentCount(); i++) {
         if (layout.getComponent(i) instanceof BinderComponent) {
           BinderComponent bc = (BinderComponent) layout.getComponent(i);
           bc.getBinder().readBean(null);
         }
       }
     });

     return layout;
   }

  @Override
   public void enter(ViewChangeEvent event) {

   }


}
