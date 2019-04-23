/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.openthinclient.flow.staticmenu;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import org.openthinclient.common.model.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.List;

/**
 * An abstract view
 *
 */
public abstract class BaseViewLayout extends Div {

    /**
     * Creates the view.
     */
    public BaseViewLayout() {

        setClassName("base-content");

        Div header = new Div();
        header.setClassName("topnav");

        HorizontalLayout headerTop = new HorizontalLayout();
        headerTop.setMargin(false);
        headerTop.addClassName("header-top");
        headerTop.setWidth("100%");

        Component searchTextField = buildSearchTextField();
        headerTop.add(searchTextField);

        Component logout = buildLogoutButton();
        headerTop.add(logout);
        headerTop.setAlignItems(FlexComponent.Alignment.CENTER);

        header.add(headerTop);


        add(header);

    }

    private Component buildLogoutButton() {

        HorizontalLayout hl = new HorizontalLayout();
//        hl.setMargin(new MarginInfo(false, true, false, false));
        hl.setSpacing(false);

        UserDetails principal = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Label circle = new Label(principal.getUsername().substring(0,1).toUpperCase());
        circle.addClassName("header-circle");
        hl.add(circle);

//        MenuBar menuBar = new MenuBar();
//        menuBar.setWidth("100%");
//        menuBar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);
//        menuBar.addStyleName(ValoTheme.MENUBAR_SMALL);
//        menuBar.addStyleName("header-menu");
//
//        hl.addComponent(menuBar);
//
//        final MenuBar.MenuItem file = menuBar.addItem(principal.getUsername(), null);
//        file.addItem(mc.getMessage(ConsoleWebMessages.UI_PROFILE), this::showProfileSubWindow);
//        file.addItem(mc.getMessage(ConsoleWebMessages.UI_LOGOUT), e -> eventBus.publish(this, new DashboardEvent.UserLoggedOutEvent()));

        return hl;
    }

    private TextField buildSearchTextField() {
        TextField searchTextField = new TextField();
        searchTextField.setPlaceholder("search");
//        searchTextField.setIcon(new ThemeResource("icon/magnify.svg"));
//        searchTextField.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        searchTextField.addClassName("header-searchfield");
//        searchTextField.addValueChangeListener(this::onFilterTextChange);
        return searchTextField;
    }

    Grid resultObjectGrid;
    private void createResultObjectGrid() {
        resultObjectGrid = new Grid<>();
        resultObjectGrid.addClassNames("directoryObjectSelectionGrid");
        resultObjectGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
//        resultObjectGrid.removeHeaderRow(0);
//        resultObjectGrid.addItemClickListener(this::resultObjectClicked);
        resultObjectGrid.setClassNameGenerator(directoryObject -> directoryObject.getClass().getSimpleName().toLowerCase()); // Style based on directoryObject class
//        Grid.Column<DirectoryObject, ThemeResource> imageColumn = resultObjectGrid.addColumn(
//            profile -> {
//                ThemeResource resource;
//                if (profile instanceof Application) {
//                    resource = ThinclientView.PACKAGES;
//                } else if (profile instanceof ApplicationGroup) {
//                    resource =  ThinclientView.APPLICATIONGROUP;
//                } else if (profile instanceof Printer) {
//                    resource =  ThinclientView.PRINTER;
//                } else if (profile instanceof HardwareType) {
//                    resource =  ThinclientView.HARDWARE;
//                } else if (profile instanceof Device) {
//                    resource =  ThinclientView.DEVICE;
//                } else if (profile instanceof Client) {
//                    resource =  ThinclientView.CLIENT;
//                } else if (profile instanceof Location) {
//                    resource =  ThinclientView.LOCATION;
//                } else if (profile instanceof User) {
//                    resource =  ThinclientView.USER;
//                } else {
//                    resource =  null;
//                }
//                return resource;
//            },
//            new ImageRenderer<>()
//        );
//        resultObjectGrid.addColumn(DirectoryObject::getName);

//        searchResultWindow = new Window(null, resultObjectGrid);
//        searchResultWindow.setClosable(false);
//        searchResultWindow.setResizable(false);
//        searchResultWindow.setDraggable(false);
//        searchResultWindow.addStyleName("header-search-result");
//        searchResultWindow.setWidthUndefined();
//
//        // fill objectGrid
//        long start = System.currentTimeMillis();
//        List<DirectoryObject> directoryObjects = new ArrayList<>();
//        directoryObjects.addAll(applicationService.findAll());
//        directoryObjects.addAll(printerService.findAll());
//        directoryObjects.addAll(deviceService.findAll());
//        directoryObjects.addAll(hardwareTypeService.findAll());
//        try {
//            directoryObjects.addAll(clientService.findAll());
//        } catch (Exception e) {
//            LOGGER.warn("Cannot find clients for search: " + e.getMessage());
//        }
//        directoryObjects.addAll(locationService.findAll());
//        ListDataProvider dataProvider = DataProvider.ofCollection(directoryObjects);
//        dataProvider.setSortOrder(source -> ((DirectoryObject) source).getName().toLowerCase(), SortDirection.ASCENDING);
//        resultObjectGrid.setDataProvider(dataProvider);
//        LOGGER.info("Setup directoryObjects-grid took " + (System.currentTimeMillis() - start) + "ms");

    }

//    private void resultObjectClicked(Grid.ItemClick<DirectoryObject> directoryObjectItemClick) {
//
//        // only take double-clicks
//        if (directoryObjectItemClick.getMouseEventDetails().isDoubleClick()) {
//
//            DirectoryObject directoryObject = directoryObjectItemClick.getItem();
//            String navigationState = null;
//            if (directoryObject instanceof ApplicationGroup) {
//                navigationState = ApplicationGroupView.NAME;
//            } else if (directoryObject instanceof Application) {
//                navigationState = ApplicationView.NAME;
//            } else if (directoryObject instanceof Client) {
//                navigationState = ClientView.NAME;
//            } else if (directoryObject instanceof Device) {
//                navigationState = DeviceView.NAME;
//            } else if (directoryObject instanceof HardwareType) {
//                navigationState = HardwaretypeView.NAME;
//            } else if (directoryObject instanceof Location) {
//                navigationState = LocationView.NAME;
//            } else if (directoryObject instanceof Printer) {
//                navigationState = PrinterView.NAME;
//            } else if (directoryObject instanceof User) {
//                navigationState = UserView.NAME;
//            }
//
//            if (navigationState != null) {
//                UI.getCurrent().removeWindow(searchResultWindow);
//                getNavigator().navigateTo(navigationState + "/" + directoryObject.getName());
//            }
//        }
//    }

//    private void onFilterTextChange(HasValue.ValueChangeEvent<String> event) {
//        if (event.getValue().length() > 0) {
//            ListDataProvider<DirectoryObject> dataProvider = (ListDataProvider<DirectoryObject>) resultObjectGrid.getDataProvider();
//            dataProvider.setFilter(directoryObject ->
//                caseInsensitiveContains(directoryObject.getName(), event.getValue()) ||
//                    clientSpecificParamContains(directoryObject, event.getValue())
//            );
//
//            // TODO: Resizing result- and window-height, improve this magic: references style .v-window-header-search-result max-height
//            int windowHeight = (dataProvider.size(new Query<>()) * 37);
//            resultObjectGrid.setHeight(windowHeight > 300 ? 300 : windowHeight, Unit.PIXELS);
//            searchResultWindow.setHeight(windowHeight > 300 ? 300 : windowHeight, Unit.PIXELS);
//            if (!UI.getCurrent().getWindows().contains(searchResultWindow)) {
//                UI.getCurrent().addWindow(searchResultWindow);
//            }
//        } else {
//            UI.getCurrent().removeWindow(searchResultWindow);
//        }
//
//    }

    private boolean clientSpecificParamContains(DirectoryObject directoryObject, String value) {
        if (directoryObject instanceof Client) {
            return ((Client) directoryObject).getMacAddress().contains(value.toLowerCase());
        }
        return false;
    }

    private Boolean caseInsensitiveContains(String where, String what) {
        return where.toLowerCase().contains(what.toLowerCase());
    }

}
