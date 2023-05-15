package example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class Hello implements RequestHandler<Object, Object>{

    public Object handleRequest(Object input, Context context) {
        System.out.println("Welcome to lambda function");
        return null;
    }
}



//actool class


package com.example.jmx;

import com.adobe.granite.jmx.annotation.Description;
import com.adobe.granite.jmx.annotation.Name;

@Description("Ace Service MBean")
public interface AceServiceMBean {
    
    @Description("Invoke an API request over SSL")
    public String invokeApiRequest(@Name("url") String url) throws Exception;

}

package com.example.jmx;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class AceServiceMBeanImpl implements AceServiceMBean {

    @Override
    public String invokeApiRequest(String url) throws Exception {
        StringBuilder response = new StringBuilder();
        try {
            URL apiUrl = new URL(url);
            HttpsURLConnection conn = (HttpsURLConnection) apiUrl.openConnection();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }}, new SecureRandom());
            conn.setSSLSocketFactory(sslContext.getSocketFactory());
            conn.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        } catch (SSLHandshakeException e) {
            // Handle SSLHandshakeException
            return "Error: SSL Handshake failed. " + e.getMessage();
        } catch (IOException e) {
            // Handle IOException
            return "Error: Unable to connect to the API endpoint. " + e.getMessage();
        }
        return response.toString();
    }
}

