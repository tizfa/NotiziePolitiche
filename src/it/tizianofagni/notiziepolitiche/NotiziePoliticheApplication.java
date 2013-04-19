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

package it.tizianofagni.notiziepolitiche;

import it.tizianofagni.notiziepolitiche.R;
import it.tizianofagni.notiziepolitiche.service.IFeedRetrieverService;
import it.tizianofagni.notiziepolitiche.dao.Category;
import it.tizianofagni.notiziepolitiche.dao.CategoryImageType;
import it.tizianofagni.notiziepolitiche.dao.DAOIOException;
import it.tizianofagni.notiziepolitiche.dao.NewsDAO;
import it.tizianofagni.notiziepolitiche.dao.NewsLocalDAO;
import it.tizianofagni.notiziepolitiche.exception.ApplicationException;
import it.tizianofagni.notiziepolitiche.service.FeedRetrieverService;
import it.tizianofagni.notiziepolitiche.utils.GenericUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Environment;
import android.os.IBinder;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;

import com.admob.android.ads.AdManager;

public class NotiziePoliticheApplication extends Application {

	private SharedPreferences.OnSharedPreferenceChangeListener listenerPreferences = new SharedPreferences.OnSharedPreferenceChangeListener() {

		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			try {
				if (feedsService != null)
					feedsService.updateConfiguration(key);
			} catch (Exception e) {
				Log.e(NotiziePoliticheApplication.class.getName(),
						"Updating service configuration", e);
				throw new RuntimeException(e);
			}
		}
	};

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {

			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			feedsService = IFeedRetrieverService.Stub.asInterface(service);
			feedsServiceIsBound = true;

			// Tell the user about this for our demo.
			// Toast.makeText(
			// NotiziePoliticheApplication.this.getApplicationContext(),
			// "DEBUG: collegato al servizio...", Toast.LENGTH_SHORT)
			// .show();
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			feedsService = null;
			feedsServiceIsBound = false;
			
			destroyApplication();
			
			/*Intent i = new Intent(getApplicationContext(),
					FeedRetrieverService.class);
			getApplicationContext().stopService(i);*/
			
			// Kill myself!
			Process.killProcess(Process.myPid());
		}
	};

	/**
	 * The feeds service.
	 */
	private IFeedRetrieverService feedsService;
	private NewsDAO dao;
	private boolean isApplicationInitialized;
	private SharedPreferences prefs;
	private static NotiziePoliticheApplication app;

	/**
	 * True if the feeds service is bound.
	 */
	private boolean feedsServiceIsBound;

	public NotiziePoliticheApplication() throws DAOIOException {
		dao = null;
		isApplicationInitialized = false;
		prefs = null;
		app = this;
	}

	/**
	 * Get the instance of this application.
	 * 
	 * @return The instance of this application.
	 */
	public static NotiziePoliticheApplication getApp() {
		return app;
	}

	@Override
	public void onCreate() {
		// Call the super.
		super.onCreate();

		// TO REMOVE.
		// TEST FOR ADMOB
		/*AdManager.setTestDevices(new String[] { AdManager.TEST_EMULATOR, // Android
																			// emulator
				"808902E2AC05B15E97D4D6EDA3F9D645", // My Acer Liquid.
		});*/
		AdManager.setAllowUseOfLocation(true);

		/*
		 * Ensure the factory implementation is loaded from the application
		 * classpath (which contains the implementation classes), rather than
		 * the system classpath (which doesn't).
		 */
		// Thread.currentThread().setContextClassLoader(
		// getClass().getClassLoader());

		try {
			// Cread DAO.
			dao = new NewsLocalDAO(getApplicationContext());

			// Init preferences.
			getPreferences();
			
			// Launch feeds service.
			Intent intent = new Intent(getApplicationContext(),
					FeedRetrieverService.class);
			startService(intent);

			// Try to bind to the service.
			doBindService();

		} catch (Exception e) {
			GenericUtils.makeMessageDlgForExit(getApplicationContext(),
					getClass().getCanonicalName(),
					"Error starting application", e);
		}
	}

	/**
	 * Get the DAO manager used to store/retrieve application data.
	 * 
	 * @return The DAO manager of the application.
	 */
	public NewsDAO getDAO() {
		return dao;
	}

	/**
	 * Get the feeds service used by the application.
	 * 
	 * @return The feeds service used by the application.
	 */
	public IFeedRetrieverService getFeedsService() {
		return feedsService;
	}

	/**
	 * Get the set of preferences of the application.
	 * 
	 * @return The set of preferences of the application.
	 */
	public SharedPreferences getPreferences() {
		if (prefs == null) {
			try {
				String contextName = getGlobalContextName();
				Context myContext = createPackageContext(contextName,
						Context.MODE_WORLD_WRITEABLE);
				PreferenceManager.setDefaultValues(myContext,
						AppConstants.PREF_NAME, Context.MODE_PRIVATE,
						R.layout.preferences, true);
				prefs = PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext());
				// Register listener for settings
				prefs.registerOnSharedPreferenceChangeListener(listenerPreferences);
			} catch (Exception e) {
				Log.e(NotiziePoliticheApplication.class.getName(), "Getting preferences", e);
				throw new RuntimeException(e);
			}
		}
		
		return prefs;
	}

	private boolean isExternalStorageAvailable() {
		String state = Environment.getExternalStorageState();

		if (!Environment.MEDIA_MOUNTED.equals(state)) {
			// We can not read and write the media
			return false;
		} else {
			return true;
		}
	}

	public void firstTimeInitApplication() throws ApplicationException {
		if (isApplicationInitialized)
			return;

		try {

			Log.i(NotiziePoliticheApplication.class.getName(), "Wait for SD card to be ready...");
			while (!isExternalStorageAvailable()) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			Log.i(NotiziePoliticheApplication.class.getName(), "Ok. The SD card is ready!");

			String version = prefs.getString(
					getResources().getString(R.string.keyApplicationVersion),
					null);
			dao.init(false);
			if (version == null) {
				// First time we launch the application. The preferences do not
				// exist.
				// Create it.
				initializeNewData();
			} else if (dao.getCategoriesSize() == 0) {
				// First time we launch the application. The preferences do not
				// exist.
				// Create it.
				initializeNewData();
			} else {
				// If the case, Force Lite version parameters.
				//forceLiteVersionParametersValues(prefs);
				
				// Check if an upgrade is necessary.
				SoftwareVersion storedVersion = new SoftwareVersion(version);
				if (!storedVersion.equals(AppConstants.applicationVersion)) {
					// Update to various versions.
					if (storedVersion.compareTo(new SoftwareVersion("1.0.6")) < 0) {
						initializeNewData();
					}
					
					// Save application version.
					Editor ed = prefs.edit();
					ed.putString(getResources().getString(R.string.keyApplicationVersion), AppConstants.applicationVersion.toString());
					ed.commit();
				}
			}

			// Register listener for settings
			prefs.registerOnSharedPreferenceChangeListener(listenerPreferences);

			isApplicationInitialized = true;
		} catch (Exception e) {
			throw new ApplicationException("Initializing application", e);
		}
	}
	
	
	private void initializeNewData() throws DAOIOException {
		// Release current DB connection.
		dao.release();
		
		// Delete old DB.
		File f = new File(Environment
				.getExternalStorageDirectory().toString()+"/NotiziePolitiche");
		Log.i(NotiziePoliticheApplication.class.getName(), "Deleting old DB data directory "+f.toString());
		GenericUtils.removeDirectory(f);
		
		// Force creation of the DB and preferences.
		dao.init(true);
		createDefaultPrefences(prefs);
		loadInitialData();
	}
	
	
	public String getGlobalContextName() {
		/*String contextName = "it.tizianofagni.notiziepolitiche.full"; 
		if (isLiteVersion()) {
			contextName = "it.tizianofagni.notiziepolitiche.lite"; 
		}
		
		return contextName;*/
		return "it.tizianofagni.notiziepolitiche";
	}
	

	private void forceLiteVersionParametersValues(SharedPreferences preferences) {
		if (isLiteVersion()) {
			Resources res = getResources();
			Editor ed = preferences.edit();
			ed.putBoolean(res.getString(R.string.keyAutoRefreshData),
					false);
			ed.commit();
		}
	}

	private void updateTo_1_0_1(SharedPreferences prefs,
			SoftwareVersion storedVersion) {
		// Does nothing.
	}


	/**
	 * Called to init application data structures.
	 */
	public boolean initApplication() throws ApplicationException {
		// Wait for the service to be available.
		if (feedsService == null)
			return false;
		
		if (isApplicationInitialized)
			return true;

		if (!isExternalStorageAvailable())
			return false;

		try {

			dao.init(false);

			isApplicationInitialized = true;

			return true;
		} catch (Exception e) {
			throw new ApplicationException("Initializing application", e);
		}
	}

	private void doBindService() {
		// Establish a connection with the service. We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		getApplicationContext().bindService(
				new Intent(getApplicationContext(),
						FeedRetrieverService.class), mConnection,
				Context.BIND_AUTO_CREATE);
		feedsServiceIsBound = true;
		
		//Log.i("DEBUG", "Bind service: process "+Process.myPid());
	}

	private void doUnbindService() {
		if (feedsServiceIsBound) {
			// Detach our existing connection.
			getApplicationContext().unbindService(mConnection);
			
			//Log.i("DEBUG", "Unbind service: process "+ Process.myPid());
			
			feedsServiceIsBound = false;
		}
	}

	private void loadInitialData() throws DAOIOException {
		dao.removeAllCategories();
		dao.removeAllArticles();
		List<Category> categories = createListOfCategories();
		for (Category cat : categories) {
			dao.addCategory(cat);
		}
	}

	private List<Category> createListOfCategories() {
		ArrayList<Category> partiesList = new ArrayList<Category>();

		Category party = new Category();
		party.setName("Alleanza per l'Italia");
		party.setCategoryImage(CategoryImageType.API.getIntValue());
		party.getFeeds()
				.add("http://news.google.it/news?pz=1&cf=all&ned=it&hl=it&as_maxm=12&q=api+rutelli+OR+tabacci+OR+lanzillotta+OR+alleanza-per-l-italia&as_qdr=a&as_drrb=q&as_mind=23&as_minm=11&cf=all&as_maxd=23&scoring=n&output=rss");
		partiesList.add(party);

		party = new Category();
		party.setName("Futuro e Libertà");
		party.setCategoryImage(CategoryImageType.FLI.getIntValue());
		party.getFeeds()
				.add("http://news.google.it/news?pz=1&cf=all&ned=it&hl=it&as_maxm=12&q=fli+fini+OR+bocchino+OR+gianfranco-fini&as_qdr=a&as_drrb=q&as_mind=23&as_minm=11&cf=all&as_maxd=23&scoring=n&output=rss");
		partiesList.add(party);

		party = new Category();
		party.setName("Italia dei Valori");
		party.setCategoryImage(CategoryImageType.IDV.getIntValue());
		party.getFeeds()
				.add("http://news.google.it/news?pz=1&cf=all&ned=it&hl=it&as_maxm=12&q=idv+di-pietro+OR+italia-dei-valori+OR+massimo-donadi+OR+donadi+OR+orlando+OR+italia-dei-valori&as_qdr=a&as_drrb=q&as_mind=23&as_minm=11&cf=all&as_maxd=23&scoring=n&output=rss");
		partiesList.add(party);

		party = new Category();
		party.setName("Lega Nord");
		party.setCategoryImage(CategoryImageType.LEGA.getIntValue());
		party.getFeeds()
				.add("http://news.google.it/news?pz=1&cf=all&ned=it&hl=it&as_maxm=12&q=lega+bossi+OR+maroni+OR+calderoli+OR+umberto-bossi+OR+roberto-maroni+OR+roberto-calderoli+OR+castelli+OR+lega-nord&as_qdr=a&as_drrb=q&as_mind=23&as_minm=11&cf=all&as_maxd=23&scoring=n&output=rss");
		partiesList.add(party);

		party = new Category();
		party.setName("Movimento per le Autonomie");
		party.setCategoryImage(CategoryImageType.MPA.getIntValue());
		party.getFeeds()
				.add("http://news.google.it/news?pz=1&cf=all&ned=it&hl=it&as_maxm=12&q=mpa+lombardo+OR+movimento+OR+autonomie&as_qdr=a&as_drrb=q&as_mind=23&as_minm=11&cf=all&as_maxd=23&scoring=n&output=rss");
		partiesList.add(party);

		party = new Category();
		party.setName("Partito Democratico");
		party.setCategoryImage(CategoryImageType.PD.getIntValue());
		party.getFeeds()
				.add("http://news.google.it/news?pz=1&cf=all&ned=it&hl=it&as_maxm=12&q=pd+bersani+OR+franceschini+OR+fassino+OR+d-alema+OR+renzi+OR+partito-democratico&as_qdr=a&as_drrb=q&as_mind=23&as_minm=11&cf=all&as_maxd=23&scoring=n&output=rss");
		partiesList.add(party);

		party = new Category();
		party.setName("Popolo della Libertà");
		party.setCategoryImage(CategoryImageType.PDL.getIntValue());
		party.getFeeds()
				.add("http://news.google.it/news?pz=1&cf=all&ned=it&hl=it&as_maxm=12&q=pdl+berlusconi+OR+cicchitto+OR+bondi+OR+la-russa+OR+letta+OR+gasparri&as_qdr=a&as_drrb=q&as_mind=23&as_minm=11&cf=all&as_maxd=23&scoring=n&output=rss");
		partiesList.add(party);

		party = new Category();
		party.setName("Sinistra Ecologia Libertà");
		party.setCategoryImage(CategoryImageType.SEL.getIntValue());
		party.getFeeds()
				.add("http://news.google.it/news?pz=1&cf=all&ned=it&hl=it&as_maxm=12&q=sel+vendola+OR+Sinistra_Ecologia_Libert%C3%A0&as_qdr=a&as_drrb=q&as_mind=23&as_minm=11&cf=all&as_maxd=23&scoring=n&output=rss");
		partiesList.add(party);

		party = new Category();
		party.setName("Unione di centro");
		party.setCategoryImage(CategoryImageType.UDC.getIntValue());
		party.getFeeds()
				.add("http://news.google.it/news?pz=1&cf=all&ned=it&hl=it&as_maxm=12&q=udc+casini+OR+cesa+OR+buttiglione&as_qdr=a&as_drrb=q&as_mind=23&as_minm=11&cf=all&as_maxd=23&scoring=n&output=rss");
		partiesList.add(party);
		
		

		return partiesList;
	}

	private void upgradePreferences(SharedPreferences prefs2,
			SoftwareVersion applicationversion, SoftwareVersion storedVersion) {

	}

	private void createDefaultPrefences(SharedPreferences preferences) {
		Resources res = getResources();
		Editor ed = preferences.edit();
		ed.clear();
		ed.putString(res.getString(R.string.keyApplicationVersion),
				AppConstants.applicationVersion.toString());
		ed.putString(res
				.getString(R.string.keyRetrieveFeedsInterval), ""
				+ AppConstants.DEFAULT_RETRIEVE_FEEDS_INTERVAL);
		ed.putString(res
				.getString(R.string.keyRemoveExpiredInterval), ""
				+ AppConstants.DEFAULT_REMOVE_EXPIRED_INTERVAL);
		ed.putString(res.getString(R.string.keyRemoveExpiredData),
				"" + AppConstants.DEFAULT_REMOVE_EXPIRED_DATA);
		ed.putString(res
				.getString(R.string.keyLastTimeDataRefreshed), ""
				+ new GregorianCalendar(1970, 1, 1).getTimeInMillis());
		ed.putString(res.getString(R.string.keyLastTimeDataExpired),
				"" + new GregorianCalendar(1970, 1, 1).getTimeInMillis());
		ed.putBoolean(res.getString(R.string.keyShowNotification),
				true);
		ed.putBoolean(res.getString(R.string.keyAutoRefreshData),
				false);
		ed.putBoolean(res.getString(R.string.keyStartServiceOnBoot),
				false);
		ed.putBoolean(
				res.getString(
						R.string.keyTrashExpiredArticlesNotRead), false);
		ed.putString(res.getString(R.string.keyNewsArticleTitleFontSize), ""+AppConstants.DEFAULT_ARTICLE_TITLE_FONT_SIZE);
		ed.putString(res.getString(R.string.keyNewsArticleDescriptionFontSize), ""+AppConstants.DEFAULT_ARTICLE_DESCRIPTION_FONT_SIZE);
		ed.commit();
	}

	/**
	 * Called to destroy application.
	 */
	public void destroyApplication() {
		try {
			if (!isApplicationInitialized)
				return;

			getDAO().release();
			doUnbindService();
			
			isApplicationInitialized = false;
		} catch (Exception e) {
			Log.e("NotiziePoliticheApplication", "Releasing application", e);
			isApplicationInitialized = false;
		}
	}

	/**
	 * Indicate if the application has been initialized.
	 * 
	 * @return True if the application has been initialized, false otherwise.
	 */
	public boolean isApplicationInitialized() {
		return this.isApplicationInitialized;
	}
	
	
	/**
	 * Indicates if the software is running as lite or full version.
	 * @return True if the software is running as lite, false if is running as
	 * full version.
	 */
	public boolean isLiteVersion() {
		return GenericUtils.isLiteVersion(this);
	}
}
