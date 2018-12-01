package green_green_avk.anotherterm.utils;

import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public final class SslHelper {
    private SslHelper() {
    }

    public static final TrustManager[] trustAllCertsMgr;
    public static final SSLContext trustAllCertsCtx;

    static {
        trustAllCertsMgr = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };
        try {
            trustAllCertsCtx = SSLContext.getInstance("SSL");
            trustAllCertsCtx.init(null, trustAllCertsMgr, new java.security.SecureRandom());
        } catch (final GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
