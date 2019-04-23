package org.openthinclient.flow;


import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import org.apache.commons.lang3.StringUtils;
import org.openthinclient.flow.rememberme.AuthService;
import org.openthinclient.i18n.LocaleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static org.openthinclient.flow.i18n.ConsoleWebMessages.*;

@Tag("sa-login-view")
@Route(value = LoginView.VIEW_NAME)
@RoutePrefix(value = "ui")
@PageTitle("Login")
public class LoginView extends FlexLayout implements BeforeEnterObserver {

    public static final String VIEW_NAME = "login";
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginView.class);

    private Label loginFailed;
    private TextField username;
    private PasswordField password;
//    private final Checkbox rememberMe;

    /**
     * AuthenticationManager is already exposed in WebSecurityConfig
     */
    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private HttpServletRequest req;

    private IMessageConveyor mc;
    private Checkbox rememberMe;

    @PostConstruct
    protected void init() {

//        setLocale(LocaleUtil.getLocaleForMessages(ConsoleWebMessages.class, UI.getCurrent().getLocale()));

        mc = new MessageConveyor(UI.getCurrent().getLocale());

        Component loginForm = buildLoginForm();
//        VerticalLayout rootLayout = new VerticalLayout(loginForm);
//        rootLayout.setSizeFull();
//        rootLayout.setComponentAlignment(loginForm, Alignment.MIDDLE_CENTER);
//        add(rootLayout);
//        setSizeFull();

        add(loginForm);

        // center the form
        setAlignItems(Alignment.CENTER);
        this.getElement().getStyle().set("height", "100%");
        this.getElement().getStyle().set("justify-content", "center");
    }

    private Component buildLoginForm() {
        final VerticalLayout loginPanel = new VerticalLayout();
        loginPanel.setSizeUndefined();
        loginPanel.addClassName("login-panel");
        loginPanel.add(buildLabels());

        loginFailed = new Label();
        loginFailed.addClassName("login-failed");
        loginFailed.setVisible(false);
        loginPanel.add(loginFailed);

        HorizontalLayout fields = new HorizontalLayout();
        fields.setSpacing(true);
        fields.addClassName("fields");

        username = new TextField(mc.getMessage(UI_LOGIN_USERNAME));
//        username.setIcon(FontAwesome.USER);
//        username.addClassName(ValoTheme.TEXTFIELD_INLINE_ICON);

        password = new PasswordField(mc.getMessage(UI_LOGIN_PASSWORD));
//        password.setIcon(FontAwesome.LOCK);
//        password.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);

        final Button signin = new Button(mc.getMessage(UI_LOGIN_LOGIN));
//        signin.addStyleName(ValoTheme.BUTTON_PRIMARY);
//        signin.setClickShortcut(KeyCode.ENTER);
        signin.focus();
        signin.addClickListener(event -> authenticateAndNavigate());

        fields.add(loginFailed, username, password, signin);
//        fields.setAlignItems(signin, Alignment.BOTTOM_LEFT);
        loginPanel.add(fields);

//        loginPanel.add(rememberMe = new CheckBox(mc.getMessage(UI_LOGIN_REMEMBERME), false));

        return loginPanel;
    }

    private Component buildLabels() {
        Div labels = new Div();
        labels.addClassName("labels");

        Label welcome = new Label(mc.getMessage(UI_LOGIN_WELCOME));
        welcome.setSizeUndefined();
//        welcome.addStyleName(ValoTheme.LABEL_H4);
//        welcome.addStyleName(ValoTheme.LABEL_COLORED);
        labels.add(welcome);

        Label title = new Label("openthinclient.org");
        title.setSizeUndefined();
//        title.addStyleName(ValoTheme.LABEL_H3);
//        title.addStyleName(ValoTheme.LABEL_LIGHT);
        labels.add(title);
        return labels;
    }

    private void authenticateAndNavigate() {
        /*
        Set an authenticated user in Spring Security and Spring MVC
        spring-security
        */
//        UsernamePasswordAuthenticationToken authReq = new UsernamePasswordAuthenticationToken(userNameTextField.getValue(), passwordField.getValue());

//        try {
//            // Set authentication
//            Authentication auth = authManager.authenticate(authReq);
//            SecurityContext sc = SecurityContextHolder.getContext();
//            sc.setAuthentication(auth);
//
//            /*
//            Navigate to the requested page:
//            This is to redirect a user back to the originally requested URL – after they log in as we are not using
//            Spring's AuthenticationSuccessHandler.
//            */
//            HttpSession session = req.getSession(false);
//            DefaultSavedRequest savedRequest = (DefaultSavedRequest) session.getAttribute("SPRING_SECURITY_SAVED_REQUEST");
//            String requestedURI = savedRequest != null ? savedRequest.getRequestURI() : "/";
//
//            this.getUI().ifPresent(sampleviews -> sampleviews.navigate(StringUtils.removeStart(requestedURI, "/")));
//        } catch (BadCredentialsException e) {
//            label.setText("Invalid username or password. Please try again.");
//        }

        if (AuthService.login(authManager, username.getValue(), password.getValue(), false /*, rememberMe.getValue()*/)) {
            HttpSession session = req.getSession(false);
            DefaultSavedRequest savedRequest = (DefaultSavedRequest) session.getAttribute("SPRING_SECURITY_SAVED_REQUEST");
            String requestedURI = savedRequest != null ? savedRequest.getRequestURI() : "/ui/";
            this.getUI().ifPresent(ui -> ui.navigate(StringUtils.removeStart(requestedURI, "/")));
        } else {
            loginFailed.setText("Invalid username or password. Please try again.");
        }
    }

    /**
    * This is to redirect user to the main URL context if (s)he has already logged in and tries to open /login
    *
    * @param beforeEnterEvent
    */
    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Anonymous Authentication is enabled in our Spring Security conf
        if (AuthService.isAuthenticated()) {
            //https://vaadin.com/docs/flow/routing/tutorial-routing-lifecycle.html
            beforeEnterEvent.rerouteTo("");
        }
    }

}