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

import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.PasswordAuthentication;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener, TextView.OnEditorActionListener {

	final String epostLoginLink = "https://www.cse.yorku.ca/~roumani/ePost/server/ep.cgi/";
	private Button loginButton;
	private Drawable error;
	private EditText username;
	private EditText password;
	private boolean triedCredentials;
	private int toastDuration = Toast.LENGTH_SHORT;
	private String usernameString;
	private String passwordString;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		// views
		username = (EditText) findViewById(R.id.username_text);
		password = (EditText) findViewById(R.id.password_text);
		loginButton = (Button) findViewById(R.id.login_button);

		// drawables
		error = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_error_black_24dp);

		// listeners
		loginButton.setOnClickListener(this);
		password.setOnEditorActionListener(this);
		password.setImeOptions(EditorInfo.IME_ACTION_DONE);
	}

	@Override
	public void onClick(View v) {
//		System.out.println("Button Clicked");
		switch (v.getId()) {
			case R.id.login_button: {
				ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
				Context context = getApplicationContext();
//				System.out.println("Checking Network");
				if (networkInfo != null && networkInfo.isConnected()) {
					if (username.getText().toString().matches("")) {
						// no input for username
						Toast toast = Toast.makeText(getApplicationContext(), "Please enter username.", toastDuration);
						toast.show();
					} else if (password.getText().toString().matches("")) {
						// no input for password
						Toast toast = Toast.makeText(getApplicationContext(), "Please enter password.", toastDuration);
						toast.show();
					} else {
						usernameString = username.getText().toString();
						passwordString = password.getText().toString();
						// change Sign In button text
						loginButton.setText("Signing In...");
						// login to ePost
//						System.out.println("Begin Login Task");
						new LoginTask().execute(epostLoginLink);
					}
				} else {
					// Not connected to network
					Toast toast = Toast.makeText(getApplicationContext(), "No network connection available.", toastDuration);
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
//					System.out.println("Clicking the Login Button");
					loginButton.performClick();
					// handled must return false so keyboard will be closed
				}
				break;
			}
		}
		return handled;
	}

	// asynchronous download of source
	private class LoginTask extends AsyncTask<String, Void, Integer> {
		@Override
		protected Integer doInBackground(String... urls) {

			// variable declaration
			int responseCode = -1;
			HttpsURLConnection urlConnection = null;
			triedCredentials = false;

			// cookie manager
//			System.out.println("Setting Cookie Manager");
			CookieManager cookieManager = new CookieManager();
			cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
			CookieHandler.setDefault(cookieManager);

			// authentication
//			System.out.println("Authenticating");
			Authenticator.setDefault(new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					if (!triedCredentials) {
						triedCredentials = true;
						return new PasswordAuthentication(usernameString, passwordString.toCharArray());
					} else {
						return null;
					}
				}
			});

			try {
				// open connection
				URL epostLink = new URL(urls[0]);
//				System.out.println("Opening connection");
				urlConnection = (HttpsURLConnection) epostLink.openConnection();
				urlConnection.setConnectTimeout(5000);
				urlConnection.setReadTimeout(5000);
				MyHostnameVerifier hv = new MyHostnameVerifier();
				urlConnection.setHostnameVerifier(hv);

				responseCode = urlConnection.getResponseCode();
//				System.out.println("Response Code: " + urlConnection.getResponseCode());
			} catch (Exception e) {
				// Add error logging
				System.out.println(e);
			} finally {
				if (urlConnection != null) {
//					System.out.println("Closing Connection");
					urlConnection.disconnect();
				}
			}
			return responseCode;
		}

		// onPostExecute displays the results of the AsyncTask.
		@Override
		protected void onPostExecute(Integer response) {
			if (response == 200) {
				// upon successful connection, go back
				SavedSharedPreferences.setUser(getApplicationContext(), usernameString, passwordString);

				finish();
			} else if (response == 401) {
				// change Sign In button back
				loginButton.setText("Sign In");
				Toast toast = Toast.makeText(getApplicationContext(), "Username and password do not match.", toastDuration);
				toast.show();
			}
		}
	}
}
