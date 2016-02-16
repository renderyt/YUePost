package com.university.appdroid.yuepost;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener, TextView.OnEditorActionListener {

	private TextView result;
	final String epostLinkString = "https://www.cse.yorku.ca/~roumani/ePost/server/ep.cgi/";
	private EditText username;
	private EditText password;
	private Button loginButton;
	private int toastDuration = Toast.LENGTH_SHORT;
	private List<String> terms;
	private List<String> years;
	private List<String> courses;
	private Drawable error;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		// views
		result = (TextView) findViewById(R.id.result);
		username = (EditText) findViewById(R.id.username_text);
		password = (EditText) findViewById(R.id.password_text);
		loginButton = (Button) findViewById(R.id.login_button);

		// drawables
		error = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_error_black_24dp);

		// view modifications

		// listeners
		loginButton.setOnClickListener(this);
		password.setOnEditorActionListener(this);
		password.setImeOptions(EditorInfo.IME_ACTION_DONE);

		// variables
		String[] termsArray = {"F", "W", "S"};
		terms = new ArrayList<String>(Arrays.asList(termsArray));
	}

	@Override
	public void onClick(View v) {
		System.out.println("Button Clicked");
		switch (v.getId()) {
			case R.id.login_button: {
				ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
				Context context = getApplicationContext();
				System.out.println("Checking Network");
				if (networkInfo != null && networkInfo.isConnected()) {
					// login to ePost
					System.out.println("Begin Login Task");
					new LoginTask().execute(epostLinkString);
				} else {
					// Not connected to network
					// TO DO - MOVE VARIABLES FOR CONVENIENCE //
					Toast toast = Toast.makeText(context, "No network connection available", toastDuration);
					toast.show();
				}
			}
		}
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		boolean handled = false;
		switch (v.getId()) {
			case R.id.password_text: {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					System.out.println("Clicking the Login Button");
					loginButton.performClick();
					// handled must return false so keyboard will be closed
				}
				break;
			}
		}
		return handled;
	}

	HostnameVerifier hostnameVerifier = new HostnameVerifier() {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			HostnameVerifier hv =
					HttpsURLConnection.getDefaultHostnameVerifier();
			return hv.verify("www.eecs.yorku.ca", session);
		}
	};

	// asynchronous download of source code
	private class LoginTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {

			String result = "";
			HttpsURLConnection urlConnection = null;

			// cookie manager
			System.out.println("Setting Cookie Manager");
			CookieManager cookieManager = new CookieManager();
			cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
			CookieHandler.setDefault(cookieManager);

			// authentication
			System.out.println("Authenticating");
			Authenticator.setDefault(new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username.getText().toString(), password.getText().toString().toCharArray());
				}
			});

			try {
				URL epostLink = new URL(urls[0]);
				System.out.println("Opening connection");
				urlConnection = (HttpsURLConnection) epostLink.openConnection();
//				urlConnection.setUseCaches(false);
				urlConnection.setConnectTimeout(5000);
				urlConnection.setReadTimeout(5000);
				urlConnection.setHostnameVerifier(hostnameVerifier);

				urlConnection.getResponseCode();
				System.out.println("Response Code: " + urlConnection.getResponseCode());
				if (urlConnection.getResponseCode() == 200) {
					System.out.println("Extracting HTML");
					result = readPage(urlConnection);
					extractYears(result);

				} else if (urlConnection.getResponseCode() == 401) {
					result = "401";
				}
			} catch (Exception e) {
				// Add error logging
				System.out.println(e);
			} finally {
				if (urlConnection != null) {
					System.out.println("Closing Connection");
					urlConnection.disconnect();
				}
			}
			return result;
		}

		// onPostExecute displays the results of the AsyncTask.
		@Override
		protected void onPostExecute(String output) {
			result.setText(output);
		}
	}

	private String readPage(HttpsURLConnection urlConnection) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(
				urlConnection.getInputStream()));
		String inputLine;
		StringBuffer content = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			content.append(inputLine);
		}
		in.close();
		return content.toString();
	}

	private void extractYears(String data) {
		// reading years
		Document doc = Jsoup.parse(data);
		Elements yearTag = doc.getElementsByTag("select");
		years = new ArrayList<String>();
		for (Element element : yearTag) {
			String name = element.attr("name");
			if (name.equals("year")) {
				System.out.println("Found select tag with year");
				Elements optionTag = element.select("option");
				for (Element innerElement : optionTag) {
					years.add(innerElement.text());
				}
			}
		}
	}
}
