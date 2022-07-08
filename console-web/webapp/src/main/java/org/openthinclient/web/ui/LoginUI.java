package org.openthinclient.web.ui;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

import java.util.function.Consumer;

import com.vaadin.annotations.Theme;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import org.openthinclient.i18n.LocaleUtil;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.CommunicationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.vaadin.spring.security.shared.VaadinSharedSecurity;


@SpringUI(path = "/login")
@Theme("openthinclient")
public class LoginUI extends UI {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginUI.class);

    @Autowired
    VaadinSharedSecurity vaadinSecurity;

    private IMessageConveyor mc;

    @Override
    protected void init(VaadinRequest request) {

        setLocale(LocaleUtil.getLocaleForMessages(ConsoleWebMessages.class, UI.getCurrent().getLocale()));

        mc = new MessageConveyor(UI.getCurrent().getLocale());

        Component loginForm = buildLoginForm();
        VerticalLayout rootLayout = new VerticalLayout(loginForm);
        rootLayout.setSizeFull();
        rootLayout.setComponentAlignment(loginForm, Alignment.MIDDLE_CENTER);
        setContent(rootLayout);
        setSizeFull();
    }

    private Component buildLoginForm() {
        final CssLayout loginPanel = new CssLayout();
        loginPanel.addStyleName("login-panel");

        Label title = new Label("openthinclient");
        title.addStyleName("title");

        Label loginFailed = new Label();
        loginFailed.addStyleName("login-failed");

        TextField usernameInput = new TextField(mc.getMessage(ConsoleWebMessages.UI_LOGIN_USERNAME));
        CssLayout username = new CssLayout(usernameInput);
        username.addStyleName("username");

        PasswordField passwordInput = new PasswordField(mc.getMessage(ConsoleWebMessages.UI_LOGIN_PASSWORD));
        CssLayout password = new CssLayout(passwordInput);
        password.addStyleName("password");

        final Button signin = new Button(mc.getMessage(ConsoleWebMessages.UI_LOGIN_LOGIN));
        signin.setClickShortcut(KeyCode.ENTER);

        CheckBox rememberMe = new CheckBox(mc.getMessage(ConsoleWebMessages.UI_LOGIN_REMEMBERME));

        loginPanel.addComponents(
            title,
            username,
            password,
            signin,
            rememberMe,
            loginFailed
        );

        usernameInput.addValueChangeListener(ev -> loginFailed.setValue(""));
        passwordInput.addValueChangeListener(ev -> loginFailed.setValue(""));
        signin.addClickListener( ev -> doLogin( usernameInput.getValue(), passwordInput.getValue(),
                                                rememberMe.getValue(),
                                                msg -> loginFailed.setValue(mc.getMessage(msg)) ) );

        signin.focus();

        return loginPanel;
    }

    private void doLogin(String username, String password, Boolean rememberMe, Consumer<ConsoleWebMessages> setError) {
        try {
            final Authentication authentication = vaadinSecurity.login(username, password, rememberMe);
            LOGGER.debug("Received UserLoginRequestedEvent for ", authentication.getPrincipal());
        } catch (AuthenticationException | AccessDeniedException ex) {
            if (ex.getCause() instanceof CommunicationException) {
                setError.accept(ConsoleWebMessages.UI_DASHBOARDUI_LOGIN_COMMUNICATION_EXCEPTION);
            } else {
                setError.accept(ConsoleWebMessages.UI_DASHBOARDUI_LOGIN_FAILED);
            }
        } catch (Exception ex) {
            setError.accept(ConsoleWebMessages.UI_DASHBOARDUI_LOGIN_UNEXPECTED_ERROR);
            LOGGER.error("Unexpected error while logging in", ex);
        }
    }
}
