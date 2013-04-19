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

package it.tizianofagni.notiziepolitiche.service;

import it.tizianofagni.notiziepolitiche.R;
import it.tizianofagni.notiziepolitiche.service.IFeedRetrieverService;
import it.tizianofagni.notiziepolitiche.service.IFeedRetrieverServiceCallback;
import it.tizianofagni.notiziepolitiche.AppConstants;
import it.tizianofagni.notiziepolitiche.NotiziePoliticheApplication;
import it.tizianofagni.notiziepolitiche.activity.CategoriesListActivity;
import it.tizianofagni.notiziepolitiche.dao.Article;
import it.tizianofagni.notiziepolitiche.dao.ArticleSearchParameters;
import it.tizianofagni.notiziepolitiche.dao.Category;
import it.tizianofagni.notiziepolitiche.dao.DAOIOException;
import it.tizianofagni.notiziepolitiche.dao.NewsDAO;
import it.tizianofagni.notiziepolitiche.dao.ArticleSearchParameters.ArticleReadState;
import it.tizianofagni.notiziepolitiche.dao.ArticleSearchParameters.ArticleState;
import it.tizianofagni.notiziepolitiche.feed.NewsFeedReader;
import it.tizianofagni.notiziepolitiche.utils.GenericUtils;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

public class FeedRetrieverService extends Service {

	public static final String INTENT_WAKE_UP_REFRESH_ALL = "INTENT_WAKE_UP_REFRESH_ALL";
	public static final String INTENT_WAKE_UP_REMOVE_EXPIRED_DATA = "INTENT_WAKE_UP_REMOVE_EXPIRED_DATA";
	public static final String INTENT_START_SERVICE_ON_BOOT = "INTENT_START_SERVICE_ON_BOOT";

	public static final String LOCK_NAME_STATIC = "it.tizianofagni.notiziepolitiche.FeedRetrieverService.Static";
	private static PowerManager.WakeLock lockStatic = null;

	private long lastRefreshAllTimeExecution;
	private long lastRemoveExpiredTimeExecution;
	private boolean removeExpiredUnreadArticles;
	private boolean autoRefresh;
	private int oldDataDays;
	private int refreshInterval;
	private int removeExpiredInterval;
	private boolean isRetrievingFeeds;
	private boolean showNotification;
	private ScheduledThreadPoolExecutor executor;
	private NewsFeedReader feedReader;
	private RemoteCallbackList<IFeedRetrieverServiceCallback> listeners;

	private PendingIntent intentRefreshAll;
	private PendingIntent intentExpiredData;

	private final IFeedRetrieverService.Stub binder = new IFeedRetrieverService.Stub() {

		@Override
		public void removeExpiredData() throws RemoteException {
			synchronized (FeedRetrieverService.this) {
				if (isRetrievingFeeds)
					return;
			}

			Log.i(FeedRetrieverService.class.getName(),
					"remove expired data: acquire lock");
			FeedRetrieverService
					.acquireStaticLock(getApplicationContext());
			executor.schedule(new RemoveExpiredDataTask(
					FeedRetrieverService.this), 1, TimeUnit.MILLISECONDS);
		}

		@Override
		public void removeCallback(IFeedRetrieverServiceCallback callback)
				throws RemoteException {
			if (callback == null)
				return;

			listeners.unregister(callback);
		}

		@Override
		public boolean refreshAll() throws RemoteException {
			NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
			if (app == null)
				return false;

			// Check if there is network connection.

			synchronized (FeedRetrieverService.this) {
				if (isRetrievingFeeds)
					return true;

				if (!isNetworkConnectionAvailable())
					return false;

				// Stop the current schedule.
				clearRefreshAllSchedule();
			}

			Log.i(FeedRetrieverService.class.getName(),
					"refresh all: acquire lock");
			FeedRetrieverService
					.acquireStaticLock(getApplicationContext());
			// Schedule a thread to refresh all parties data.
			executor.schedule(new AllFeedsRetrieverTask(
					FeedRetrieverService.this), 1, TimeUnit.MILLISECONDS);
			return true;
		}

		@Override
		public boolean refresh(int partyID) throws RemoteException {
			synchronized (FeedRetrieverService.this) {
				if (isRetrievingFeeds)
					return true;
			}

			if (!isNetworkConnectionAvailable())
				return false;

			try {
				NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
				NewsDAO dao = app.getDAO();
				Category party = dao.getCategory(partyID);
				if (party == null) {
					Log.e(FeedRetrieverService.class.getName(),
							"Can not find the specified partyID: " + partyID);
					// Skip silently the operation.
					return true;
				}

				Log.i(FeedRetrieverService.class.getName(),
						"refresh party: acquire lock");
				FeedRetrieverService
						.acquireStaticLock(getApplicationContext());
				executor.schedule(new PartyRetrieverTask(
						FeedRetrieverService.this, party), 1,
						TimeUnit.MILLISECONDS);
				return true;
			} catch (Exception e) {
				throw new ServiceRemoteException("Refreshing party ID"
						+ partyID + ": " + e.getMessage());
			}
		}

		@Override
		public void addCallback(IFeedRetrieverServiceCallback callback)
				throws RemoteException {
			if (callback == null)
				return;

			listeners.register(callback);
		}

		@Override
		public void updateConfiguration(String parameter)
				throws RemoteException {
			configureServiceParameters();
		}

		@Override
		public void quit() throws RemoteException {
			NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
			app.destroyApplication();
			stopSelf();
		}
	};

	private static class RemoveExpiredDataTask implements Runnable {
		private WeakReference<FeedRetrieverService> service;

		public RemoveExpiredDataTask(FeedRetrieverService service) {
			this.service = new WeakReference<FeedRetrieverService>(
					service);
		}

		public void run() {
			FeedRetrieverService se = service.get();
			if (se == null)
				return;
			try {

				// Remove expired data.
				se.removeExpiredData();

				NotiziePoliticheApplication app = (NotiziePoliticheApplication) se
						.getApplication();
				SharedPreferences prefs = app.getPreferences();
				synchronized (se) {
					GregorianCalendar gc = new GregorianCalendar();
					Editor edt = prefs.edit();
					edt.putString(
							se.getResources().getString(
									R.string.keyLastTimeDataExpired),
							"" + gc.getTimeInMillis());
					edt.commit();
					se.lastRemoveExpiredTimeExecution = gc.getTimeInMillis();

					// Schedule the next execution.
					se.scheduleExpiredDataService();
				}

			} catch (Exception e) {
				Log.w(FeedRetrieverService.class.getName(),
						"Removing expired data: "
								+ GenericUtils.getStackTrace(e));
			} finally {
				Log.i(FeedRetrieverService.class.getName(),
						"release lock");
				getLock(se.getApplicationContext()).release();
			}
		}
	}

	private static class AllFeedsRetrieverTask implements Runnable {

		private WeakReference<FeedRetrieverService> service;

		public AllFeedsRetrieverTask(FeedRetrieverService service) {
			this.service = new WeakReference<FeedRetrieverService>(
					service);
		}

		@Override
		public void run() {
			FeedRetrieverService se = service.get();
			if (se == null)
				return;

			try {

				boolean showNotification = true;
				synchronized (se) {
					if (se.isRetrievingFeeds)
						return;

					se.isRetrievingFeeds = true;
					showNotification = se.showNotification;
				}

				Log.i(FeedRetrieverService.class.getCanonicalName(),
						"Start retrieving feeds...");

				// Retrieve all new feeds.
				int newArticles = se.retrieveAllFeeds();

				NotiziePoliticheApplication app = (NotiziePoliticheApplication) se
						.getApplication();
				SharedPreferences prefs = app.getPreferences();
				synchronized (se) {
					se.isRetrievingFeeds = false;
					GregorianCalendar gc = new GregorianCalendar();
					Editor edt = prefs.edit();
					edt.putString(
							se.getResources().getString(
									R.string.keyLastTimeDataRefreshed),
							"" + gc.getTimeInMillis());
					edt.commit();
					se.lastRefreshAllTimeExecution = gc.getTimeInMillis();

					// Schedule the next execution.
					se.scheduleRetrieveService();
				}

				newArticles = se.getNumberOfNewArticles();

				// if the case, signal a notification.
				if (newArticles > 0 && showNotification) {
					se.sendNotification(newArticles);
				}

				Log.i(FeedRetrieverService.class.getCanonicalName(),
						"Retrieved " + newArticles + " new articles!");

			} catch (Exception e) {
				Log.e(FeedRetrieverService.class.getName(),
						"Retrieving feeds data: "
								+ GenericUtils.getStackTrace(e));
			} finally {
				Log.i(FeedRetrieverService.class.getName(),
						"release lock");
				getLock(se.getApplicationContext()).release();
			}
		}

	}

	private static class PartyRetrieverTask implements Runnable {

		private Category party;
		private WeakReference<FeedRetrieverService> service;

		public PartyRetrieverTask(FeedRetrieverService service,
				Category party) {
			this.party = party;
			this.service = new WeakReference<FeedRetrieverService>(
					service);
		}

		@Override
		public void run() {
			FeedRetrieverService se = service.get();
			if (se == null)
				return;

			try {

				GregorianCalendar gc = new GregorianCalendar();
				gc.add(Calendar.DAY_OF_MONTH, -se.oldDataDays);
				Date dOld = gc.getTime();

				Log.i(FeedRetrieverService.class.getCanonicalName(),
						"Start retrieving feeds for party " + party.getName());

				int newArticles = se.retrieveFeedsFromParty(party, dOld);

				Log.i(FeedRetrieverService.class.getCanonicalName(),
						"Retrieved " + newArticles + " new articles for party "
								+ party.getName() + "!");

			} catch (Exception e) {
				Log.w(FeedRetrieverService.class.getName(),
						"Retrieving feeds data: "
								+ GenericUtils.getStackTrace(e));
			} finally {
				Log.i(FeedRetrieverService.class.getName(),
						"release lock");
				getLock(se.getApplicationContext()).release();
			}
		}

	}

	public static void acquireStaticLock(Context context) {
		getLock(context).acquire();
	}

	synchronized private static PowerManager.WakeLock getLock(Context context) {
		if (lockStatic == null) {
			PowerManager mgr = (PowerManager) context
					.getSystemService(Context.POWER_SERVICE);

			lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					LOCK_NAME_STATIC);
			lockStatic.setReferenceCounted(true);
		}

		return (lockStatic);
	}

	public FeedRetrieverService() {
		executor = new ScheduledThreadPoolExecutor(3);
		isRetrievingFeeds = false;
		feedReader = new NewsFeedReader();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		try {
			listeners = new RemoteCallbackList<IFeedRetrieverServiceCallback>();
			NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
			app.firstTimeInitApplication();

			Intent i = new Intent(getApplicationContext(),
					RefreshAllReceiver.class);
			intentRefreshAll = PendingIntent.getBroadcast(
					getApplicationContext(), 0, i,
					PendingIntent.FLAG_UPDATE_CURRENT);

			i = new Intent(getApplicationContext(),
					RemoveExpiredDataReceiver.class);
			intentExpiredData = PendingIntent.getBroadcast(
					getApplicationContext(), 0, i,
					PendingIntent.FLAG_UPDATE_CURRENT);

			configureServiceParameters();

		} catch (Exception e) {
			Log.e(getClass().getName(), "Creating the feeds service", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		listeners = null;

		// End the service.
		clearRefreshAllSchedule();
		clearExpiredDataSchedule();

		executor.shutdown();

		NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
		app.destroyApplication();

		executor = null;
		listeners = null;

		Log.i(FeedRetrieverService.class.getName(), "Destroy service: process " + Process.myPid());
		Process.killProcess(Process.myPid());
	}

	private synchronized void configureServiceParameters() {
		try {
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext());

			Resources r = getResources();
			synchronized (this) {
				autoRefresh = prefs.getBoolean(
						r.getString(R.string.keyAutoRefreshData), true);
				showNotification = prefs.getBoolean(
						r.getString(R.string.keyShowNotification), true);
				removeExpiredUnreadArticles = prefs.getBoolean(
						r.getString(R.string.keyTrashExpiredArticlesNotRead),
						false);
				oldDataDays = Integer.parseInt(prefs.getString(getResources()
						.getString(R.string.keyRemoveExpiredData), ""
						+ AppConstants.DEFAULT_REMOVE_EXPIRED_DATA));
				refreshInterval = Integer
						.parseInt(prefs
								.getString(
										getResources()
												.getString(
														R.string.keyRetrieveFeedsInterval),
										""
												+ AppConstants.DEFAULT_RETRIEVE_FEEDS_INTERVAL)) * 1000 * 60;

				removeExpiredInterval = Integer
						.parseInt(prefs
								.getString(
										getResources()
												.getString(
														R.string.keyRemoveExpiredInterval),
										""
												+ AppConstants.DEFAULT_REMOVE_EXPIRED_INTERVAL)) * 1000 * 60;

				lastRefreshAllTimeExecution = Long.parseLong(prefs.getString(
						getResources().getString(
								R.string.keyLastTimeDataRefreshed), "-1"));
				lastRemoveExpiredTimeExecution = Long
						.parseLong(prefs.getString(
								getResources().getString(
										R.string.keyLastTimeDataExpired), "-1"));

				scheduleRetrieveService();
				scheduleExpiredDataService();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void scheduleExpiredDataService() {
		NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
		if (app == null)
			return;

		// End the service if it was previously started.
		clearExpiredDataSchedule();

		try {
			if (lastRemoveExpiredTimeExecution == -1) {
				GregorianCalendar gc = new GregorianCalendar(1975, 1, 1);
				lastRemoveExpiredTimeExecution = gc.getTimeInMillis();
			}
			// Compute next time to start the refreshing of data.
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTimeInMillis(lastRemoveExpiredTimeExecution);
			gc.add(Calendar.MILLISECOND, removeExpiredInterval);
			GregorianCalendar gcNow = new GregorianCalendar();
			long nextExecDelay = gc.getTimeInMillis() - gcNow.getTimeInMillis();

			long timeToStart = gcNow.getTimeInMillis();
			if (nextExecDelay > 0)
				timeToStart = gc.getTimeInMillis();
			else {
				gcNow.add(Calendar.SECOND, 5);
			}

			AlarmManager mgr = (AlarmManager) getApplicationContext()
					.getSystemService(Context.ALARM_SERVICE);
			mgr.set(AlarmManager.RTC_WAKEUP, timeToStart, intentExpiredData);

			GregorianCalendar gcScheduled = new GregorianCalendar();
			gcScheduled.setTimeInMillis(timeToStart);
			Log.i(FeedRetrieverService.class.getName(),
					"Next execution time of expired data: "
							+ gcScheduled.getTime().toString());

		} catch (Exception e) {
			GenericUtils
					.makeMessageDlgForExit(
							app.getApplicationContext(),
							getClass().getCanonicalName(),
							"Can not initialize the service that periodically retrieve feeds",
							e);
		}

	}

	/**
	 * Schedule the next execution of refresh all command.
	 */
	private void scheduleRetrieveService() {
		NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
		if (app == null)
			return;

		if (isRetrievingFeeds)
			return;

		// End the service if it was previously started.
		clearRefreshAllSchedule();

		if (!autoRefresh) {
			Log.i(FeedRetrieverService.class.getName(),
					"The auto refresh mode is disabled");
			return;
		}

		try {
			if (lastRefreshAllTimeExecution == -1) {
				GregorianCalendar gc = new GregorianCalendar(1975, 1, 1);
				lastRefreshAllTimeExecution = gc.getTimeInMillis();
			}
			// Compute next time to start the refreshing of data.
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTimeInMillis(lastRefreshAllTimeExecution);
			gc.add(Calendar.MILLISECOND, refreshInterval);
			GregorianCalendar gcNow = new GregorianCalendar();
			long nextExecDelay = gc.getTimeInMillis() - gcNow.getTimeInMillis();

			long timeToStart = gcNow.getTimeInMillis();
			if (nextExecDelay > 0)
				timeToStart = gc.getTimeInMillis();
			else {
				gcNow.add(Calendar.SECOND, 5);
			}

			AlarmManager mgr = (AlarmManager) getApplicationContext()
					.getSystemService(Context.ALARM_SERVICE);
			mgr.set(AlarmManager.RTC_WAKEUP, timeToStart, intentRefreshAll);

			GregorianCalendar gcScheduled = new GregorianCalendar();
			gcScheduled.setTimeInMillis(timeToStart);
			Log.i(getClass().getName(), "Next execution time of refresh all: "
					+ gcScheduled.getTime().toString());

		} catch (Exception e) {
			GenericUtils
					.makeMessageDlgForExit(
							app.getApplicationContext(),
							getClass().getCanonicalName(),
							"Can not initialize the service that periodically retrieve feeds",
							e);
		}
	}

	private boolean clearRefreshAllSchedule() {
		// Remove previous schedule from alarm manager.
		AlarmManager mgr = (AlarmManager) getApplicationContext()
				.getSystemService(Context.ALARM_SERVICE);
		mgr.cancel(intentRefreshAll);
		return true;
	}

	private boolean clearExpiredDataSchedule() {
		// Remove previous schedule from alarm manager.
		AlarmManager mgr = (AlarmManager) getApplicationContext()
				.getSystemService(Context.ALARM_SERVICE);
		mgr.cancel(intentExpiredData);
		return true;
	}

	private void removeExpiredArticles(int oldDataDays) throws Exception {
		GregorianCalendar gc = new GregorianCalendar();
		gc.add(Calendar.DAY_OF_MONTH, -oldDataDays);
		Date dOld = gc.getTime();

		NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
		NewsDAO dao = app.getDAO();
		ArticleSearchParameters parms = new ArticleSearchParameters();
		if (removeExpiredUnreadArticles)
			parms.setReadState(ArticleReadState.BOTH_READ_AND_UNREAD);
		else
			parms.setReadState(ArticleReadState.READ);
		parms.setArticleState(ArticleState.BOTH_VALID_AND_TRASHED);
		parms.setPreferred(false);
		parms.setEndDate(dOld);
		int numArticlesToDelete = dao.getArticlesSize(parms);
		if (numArticlesToDelete <= 0)
			return;

		ArrayList<Integer> toRemove = new ArrayList<Integer>();
		List<Article> results = dao.getArticles(parms, 0, numArticlesToDelete);
		for (Article ar : results) {
			toRemove.add(ar.getArticleID());
		}
		dao.removeArticles(toRemove);
	}

	private void removeExpiredData() throws Exception {
		// Remove all articles.
		Log.i(FeedRetrieverService.class.getName(),
				"Remove expired articles");
		removeExpiredArticles(this.oldDataDays);

		// Clear older application cache.
		Log.i(FeedRetrieverService.class.getName(),
				"Clear old cache data");
		GenericUtils.clearCache(getApplicationContext(),
				AppConstants.DEFAULT_REMOVE_EXPIRED_CACHED_DATA);
	}

	/**
	 * Retrieve all new data from all parties.
	 * 
	 * @return
	 */
	private int retrieveAllFeeds() {
		try {

			GregorianCalendar gc = new GregorianCalendar();
			gc.add(Calendar.DAY_OF_MONTH, -oldDataDays);
			Date dOld = gc.getTime();

			NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
			NewsDAO dao = app.getDAO();
			int newArticles = 0;

			int numParties = dao.getCategoriesSize();
			List<Category> parties = dao.getCategories(0, numParties);
			for (Category party : parties) {
				try {
					newArticles += retrieveFeedsFromParty(party, dOld);
				} catch (Exception e) {
					Log.w(FeedRetrieverService.class.getName(),
							"Error retrieving data for party "
									+ party.getName() + ": "
									+ GenericUtils.getStackTrace(e));
					continue;
				}
			}

			// Signal that new articles has been retrieved.
			signalNewArticlesRetrieved(newArticles);

			return newArticles;

		} catch (Exception e) {
			throw new RuntimeException("Bug in code", e);
		}
	}

	private int retrieveFeedsFromParty(Category party, Date maxTimeOld)
			throws ServiceIOException {
		try {
			int newArticles = 0;

			signalStartRetrievingArticlesForParty(party);

			NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
			NewsDAO dao = app.getDAO();
			List<String> feeds = party.getFeeds();
			for (String feed : feeds) {
				List<Article> articles = null;
				try {
					articles = retrieveFeedData(new URL(feed), maxTimeOld);
				} catch (Exception e) {
					Log.w(FeedRetrieverService.class.getName(),
							"Error accessing feed " + feed + ": "
									+ e.getMessage());
					continue;
				}

				if (articles.size() > 0) {
					List<Integer> added = dao.addArticles(articles, false);
					for (int articleID : added) {
						dao.linkArticleToCategory(articleID,
								party.getCategoryID());
					}

					newArticles += added.size();
				}
			}

			signalEndRetrievingArticlesForParty(party);

			// Signal new articles for the current threads.
			signalNewArticlesRetrievedForParty(party, newArticles);

			return newArticles;
		} catch (Exception e) {
			throw new ServiceIOException("Retrieving feeds data", e);
		}
	}

	private List<Article> retrieveFeedData(URL url, Date maxTimeOld)
			throws ServiceIOException {
		try {
			List<Article> articles = feedReader.getNews(url);
			for (int i = 0; i < articles.size(); i++) {
				Article ar = articles.get(i);
				if (ar.getDate().before(maxTimeOld)) {
					articles.remove(i);
					i--;
				}
			}

			return articles;
		} catch (Exception e) {
			throw new ServiceIOException("Retrieving feed " + url.toString(), e);
		}
	}

	private int getNumberOfNewArticles() throws DAOIOException {
		NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
		NewsDAO dao = app.getDAO();
		ArticleSearchParameters parms = new ArticleSearchParameters();
		parms.setReadState(ArticleReadState.UNREAD);
		return dao.getArticlesSize(parms);
	}

	private void sendNotification(int numNewsArticles) {
		// Get the notification manager.
		NotificationManager manager = (NotificationManager) getApplication()
				.getApplicationContext().getSystemService(
						Context.NOTIFICATION_SERVICE);

		Context ctx = getApplication().getApplicationContext();
		String briefMsg = ctx.getString(R.string.brief_notification_text, GenericUtils.getAppName(ctx));

		/* Create a notification */
		Notification notification = new Notification(R.drawable.notification, // An
																				// Icon
																				// to
																				// display
				briefMsg, // the text to display in the ticker
				System.currentTimeMillis()); // the time for the notification

		/* Starting an intent */
		String title = ctx.getString(R.string.long_title_notification_text, GenericUtils.getAppName(ctx));
		String details = ctx.getString(R.string.long_notification_text,
				numNewsArticles);
		Intent notificationIntent = new Intent(ctx,
				CategoriesListActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0,
				notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		notification.setLatestEventInfo(ctx, title, details, contentIntent);

		notificationIntent.putExtra(
				AppConstants.NOTIFICATION_SHOW_NEW_ARTICLES, 1);

		notification.ledOnMS = 200; // Set led blink (Off in ms)
		notification.ledOffMS = 200; // Set led blink (Off in ms)
		notification.ledARGB = 0x9400d3; // Set led color

		notification.flags |= Notification.FLAG_SHOW_LIGHTS
				| Notification.FLAG_AUTO_CANCEL;

		/* Sent Notification to notification bar */
		manager.notify(AppConstants.NOTIFICATION_NEW_ARTICLES, notification);

	}

	private void signalStartRetrievingArticlesForParty(Category party)
			throws RemoteException {
		int s = listeners.beginBroadcast();
		for (int i = 0; i < s; i++) {
			try {
				listeners.getBroadcastItem(i).startRetrievingArticles(
						party.getCategoryID());
			} catch (Exception e) {

			}
		}
		listeners.finishBroadcast();
	}

	private synchronized void signalNewArticlesRetrieved(int numNewArticles)
			throws RemoteException {
		int s = listeners.beginBroadcast();
		for (int i = 0; i < s; i++) {
			try {
				listeners.getBroadcastItem(i).newArticlesRetrieved(
						numNewArticles);
			} catch (Exception e) {

			}
		}
		listeners.finishBroadcast();
	}

	private synchronized void signalNewArticlesRetrievedForParty(
			Category party, int numNewArticles) throws RemoteException {
		int s = listeners.beginBroadcast();
		for (int i = 0; i < s; i++) {
			try {
				listeners.getBroadcastItem(i).newArticlesRetrievedForCategory(
						party.getCategoryID(), numNewArticles);
			} catch (Exception e) {

			}
		}
		listeners.finishBroadcast();
	}

	private synchronized void signalEndRetrievingArticlesForParty(Category party)
			throws RemoteException {
		int s = listeners.beginBroadcast();
		for (int i = 0; i < s; i++) {
			try {
				listeners.getBroadcastItem(i).endRetrievingArticles(
						party.getCategoryID());
			} catch (Exception e) {

			}
		}
		listeners.finishBroadcast();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);

		// Here, we should handle Alarm service requests to refresh all data
		// sources and
		// to perform cleanup of expired data.
		Bundle b = intent.getExtras();
		if (b == null)
			return;

		// If here, the wake lock is already acquired.
		if (b.containsKey(INTENT_WAKE_UP_REFRESH_ALL)) {
			if (!isNetworkConnectionAvailable()) {
				try {

					Log.i(FeedRetrieverService.class.getName(),
							"The connection is not available. Reschedule the refresh operation later.");
					synchronized (this) {
						NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
						SharedPreferences prefs = app.getPreferences();
						GregorianCalendar gc = new GregorianCalendar();
						Editor edt = prefs.edit();
						edt.putString(
								getResources().getString(
										R.string.keyLastTimeDataRefreshed), ""
										+ gc.getTimeInMillis());
						edt.commit();
						lastRefreshAllTimeExecution = gc.getTimeInMillis();
						// Schedule the next execution.
						scheduleRetrieveService();
					}

				} finally {
					Log.i(FeedRetrieverService.class.getName(),
							"No connection available. Release lock.");
					getLock(getApplicationContext()).release();
				}

			} else {
				executor.schedule(new AllFeedsRetrieverTask(this), 0,
						TimeUnit.MILLISECONDS);
			}
		} else if (b.containsKey(INTENT_WAKE_UP_REMOVE_EXPIRED_DATA)) {
			executor.schedule(new RemoveExpiredDataTask(this), 0,
					TimeUnit.MILLISECONDS);
		} else if (b.containsKey(INTENT_START_SERVICE_ON_BOOT)) {
			try {
				NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
				SharedPreferences prefs = app.getPreferences();
				boolean toStart = prefs.getBoolean(
						getResources()
								.getString(R.string.keyStartServiceOnBoot),
						false);
				if (!toStart) {
					Log.i(FeedRetrieverService.class.getName(),
							"Stop service at boot time");
					app.destroyApplication();
					stopSelf();
				}

			} finally {
				getLock(getApplicationContext()).release();
				Log.i(FeedRetrieverService.class.getName(),
				"Release lock");
			}
		}
	}

	private boolean isNetworkConnectionAvailable() {
		ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		if (connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
				.getState() == NetworkInfo.State.CONNECTED
				|| connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
						.getState() == NetworkInfo.State.CONNECTED) {
			// You are online
			return true;

		} else {
			// You are offline.
			return false;
		}
	}
}
