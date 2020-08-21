# AM-Seraph-Authenticator

The AM Seraph Authenticator enables Single Sign On (SSO) and Single Logout (SLO) with ForgeRock Access Management
 for Atlassian Jira and Confluence.
 
## Usage

To deploy the authenticator, download the latest jar from [here](https://github.com/ForgeRock/AM-Seraph-Authenticator/releases/latest).
After that, download the `AMConfig.properties` file from [here](https://raw.githubusercontent.com/ForgeRock/AM-Seraph-Authenticator/master/AMConfig.properties).
This property file contains configuration that will be used by the authenticator.

Edit the following fields in the `AMConfig.properties` file:
* **amBaseUri** - The Access Management Base URI, an example would be `http://openam.example.com:8080/openam`.
* **amCookieName** - The Access Management Session Cookie Name. By default, the cookie name used by AM is
 `iPlanetDirectoryPro`. If the session cookie name has been changed, the value would be located in the Access Management
  console at Configure > Server Defaults > Security > Cookie > Cookie Name.
* **readTimeout** - Sets the default read timeout for new HTTP connections to AM in milliseconds. A value of 0 means no
 timeout, otherwise values must be between 1 and `Integer.MAX_VALUE`. The default value is 10 seconds.
* **connectTimeout** - Sets the default connect timeout for new HTTP connections to AM in milliseconds. A value of 0 means no
  timeout, otherwise values must be between 1 and `Integer.MAX_VALUE`. The default value is 10 seconds.
* **maxIdleConnections** - Sets the maximum number of idle connections to keep in the connection pool. The default
 value is 5.
* **keepAliveDuration** - Sets the time in milliseconds to keep the connection alive in the connection pool before
 closing it. The default value is 5 minutes.

The next steps will differ depending on whether the authenticator is being deploying for Jira or Confluence. **Note**: Jira and Confluence must be deployed on the same domain as ForgeRock Access Management to achieve SSO and SLO.
 
### Atlassian Jira

1. Copy the downloaded jar to:`<Atlassian Jira Installation Directory>/atlassian-jira/WEB-INF/lib/`
2. Copy the `AMConfig.properties` file to:`<Atlassian Jira Installation Directory>/atlassian-jira/WEB-INF/classes/`
3. Edit the `seraph-config.xml` file located at: `<Atlassian Jira Installation Directory>/atlassian-jira/WEB-INF/classes/seraph-config.xml`
  * Replace the following values where `http://openam.example.com:8080/openam` is the value of your AM base URL:
    ```
    <param-name>login.url</param-name>   
    <param-value>http://openam.example.com:8080/openam?goto=${originalurl}</param-value>
    
    <param-name>link.login.url</param-name>
    <param-value>http://openam.example.com:8080/openam?goto=${originalurl}</param-value>

    <param-name>logout.url</param-name>
    <param-value>http://openam.example.com:8080/openam/XUI/?goto={{URL_Encoded_Value_Of_Jira_URL}}#logout</param-value>
    ```
  * Disable the default authenticator in use by Jira by commenting out the following line :
    ```
    <authenticator class="com.atlassian.jira.security.login.JiraSeraphAuthenticator"/>
    ```
  * Add the AM Seraph Authenticator by adding the following line:
    ```
    <authenticator class="org.forgerock.openam.extensions.AMJiraAuthenticator"/>
    ```
 4\. (Optional) To remove the login gadget shown on the dashboard screen, edit the following files:
  * `<Atlassian Jira Installation Directory>/atlassian-jira/WEB-INF/classes/jpm.xml`
    * Change the `jira.disable.login.gadget` property to be the following:
    ```
        <property>
            <key>jira.disable.login.gadget</key>
            <default-value>true</default-value>
            <type>boolean</type>
            <admin-editable>true</admin-editable>
            <sysadmin-editable>true</sysadmin-editable>
        </property>
    ```
  * Either edit this file or add if it doesn't exist `<Atlassian Jira Installation Directory>/atlassian-jira/WEB-INF/classes/jira-config.properties`
    ```
    jira.disable.login.gadget=true
    ```
 5\. Restart Jira for the changes to take effect.
### Atlassian Confluence

1. Copy the downloaded jar to:`<Atlassian Confluence Installation Directory>/confluence/WEB-INF/lib/`
2. Copy the `AMConfig.properties` file to:`<Atlassian Confluence Installation Directory>/confluence/WEB-INF/classes/`
3. Edit the `seraph-config.xml` file located at: `<Atlassian Confluence Installation Directory>/confluence/WEB-INF/lib/seraph-config.xml`
  * Replace the following values where `http://openam.example.com:8080/openam` is the value of your AM base URL:
    ```
    <param-name>login.url</param-name>   
    <param-value>http://openam.example.com:8080/openam?goto=${originalurl}</param-value>
    
    <param-name>link.login.url</param-name>
    <param-value>http://openam.example.com:8080/openam?goto=${originalurl}</param-value>
    ```
  * Disable the default authenticator in use by Confluence by commenting out the following line:
    ```
    <authenticator class="org.forgerock.openam.extensions.ConfluenceAuthenticator"/>
    ```
  * Add the AM Seraph Authenticator by adding the following line:
    ```
    <authenticator class="org.forgerock.openam.extensions.AMConfluenceAuthenticator"/>
    ```
 4\. To achieve Single Log Out in Confluence, follow the steps outlined [here](https://confluence.atlassian.com/confkb/changing-the-destination-of-the-logout-link-225119623.html):
  * Set the `class="com.atlassian.confluence.user.actions.LogoutAction"` action to the following:
    ```
    <action name="logout" class="com.atlassian.confluence.user.actions.LogoutAction">
        <interceptor-ref name="defaultStack"/>
        <result name="error" type="velocity">/logout.vm</result>
        <result name="success" type="redirect">http://openam.example.com:8080/openam/XUI/?goto={{URL_Encoded_Value_Of_Post_Logout_URL}}#logout</result>
    </action>
    ```
 5\. Restart Jira for the changes to take effect.
   
