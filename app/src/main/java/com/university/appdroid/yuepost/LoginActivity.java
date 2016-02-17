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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener, TextView.OnEditorActionListener {

	private TextView result;
	final String epostLinkBase = "https://www.cse.yorku.ca/~roumani/ePost/server/ep.cgi/";
	private String epostLink;
	private EditText username;
	private EditText password;
	private Button loginButton;
	private int toastDuration = Toast.LENGTH_SHORT;
	//	private List<String> years;
	private String year;
	private String term;
	private HashMap<String, List<Integer>> terms;
	private List<String> courses;
	private List<String> enrolled;
	private Drawable error;
	private boolean triedCredentials;
	private Calendar calendar;

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
		final Integer[] fallArray = {8, 9, 10, 11};
		final Integer[] winterArray = {0, 1, 2, 3};
		final Integer[] summerArray = {4, 5, 6, 7};
		final List<Integer> fallList = new ArrayList<Integer>(Arrays.asList(fallArray));
		final List<Integer> winterList = new ArrayList<Integer>(Arrays.asList(winterArray));
		final List<Integer> summerList = new ArrayList<Integer>(Arrays.asList(summerArray));
		terms = new HashMap<String, List<Integer>>();
		terms.put("F", fallList);
		terms.put("W", winterList);
		terms.put("S", summerList);
		Calendar calendar = new GregorianCalendar();
		int currentMonth = calendar.get(Calendar.MONTH);
		int currentYear = calendar.get(Calendar.YEAR);
		term = getSemester(currentMonth);
		year = getYear(term, currentYear);
		buildRequest();
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
					System.out.println(epostLink);
					new LoginTask().execute(epostLink);
				} else {
					// Not connected to network
					// CHANGE TO ALERT DIALOG
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
			triedCredentials = false;

			// cookie manager
			System.out.println("Setting Cookie Manager");
			CookieManager cookieManager = new CookieManager();
			cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
			CookieHandler.setDefault(cookieManager);

			// authentication
			System.out.println("Authenticating");
			Authenticator.setDefault(new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					if (!triedCredentials) {
						triedCredentials = true;
						return new PasswordAuthentication(username.getText().toString(), password.getText().toString().toCharArray());
					} else {
						return null;
					}
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
					// upon successful connection
					// SHOULD OPEN A NEW ACTIVITY AND DO DATA READING THERE
					result = getPage(urlConnection);
					getCourses(result);
//					getEnrolledCourses(urlConnection);

					//	Intent intent = new Intent(this, MainActivity.class);
				} else if (urlConnection.getResponseCode() == 401) {
					// bad credentials
					Toast toast = Toast.makeText(getApplicationContext(), "Username and password do not match.", toastDuration);
					toast.show();
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
			return courses.toString();
		}

		// onPostExecute displays the results of the AsyncTask.
		@Override
		protected void onPostExecute(String output) {
			result.setText(output);
		}
	}

	private String getPage(HttpsURLConnection urlConnection) throws IOException {
		System.out.println("Reading Page");
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

	private String getSemester(int month) {
		// get current month and year
		System.out.println("Getting Semester");
		String semester = "";
		Iterator it = terms.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			List list = (List) pair.getValue();
			for (Object m : list) {
				if (m == month) {
					semester = (String) pair.getKey();
				}
			}
		}
		return semester;
	}

	private String getYear(String semester, int year) {
		System.out.println("Getting year");
		String semesterYear = "";
		if (semester == "F") {
			int tempInteger = year;
			tempInteger++;
			String tempString = String.valueOf(tempInteger);
			tempString = tempString.substring(tempString.length() - 2, tempString.length());
			semesterYear = String.valueOf(year + "-" + tempString);
		} else if (semester == "W" || semester == "S") {
			int tempInteger = year;
			year--;
			String tempString = String.valueOf(tempInteger);
			tempString = tempString.substring(tempString.length() - 2, tempString.length());
			semesterYear = String.valueOf(year + "-" + tempString);
		}
		return semesterYear;
	}

	private void buildRequest() {
		System.out.println("Building initial request");
		epostLink = epostLinkBase + "?year=" + year + "&term=" + term;
	}

	private void buildRequest(String course) {
		System.out.println("Building course request");
		epostLink = epostLinkBase + "?year=" + year + "&term=" + term + "&course=" + course;
	}

	private void getCourses(String data) {
		System.out.println("Getting courses");
		courses = new ArrayList<String>();
		Document doc = Jsoup.parse(data);
		Element courseElement = doc.getElementById("course");
		Elements optionTag = courseElement.select("option");
		for (Element innerElement : optionTag) {
			// test if it is an actual course code
			if (innerElement.text().length() > 1) {
				courses.add(innerElement.text());
			}
		}
	}

	private void getEnrolledCourses(HttpsURLConnection urlConnection) throws IOException {
		System.out.println("Getting enrolled courses");
		enrolled = new ArrayList<String>();
		for (String course : courses) {
			buildRequest(course);
			URL url = new URL(epostLink);
			urlConnection = (HttpsURLConnection) url.openConnection();
			String content = getPage(urlConnection);

			Document doc = Jsoup.parse(content);
			Elements tableElements = doc.getElementsByTag("table");
		}
	}
}
