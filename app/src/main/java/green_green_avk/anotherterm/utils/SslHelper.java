package green_green_avk.anotherterm.utils;

import android.annotation.SuppressLint;

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

                    @SuppressLint("TrustAllX509TrustManager")
                    public void checkClientTrusted(final java.security.cert.X509Certificate[] certs,
                                                   final String authType) {
                    }

                    @SuppressLint("TrustAllX509TrustManager")
                    public void checkServerTrusted(final java.security.cert.X509Certificate[] certs,
                                                   final String authType) {
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
