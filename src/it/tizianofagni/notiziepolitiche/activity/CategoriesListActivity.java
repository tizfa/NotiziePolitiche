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
import it.tizianofagni.notiziepolitiche.service.IFeedRetrieverService;
import it.tizianofagni.notiziepolitiche.service.IFeedRetrieverServiceCallback;
import it.tizianofagni.notiziepolitiche.AppConstants;
import it.tizianofagni.notiziepolitiche.ApplicationInitializerTask;
import it.tizianofagni.notiziepolitiche.NotiziePoliticheApplication;
import it.tizianofagni.notiziepolitiche.ApplicationInitializerTask.ApplicationInitializerListener;
import it.tizianofagni.notiziepolitiche.dao.ArticleSearchParameters;
import it.tizianofagni.notiziepolitiche.dao.Category;
import it.tizianofagni.notiziepolitiche.dao.CategoryImageType;
import it.tizianofagni.notiziepolitiche.dao.DAOIOException;
import it.tizianofagni.notiziepolitiche.dao.NewsDAO;
import it.tizianofagni.notiziepolitiche.dao.ArticleSearchParameters.ArticleReadState;
import it.tizianofagni.notiziepolitiche.utils.GenericUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.admob.android.ads.AdListener;
import com.admob.android.ads.AdView;

public class CategoriesListActivity extends Activity {

	private final static class Initializer implements
			ApplicationInitializerListener {

		private WeakReference<CategoriesListActivity> act;

		private List<Category> categoriesList;

		public Initializer(CategoriesListActivity act, Object lastInstance) {
			this.act = new WeakReference<CategoriesListActivity>(act);
			categoriesList = lastInstance != null ? (List<Category>) lastInstance
					: null;
		}

		@Override
		public void applicationInitialized() {

			final CategoriesListActivity a = act.get();
			if (a == null)
				return;

			// Get the list of valid categories.
			a.inProgress = new HashMap<Integer, Integer>();
			try {
				a.categoriesList = categoriesList;
				if (a.categoriesList == null)
					a.categoriesList = a.getListOfCategories();
			} catch (Exception e) {
				Log.e(CategoriesListActivity.class.getName(),
						"Reading the categories list", e);
				throw new RuntimeException(e);
			}

			CategoryAdapter adapter = new CategoryAdapter(a,
					R.layout.political_party_item, a.categoriesList);

			// utilizzo dell'adapter
			ListView lv = ((ListView) a.findViewById(R.id.partiesListView));
			lv.setAdapter(adapter);
			lv.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View v,
						int position, long longID) {
					Category category = a.categoriesList.get(position);
					ArticleSearchParameters parms = new ArticleSearchParameters();
					parms.setReadState(ArticleReadState.BOTH_READ_AND_UNREAD);
					parms.setCategoryID(category.getCategoryID());
					Intent i = new Intent(a, NewsListActivity.class);
					i.putExtra(AppConstants.INTENT_SEARCH_PARMS_KEY, parms);
					a.startActivity(i);
				}
			});

			a.registerForContextMenu(lv);
			a.updateUnreadedArticles();
			a.attachToFeedsService();

			a.isActivityInitialized = true;

			// Eventually, handle search intent.
			a.handleSearchIntent(a.getIntent());
			
			// Show dialog for DB empty, if the case.
			showDlgDBEmpty(a);
		}
		
		
		private void showDlgDBEmpty(CategoriesListActivity a) {
			NotiziePoliticheApplication app = (NotiziePoliticheApplication) a
					.getApplication();
			ArticleSearchParameters parms = new ArticleSearchParameters();
			try {
				int num = app.getDAO().getArticlesSize(parms);
				if (num == 0) {
					AlertDialog.Builder builder = new AlertDialog.Builder(a);
					builder.setMessage(a.getString(R.string.no_data_in_db))
							.setPositiveButton(a.getString(R.string.ok_btn),
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {
											dialog.dismiss();
										}
									});
					AlertDialog alert = builder.create();
					alert.show();
				}
			} catch (Exception e) {
				Log.e(CategoriesListActivity.class.getName(),
						"Getting the number of stored articles", e);
				throw new RuntimeException(e);
			}
		}

	}

	private final class AdMobListener implements AdListener {

		private WeakReference<CategoriesListActivity> act;

		public AdMobListener(CategoriesListActivity act) {
			this.act = new WeakReference<CategoriesListActivity>(act);
		}

		@Override
		public void onFailedToReceiveAd(AdView arg0) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					CategoriesListActivity a = act.get();
					if (a == null)
						return;

					View v = a.findViewById(R.id.layout_main_ad);
					v.setVisibility(View.GONE);
					// Log.i(CategoriesListActivity.class.getCanonicalName(),"Ads not available");
				}
			});
		}

		@Override
		public void onFailedToReceiveRefreshedAd(AdView arg0) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					CategoriesListActivity a = act.get();
					if (a == null)
						return;

					View v = a.findViewById(R.id.layout_main_ad);
					v.setVisibility(View.GONE);
					// Log.i(CategoriesListActivity.class.getCanonicalName(),"Ads not available");
				}
			});
		}

		@Override
		public void onReceiveAd(AdView arg0) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					CategoriesListActivity a = act.get();
					if (a == null)
						return;

					View v = a.findViewById(R.id.layout_main_ad);
					v.setVisibility(View.VISIBLE);
					// Log.i(CategoriesListActivity.class.getCanonicalName(),"Ads available!");
				}
			});

		}

		@Override
		public void onReceiveRefreshedAd(AdView arg0) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					CategoriesListActivity a = act.get();
					if (a == null)
						return;

					View v = a.findViewById(R.id.layout_main_ad);
					v.setVisibility(View.VISIBLE);
					// Log.i(CategoriesListActivity.class.getCanonicalName(),"Ads available!");
				}
			});
		}

	}

	private final AdMobListener listenerAdMob = new AdMobListener(this);

	private static class ReadAllTask extends
			AsyncTask<Integer, Integer, Integer> {

		private WeakReference<CategoriesListActivity> act;
		private ProgressDialog progressDlg;

		public ReadAllTask(CategoriesListActivity act,
				ProgressDialog progressDlg) {
			this.act = new WeakReference<CategoriesListActivity>(act);
			this.progressDlg = progressDlg;
		}

		@Override
		protected Integer doInBackground(Integer... params) {

			CategoriesListActivity activity = act.get();
			if (activity == null)
				return 0;

			activity.markAllArticlesAsRead();
			return 0;
		}

		@Override
		protected void onPostExecute(Integer result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);

			CategoriesListActivity activity = act.get();
			if (activity == null)
				return;

			activity.updateUnreadedArticles();
			progressDlg.cancel();
		}
	}

	private static class FeedRetrieverServiceCallback extends
			IFeedRetrieverServiceCallback.Stub {

		private WeakReference<CategoriesListActivity> act;

		public FeedRetrieverServiceCallback(CategoriesListActivity act) {
			this.act = new WeakReference<CategoriesListActivity>(act);
		}

		@Override
		public void startRetrievingArticles(final int categoryID)
				throws RemoteException {
			final CategoriesListActivity a = act.get();
			if (a == null)
				return;

			a.runOnUiThread(new Runnable() {
				public void run() {
					try {
						NotiziePoliticheApplication app = (NotiziePoliticheApplication) a
								.getApplication();
						Category category = app.getDAO()
								.getCategory(categoryID);
						int idx = category.getCategoryID();
						a.inProgress.put(idx, idx);
						ListView lv = ((ListView) a
								.findViewById(R.id.partiesListView));
						CategoryAdapter ad = (CategoryAdapter) lv.getAdapter();
						ad.notifyDataSetChanged();
					} catch (Exception e) {
						Log.e(CategoriesListActivity.class.getName(),
								"Signal start of retrieving articles", e);
						throw new RuntimeException(e);
					}
				}
			});

		}

		@Override
		public void newArticlesRetrievedForCategory(final int categoryID,
				final int numNewArticles) throws RemoteException {
			final CategoriesListActivity a = act.get();
			if (a == null)
				return;

			a.runOnUiThread(new Runnable() {
				public void run() {
					NotiziePoliticheApplication app = (NotiziePoliticheApplication) a
							.getApplication();
					NewsDAO dao = app.getDAO();
					Category category = null;
					ArticleSearchParameters parms = new ArticleSearchParameters();
					parms.setCategoryID(categoryID);
					parms.setReadState(ArticleReadState.UNREAD);
					try {
						category = dao.getCategory(categoryID);
						int numArticles = app.getDAO().getArticlesSize(parms);
						a.updateNewArticles(category, numArticles);
						ListView lv = (ListView) a
								.findViewById(R.id.partiesListView);
						CategoryAdapter adp = (CategoryAdapter) lv.getAdapter();
						adp.notifyDataSetChanged();
					} catch (Exception e) {
						if (category != null)
							a.updateNewArticles(category, numNewArticles);
					}
				}
			});

		}

		@Override
		public void newArticlesRetrieved(int numNewArticles)
				throws RemoteException {
			// TODO Auto-generated method stub

		}

		@Override
		public void endRetrievingArticles(final int categoryID)
				throws RemoteException {
			final CategoriesListActivity a = act.get();
			if (a == null)
				return;

			a.runOnUiThread(new Runnable() {
				public void run() {
					try {
						NotiziePoliticheApplication app = (NotiziePoliticheApplication) a
								.getApplication();
						Category category = app.getDAO()
								.getCategory(categoryID);
						a.inProgress.remove(category.getCategoryID());
						ListView lv = ((ListView) a
								.findViewById(R.id.partiesListView));
						CategoryAdapter ad = (CategoryAdapter) lv.getAdapter();
						ad.notifyDataSetChanged();
					} catch (Exception e) {
						Log.e(CategoriesListActivity.class.getName(),
								"Signal end of retrieving articles", e);
						throw new RuntimeException(e);
					}
				}
			});
		}
	}

	private FeedRetrieverServiceCallback serviceCallback = new FeedRetrieverServiceCallback(
			this);

	private static class CategoryAdapter extends ArrayAdapter<Category> {

		private List<Category> items;

		private WeakReference<CategoriesListActivity> act;

		public CategoryAdapter(CategoriesListActivity act,
				int textViewResourceId, List<Category> items) {
			super(act, textViewResourceId, items);
			this.act = new WeakReference<CategoriesListActivity>(act);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			CategoriesListActivity activity = act.get();
			if (activity == null)
				return null;

			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) activity
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.political_party_item, null);
			}

			Category pp = items.get(position);
			ImageView categoryImage = (ImageView) v
					.findViewById(R.id.partyImage);
			categoryImage.setImageDrawable(activity.getResources().getDrawable(
					CategoryImageType.fromIntValue(pp.getCategoryImage())
							.getResourceIDValue()));

			TextView categoryName = (TextView) v.findViewById(R.id.partyName);
			categoryName.setText(pp.getName());

			TextView status = (TextView) v.findViewById(R.id.status);
			int numNewArticles = activity.getUnreadArticlesSize(pp);
			status.setText(activity.getCategoryStateMessage(numNewArticles));
			if (numNewArticles > 0) {
				status.setTypeface(null, Typeface.BOLD);
			} else {
				status.setTypeface(null, Typeface.NORMAL);
			}

			ProgressBar pb = (ProgressBar) v
					.findViewById(R.id.partyUpdateProgress);
			if (activity.inProgress.containsKey(pp.getCategoryID())) {
				pb.setIndeterminate(true);
				pb.setVisibility(View.VISIBLE);
			} else {
				pb.setIndeterminate(false);
				pb.setVisibility(View.GONE);
			}

			return v;
		}
	}

	private List<Category> categoriesList;

	private HashMap<Integer, Integer> inProgress;

	private boolean isActivityInitialized = false;

	private void updateNewArticles(final Category category,
			final int numNewArticles) {

		ListView lv = ((ListView) findViewById(R.id.partiesListView));
		for (int i = 0; i < lv.getChildCount(); i++) {
			View v = lv.getChildAt(i);
			TextView tvName = (TextView) v.findViewById(R.id.partyName);
			if (tvName.getText().equals(category.getName())) {
				TextView tvStatus = (TextView) v.findViewById(R.id.status);

				String msg = getCategoryStateMessage(numNewArticles);
				tvStatus.setText(msg);
				break;
			}
		}
	}

	private View findPoliticalCategoryView(Category category) {
		ListView lv = ((ListView) findViewById(R.id.partiesListView));
		if (lv == null)
			return null;
		int first = lv.getFirstVisiblePosition();
		int count = lv.getChildCount();
		for (int i = 0; i < count; i++) {
			Category p = (Category) lv.getItemAtPosition(first + i);
			if (p.getCategoryID() == category.getCategoryID()) {
				return lv.getChildAt(first + i);
			}
		}

		return null;
	}

	private String getCategoryStateMessage(int numNewArticles) {
		if (numNewArticles != 0) {
			return getResources().getString(R.string.party_status_new,
					numNewArticles);
		} else {
			return getResources().getString(R.string.party_status_nomessagges);
		}
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();

	}

	private void detachFromFeedsService() {
		try {
			NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
			if (app.isApplicationInitialized()) {
				if (app.getFeedsService() != null) {
					app.getFeedsService().removeCallback(serviceCallback);
				}
			}
		} catch (Exception e) {
			Log.e(CategoriesListActivity.class.getName(),
					"Error detaching from feeds service", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		// Update the number of articles unread.
		try {
			updateUnreadedArticles();
		} catch (Exception e) {

		}

		// Remove notification from status bar.
		NotificationManager manager = (NotificationManager) getApplication()
				.getApplicationContext().getSystemService(
						Context.NOTIFICATION_SERVICE);
		manager.cancel(AppConstants.NOTIFICATION_NEW_ARTICLES);
	}

	@Override
	protected void onPause() {

		super.onPause();

		// stopAdsMonitor();

		resetCategoryProgressBars();

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main_view);

		setupAdMob();
		
		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_DITHER);
		window.setFormat(PixelFormat.RGBA_8888);
		setVisible(true);

		String appName = GenericUtils.getAppName(this);
		setTitle(appName + " - "
				+ getResources().getString(R.string.app_title_categories));

		isActivityInitialized = false;

		// Wait for the application to init.
		ApplicationInitializerTask<CategoriesListActivity> task = new ApplicationInitializerTask<CategoriesListActivity>(
				this, new Initializer(this, getLastNonConfigurationInstance()));
		task.execute(0);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		if (!isActivityInitialized)
			return null;

		return this.categoriesList;
	}

	private void setupAdMob() {
		LinearLayout layoutMainAd = (LinearLayout) findViewById(R.id.layout_main_ad);
		AdView adView = (AdView) layoutMainAd.findViewById(R.id.main_ad);
		layoutMainAd.setVisibility(View.VISIBLE);
		adView.setRequestInterval(30);
		adView.setKeywords("politica notizie parlamento repubblica berlusconi fini casini rutelli bossi maroni presidente");
		adView.setAdListener(listenerAdMob);
		adView.setEnabled(true);
		/*if (GenericUtils.isLiteVersion(this)) {
			layoutMainAd.setVisibility(View.VISIBLE);
			adView.setRequestInterval(30);
			adView.setKeywords("calcio serie-a risultati juventus milan inter napoli roma lazio notizie partita classifica");
			adView.setAdListener(listenerAdMob);
			adView.setEnabled(true);
		} else {
			adView.setEnabled(false);
			layoutMainAd.setVisibility(View.GONE);
			layoutMainAd.removeView(adView);
		}*/
	}

	/*
	 * private void stopAdsMonitor() { LinearLayout lay = (LinearLayout)
	 * findViewById(R.id.layout_main_ad); AdWhirlNotifierLayout whirl =
	 * (AdWhirlNotifierLayout) lay.getChildAt(0); whirl.stopObserveAds(); }
	 * 
	 * private void startAdsMonitor() { LinearLayout lay = (LinearLayout)
	 * findViewById(R.id.layout_main_ad); AdWhirlNotifierLayout whirl =
	 * (AdWhirlNotifierLayout) lay.getChildAt(0); whirl.startObserveAds(); }
	 * 
	 * private void destroyAdsMonitor() { LinearLayout lay = (LinearLayout)
	 * findViewById(R.id.layout_main_ad); AdWhirlNotifierLayout whirl =
	 * (AdWhirlNotifierLayout) lay.getChildAt(0); whirl.destroyObserver();
	 * whirl.removeListener(listenerAds); }
	 * 
	 * 
	 * 
	 * private void setupAdWhirl() { // Setup AdWhirl
	 * AdWhirlManager.setConfigExpireTimeout(1000 * 60 * 5);
	 * 
	 * AdWhirlTargeting.setGender(AdWhirlTargeting.Gender.UNKNOWN);
	 * AdWhirlTargeting
	 * .setKeywords("notizie politica partito elezioni italia");
	 * AdWhirlTargeting.setTestMode(true); AdManager.setTestDevices(new String[]
	 * { AdManager.TEST_EMULATOR });
	 * 
	 * final int DIP_HEIGHT = 52; final float DENSITY =
	 * getResources().getDisplayMetrics().density; int scaledHeight = (int)
	 * (DENSITY * DIP_HEIGHT + 0.5f); LinearLayout layout = (LinearLayout)
	 * findViewById(R.id.layout_main_ad); AdWhirlNotifierLayout adWhirlLayout =
	 * new AdWhirlNotifierLayout(this, AppConstants.AD_WHIRL_KEY);
	 * adWhirlLayout.addListener(this.listenerAds);
	 * adWhirlLayout.setAdWhirlInterface(this);
	 * 
	 * Display d = this.getWindowManager().getDefaultDisplay();
	 * RelativeLayout.LayoutParams adWhirlLayoutParams = new
	 * RelativeLayout.LayoutParams( d.getWidth(), scaledHeight);
	 * layout.removeAllViews(); layout.addView(adWhirlLayout,
	 * adWhirlLayoutParams); layout.invalidate(); adWhirlLayout.initObserver();
	 * }
	 */

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.layout.menu_party_context, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		try {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
					.getMenuInfo();
			switch (item.getItemId()) {
			case R.id.menu_party_read_all:
				makeAllArticlesRead(info.id);
				return true;
			case R.id.menu_party_refresh:
				refreshArticles(info.id);
				return true;
			default:
				return super.onContextItemSelected(item);
			}
		} catch (Exception e) {
			Log.e(CategoriesListActivity.class.getName(),
					"Executing the operation", e);
			throw new RuntimeException(e);
		}
	}

	private void refreshArticles(long id) throws RemoteException {
		Category category = categoriesList.get((int) id);
		NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
		IFeedRetrieverService service = app.getFeedsService();
		if (!service.refresh(category.getCategoryID())) {
			CharSequence text = getResources().getString(
					R.string.no_network_connection);
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(this, text, duration);
			toast.show();
		}
	}

	private void makeAllArticlesRead(long id) {
		try {
			Category category = categoriesList.get((int) id);
			NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
			NewsDAO dao = app.getDAO();
			dao.markAllArticlesAsRead(category.getCategoryID());
			updateNewArticles(category, 0);
			ListView lv = (ListView) findViewById(R.id.partiesListView);
			CategoryAdapter adp = (CategoryAdapter) lv.getAdapter();
			adp.notifyDataSetChanged();
		} catch (Exception e) {
			Log.e("CategoriesListActivity", "Marking all articles as read", e);
			throw new RuntimeException(e);
		}

	}

	private int getUnreadArticlesSize(Category p) {
		try {
			NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
			NewsDAO dao = app.getDAO();
			ArticleSearchParameters parms = new ArticleSearchParameters();
			parms.setCategoryID(p.getCategoryID());
			parms.setReadState(ArticleReadState.UNREAD);
			return dao.getArticlesSize(parms);
		} catch (Exception e) {
			Log.e("CategoriesListActivity",
					"Getting the number of unread articles", e);
			return 0;
		}
	}

	private void updateUnreadedArticles() {
		try {
			NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
			NewsDAO dao = app.getDAO();
			if (!app.isApplicationInitialized())
				return;

			int numCats = dao.getCategoriesSize();
			List<Category> cats = dao.getCategories(0, numCats);
			for (Category category : cats) {
				ArticleSearchParameters parms = new ArticleSearchParameters();
				parms.setReadState(ArticleReadState.UNREAD);
				parms.setCategoryID(category.getCategoryID());
				int newArticles = dao.getArticlesSize(parms);
				updateNewArticles(category, newArticles);
			}

			ListView lv = (ListView) findViewById(R.id.partiesListView);
			BaseAdapter adp = (BaseAdapter) lv.getAdapter();
			if (adp != null)
				adp.notifyDataSetChanged();
		} catch (Exception e) {
			Log.e("CategoriesListActivity",
					"Updating number of unreaded articles", e);
		}
	}

	private List<Category> getListOfCategories() throws DAOIOException {
		NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
		NewsDAO dao = app.getDAO();
		int numCats = dao.getCategoriesSize();
		return new ArrayList<Category>(dao.getCategories(0, numCats));
	}

	private void initApplication() throws Exception {
		// First initialize the application data.
		NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
		if (!app.isApplicationInitialized()) {
			app.initApplication();
		}
	}

	private void attachToFeedsService() {
		// We want to monitor the service for as long as we are
		// connected to it.
		try {
			NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
			app.getFeedsService().addCallback(serviceCallback);
		} catch (RemoteException e) {
			// In this case the service has crashed before we could even
			// do anything with it; we can count on soon being
			// disconnected (and then reconnected if it can be restarted)
			// so there is no need to do anything here.
			Log.e(CategoriesListActivity.class.getName(),
					"Error registering callback on background service", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		detachFromFeedsService();

		NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
		app.getDAO().freeResources();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			finish();
			return true;
			// moveTaskToBack(true);
			// return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		findViewById(R.id.mainViewID).invalidate();
	}

	private void resetCategoryProgressBars() {
		if (inProgress != null)
			inProgress.clear();
		ListView lv = ((ListView) findViewById(R.id.partiesListView));
		BaseAdapter adp = (BaseAdapter) lv.getAdapter();
		if (adp != null)
			adp.notifyDataSetChanged();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.layout.menu_party_options, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		try {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
					.getMenuInfo();
			switch (item.getItemId()) {
			case R.id.menu_party_options_refresh_all:
				refreshAllArticles();
				return true;
			case R.id.menu_party_options_search:
				onSearchRequested();
				return true;
			case R.id.menu_party_options_read_all:
				ProgressDialog progressDlg = ProgressDialog.show(
						this,
						getResources().getString(R.string.waiting_messagge),
						getResources().getString(
								R.string.marking_all_messages_as_read), true);
				ReadAllTask task = new ReadAllTask(this, progressDlg);
				task.execute();
				return true;
			case R.id.menu_party_options_quit:
				quitApplication();
				return true;
			case R.id.menu_party_options_settings:
				showSettings();
				return true;

			default:
				return super.onContextItemSelected(item);
			}
		} catch (Exception e) {
			Log.e(CategoriesListActivity.class.getName(),
					"Error executing the operation", e);
			throw new RuntimeException(e);
		}
	}

	private void searchArticles(String query) {
		ArticleSearchParameters parms = new ArticleSearchParameters();
		parms.setQueryText(query);
		Intent i = new Intent(this, NewsListActivity.class);
		i.putExtra(AppConstants.INTENT_SEARCH_PARMS_KEY, parms);
		startActivity(i);
	}

	private void markAllArticlesAsRead() {
		try {
			NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
			NewsDAO dao = app.getDAO();
			dao.markAllArticlesAsRead();
			/*
			 * ArticleSearchParameters parms = new ArticleSearchParameters();
			 * parms.setReadState(ArticleReadState.UNREAD);
			 * 
			 * boolean done = false; int numArticlesInBlock = 10; while (!done)
			 * { int s = dao.getArticlesSize(parms); if (s == 0) { done = true;
			 * continue; } int toGet = Math.min(s, numArticlesInBlock);
			 * List<Article> articles = dao.getArticles(parms, 0, toGet);
			 * ArrayList<Integer> ids = new ArrayList<Integer>(); for (int i =
			 * 0; i < articles.size(); i++) {
			 * ids.add(articles.get(i).getArticleID());
			 * articles.get(i).setRead(true); } dao.setArticles(ids, articles);
			 * }
			 */

		} catch (Exception e) {
			Log.e(CategoriesListActivity.class.getName(),
					"Making all articles read", e);
		}

	}

	private void showSettings() {
		Intent intent = new Intent(getApplicationContext(),
				SettingsActivity.class);
		startActivity(intent);
	}

	private void quitApplication() {

		/*
		 * detachFromFeedsService();
		 * 
		 * NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
		 * 
		 * // Tell the service to quit. app.getFeedsService().quit();
		 * 
		 * 
		 * app.destroyApplication();
		 * 
		 * // Kill myself! Process.killProcess(Process.myPid());
		 */

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getResources().getString(R.string.exit_question))
				.setCancelable(false)
				.setPositiveButton("Si", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						try {
							detachFromFeedsService();

							// Clean application data and terminate feed
							// retriever
							// service.
							NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();

							// Tell the service to quit.
							app.getFeedsService().quit();

							// Destroy this appplication.
							app.destroyApplication();

							// Kill myself.
							Process.killProcess(Process.myPid());
						} catch (Exception e) {
							Log.e(CategoriesListActivity.class.getName(),
									"Closing application", e);
							throw new RuntimeException(e);
						}
					}
				})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void refreshAllArticles() throws RemoteException {
		NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
		if (!app.getFeedsService().refreshAll()) {
			CharSequence text = getResources().getString(
					R.string.no_network_connection);
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(this, text, duration);
			toast.show();
		}
	}

	private void showCreditsDialog() {

		final String credits = "Autore: Tiziano Fagni (<a href=\"mailto:tiziano@tizianofagni.it\">tiziano@tizianofagni.it</a>)<br>"
				+ "Contatti: <a href=\"http://www.tizianofagni.it\">http://www.tizianofagni.it</a><br>"
				+ "<br>Crediti<br><br>Le icone utilizzate all'interno del software fanno parte del set di "
				+ "icone \"AwOken\", accessibile al link&nbsp;<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\">"
				+ "<a href=\"http://alecive.deviantart.com/art/AwOken-Awesome-Token-1-5-163570862\">http://alecive.deviantart.com/art/AwOken-Awesome-Token-1-5-163570862</a>.<br>"
				+ "<br>Tutti i contenuti accessibili tramite il software appartengono ai rispettivi proprietari (agenzie di stampa, quotidiani online, "
				+ "ecc.)&nbsp; e il software si limita soltanto a segnalare le notizie.<br>";
		Spanned spannedText = Html.fromHtml(credits);

		Dialog dialog = new Dialog(this);

		dialog.setContentView(R.layout.about_dlg);
		dialog.setTitle(getResources().getString(R.string.software_info_title));
		TextView text = (TextView) dialog.findViewById(R.id.creditsApplication);
		text.setText(spannedText);

		View v = dialog.findViewById(R.id.layoutApplicationAbout);
		TextView titleApp = (TextView) v.findViewById(R.id.titleApplication);

		NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
		if (app.isLiteVersion()) {
			titleApp.setText(getResources().getString(
					R.string.software_info_about,
					getResources().getString(R.string.app_name_lite),
					AppConstants.applicationVersion.toString()));
		} else {
			titleApp.setText(getResources().getString(
					R.string.software_info_about,
					getResources().getString(R.string.app_name_full),
					AppConstants.applicationVersion.toString()));
		}

		dialog.show();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		handleSearchIntent(intent);
	}

	private void handleSearchIntent(Intent i) {
		if (!isActivityInitialized)
			return;

		if (Intent.ACTION_SEARCH.equals(i.getAction())) {
			NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
			if (app.isLiteVersion()) {
				GenericUtils.showDlgForProVersion(this);
				return;
			}

			String query = i.getStringExtra(SearchManager.QUERY);
			searchArticles(query);
		}
	}
}