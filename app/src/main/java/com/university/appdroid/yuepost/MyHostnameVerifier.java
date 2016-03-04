package com.university.appdroid.yuepost;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

/**
 * Created by Renatius on 2/18/2016.
 */
public class MyHostnameVerifier implements javax.net.ssl.HostnameVerifier {
	@Override
	public boolean verify(String hostname, SSLSession session) {
		javax.net.ssl.HostnameVerifier hv =
				HttpsURLConnection.getDefaultHostnameVerifier();
		return hv.verify("www.eecs.yorku.ca", session) || hv.verify("www.cse.yorku.ca", session);
	}
}
