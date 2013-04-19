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
import it.tizianofagni.notiziepolitiche.ApplicationInitializerTask;
import it.tizianofagni.notiziepolitiche.NotiziePoliticheApplication;
import it.tizianofagni.notiziepolitiche.ApplicationInitializerTask.ApplicationInitializerListener;
import it.tizianofagni.notiziepolitiche.dao.Article;
import it.tizianofagni.notiziepolitiche.dao.ArticleSearchParameters;
import it.tizianofagni.notiziepolitiche.dao.Category;
import it.tizianofagni.notiziepolitiche.dao.CategoryImageType;
import it.tizianofagni.notiziepolitiche.dao.NewsDAO;
import it.tizianofagni.notiziepolitiche.dao.ArticleSearchParameters.ArticleReadState;
import it.tizianofagni.notiziepolitiche.dao.ArticleSearchParameters.ArticleState;
import it.tizianofagni.notiziepolitiche.utils.GenericUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.admob.android.ads.AdListener;
import com.admob.android.ads.AdManager;
import com.admob.android.ads.AdView;

public class NewsListActivity extends Activity {

	private final static class Initializer implements
			ApplicationInitializerListener {

		private WeakReference<NewsListActivity> act;

		private ConfigChangeState oldState;

		public Initializer(NewsListActivity act, Object lastInstance) {
			this.act = new WeakReference<NewsListActivity>(act);
			oldState = (lastInstance != null) ? (ConfigChangeState) lastInstance
					: null;
		}

		@Override
		public void applicationInitialized() {
			
			final NewsListActivity a = act.get();
			if (a == null)
				return;
			
			Window window = a.getWindow();
			window.addFlags(WindowManager.LayoutParams.FLAG_DITHER);
			window.setFormat(PixelFormat.RGBA_8888);

			if (oldState != null) {
				ConfigChangeState state = oldState;
				a.articlesAvailable = state.articlesAvailable;
				a.readState = state.readState;
				a.articleState = state.articleState;
				a.fillListViewArticles(a.articlesAvailable);
				a.checkNoItems();
			} else {
				Bundle extras = a.getIntent().getExtras();
				ArticleSearchParameters parms = (ArticleSearchParameters) extras
						.get(AppConstants.INTENT_SEARCH_PARMS_KEY);
				if (parms == null)
					throw new RuntimeException(
							"The search parameters object is 'null'");

				a.readState = parms.getReadState();
				a.articleState = parms.getArticleState();
				a.loadArticles(parms);
			}

			a.initListViewArticles();

			a.isActivityInitialized = true;
		}

	}

	private static class ClickToBrowseListener implements OnItemClickListener {
		private final ArrayList<ArticleItem> result;
		private WeakReference<NewsListActivity> act;

		private ClickToBrowseListener(NewsListActivity act,
				ArrayList<ArticleItem> result) {
			this.act = new WeakReference<NewsListActivity>(act);
			this.result = result;
		}

		@Override
		public void onItemClick(AdapterView<?> parent, View v,
				final int position, long longID) {
			NewsListActivity activity = act.get();
			if (activity == null)
				return;

			ArticleItem arItem = result.get(position);
			if (arItem.isDayItem())
				return;
			Article ar = arItem.article;
			final CheckBox cbUnread = (CheckBox) v.findViewById(R.id.checkRead);
			cbUnread.setChecked(false);
			activity.makeArticleRead(ar, true, position);
			Intent i = new Intent(activity.getApplicationContext(),
					BrowseArticleActivity.class);
			i.putExtra(AppConstants.INTENT_BROWSE_URL_KEY, new String(ar
					.getUrl().toString()));
			activity.startActivity(i);
		}
	}

	private final class AdMobListener implements AdListener {

		private WeakReference<NewsListActivity> act;

		public AdMobListener(NewsListActivity act) {
			this.act = new WeakReference<NewsListActivity>(act);
		}

		@Override
		public void onFailedToReceiveAd(AdView arg0) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					NewsListActivity a = act.get();
					if (a == null)
						return;

					View v = a.findViewById(R.id.layout_news_ad);
					v.setVisibility(View.GONE);
					// Log.i(NewsListActivity.class.getCanonicalName(),
					// "Ads not available");
				}
			});
		}

		@Override
		public void onFailedToReceiveRefreshedAd(AdView arg0) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					NewsListActivity a = act.get();
					if (a == null)
						return;

					View v = a.findViewById(R.id.layout_news_ad);
					v.setVisibility(View.GONE);
					// Log.i(NewsListActivity.class.getCanonicalName(),"Ads not available");
				}
			});
		}

		@Override
		public void onReceiveAd(AdView arg0) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					NewsListActivity a = act.get();
					if (a == null)
						return;

					View v = a.findViewById(R.id.layout_news_ad);
					v.setVisibility(View.VISIBLE);
					// Log.i(NewsListActivity.class.getCanonicalName(),"Ads available!");
				}
			});

		}

		@Override
		public void onReceiveRefreshedAd(AdView arg0) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					NewsListActivity a = act.get();
					if (a == null)
						return;

					View v = a.findViewById(R.id.layout_news_ad);
					v.setVisibility(View.VISIBLE);
					// Log.i(NewsListActivity.class.getCanonicalName(),"Ads available!");
				}
			});
		}

	}

	private final AdMobListener listenerAdMob = new AdMobListener(this);

	private static class ConfigChangeState {
		ArrayList<ArticleItem> articlesAvailable;
		private ArticleReadState readState;
		private ArticleState articleState;
	}

	private static class ArticleItem {
		private Article article;
		private Date day;

		public boolean isDayItem() {
			return day != null;
		}

		public Article getArticle() {
			return article;
		}

		public void setArticle(Article article) {
			this.article = article;
		}

		public Date getDay() {
			return day;
		}

		public void setDay(Date day) {
			this.day = day;
		}

	}

	private static class ViewDayCache {
		private View baseView;
		private TextView tvDay;

		public ViewDayCache(View baseView) {
			this.baseView = baseView;
		}

		public TextView getTextViewDay() {
			if (tvDay == null) {
				tvDay = (TextView) baseView.findViewById(R.id.newsItemDay);
			}

			return tvDay;
		}
	}

	private static class ViewArticleCache {
		private View baseView;
		private TextView tvTitle;
		private TextView tvDescription;
		private TextView tvDay;
		private TextView tvYear;
		private TextView tvTime;
		private CheckBox cbRead;

		public ViewArticleCache(View baseView) {
			this.baseView = baseView;
		}

		public TextView getTitle() {
			if (tvTitle == null) {
				tvTitle = (TextView) baseView.findViewById(R.id.newsTitle);
			}

			return tvTitle;
		}

		public TextView getDescription() {
			if (tvDescription == null) {
				tvDescription = (TextView) baseView
						.findViewById(R.id.newsDescription);
			}

			return tvDescription;
		}

		public TextView getDay() {
			if (tvDay == null) {
				tvDay = (TextView) baseView.findViewById(R.id.newsDay);
			}

			return tvDay;
		}

		public TextView getYear() {
			if (tvYear == null) {
				tvYear = (TextView) baseView.findViewById(R.id.newsYear);
			}

			return tvYear;
		}

		public TextView getTime() {
			if (tvTime == null) {
				tvTime = (TextView) baseView.findViewById(R.id.newsTime);
			}

			return tvTime;
		}

		public CheckBox getCheckRead() {
			if (cbRead == null) {
				cbRead = (CheckBox) baseView.findViewById(R.id.checkRead);
			}

			return cbRead;
		}
	}

	private static class ArticleAdapter extends BaseAdapter {

		private final int VIEW_DAY = 0;
		private final int VIEW_ARTICLE = 1;

		private WeakReference<NewsListActivity> act;

		public ArticleAdapter(NewsListActivity act) {
			this.act = new WeakReference<NewsListActivity>(act);
		}

		@Override
		public int getCount() {
			NewsListActivity a = act.get();
			if (a == null)
				return 0;

			return a.articlesAvailable.size();
		}

		@Override
		public Object getItem(int position) {
			NewsListActivity a = act.get();
			if (a == null)
				return null;

			return a.articlesAvailable.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			NewsListActivity a = act.get();
			if (a == null)
				return null;

			ArticleItem aiItem = a.articlesAvailable.get(position);
			if (aiItem.isDayItem()) {
				return getDayViewInternal(a, position, convertView, parent);
			} else {
				return getArticleViewInternal(a, position, convertView, parent);
			}

		}

		@Override
		public boolean isEnabled(int position) {

			NewsListActivity a = act.get();
			if (a == null)
				return false;

			ArticleItem aiItem = a.articlesAvailable.get(position);
			if (aiItem.isDayItem())
				return false;
			else
				return true;
		}

		private View getNoArticlesView() {
			LayoutInflater inflater = act.get().getLayoutInflater();
			View v = inflater.inflate(R.layout.info_no_items, null);
			TextView tv = (TextView) v.findViewById(R.id.infoNoItemsText);
			tv.setText(act.get().getResources()
					.getString(R.string.no_news_shown));
			return tv;
		}

		private View getArticleViewInternal(final NewsListActivity ar,
				final int position, View convertView, ViewGroup parent) {

			View rowView = convertView;
			ViewArticleCache viewCache;
			if (rowView == null) {
				LayoutInflater inflater = ar.getLayoutInflater();
				rowView = inflater.inflate(R.layout.news_item, null);
				viewCache = new ViewArticleCache(rowView);
				rowView.setTag(viewCache);
			} else {
				viewCache = (ViewArticleCache) rowView.getTag();
			}

			NotiziePoliticheApplication app = (NotiziePoliticheApplication) ar.getApplication();
			SharedPreferences prefs = app.getPreferences();
			if (prefs == null)
				return rowView;

			NewsListActivity activity = act.get();
			ArticleItem aiItem = ar.articlesAvailable.get(position);
			Article a = aiItem.article;
			if (a != null) {
				TextView tt = viewCache.getTitle();
				TextView bt = viewCache.getDescription();

				if (tt != null) {
					tt.setText(a.getTitle());
					int s = Integer.parseInt(prefs.getString(app.getResources()
							.getString(R.string.keyNewsArticleTitleFontSize),
							"" + AppConstants.DEFAULT_ARTICLE_TITLE_FONT_SIZE));
					tt.setTextSize(s);
					
					if (a.isRead())
						tt.setTextColor(Color.parseColor(activity.textReadColor));
					else
						tt.setTextColor(Color.parseColor(activity.textUnreadColor));
				}
				if (bt != null) {
					String desc = a.getDescription();
					bt.setText(desc);
					int s = Integer
							.parseInt(prefs
									.getString(
											app.getResources()
													.getString(
															R.string.keyNewsArticleDescriptionFontSize),
											""
													+ AppConstants.DEFAULT_ARTICLE_DESCRIPTION_FONT_SIZE));
					bt.setTextSize(s);
					if (a.isRead())
						bt.setTextColor(Color.parseColor(activity.textReadColor));
					else
						bt.setTextColor(Color.parseColor(activity.textUnreadColor));
				}

				GregorianCalendar gc = new GregorianCalendar();
				gc.setTime(a.getDate());
				TextView tvDay = viewCache.getDay();
				tvDay.setText(ar.formatNumber(gc.get(Calendar.DAY_OF_MONTH))
						+ " " + ar.months[gc.get(Calendar.MONTH)]);
				TextView tvYear = viewCache.getYear();
				tvYear.setText("" + gc.get(Calendar.YEAR));
				TextView tvTime = viewCache.getTime();
				tvTime.setText(ar.formatNumber(gc.get(Calendar.HOUR_OF_DAY))
						+ ":" + ar.formatNumber(gc.get(Calendar.MINUTE)));

				final CheckBox cbUnread = viewCache.getCheckRead();
				final Article art = a;
				if (art.isRead())
					cbUnread.setChecked(false);
				else
					cbUnread.setChecked(true);
				cbUnread.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {

						if (cbUnread.isChecked()) {
							if (!art.isRead())
								return;

							ar.makeArticleRead(art, false, position);
						} else {
							if (art.isRead())
								return;

							ar.makeArticleRead(art, true, position);
						}
					}
				});
			}
			return rowView;
		}

		private View getDayViewInternal(NewsListActivity a, int position,
				View convertView, ViewGroup parent) {

			View rowView = convertView;
			ViewDayCache viewCache;
			if (rowView == null) {
				LayoutInflater inflater = a.getLayoutInflater();
				rowView = inflater.inflate(R.layout.news_item_day, null);
				viewCache = new ViewDayCache(rowView);
				rowView.setTag(viewCache);
			} else {
				viewCache = (ViewDayCache) rowView.getTag();
			}

			TextView tvDay = viewCache.getTextViewDay();
			ArticleItem aiItem = a.articlesAvailable.get(position);
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTime(aiItem.day);
			String str = act.get().days[gc.get(Calendar.DAY_OF_WEEK) - 1] + " "
					+ gc.get(Calendar.DAY_OF_MONTH) + "/"
					+ (gc.get(Calendar.MONTH) + 1) + "/"
					+ gc.get(Calendar.YEAR);
			tvDay.setText(str);

			return rowView;
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getItemViewType(int position) {
			NewsListActivity a = act.get();
			if (a == null)
				return 0;

			if (a.articlesAvailable.get(position).isDayItem())
				return VIEW_DAY;
			else
				return VIEW_ARTICLE;
		}
	}

	private static class RetrieveArticlesTask extends
			AsyncTask<ArticleSearchParameters, Integer, ArrayList<Article>> {

		private WeakReference<NewsListActivity> act;

		public RetrieveArticlesTask(NewsListActivity act) {
			this.act = new WeakReference<NewsListActivity>(act);
		}

		protected ArrayList<Article> doInBackground(
				ArticleSearchParameters... urls) {
			try {
				NewsListActivity activity = act.get();
				if (activity == null)
					return null;
				NotiziePoliticheApplication app = (NotiziePoliticheApplication) activity
						.getApplication();
				NewsDAO dao = app.getDAO();
				ArticleSearchParameters parms = urls[0];
				int s = dao.getArticlesSize(parms);
				List<Article> articles = dao.getArticles(parms, 0, s);
				ArrayList<Article> ret = new ArrayList<Article>();
				ret.addAll(articles);
				return ret;
			} catch (Exception e) {
				throw new RuntimeException("Retrieving articles", e);
			}
		}

		protected void onProgressUpdate(Integer... progress) {

		}

		protected void onPostExecute(ArrayList<Article> result) {
			if (result == null) {
				// Skip silently...

			} else {
				NewsListActivity activity = act.get();
				if (activity == null)
					return;
				act.clear();

				activity.articlesAvailable.clear();

				fillArticlesWithDays(activity.articlesAvailable, result);
				result.clear();

				activity.fillListViewArticles(activity.articlesAvailable);
				activity.checkNoItems();

				ProgressDialog dlg = activity.progressDlg.get();
				if (dlg != null && dlg.isShowing()) {
					dlg.dismiss();
				}

			}

		}

		private void fillArticlesWithDays(
				ArrayList<ArticleItem> articlesAvailable,
				ArrayList<Article> result) {
			Date curDate = new GregorianCalendar(1970, 1, 1).getTime();
			GregorianCalendar gc = new GregorianCalendar();
			for (Article ar : result) {
				Date dateArticle = ar.getDate();
				GregorianCalendar gcArticlePrecise = new GregorianCalendar();
				gcArticlePrecise.setTime(dateArticle);
				gc.clear();
				gc.set(Calendar.DAY_OF_MONTH,
						gcArticlePrecise.get(Calendar.DAY_OF_MONTH));
				gc.set(Calendar.MONTH, gcArticlePrecise.get(Calendar.MONTH));
				gc.set(Calendar.YEAR, gcArticlePrecise.get(Calendar.YEAR));

				if (!curDate.equals(gc.getTime())) {
					ArticleItem item = new ArticleItem();
					item.day = gc.getTime();
					item.article = null;
					curDate = gc.getTime();
					articlesAvailable.add(item);

					item = new ArticleItem();
					item.day = null;
					item.article = ar;
					articlesAvailable.add(item);
				} else {
					ArticleItem item = new ArticleItem();
					item.day = null;
					item.article = ar;
					articlesAvailable.add(item);
				}
			}
		}

	}

	
	private final String textReadColor = "#525252";
	private final String textUnreadColor = "#000000";
	private boolean isActivityInitialized;
	private WeakReference<ProgressDialog> progressDlg;

	private final String[] months = new String[] { "Gen", "Feb", "Mar", "Apr",
			"Mag", "Giu", "Lug", "Ago", "Set", "Ott", "Nov", "Dic" };
	private final String[] days = new String[] { "Dom", "Lun", "Mar", "Mer",
			"Gio", "Ven", "Sab" };

	private ArrayList<ArticleItem> articlesAvailable;

	private ArticleReadState readState;
	private ArticleState articleState;

	private void makeArticleRead(Article ar, boolean read, int position) {
		try {
			ar.setRead(read);
			NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
			NewsDAO dao = app.getDAO();
			dao.setArticle(ar.getArticleID(), ar);
			
			
			if ((read && readState == ArticleReadState.UNREAD)
					|| (!read && readState == ArticleReadState.READ)) {
				int prevPosition = position - 1;
				int nextPosition = position;
				articlesAvailable.remove(position);
				ArticleItem arItem = articlesAvailable.get(prevPosition);
				if (arItem.isDayItem()) {
					if (nextPosition < articlesAvailable.size()
							&& articlesAvailable.get(nextPosition).isDayItem()) {
						articlesAvailable.remove(prevPosition);
					} else if (nextPosition >= articlesAvailable.size()) {
						articlesAvailable.remove(prevPosition);
					}

					checkNoItems();
				}
				
				
				/*ArticleAdapter adapter = new ArticleAdapter(
						NewsListActivity.this);
				ListView lv = (ListView) findViewById(R.id.newsListView);
				lv.setAdapter(adapter);*/
			}
			
			ListView lv = (ListView) findViewById(R.id.newsListView);
			BaseAdapter adp = (BaseAdapter) lv.getAdapter();
			adp.notifyDataSetChanged();

		} catch (Exception e) {
			Log.e("NewsListActivity", "Error setting state of an article", e);
		}
	}

	private void fillListViewArticles(final ArrayList<ArticleItem> result) {
		// Add the articles to listview.
		ArticleAdapter adapter = new ArticleAdapter(NewsListActivity.this);
		((ListView) findViewById(R.id.newsListView)).setAdapter(adapter);

		ListView lv = (ListView) findViewById(R.id.newsListView);
		lv.setOnItemClickListener(new ClickToBrowseListener(this, result));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.news_view);
		
		String appName = GenericUtils.getAppName(this);
		setTitle(appName);
		
		setupAdMob();
		
		// DEBUG
		//GenericUtils.logHeap(this.getClass());

		isActivityInitialized = false;
		articlesAvailable = null;
		articleState = ArticleState.VALID;
		readState = ArticleReadState.BOTH_READ_AND_UNREAD;

		// Wait for the application to init.
		ApplicationInitializerTask<NewsListActivity> task = new ApplicationInitializerTask<NewsListActivity>(
				this, new Initializer(this, getLastNonConfigurationInstance()));
		task.execute(0);
	}


	private void setupAdMob() {
		LinearLayout layoutMainAd = (LinearLayout) findViewById(R.id.layout_news_ad);
		AdView adView = (AdView) layoutMainAd.findViewById(R.id.news_ad);
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
			layoutMainAd.setVisibility(View.GONE);
			adView.setEnabled(false);
		}*/
	}

	private void loadArticles(ArticleSearchParameters parms) {
		articlesAvailable = new ArrayList<ArticleItem>();

		// Show the waiting dialog.
		progressDlg = new WeakReference<ProgressDialog>(ProgressDialog.show(
				this, getResources().getString(R.string.waiting_messagge),
				getResources().getString(R.string.retrieving_news_messagge),
				true));
		progressDlg.get().setOwnerActivity(this);

		setTitle(getViewTitle(parms));

		// Load all articles.
		RetrieveArticlesTask task = new RetrieveArticlesTask(this);
		task.execute(parms);
	}

	private void initListViewArticles() {
		ListView lv = (ListView) findViewById(R.id.newsListView);
		registerForContextMenu(lv);
	}

	private String formatNumber(int number) {
		StringBuilder sb = new StringBuilder();
		Formatter formatter = new Formatter(sb, Locale.ITALIAN);
		formatter.format("%1$02d", number);
		return sb.toString();
	}

	private String getViewTitle(ArticleSearchParameters parms) {
		try {
			NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
			NewsDAO dao = app.getDAO();
			Resources res = getResources();
			if (parms.getCategoryID() == -1) {
				return getAppName()
						+ " - Lista articoli richiesti";
			} else {
				Category party = dao.getCategory(parms.getCategoryID());
				return getAppName() + " - "
						+ party.getName();
			}
		} catch (Exception e) {
			throw new RuntimeException("Getting the title of the view", e);
		}
	}
	
	private String getAppName() {
		NotiziePoliticheApplication app =(NotiziePoliticheApplication) getApplication();
		if (app.isLiteVersion()) {
			return getResources().getString(R.string.app_name_lite);
		} else {
			return getResources().getString(R.string.app_name_full);
		}
	}
	

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.layout.menu_article_context, menu);
		MenuItem item = menu.getItem(0);
		if (articleState == ArticleState.VALID) {
			item.setTitle(getResources().getString(R.string.menu_article_trash));
		} else {
			item.setTitle(getResources().getString(
					R.string.menu_article_untrash));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.layout.menu_article_options, menu);

		return true;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.menu_article_trash:
			if (articleState == ArticleState.VALID)
				trashArticle(info.id);
			else
				untrashArticle(info.id);
			return true;
		case R.id.menu_article_share:
			shareArticle(info.id);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	private void shareArticle(long id) {
		
		NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
		if (app.isLiteVersion()) {
			GenericUtils.showDlgForProVersion(this);
			return;
		}
		
		ArticleItem arItem = articlesAvailable.get((int) id);

		Intent i = new Intent(android.content.Intent.ACTION_SEND);
		i.setType("text/plain");
		i.putExtra(Intent.EXTRA_SUBJECT,
				getResources().getString(R.string.share_subject));
		i.putExtra(Intent.EXTRA_TEXT, arItem.getArticle().getUrl().toString());
		startActivity(Intent.createChooser(i,
				getResources().getString(R.string.share_title)));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.menu_article_options_read_state:
			chooseArticlesReadState();
			return true;
		case R.id.menu_article_options_state:
			chooseArticlesState();
			return true;

		default:
			return super.onContextItemSelected(item);
		}
	}

	private void chooseArticlesState() {
		Resources r = getResources();
		final String items[] = {
				r.getString(R.string.menu_article_options_state_untrashed),
				r.getString(R.string.menu_article_options_state_trashed) };

		Bundle extras = getIntent().getExtras();
		final ArticleSearchParameters parms = (ArticleSearchParameters) extras
				.get(AppConstants.INTENT_SEARCH_PARMS_KEY);

		final AlertDialog.Builder ab = new AlertDialog.Builder(
				NewsListActivity.this);
		ab.setTitle(r.getString(R.string.menu_article_options_state_title));
		ab.setSingleChoiceItems(items, getArticleStateValue(articleState),
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int whichButton) {
						if (whichButton == 0) {
							articleState = ArticleState.VALID;
						} else if (whichButton == 1) {
							articleState = ArticleState.TRASHED;
						} else
							throw new RuntimeException(
									"Bug in code. Invalid condition!");

						parms.setReadState(readState);
						parms.setArticleState(articleState);

						dialog.dismiss();
						closeOptionsMenu();
						loadArticles(parms);
					}
				});
		ab.show();
	}

	private int getArticleStateValue(ArticleState state) {
		if (state == ArticleState.VALID)
			return 0;
		else if (state == ArticleState.TRASHED)
			return 1;
		else
			return 0;
	}

	private int getArticleReadStateValue(ArticleReadState state) {
		if (state == ArticleReadState.BOTH_READ_AND_UNREAD)
			return 2;
		else if (state == ArticleReadState.READ)
			return 0;
		else
			return 1;
	}

	private void chooseArticlesReadState() {
		Resources r = getResources();
		final String items[] = {
				r.getString(R.string.menu_article_options_read_state_read),
				r.getString(R.string.menu_article_options_read_state_unread),
				r.getString(R.string.menu_article_options_read_state_both) };

		Bundle extras = getIntent().getExtras();
		final ArticleSearchParameters parms = (ArticleSearchParameters) extras
				.get(AppConstants.INTENT_SEARCH_PARMS_KEY);

		final AlertDialog.Builder ab = new AlertDialog.Builder(
				NewsListActivity.this);
		ab.setTitle(r.getString(R.string.menu_article_options_read_state_title));
		ab.setSingleChoiceItems(items, getArticleReadStateValue(readState),
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int whichButton) {
						if (whichButton == 0) {
							readState = ArticleReadState.READ;
						} else if (whichButton == 1) {
							readState = ArticleReadState.UNREAD;
						} else {
							readState = ArticleReadState.BOTH_READ_AND_UNREAD;
						}

						parms.setReadState(readState);
						parms.setArticleState(articleState);

						dialog.dismiss();
						closeOptionsMenu();
						loadArticles(parms);
					}
				});
		ab.show();
	}

	private void trashArticle(long id) {
		try {
			NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
			NewsDAO dao = app.getDAO();
			ArticleItem arItem = articlesAvailable.get((int) id);
			if (arItem.isDayItem())
				return;

			Article ar = arItem.article;
			ar.setRemoved(true);
			dao.setArticle(ar.getArticleID(), ar);

			int pos = (int) id;
			int prevPosition = pos - 1;
			int nextPosition = pos;
			articlesAvailable.remove(pos);
			arItem = articlesAvailable.get(prevPosition);
			if (arItem.isDayItem()) {
				if (nextPosition < articlesAvailable.size()
						&& articlesAvailable.get(nextPosition).isDayItem()) {
					articlesAvailable.remove(prevPosition);
				} else if (nextPosition >= articlesAvailable.size()) {
					articlesAvailable.remove(prevPosition);
				}
				checkNoItems();
			}

			ListView lv = (ListView) findViewById(R.id.newsListView);
			BaseAdapter adp = (BaseAdapter) lv.getAdapter();
			adp.notifyDataSetChanged();
		} catch (Exception e) {
			Log.e("NewsListActivity", "Trashing article", e);
			throw new RuntimeException(e);
		}
	}

	private void untrashArticle(long id) {
		try {
			NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
			NewsDAO dao = app.getDAO();
			ArticleItem arItem = articlesAvailable.get((int) id);
			if (arItem.isDayItem())
				return;

			Article ar = arItem.article;
			ar.setRemoved(false);
			dao.setArticle(ar.getArticleID(), ar);

			int pos = (int) id;
			int prevPosition = pos - 1;
			int nextPosition = pos;
			articlesAvailable.remove(pos);
			arItem = articlesAvailable.get(prevPosition);
			if (arItem.isDayItem()) {
				ListView lv = (ListView) findViewById(R.id.newsListView);
				if (nextPosition < articlesAvailable.size()
						&& articlesAvailable.get(nextPosition).isDayItem()) {
					articlesAvailable.remove(prevPosition);
					lv.invalidate();
				} else if (nextPosition >= articlesAvailable.size()) {
					articlesAvailable.remove(prevPosition);
					lv.invalidate();
				}

				checkNoItems();
			}

			ListView lv = (ListView) findViewById(R.id.newsListView);
			BaseAdapter adp = (BaseAdapter) lv.getAdapter();
			adp.notifyDataSetChanged();
		} catch (Exception e) {
			Log.e("NewsListActivity", "Trashing article", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			finish();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	private void checkNoItems() {
		if (articlesAvailable.size() == 0) {
			View v = findViewById(R.id.infoItem);
			v.setVisibility(View.VISIBLE);
		} else {
			View v = findViewById(R.id.infoItem);
			v.setVisibility(View.GONE);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		/*
		 * if (progressDlg != null) { ProgressDialog dlg = progressDlg.get(); if
		 * (dlg != null) dlg.dismiss(); }
		 */

		NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
		app.getDAO().freeResources();

		// DEBUG
		//GenericUtils.logHeap(this.getClass());
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		if (!isActivityInitialized)
			return null;

		ConfigChangeState state = new ConfigChangeState();

		state.articlesAvailable = articlesAvailable;
		state.articleState = articleState;
		state.readState = readState;
		return state;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}
}
