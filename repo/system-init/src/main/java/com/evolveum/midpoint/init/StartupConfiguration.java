/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.init;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.configuration.*;
import org.apache.commons.lang.NotImplementedException;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;

public class StartupConfiguration implements MidpointConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(StartupConfiguration.class);
    private static final String USER_HOME = "user.home";
    private static final String MIDPOINT_HOME = "midpoint.home";

    private CompositeConfiguration config = null;
    private Document xmlConfigAsDocument = null;        // just in case when we need to access original XML document
    private String configFilename = null;

    /**
     * Default constructor
     */
    public StartupConfiguration() {
        this.configFilename = "config.xml";
    }

    /**
     * Constructor
     *
     * @param configFilename alternative configuration file
     */
    public StartupConfiguration(String configFilename) {
        this.configFilename = configFilename;
    }

    /**
     * Get current configuration file name
     *
     * @return
     */
    public String getConfigFilename() {
        return this.configFilename;
    }

    /**
     * Set configuration filename
     *
     * @param configFilename
     */
    public void setConfigFilename(String configFilename) {
        this.configFilename = configFilename;
    }

    @Override
    public String getMidpointHome() {
        return System.getProperty(MIDPOINT_HOME);
    }

    @Override
    public Configuration getConfiguration(String componentName) {
        if (null == componentName) {
            throw new IllegalArgumentException("NULL argument");
        }
        Configuration sub = config.subset(componentName);
        // Insert replacement for relative path to midpoint.home else clean
        // replace
        if (getMidpointHome() != null) {
            sub.addProperty(MIDPOINT_HOME, getMidpointHome());
        } else {
            @SuppressWarnings("unchecked")
            Iterator<String> i = sub.getKeys();
            while (i.hasNext()) {
                String key = i.next();
                sub.setProperty(key, sub.getString(key).replace("${" + MIDPOINT_HOME + "}/", ""));
                sub.setProperty(key, sub.getString(key).replace("${" + MIDPOINT_HOME + "}", ""));
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Configuration for {} :", componentName);
            @SuppressWarnings("unchecked")
            Iterator<String> i = sub.getKeys();
            while (i.hasNext()) {
                String key = i.next();
                LOGGER.debug("    {} = {}", key, sub.getString(key));
            }
        }
        return sub;
    }

    /**
     * Initialize system configuration
     */
    public void init() {
        welcome();    
        if (System.getProperty(MIDPOINT_HOME) == null || System.getProperty(MIDPOINT_HOME).isEmpty()) {
            LOGGER.warn("*****************************************************************************************");
            LOGGER.warn(MIDPOINT_HOME
                    + " is not set ! Using default configuration, for more information see http://wiki.evolveum.com/display/midPoint/");
            LOGGER.warn("*****************************************************************************************");

            System.out.println("*******************************************************************************");
            System.out.println(MIDPOINT_HOME + " is not set ! Using default configuration, for more information");
            System.out.println("                 see http://wiki.evolveum.com/display/midPoint/");
            System.out.println("*******************************************************************************");

			if (getConfigFilename().startsWith("test")) {
				String midpointHome = "./target/midpoint-home";
				System.setProperty(MIDPOINT_HOME, midpointHome);
			} else {

				String userHome = System.getProperty(USER_HOME);
				if (!userHome.endsWith("/")) {
					userHome += "/";
				}
				userHome += "midpoint";
				System.setProperty(MIDPOINT_HOME, userHome);
				LOGGER.warn("Setting {} to '{}'.", new Object[] { MIDPOINT_HOME, userHome });
				System.out.println("Setting " + MIDPOINT_HOME + " to '" + userHome + "'.");
			}
		}

        loadConfiguration();
    }

    /**
     * Load system configuration
     */
    public void load() {
        loadConfiguration();
    }

    /**
     * Save system configuration
     *
     * @TODO not implement yet
     */
    public void save() {
        throw new NotImplementedException();
    }

    /**
     * Loading logic
     */
    private void loadConfiguration() {
        if (config != null) {
            config.clear();
        } else {
            config = new CompositeConfiguration();
        }

        DocumentBuilder documentBuilder = DOMUtil.createDocumentBuilder();          // we need namespace-aware document builder (see GeneralChangeProcessor.java)

        /* configuration logic */
        // load from midpoint.home
        if (null != System.getProperty(MIDPOINT_HOME)) {
            try {
                //Fix missing last slash in path
                if (!System.getProperty(MIDPOINT_HOME).endsWith("/")) {
                    System.setProperty(MIDPOINT_HOME, System.getProperty(MIDPOINT_HOME) + "/");
                }

                //Load configuration
                String path = System.getProperty(MIDPOINT_HOME) + this.getConfigFilename();
                LOGGER.info("Loading midPoint configuration from file {}", path);
                File f = new File(path);
                if (!f.exists()) {
                    LOGGER.warn("Configuration file {} does not exists. Need to do extraction ...", path);

                    ApplicationHomeSetup ah = new ApplicationHomeSetup();
                    ah.init(MIDPOINT_HOME);
                    ClassPathUtil.extractFileFromClassPath(this.getConfigFilename(), path);

                }
                this.setConfigFilename(path);
                //Load and parse properties
                config.addProperty(MIDPOINT_HOME, System.getProperty(MIDPOINT_HOME));
                createXmlConfiguration(documentBuilder);
            } catch (ConfigurationException e) {
                String message = "Unable to read configuration file [" + this.getConfigFilename() + "]: " + e.getMessage();
                LOGGER.error(message);
                System.out.println(message);
                throw new SystemException(message, e);      // there's no point in continuing with midpoint initialization
            }

        } else {
            // Load from class path
            try {
                createXmlConfiguration(documentBuilder);
            } catch (ConfigurationException e) {
                String message = "Unable to read configuration file [" + this.getConfigFilename() + "]: " + e.getMessage();
                LOGGER.error(message);
                System.out.println(message);
                throw new SystemException(message, e);
            }
        }
    }

    private void createXmlConfiguration(DocumentBuilder documentBuilder) throws ConfigurationException {
        XMLConfiguration xmlConfig = new XMLConfiguration();
        xmlConfig.setDocumentBuilder(documentBuilder);
        xmlConfig.setFileName(this.getConfigFilename());
        xmlConfig.load();
        config.addConfiguration(xmlConfig);

        xmlConfigAsDocument = DOMUtil.parseFile(this.getConfigFilename());
    }

    @Override
    public Document getXmlConfigAsDocument() {
        return xmlConfigAsDocument;
    }

    @Override
    public String toString() {
        @SuppressWarnings("unchecked")
        Iterator<String> i = config.getKeys();
        StringBuilder sb = new StringBuilder();
        while (i.hasNext()) {
            String key = i.next();
            sb.append(key);
            sb.append(" = ");
            sb.append(config.getString(key));
            sb.append("; ");
        }
        return sb.toString();
    }

    private void welcome() {
        try {
            Configuration info = new PropertiesConfiguration("midpoint.info");
            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
            LOGGER.info("+--------------------------------------------------------------------------------------------+");
            LOGGER.info("___________                                    __    | ");
            LOGGER.info("\\_   _____/ ____    ____   ___________   ____ |  | __| ");
            LOGGER.info(" |    __)_ /    \\  / ___\\_/ __ \\_  __ \\_/ __ \\|  |/ /| ");
            LOGGER.info(" |        \\   |  \\/ /_/  >  ___/|  | \\/\\  ___/|    < | ");
            LOGGER.info("/_______  /___|  /\\___  / \\___  >__|    \\___  >__|_ \\| powered by Midpoint  from Evolveum.com & Biznet.com.tr supported by Ulakbim TR");
            LOGGER.info("        \\/     \\//_____/      \\/            \\/     \\/|");
            LOGGER.info("|  Licensed under the Apache License, Version 2.0 see: http://www.apache.org/licenses/LICENSE-2.0");
            LOGGER.info("|  Version :  " + info.getString("midpoint.version"));
//			try {
//				LOGGER.info("|  Build   :  " + info.getString("midpoint.build") + " at "
//						+ formatter.format(new Date(info.getLong("midpoint.timestamp"))));
//			} catch (NumberFormatException ex) {
//				LOGGER.info("|  Build   :  " + info.getString("midpoint.build"));
//			}
         //   LOGGER.info("|  Sources :  " + info.getString("midpoint.scm") + "  branch:  " + info.getString("midpoint.branch"));
         //   LOGGER.info("|  Bug reporting system : " + info.getString("midpoint.jira"));
          //  LOGGER.info("|  Product information : http://wiki.evolveum.com/display/midPoint");
            LOGGER.info("+---------------------------------------------------------------------------------------------+");
        } catch (ConfigurationException e) {
            //NOTHING just skip
        }
    }
}
