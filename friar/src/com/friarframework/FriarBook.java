package com.friarframework;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class FriarBook extends Activity {
	// Requires trailing slash!
	final String BASE_URL = "file:///sdcard/sample/book/"; 
	final String MIME_TYPE = "text/html";
	final String ENCODING = "utf-8";

	WebView webView;
	List<String> htmlFiles = new ArrayList<String>();
	HashMap<String, Integer> htmlMap = new HashMap<String, Integer>();
	int currentPage = 0;
	int totalPages = 0;

	GestureDetector gestureDetector;
	SimpleOnGestureListener gestureListener = new SimpleOnGestureListener() {
		private final int SWIPE_MIN_DISTANCE = 100;
		private final int SWIPE_MAX_DISTANCE = 350;
		private final int SWIPE_MIN_VELOCITY = 100;

		@Override
		public boolean onDown(MotionEvent event) 
		{
			return false;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			final float xDistance = Math.abs(e1.getX() - e2.getX());
			final float yDistance = Math.abs(e1.getY() - e2.getY());

			if (xDistance > this.SWIPE_MAX_DISTANCE || yDistance > this.SWIPE_MAX_DISTANCE) {
				return false;
			}

			velocityX = Math.abs(velocityX);
			velocityY = Math.abs(velocityY);

			if (velocityX > this.SWIPE_MIN_VELOCITY && xDistance > this.SWIPE_MIN_DISTANCE) {
				if (e1.getX() > e2.getX()) { // right to left
					if (currentPage + 1 >= totalPages) {
						showToast("This is the last page of the book.");
					} else {
						showUrl(++currentPage);
						return true;
					}
				} else {
					if (currentPage - 1 < 0) {
						showToast("This is the first page of the book.");
					} else {
						showUrl(--currentPage);
						return true;
					}
				}
			}

			return false;
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
try
{
	htmlFiles = loadBook();
}
catch(IOException e)
{
	e.printStackTrace();
}
		
		totalPages = htmlFiles.size();

		int count = 0;
		for (String filename : htmlFiles) {
			htmlMap.put(filename, count++);
		}
		
		webView = (WebView) findViewById(R.id.webview);
		webView.setWebViewClient(new FriarWebViewClient());
		webView.getSettings().setJavaScriptEnabled(true);

		gestureDetector = new GestureDetector(gestureListener);
		webView.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View wv, MotionEvent event) {
				gestureDetector.onTouchEvent(event);
				return false;
			}
		});

		if (savedInstanceState != null && savedInstanceState.get("currentPage") != null) {
			currentPage = savedInstanceState.getInt("currentPage");
		}
		webView.loadUrl(BASE_URL + htmlFiles.get(currentPage));
		System.out.println(BASE_URL + htmlFiles.get(currentPage));
	}

	// Handle Android physical back button.
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
			webView.goBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putInt("currentPage", currentPage);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		currentPage = savedInstanceState.getInt("currentPage");
	}

	private List<String> loadBook() throws IOException
	{
		File dir= Environment.getExternalStorageDirectory();
		File YourFile= new File(dir,"sample/book/book.json");
		FileInputStream stream = new FileInputStream(YourFile);
		String jstring;
		try {
			FileChannel fc= stream.getChannel();
			MappedByteBuffer bb=fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
			jstring=Charset.defaultCharset().decode(bb).toString();
			try
			{
				String json=convertStreamToString(stream);
				JSONObject jsonobject=(JSONObject)new JSONTokener(json).nextValue();
				JSONArray content=jsonobject.getJSONArray("contents");
				for(int index=0;index<content.length();index++)
				{
					htmlFiles.add(content.getString(index));
				}
			}
			catch (JSONException e) 
			{
				// TODO: handle exception
				e.printStackTrace();
			}
		}
		catch (FileNotFoundException e1) 
		{
			// TODO: handle exception
			e1.printStackTrace();
		}
		finally
		{
			stream.close();
		}
		return htmlFiles;
	}

	private String convertStreamToString(FileInputStream is) {
		return new Scanner(is).useDelimiter("\\A").next();
	}

	private void showUrl(int pageNum) {
		assert pageNum >= 0 && pageNum < totalPages;

		String filename = htmlFiles.get(pageNum);
		String url = BASE_URL + filename;
		webView.loadUrl(url);
		showToast(currentPage + "");
	}

	private void showToast(final String text) {
		Toast t = Toast.makeText(getBaseContext(), text, Toast.LENGTH_SHORT);
		t.show();
		System.out.println(text);
	}

	class FriarWebViewClient extends WebViewClient {
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			try {
				URI uri = new URI(url);
				String[] segments = uri.getPath().split("/");
				String filename = segments[segments.length - 1];
				currentPage = htmlMap.get(filename);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			System.out.println(currentPage + " " + url);
		}

        @Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
	        if (url != null && url.startsWith("http://")) {
	            view.getContext().startActivity(
	                new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
	            return true;
	        } else {
	            return false;
	        }
	    }
	}
}
