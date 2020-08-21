/*
  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

  Copyright (c) 2010 ForgeRock AS. All Rights Reserved

  The contents of this file are subject to the terms
  of the Common Development and Distribution License
  (the License). You may not use this file except in
  compliance with the License.

  You can obtain a copy of the License at
  http://forgerock.org/license/CDDLv1.0.html
  See the License for the specific language governing
  permission and limitations under the License.

  When distributing Covered Code, include this CDDL
  Header Notice in each file and include the License file
  at http://forgerock.org/license/CDDLv1.0.html
  If applicable, add the following below the CDDL Header,
  with the fields enclosed by brackets [] replaced by
  your own identifying information:

  "Portions Copyrighted [year] [name of copyright owner]"

 */
package org.forgerock.openam.extensions;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.h2.util.StringUtils;

import com.atlassian.jira.security.login.JiraSeraphAuthenticator;
import com.atlassian.seraph.auth.AuthenticatorException;
import com.atlassian.seraph.util.RedirectUtils;

/**
 * @author Frank Gasparovic
 */
public class AMJiraAuthenticator extends JiraSeraphAuthenticator {
    private static final Logger log = Logger.getLogger(AMJiraAuthenticator.class);
    AMRequestUtility requestUtility;

    public AMJiraAuthenticator() {
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
