package jrds;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import jrds.factories.xml.EntityResolver;
import jrds.factories.xml.JrdsDocument;
import jrds.webapp.Configuration;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.mortbay.jetty.testing.ServletTester;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

final public class Tools {
    public static DocumentBuilder dbuilder = null;
    public static XPath xpather = null;

    static final Appender app = new WriterAppender() {
        public void doAppend(LoggingEvent event) {
            System.out.println(event.getLevel() + ": " + event.getMessage());
        }
    };

    static public void configure() throws IOException {
        Locale.setDefault(new Locale("POSIX"));
        System.getProperties().setProperty("java.awt.headless","true");
        System.setProperty("java.io.tmpdir",  "tmp");
        LogManager.getLoggerRepository().resetConfiguration();
        jrds.JrdsLoggerConfiguration.initLog4J();
        //app.setName(jrds.JrdsLoggerConfiguration.APPENDER);
        //app.setLayout(new PatternLayout("[%d] %5p %c : %m%n"));
        //jrds.JrdsLoggerConfiguration.putAppender(app);
    }

    static public void prepareXml() throws ParserConfigurationException {
        DocumentBuilderFactory instance = DocumentBuilderFactory.newInstance();
        instance.setIgnoringComments(true);
        instance.setValidating(true);
        instance.setExpandEntityReferences(false);
        dbuilder = instance.newDocumentBuilder();
        dbuilder.setEntityResolver(new EntityResolver());
        dbuilder.setErrorHandler(new ErrorHandler() {
            public void error(SAXParseException exception) throws SAXException {
                throw exception;
            }
            public void fatalError(SAXParseException exception) throws SAXException {
                throw exception;
            }
            public void warning(SAXParseException exception) throws SAXException {
                throw exception;
            }
        });
        xpather = XPathFactory.newInstance().newXPath();
    }

    static public void prepareXml(boolean validating) throws ParserConfigurationException {
        DocumentBuilderFactory instance = DocumentBuilderFactory.newInstance();
        instance.setIgnoringComments(true);
        instance.setValidating(validating);
        instance.setExpandEntityReferences(false);
        dbuilder = instance.newDocumentBuilder();
        dbuilder.setEntityResolver(new EntityResolver());
        if(validating)
            dbuilder.setErrorHandler(new ErrorHandler() {
                public void error(SAXParseException exception) throws SAXException {
                    throw exception;
                }
                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }
                public void warning(SAXParseException exception) throws SAXException {
                    throw exception;
                }
            });
        xpather = XPathFactory.newInstance().newXPath();
    }

    static public JrdsDocument parseRessource(String name) throws Exception {
        InputStream is = Tools.class.getResourceAsStream("/ressources/" + name);
        return parseRessource(is);
    }

    static public JrdsDocument parseRessource(InputStream is) throws Exception {
        return new JrdsDocument(Tools.dbuilder.parse(is));
    }

    static public JrdsDocument parseString(String s) throws Exception { 
        InputStream is = new ByteArrayInputStream(s.getBytes());
        JrdsDocument d = Tools.parseRessource(is);
        return d;
    }

    static public void setLevel(Logger logger, Level level, String... allLoggers) {
        Appender app = Logger.getLogger("jrds").getAppender(JrdsLoggerConfiguration.APPENDERNAME);
        //The system property override the code log level
        if(System.getProperty("jrds.testloglevel") != null){
            level = Level.toLevel(System.getProperty("jrds.testloglevel"));
        }
        logger.setLevel(level);
        for(String loggerName: allLoggers) {
            Logger l = Logger.getLogger(loggerName);
            l.setLevel(level);
            if(l.getAppender(JrdsLoggerConfiguration.APPENDERNAME) != null) {
                l.addAppender(app);
            }
        }
    }

    static public void setLevel(Level level, String... allLoggers) {
        setLevel(allLoggers, level);
    }

    static public void setLevel(String[] allLoggers, Level level) {
        Appender app = Logger.getLogger("jrds").getAppender(JrdsLoggerConfiguration.APPENDERNAME);
        for(String loggerName: allLoggers) {
            Logger l = Logger.getLogger(loggerName);
            l.setLevel(level);
            if(l.getAppender(JrdsLoggerConfiguration.APPENDERNAME) != null) {
                l.addAppender(app);
            }
        }
    }

    static public Element appendElement(Node n, String name, Map<String, String> attributes) {
        Document d = n.getOwnerDocument();
        Element e = d.createElement(name);
        if(attributes != null)
            for(Map.Entry<String, String> a: attributes.entrySet()) {
                e.setAttribute(a.getKey(), a.getValue());
            }
        n.appendChild(e);
        return e;
    }

    static public Node appendString(Node n, String xmlString) throws Exception {
        Document d = parseString(xmlString);

        Element docElem = d.getDocumentElement();
        Node newNode = n.getOwnerDocument().importNode(docElem, true);
        n.appendChild(newNode);

        return newNode;
    }

    static public void getServer(Map<String, String> properties) {
        ServletTester tester=new ServletTester();
        tester.setContextPath("/");
        ServletContext sc =  tester.getContext().getServletContext();

        for(Map.Entry<String, String> e: properties.entrySet()) {
            System.setProperty("jrds." + e.getKey(), e.getValue());
        }

        Configuration c = new Configuration(sc);
        sc.setAttribute(Configuration.class.getName(), c);

        Properties sp = System.getProperties();
        for(Object  key: sp.keySet()) {
            if(key.toString().startsWith("jrds.")) {
                sp.remove(key);
            }
        }
    }

    static public URI pathToUrl(String pathname) {
        File path = new File(pathname);
        return path.toURI();
    }

    static public List<LoggingEvent> getLockChecker(String... loggers) {
        final List<LoggingEvent> logs = new ArrayList<LoggingEvent>();
        Appender ta = new AppenderSkeleton() {
            @Override
            protected void append(LoggingEvent arg0) {
                logs.add(arg0);
            }
            public void close() {
                logs.clear();
            }
            public boolean requiresLayout() {
                return false;
            }
        };

        for(String loggername: loggers) {
            Logger logger = Logger.getLogger(loggername);
            logger.addAppender(ta);
        }
        return logs;
    }

    static private final String[] dirs = new String[] {"configdir", "rrddir", "tmpdir"};
    static private final Random r = new Random();
    static public final PropertiesManager getCleanPM() {
        File newtmpdir = new File(System.getProperty("java.io.tmpdir"), "jrds" + r.nextInt());;
        PropertiesManager pm = new PropertiesManager();
        Map<String, File> dirMap = new HashMap<String, File>(dirs.length);
        for(String dirname: dirs) {
            File dir = new File(newtmpdir, dirname);;
            pm.setProperty(dirname, dir.getPath());
            dirMap.put(dirname, dir);
        }
        pm.setProperty("autocreate", "true");
        return pm;
    }

    static public final void shortPM(PropertiesManager pm) {
        pm.setProperty("configdir", "tmp/config");
        pm.setProperty("rrddir", "tmp");
        pm.setProperty("strictparsing", "true");
        pm.setProperty("autocreate", "true");
        pm.update();
        pm.libspath.clear();
    }
    
    static public final PropertiesManager getEmptyProperties() {
        PropertiesManager pm = new PropertiesManager();
        pm.update();
        pm.configdir = null;
        pm.strictparsing = true;
        pm.loglevel = Level.ERROR;
        pm.extensionClassLoader = PropertiesManager.class.getClassLoader();
        pm.libspath.clear();
        return pm;
    }
}
