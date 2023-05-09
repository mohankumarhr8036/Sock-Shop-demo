import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.management.Attribute;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class AEM_ACT_Tool_JMX {
    private static final String JMX_SERVICE_URL = "service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi";
    private static final String AEM_JMX_OBJECT_NAME = "com.adobe.granite:type=Repository";
    private static final String AEM_HEALTH_CHECK_MBEAN_NAME = "com.adobe.granite:type=HealthCheck,name=";

    public static void main(String[] args) throws Exception {
        // Connect to the MBean server using JMX
        JMXServiceURL jmxUrl = new JMXServiceURL(JMX_SERVICE_URL);
        JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxUrl, null);
        MBeanServerConnection mbeanServer = jmxConnector.getMBeanServerConnection();

        // Get the AEM JMX object
        ObjectName aemObjectName = new ObjectName(AEM_JMX_OBJECT_NAME);

        // Check if the AEM instance is running
        boolean isRunning = (boolean) mbeanServer.getAttribute(aemObjectName, "Running");
        System.out.println("AEM instance is running: " + isRunning);

        // Get the health check MBeans
        ObjectName[] healthCheckMBeans = (ObjectName[]) mbeanServer.getAttribute(aemObjectName, "HealthChecks");
        System.out.println("Health check MBeans: " + Arrays.toString(healthCheckMBeans));

        // Run a health check
        String healthCheckName = "<health check name>"; // Replace with your health check name
        ObjectName healthCheckObjectName = new ObjectName(AEM_HEALTH_CHECK_MBEAN_NAME + healthCheckName);
        Map<String, Object> params = new HashMap<>();
        params.put("timeoutInMs", 10000);
        Object[] argsArray = new Object[] { params };
        String[] signature = new String[] { "java.util.Map" };
        Object result = mbeanServer.invoke(healthCheckObjectName, "execute", argsArray, signature);
        System.out.println("Health check result: " + result);
    }
}
