package org.forgerock.openam.extensions;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.h2.util.StringUtils;
import org.jetbrains.annotations.Nullable;

import com.atlassian.confluence.user.ConfluenceAuthenticator;
import com.atlassian.seraph.auth.AuthenticatorException;
import com.atlassian.seraph.util.RedirectUtils;

/**
 * @author Frank Gasparovic
 */
public class AMConfluenceAuthenticator extends ConfluenceAuthenticator {
    private static final Logger log = Logger.getLogger(AMConfluenceAuthenticator.class);
    AMRequestUtility requestUtility;


    public AMConfluenceAuthenticator() {
        this.requestUtility = AMRequestUtility.getInstance();
    }


    @Override
    public Principal getUserFromBasicAuthentication(HttpServletRequest request, HttpServletResponse response) {
        Principal p = super.getUserFromBasicAuthentication(request, response);
        log.error("ForgeRock Basic Authentication Failure");
        return p;
    }

    @Override
    public Principal getUserFromCookie(HttpServletRequest request, HttpServletResponse response) {
        Principal p = super.getUserFromCookie(request, response);
        log.error("ForgeRock Cookie Authentication Failure");
        return p;
    }

    @Override
    public Principal getUserFromSession(HttpServletRequest request) {
        Principal p = super.getUserFromSession(request);
        log.error("ForgeRock Session Authentication Failure");
        return p;
    }

    @Override
    public boolean authenticate(Principal user, String password) {
        try {
            requestUtility.AMAuthenticate(user.getName());
            return true;
        } catch (Exception ex) {
            log.error("Unable to authenticate user", ex);
            return false;
        }
    }

    @Override
    public Principal getUser(HttpServletRequest request, HttpServletResponse response) {
        return getUser1(request);

    }

    @Nullable
    private Principal getUser1(HttpServletRequest request) {
        Principal user = null;

        try {
            request.getSession(true);
            log.debug("Trying seamless Single Sign-on...");

            String username = requestUtility.obtainUsername(request);
            if (log.isDebugEnabled()) {
                log.debug("Got username = " + username);
            }

            if (username != null) {
                if (request.getSession() != null && request.getSession().getAttribute(LOGGED_IN_KEY) != null &&
                        StringUtils.equals(((Principal) request.getSession().getAttribute(LOGGED_IN_KEY)).getName(),
                                           username)) {
                    log.debug("Session found; user already logged in");
                    user = (Principal) request.getSession().getAttribute(LOGGED_IN_KEY);
                } else {
                    user = getUserAndSetSession(request, username);
                }
            } else {
                String redirectUrl = RedirectUtils.getLoginUrl(request);
                if (log.isDebugEnabled()) {
                    log.debug("Username is null; redirecting to " + redirectUrl);
                }
                return null;
            }
        } catch (Exception ex) {
            log.error("Exception when getting user", ex);
        }
        return user;
    }

    private Principal getUserAndSetSession(HttpServletRequest request, String username) {
        Principal user;
        user = getUser(username);
        if (log.isDebugEnabled()) {
            log.debug("Logged in via SSO, with User " + user);
        }

        request.getSession().setAttribute(LOGGED_IN_KEY, user);
        request.getSession().setAttribute(LOGGED_OUT_KEY, null);
        return user;
    }

    @Override
    public boolean logout(HttpServletRequest request, HttpServletResponse response) {
        boolean result = false;
        try {
            request.getSession().setAttribute(LOGGED_OUT_KEY, request.getSession().getAttribute(LOGGED_IN_KEY));
            request.getSession().removeAttribute(LOGGED_IN_KEY);
            result = doLogout(request, response);
        } catch (Exception ex) {
            log.error("Exception during logout", ex);
        }
        return result;
    }

    private boolean doLogout(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticatorException {
        return super.logout(request, response);
    }
}
