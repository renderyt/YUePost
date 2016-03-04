package com.university.appdroid.yuepost;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

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

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Renatius on 2/16/2016.
 */
public class MainActivity extends AppCompatActivity {

	private String year;
	private String term;
	private HashMap<String, List<Integer>> terms;
	private List<String> courses;
	private Calendar calendar;
	final String epostLinkBase = "https://www.cse.yorku.ca/~roumani/ePost/server/ep.cgi/";
	private String epostLink;
	private boolean rememberMe;
	private RecyclerView recyclerView;
	private RecyclerView.Adapter adapter;
	private RecyclerView.LayoutManager mLayoutManager;
	private List<Course> enrolled;
	private boolean triedCredentials;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		// recycler view
		recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
		mLayoutManager = new LinearLayoutManager(this);
		recyclerView.setLayoutManager(mLayoutManager);
		enrolled = new ArrayList<>();
		adapter = new RecyclerAdapter(enrolled);
		recyclerView.setAdapter(adapter);

		initializeState();

		if (SavedSharedPreferences.getUsername(MainActivity.this).length() == 0) {
			Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
			MainActivity.this.startActivity(intent);
		} else {
			new ExtractTask().execute(epostLink);
		}

		buildRequest();
		//if (empty)
	}

	@Override
	public void onResume() {
		super.onResume();
		if (SavedSharedPreferences.getUsername(MainActivity.this).length() == 0) {
			Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
			MainActivity.this.startActivity(intent);
		} else {
			new ExtractTask().execute(epostLink);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		SavedSharedPreferences.clearUser(getApplicationContext());
	}

	// asynchronous download of source code
	private class ExtractTask extends AsyncTask<String, Void, Integer> {
		@Override
		protected Integer doInBackground(String... urls) {

			// variable declaration
			int responseCode = -1;
			HttpsURLConnection urlConnection = null;
			String content = "";

			// cookie manager
			// System.out.println("Setting Cookie Manager");
			CookieManager cookieManager = new CookieManager();
			cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
			CookieHandler.setDefault(cookieManager);

			// authentication
			// System.out.println("Authenticating");
			Authenticator.setDefault(new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(SavedSharedPreferences.getUsername(getApplicationContext()), SavedSharedPreferences.getPassword(getApplicationContext()).toCharArray());
				}
			});

			try {
				URL epostLink = new URL(urls[0]);
//				System.out.println("Opening connection to " + urls[0]);
				urlConnection = (HttpsURLConnection) epostLink.openConnection();
				urlConnection.setConnectTimeout(5000);
				urlConnection.setReadTimeout(5000);
				MyHostnameVerifier hv = new MyHostnameVerifier();
				urlConnection.setHostnameVerifier(hv);
				responseCode = urlConnection.getResponseCode();
				System.out.println("Response Code: " + urlConnection.getResponseCode());

				content = getPage(urlConnection);
				getCourses(content);
				getEnrolledCourses();

			} catch (Exception e) {
				// Add error logging
				System.out.println(e);
			} finally {
				if (urlConnection != null) {
					System.out.println("Closing Connection");
					urlConnection.disconnect();
				}
			}
			return responseCode;
		}

		// onPostExecute displays the results of the AsyncTask.
		@Override
		protected void onPostExecute(Integer response) {
			if (response == 200) {
				// upon successful connection, populate the listview
			}
		}
	}

	private String getPage(HttpsURLConnection urlConnection) throws IOException {
//		System.out.println("Reading Page");
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
//		System.out.println("Getting Semester");
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
//		System.out.println("Getting year");
		String semesterYear = "";
		if (semester == "Fall") {
			int tempInteger = year;
			tempInteger++;
			String tempString = String.valueOf(tempInteger);
			tempString = tempString.substring(tempString.length() - 2, tempString.length());
			semesterYear = String.valueOf(year + "-" + tempString);
		} else if (semester == "Winter" || semester == "Summer") {
			int tempInteger = year;
			year--;
			String tempString = String.valueOf(tempInteger);
			tempString = tempString.substring(tempString.length() - 2, tempString.length());
			semesterYear = String.valueOf(year + "-" + tempString);
		}
		return semesterYear;
	}

	private void buildRequest() {
//		System.out.println("Building initial request");
		epostLink = epostLinkBase + "?year=" + year + "&term=" + term.charAt(0);
	}

	private String buildRequest(String course) {
//		System.out.println("Building course request");
		return epostLinkBase + "?year=" + year + "&term=" + term.charAt(0) + "&course=" + course;
	}

	private void getCourses(String data) {
//		System.out.println("Getting courses");
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

	private void getEnrolledCourses() throws IOException {
		for (String course : courses) {

			URL url = new URL(buildRequest(course));
			HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
			MyHostnameVerifier hv = new MyHostnameVerifier();
			urlConnection.setHostnameVerifier(hv);
			int responseCode = urlConnection.getResponseCode();
			if (responseCode == 200) {
				String content = getPage(urlConnection);
				Document doc = Jsoup.parse(content);
				Elements comment = doc.select("b > i > font");
				List<String> components = new ArrayList<>();
				if (comment.text().contains("You are not registered in this course!")) {
					// do not add to courses enrolled
				} else if (comment.text().contains("No ePost data exists for this course at this time.")) {
					// add comment to courses enrolled
					components.add("No ePost data exists for this course at this time.");
					enrolled.add(new Course(course, components));
				} else {
					Elements tableElements2 = doc.select("table[cellpadding=3]");
					for (Element element : tableElements2) {
						Elements rows = element.getElementsByTag("tr");
						for (Element row : rows) {
							components.add(row.text());
						}
					}
					enrolled.add(new Course(course, components));
				}
			}
		}
	}

	private void initializeState() {
		rememberMe = false;
		final Integer[] fallArray = {8, 9, 10, 11};
		final Integer[] winterArray = {0, 1, 2, 3};
		final Integer[] summerArray = {4, 5, 6, 7};
		final List<Integer> fallList = new ArrayList<Integer>(Arrays.asList(fallArray));
		final List<Integer> winterList = new ArrayList<Integer>(Arrays.asList(winterArray));
		final List<Integer> summerList = new ArrayList<Integer>(Arrays.asList(summerArray));
		terms = new HashMap<String, List<Integer>>();
		terms.put("Fall", fallList);
		terms.put("Winter", winterList);
		terms.put("Summer", summerList);
		Calendar calendar = new GregorianCalendar();
		int currentMonth = calendar.get(Calendar.MONTH);
		int currentYear = calendar.get(Calendar.YEAR);
		term = getSemester(currentMonth);
		year = getYear(term, currentYear);
//		setTitle("EECS " + term + " " + year);
		setTitle("Current " + term + " Courses");
	}
}
