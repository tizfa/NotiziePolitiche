/*
 * Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package it.tizianofagni.notiziepolitiche.activity;

import it.tizianofagni.notiziepolitiche.R;
import it.tizianofagni.notiziepolitiche.AppConstants;
import it.tizianofagni.notiziepolitiche.NotiziePoliticheApplication;
import it.tizianofagni.notiziepolitiche.utils.GenericUtils;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

public class BrowseArticleActivity extends Activity {

	private WebView webView;
	
	private static final class CustomWebClient extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {

			view.loadUrl(url);

			return true;
		}
	}

	private static final class NextClickListener implements View.OnClickListener {

		private WeakReference<BrowseArticleActivity> act;

		public NextClickListener(BrowseArticleActivity act) {
			this.act = new WeakReference<BrowseArticleActivity>(act);
		}

		@Override
		public void onClick(View v) {
			BrowseArticleActivity activity = act.get();
			if (activity == null)
				return;

			WebView web = activity.getWebView();
			if (web == null)
				return;
			web.goForward();
		}
	}
	
	private static final class ShareClickListener implements View.OnClickListener {

		private WeakReference<BrowseArticleActivity> act;

		public ShareClickListener(BrowseArticleActivity act) {
			this.act = new WeakReference<BrowseArticleActivity>(act);
		}

		@Override
		public void onClick(View v) {
			BrowseArticleActivity activity = act.get();
			if (activity == null)
				return;
			
			NotiziePoliticheApplication app = (NotiziePoliticheApplication) activity.getApplication();
			if (app == null)
				return;
			
			if (app.isLiteVersion()) {
				GenericUtils.showDlgForProVersion(activity);
				return;
			}
			
			WebView web = activity.getWebView();
			if (web == null)
				return;
			
			Intent i=new Intent(android.content.Intent.ACTION_SEND);
			i.setType("text/plain");
			i.putExtra(Intent.EXTRA_SUBJECT, activity.getResources().getString(R.string.share_subject));
			i.putExtra(Intent.EXTRA_TEXT, web.getUrl().toString());
			activity.startActivity(Intent.createChooser(i, activity.getResources().getString(R.string.share_title)));
		}
	}
	

	private static final class PreviousClickListener implements View.OnClickListener {

		private WeakReference<BrowseArticleActivity> act;

		public PreviousClickListener(BrowseArticleActivity act) {
			this.act = new WeakReference<BrowseArticleActivity>(act);
		}

		@Override
		public void onClick(View v) {
			BrowseArticleActivity activity = act.get();
			if (activity == null)
				return;

			WebView web = activity.getWebView();
			if (web == null)
				return;
			web.goBack();
		}
	}

	private static final class ExtBrowserClickListener implements
			View.OnClickListener {

		private WeakReference<BrowseArticleActivity> act;

		public ExtBrowserClickListener(BrowseArticleActivity act) {
			this.act = new WeakReference<BrowseArticleActivity>(act);
		}

		@Override
		public void onClick(View v) {
			BrowseArticleActivity activity = act.get();
			if (activity == null)
				return;

			WebView web = activity.getWebView();

			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(web
					.getUrl()));
			activity.startActivity(intent);
		}
	}

	private static final class CustomWebChromeClient extends WebChromeClient {

		private WeakReference<BrowseArticleActivity> act;

		public CustomWebChromeClient(BrowseArticleActivity act) {
			this.act = new WeakReference<BrowseArticleActivity>(act);
		}

		public void onProgressChanged(WebView view, int progress) {
			BrowseArticleActivity activity = act.get();
			if (activity == null)
				return;

			// Make the bar disappear after URL is loaded, and changes
			// string to Loading...
			activity.setTitle(activity.getResources().getString(
					R.string.browse_loading_document));
			activity.setProgress(progress * 100); // Make the bar
													// disappear after URL
													// is loaded

			WebView web = activity.getWebView();

			// Return the app name after finish loading
			if (progress == 100) {
				
				NotiziePoliticheApplication app =(NotiziePoliticheApplication) activity.getApplication();
				if (app.isLiteVersion()) {
					activity.setTitle(activity.getResources().getString(R.string.app_name_lite)
							+ " - " + web.getTitle());
				} else {
					activity.setTitle(activity.getResources().getString(R.string.app_name_full)
							+ " - " + web.getTitle());
				}
				
				activity.setToolbarButtonsState();
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.getWindow().requestFeature(Window.FEATURE_PROGRESS);

		// DEBUG
		//GenericUtils.logHeap(this.getClass());
		

		Window window = getWindow();
		window.setFormat(PixelFormat.RGBA_8888);

		setContentView(R.layout.browse_article);
		
		// Cause memory leaks, add programmatically a WebView with application context.
		LinearLayout layout = (LinearLayout) findViewById(R.id.mainBrowserLayout);
		webView = new WebView(this.getApplicationContext());
		LayoutParams parms = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		parms.weight = 1;
		webView.setLayoutParams(parms);
		layout.addView(webView);
		layout.invalidate();
		
		// Makes Progress bar Visible
		getWindow().setFeatureInt(Window.FEATURE_PROGRESS,
				Window.PROGRESS_VISIBILITY_ON);

		initWebView();
		
		setupButtons();
		setToolbarButtonsState();
		
		if (savedInstanceState == null) {
			loadWebPage();
		} else {
			WebView web = getWebView();
			web.restoreState(savedInstanceState);
		}

		setupButtons();
	}

	
	private WebView getWebView() {
		return webView;
	}
	
	
	private void initApplication() throws Exception {
		// First initialize the application data.
		NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
		if (!app.isApplicationInitialized()) {
			app.initApplication();
		}
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}

	private void loadWebPage() {

		String pageUrl = null;
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			if (extras.get(AppConstants.INTENT_BROWSE_URL_KEY) != null) {
				pageUrl = extras.getString(AppConstants.INTENT_BROWSE_URL_KEY);
			}
		}

		if (pageUrl != null) {
			showDocument(pageUrl);
		} else {
			WebView web = getWebView();
			String mimetype = "text/html";
			String encoding = "UTF-8";
			String htmldata = "<html><body><p>Ooops....l'articolo richiesto per qualche motivo non è disponibile!</p></body></html>";
			web.loadData(htmldata, mimetype, encoding);
		}
	}

	private void initWebView() {
		final WebView web = getWebView();
		web.getSettings().setJavaScriptEnabled(true);
		web.getSettings().setSupportZoom(true);
		web.getSettings().setUseWideViewPort(true);
		web.getSettings().setBuiltInZoomControls(true);
		web.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
		web.setWebChromeClient(new CustomWebChromeClient(this));
		web.setWebViewClient(new CustomWebClient());

	}

	private void setupButtons() {

		ImageButton btnPrevious = (ImageButton) findViewById(R.id.browse_previous_btn);
		ImageButton btnNext = (ImageButton) findViewById(R.id.browse_next_btn);
		ImageButton btnInternet = (ImageButton) findViewById(R.id.browse_internet_btn);
		ImageButton btnShare = (ImageButton) findViewById(R.id.browse_share_btn);
		
		// Set listeners.
		btnInternet.setOnClickListener(new ExtBrowserClickListener(this));
		btnPrevious.setOnClickListener(new PreviousClickListener(this));
		btnNext.setOnClickListener(new NextClickListener(this));
		btnShare.setOnClickListener(new ShareClickListener(this));
	}

	private void setToolbarButtonsState() {
		ImageButton btnPrevious = (ImageButton) findViewById(R.id.browse_previous_btn);
		ImageButton btnNext = (ImageButton) findViewById(R.id.browse_next_btn);
		ImageButton btnInternet = (ImageButton) findViewById(R.id.browse_internet_btn);

		btnPrevious.setEnabled(true);
		btnNext.setEnabled(true);
		btnInternet.setEnabled(true);

		WebView web = getWebView();
		if (!web.canGoBack()) {
			btnPrevious.setEnabled(false);
		}

		if (!web.canGoForward()) {
			btnNext.setEnabled(false);
		}
	}

	private void showDocument(String url) {
		WebView web = getWebView();
		web.loadUrl(url);
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			WebView web = getWebView();
			web.stopLoading();
			finish();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}
	
	

	@Override
	protected void onDestroy() {
		// DEBUG
		//GenericUtils.logHeap(this.getClass());
		
		WebView web = getWebView();
		LinearLayout layout = (LinearLayout) findViewById(R.id.mainBrowserLayout);
		layout.removeView(web);
		web.destroy();
		webView = null;
		
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		WebView web = getWebView();
		web.saveState(outState);
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		
		setToolbarButtonsState();
	}
}
