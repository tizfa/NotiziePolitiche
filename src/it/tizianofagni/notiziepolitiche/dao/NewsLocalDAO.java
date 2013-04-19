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

package it.tizianofagni.notiziepolitiche.dao;

import it.tizianofagni.notiziepolitiche.R;
import it.tizianofagni.notiziepolitiche.dao.ArticleSearchParameters.ArticleReadState;
import it.tizianofagni.notiziepolitiche.dao.ArticleSearchParameters.ArticleState;
import it.tizianofagni.notiziepolitiche.utils.GenericUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.StringTokenizer;

import org.jsoup.Jsoup;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

public class NewsLocalDAO implements NewsDAO {

	/**
	 * The directory in the SD card where to store application data.
	 */
	private final static String APPLICATION_DATA_DIR = Environment
			.getExternalStorageDirectory().toString();

	private SQLiteDatabase db;

	private NewsDBHelper dbHelper;


	public NewsLocalDAO(Context ctx) throws DAOIOException {
		dbHelper = new NewsDBHelper(ctx);
		dbHelper.setStorageOnCard(true);
		String dbDir = APPLICATION_DATA_DIR+"/";
		if (GenericUtils.isLiteVersion(ctx))
			dbDir += ctx.getResources().getString(R.string.app_name_lite);
		else
			dbDir += ctx.getResources().getString(R.string.app_name_full);
		dbDir += "/db";
		
		dbHelper.setStoragePath(dbDir);
	}

	private void checkExternalStorageState() throws DAOIOException {
		String state = Environment.getExternalStorageState();

		if (!Environment.MEDIA_MOUNTED.equals(state)) {
			// We can not read and write the media
			Log.e(getClass().getName(), "The SD card has not read/write access");
			/*
			 * AlertDialog dlg = new AlertDialog.Builder(ctx).create();
			 * dlg.setMessage(ctx.getString(R.string.sd_card_not_available));
			 * dlg.setButton(Dialog.BUTTON_POSITIVE, "Ok", new
			 * DialogInterface.OnClickListener() {
			 * 
			 * public void onClick(DialogInterface dialog, int which) {
			 * System.exit(-1); } });
			 * 
			 * dlg.setCancelable(false); dlg.show();
			 */
			throw new DAOIOException("The SD card has not read/write access");
		}
	}

	public void init(boolean forceCreation) throws DAOIOException {
		try {
			if (dbHelper.isOpen())
				dbHelper.close();
			dbHelper.setForceCreation(forceCreation);
			checkExternalStorageState();
			dbHelper.open();
			db = dbHelper.getWritableDatabase();
		} catch (Exception e) {
			throw new DAOIOException("Initializing DAO", e);
		}
	}

	public void release() throws DAOIOException {
		try {
			if (!dbHelper.isOpen())
				return;

			dbHelper.close();
			db = null;

		} catch (Exception e) {
			throw new DAOIOException("Releasing DAO resources", e);
		}
	}

	public int addArticle(Article article) throws DAOIOException {
		try {
			// First add article to DB.
			int articleID = addArticleInternal(article, true);
			return articleID;
		} catch (Exception e) {
			throw new DAOIOException("Adding an article to Lucene index", e);
		}
	}

	private int addArticleInternal(Article article, boolean commit)
			throws DAOIOException {
		try {
			// First add article to DB.
			int articleID = addArticleDBInternal(article, commit);
			article.setArticleID(articleID);

			return articleID;
		} catch (Exception e) {
			throw new DAOIOException("Adding an article to Lucene index", e);
		}
	}

	private int addArticleDBInternal(Article article, boolean inTransaction)
			throws DAOIOException {
		if (inTransaction)
			db.beginTransaction();
		try {

			// First add article to "articles" table.
			ContentValues values = new ContentValues();
			values.put("title", article.getTitle());
			values.put("description", getTextContent(article.getDescription()));
			// values.put("description", article.getDescription());
			values.put("url", article.getUrl().toString());
			values.put("published_date", article.getDate().getTime());
			values.put("is_read", article.isRead() ? "1" : "0");
			values.put("is_removed", article.isRemoved() ? "1" : "0");
			values.put("preferred", article.isPreferred() ? "1" : "0");
			values.put("feed_url_md5", article.getFeedURLMd5());
			int idArticle = (int) db.insert("articles", null, values);
			if (idArticle == -1)
				return -1;

			if (inTransaction)
				db.setTransactionSuccessful();
			return idArticle;
		} finally {
			if (inTransaction)
				db.endTransaction();
		}
	}

	private String getTextContent(String htmlContent) {
		org.jsoup.nodes.Document doc = Jsoup.parse(htmlContent);
		return doc.body().text();
	}

	public List<Integer> addArticles(List<Article> articles,
			boolean allowDuplicates) throws DAOIOException {
		try {
			ArrayList<Integer> ids = new ArrayList<Integer>();
			db.beginTransaction();
			for (Article article : articles) {
				if (!allowDuplicates) {
					Article res = getArticle(article.getFeedURLMd5());
					if (res != null)
						continue;
				}
				ids.add(addArticleInternal(article, false));
			}
			db.setTransactionSuccessful();
			db.endTransaction();

			return ids;
		} catch (Exception e) {
			throw new DAOIOException("Adding articles", e);
		}
	}

	private Article buildArticle(Cursor c) throws Exception {
		Article a = new Article();
		a.setArticleID(c.getInt(c.getColumnIndexOrThrow("article_id")));
		Long time = c.getLong(c.getColumnIndex("published_date"));
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTimeInMillis(time);
		a.setDate(gc.getTime());
		String descr = c.getString(c.getColumnIndex("description"));
		descr = descr.substring(0, Math.min(250, descr.length()));
		a.setDescription(descr);
		a.setRead(c.getInt(c.getColumnIndex("is_read")) == 1 ? true : false);
		a.setTitle(c.getString(c.getColumnIndex("title")));
		a.setUrl(new URL(c.getString(c.getColumnIndex("url"))));
		a.setFeedURLMd5(c.getString(c.getColumnIndex("feed_url_md5")));
		a.setPreferred(c.getInt(c.getColumnIndex("preferred")) == 1 ? true
				: false);
		a.setRemoved(c.getInt(c.getColumnIndex("is_removed")) == 1 ? true
				: false);

		return a;
	}

	public Article getArticle(int articleID) {
		Cursor c = db.query("articles", null, "article_id=" + articleID, null,
				null, null, null);
		try {
			if (c.getCount() == 0)
				return null;

			if (!c.moveToFirst())
				return null;
			Article a = buildArticle(c);
			return a;
		} catch (Exception e) {
			throw new RuntimeException("Bug in code", e);
		} finally {
			c.close();
		}
	}

	@Override
	public Article getArticle(String articleURIMd5) {
		Cursor c = db.query("articles", null, "feed_url_md5=?",
				new String[] { articleURIMd5 }, null, null, null);
		try {
			if (c.getCount() == 0)
				return null;

			if (!c.moveToFirst())
				return null;
			Article a = buildArticle(c);
			return a;
		} catch (Exception e) {
			throw new RuntimeException("Bug in code", e);
		} finally {
			c.close();
		}
	}

	public boolean setArticle(int articleID, Article article)
			throws DAOIOException {

		try {
			boolean res = setArticleDB(articleID, article, true);
			return res;
		} catch (Exception e) {
			throw new DAOIOException("Setting article", e);
		}
	}

	public List<Boolean> setArticles(List<Integer> articleIDs,
			List<Article> articles) throws DAOIOException {

		db.beginTransaction();
		try {
			ArrayList<Boolean> results = new ArrayList<Boolean>();
			int cont = 0;
			boolean res = false;
			for (int articleID : articleIDs) {
				Article article = articles.get(cont++);
				res = setArticleDB(articleID, article, false);
				results.add(res);
			}

			db.setTransactionSuccessful();

			return results;

		} catch (Exception e) {
			throw new DAOIOException("Setting articles", e);
		} finally {
			db.endTransaction();
		}
	}

	private boolean setArticleDB(int articleID, Article article,
			boolean inTransaction) throws DAOIOException {
		if (inTransaction)
			db.beginTransaction();
		try {

			article.setArticleID(articleID);

			// First add article to "articles" table.
			ContentValues values = new ContentValues();
			values.put("title", article.getTitle());
			values.put("description", article.getDescription());
			values.put("url", article.getUrl().toString());
			values.put("published_date", article.getDate().getTime());
			values.put("is_read", article.isRead() ? 1 : 0);
			values.put("preferred", article.isPreferred() ? 1 : 0);
			values.put("is_removed", article.isRemoved() ? 1 : 0);
			values.put("feed_url_md5", article.getFeedURLMd5());
			int rowAffected = (int) db.update("articles", values,
					"article_id=?", new String[] { "" + articleID });
			if (rowAffected <= 0)
				return false;

			if (inTransaction)
				db.setTransactionSuccessful();
			return true;
		} finally {
			if (inTransaction)
				db.endTransaction();
		}
	}

	private boolean removeArticleDB(int articleID, boolean inTransaction)
			throws DAOIOException {
		if (inTransaction)
			db.beginTransaction();
		try {
			// Remove article.
			int res = db.delete("articles", "article_id=?", new String[] { ""
					+ articleID });
			if (res <= 0)
				return false;

			// Remove references between articles and categories.
			res = db.delete("articles_categories", "article_id=?",
					new String[] { "" + articleID });
			if (res == -1)
				return false;

			List<Integer> categories = getCategoriesFromArticleID(articleID);

			for (int categoryID : categories) {
				unlinkArticleToCategory(articleID, categoryID);
			}

			if (inTransaction)
				db.setTransactionSuccessful();
			return true;

		} finally {
			if (inTransaction)
				db.endTransaction();
		}
	}

	@Override
	public boolean removeArticle(int articleID) throws DAOIOException {
		boolean res = removeArticleDB(articleID, true);
		return res;
	}

	public List<Article> getArticles(ArticleSearchParameters searchParms,
			int startIndex, int numArticles) throws DAOIOException {
		if (searchParms == null)
			throw new NullPointerException(
					"The specified search parameters are 'null'");

		String query = getQuery(searchParms, numArticles, startIndex);
		Cursor c = db.rawQuery(query, null);
		try {
			if (!c.moveToPosition(startIndex))
				return new ArrayList<Article>();
			
			ArrayList<Article> results = new ArrayList<Article>();
			for (int i = 0; i < numArticles; i++) {
				Article article = buildArticle(c);
				results.add(article);

				if (!c.moveToNext())
					break;
			}

			return results;
		} catch (Exception e) {
			throw new RuntimeException("Bug in code", e);
		} finally {
			c.close();
		}
	}

	
	private String getWherePartOfQuery(ArticleSearchParameters searchParms, int limit, int offset) {
		StringBuilder sb = new StringBuilder();
		
		ArrayList<String> conditions = new ArrayList<String>();
		conditions.add("ar.article_id = ap.article_id");
		if (searchParms.getCategoryID() != -1) {
			conditions.add("ap.category_id = "
					+ searchParms.getCategoryID());
		}

		conditions.add(" (ar.published_date >= "
				+ searchParms.getStartDate().getTime()
				+ " and ar.published_date <= "
				+ searchParms.getEndDate().getTime() + ")");
		if (searchParms.getReadState() != ArticleReadState.BOTH_READ_AND_UNREAD) {
			conditions
					.add("ar.is_read = "
							+ (searchParms.getReadState() == ArticleReadState.READ ? "1"
									: "0"));
		}

		if (searchParms.getArticleState() != ArticleState.BOTH_VALID_AND_TRASHED) {
			conditions
					.add("ar.is_removed = "
							+ (searchParms.getArticleState() == ArticleState.TRASHED ? "1"
									: "0"));
		}

		conditions.add("ar.preferred = "
				+ (searchParms.isPreferred() ? "1" : "0"));
		
		if (!searchParms.getQueryText().equals("")) {
			String text = searchParms.getQueryText();
			//conditions.add("("+buildMatchQuery("ar.title", text) +" OR "+buildMatchQuery("ar.description", text)+")");
			conditions.add("("+buildANDLikeQuery("ar.title", text) +" OR "+buildANDLikeQuery("ar.description", text)+")");
		}

		for (int i = 0; i < conditions.size(); i++) {
			if (i == 0)
				sb.append(conditions.get(i) + " ");
			else
				sb.append("AND " + conditions.get(i) + " ");
		}

		sb.append(" ORDER BY ar.published_date DESC ");
		//sb.append(" LIMIT "+offset+" , "+limit);
		return sb.toString();
	}
	
	
	private String buildANDLikeQuery(String field, String text) {
		String operator = "AND";
		StringTokenizer tokenizer = new StringTokenizer(text);
		StringBuilder sbQuery = new StringBuilder();
		sbQuery.append("(");
		int cont = 0;
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (cont == 0)
				sbQuery.append(field+" LIKE '%"+token+"%'");
			else
				sbQuery.append(" "+operator+" "+ field+" LIKE '%"+token+"%'");
			cont++;
		}
		sbQuery.append(")");
		return sbQuery.toString();
	}
	
	
	private String getQueryForSize(ArticleSearchParameters searchParms) {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT COUNT(ar.article_id) numArticles from ARTICLES ar, articles_categories ap WHERE ");
		sb.append(getWherePartOfQuery(searchParms, 1, 0));
		return sb.toString();
	}

	private String getQuery(ArticleSearchParameters searchParms, int limit, int offset) {

		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ar.* from ARTICLES ar, articles_categories ap WHERE ");
		sb.append(getWherePartOfQuery(searchParms, limit, offset));
		return sb.toString();
	}

	public int getArticlesSize(ArticleSearchParameters searchParms)
			throws DAOIOException {

		if (searchParms == null)
			throw new NullPointerException(
					"The specified search parameters are 'null'");

		String query = getQueryForSize(searchParms);
		Cursor c = db.rawQuery(query, null);
		if (!c.moveToFirst())
			return 0;
		try {
			return c.getInt(0);
		} catch (Exception e) {
			throw new RuntimeException("Bug in code", e);
		} finally {
			c.close();
		}
	}

	private boolean removeAllArticlesDB() throws Exception {
		db.beginTransaction();
		try {
			db.delete("articles", "1", null);
			db.delete("articles_categories", "1", null);
			db.setTransactionSuccessful();
			return true;
		} finally {
			db.endTransaction();
		}
	}

	public boolean removeAllArticles() throws DAOIOException {

		try {
			removeAllArticlesDB();
			return true;
		} catch (Exception e) {
			throw new DAOIOException("Removing all articles", e);
		}
	}

	@Override
	public List<Boolean> removeArticles(List<Integer> articleIDs)
			throws DAOIOException {
		if (articleIDs == null)
			throw new NullPointerException("The list of articles is 'null'");

		db.beginTransaction();
		try {
			ArrayList<Boolean> results = new ArrayList<Boolean>();
			boolean res = false;
			for (int articleID : articleIDs) {
				res = removeArticleDB(articleID, false);
				if (!res) {
					results.add(res);
					continue;
				}

				results.add(true);
			}

			db.setTransactionSuccessful();

			return results;
		} catch (Exception e) {
			throw new DAOIOException("Removing articles", e);
		} finally {
			db.endTransaction();
		}
	}

	private List<Integer> getCategoriesFromArticleID(int articleID) {
		String query = "SELECT category_id FROM articles_categories where article_id = "
				+ articleID;
		Cursor c = db.rawQuery(query, null);
		ArrayList<Integer> ret = new ArrayList<Integer>();
		try {
			int size = c.getCount();
			if (size == 0)
				return new ArrayList<Integer>();

			if (!c.moveToPosition(0))
				return new ArrayList<Integer>();

			for (int i = 0; i < size; i++) {
				ret.add(c.getInt(c.getColumnIndex("category_id")));

				if (!c.moveToNext())
					break;
			}

			return ret;
		} catch (Exception e) {
			throw new RuntimeException("Bug in code", e);
		} finally {
			c.close();
		}
	}

	@Override
	public int addCategory(Category category) throws DAOIOException {
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put("name", category.getName());
			values.put("img_id", category.getCategoryImage());
			values.put("feeds", category.getFeeds().get(0));

			int res = (int) db.insert("categories", null, values);
			if (res != -1)
				db.setTransactionSuccessful();
			return res;
		} finally {
			db.endTransaction();
		}
	}

	private Category buildCategory(Cursor c) {
		Category p = new Category();
		p.setCategoryID(c.getInt(c.getColumnIndex("category_id")));
		p.setName(c.getString(c.getColumnIndex("name")));
		p.setCategoryImage(c.getInt(c.getColumnIndex("img_id")));
		p.getFeeds().add(c.getString(c.getColumnIndex("feeds")));

		return p;
	}

	@Override
	public Category getCategory(int categoryID) throws DAOIOException {
		Cursor c = db.query("categories", null, "category_id=" + categoryID, null, null,
				null, null);
		try {
			if (c.getCount() == 0)
				return null;

			if (!c.moveToFirst())
				return null;
			Category p = buildCategory(c);
			return p;
		} catch (Exception e) {
			throw new RuntimeException("Bug in code", e);
		} finally {
			c.close();
		}
	}

	@Override
	public boolean setCategory(int categoryID, Category place)
			throws DAOIOException {
		ContentValues values = new ContentValues();
		values.put("name", place.getName());
		values.put("img_id", place.getCategoryImage());

		int numRowsUpdated = db.update("categories", values, "category_id=?",
				new String[] { "" + categoryID });
		return (numRowsUpdated > 0);
	}

	@Override
	public boolean removeCategory(int categoryID) throws DAOIOException {
		int numRows = db.delete("categories", "category_id=?", new String[] { ""
				+ categoryID });
		return (numRows > 0);
	}

	@Override
	public boolean removeAllCategories() throws DAOIOException {
		int numRows = db.delete("categories", "1", null);
		db.delete("articles_categories", "1", null);
		return (numRows > 0);
	}

	@Override
	public List<Category> getCategories(int startIndex, int numCategories)
			throws DAOIOException {
		Cursor c = db.query("categories", null, null, null, null, null, null);
		try {
			if (c.getCount() == 0)
				return new ArrayList<Category>();

			if (!c.moveToPosition(startIndex))
				return new ArrayList<Category>();

			ArrayList<Category> results = new ArrayList<Category>();
			for (int i = 0; i < numCategories; i++) {
				Category place = buildCategory(c);
				results.add(place);

				if (!c.moveToNext())
					break;
			}

			return results;
		} catch (Exception e) {
			throw new RuntimeException("Bug in code", e);
		} finally {
			c.close();
		}
	}

	@Override
	public int getCategoriesSize() throws DAOIOException {
		Cursor c = db.rawQuery("select count(*) from categories", null); 
		if (!c.moveToFirst())
			return 0;
		try {
			int numRows = c.getInt(0);
			return numRows;
		} finally {
			c.close();
		}
	}

	private boolean linkArticleToCategoryInternal(int articleID, int categoryID,
			boolean inTransaction) {
		if (inTransaction)
			db.beginTransaction();

		Cursor c = db.query("articles_categories", null,
				"category_id=? and article_id=?", new String[] { "" + categoryID,
						"" + articleID }, null, null, null, null);
		try {

			if (c.getCount() > 0)
				return true;

			ContentValues cv = new ContentValues();
			cv.put("article_id", articleID);
			cv.put("category_id", categoryID);
			int res = (int) db.insert("articles_categories", null, cv);
			if (res != -1) {
				if (inTransaction)
					db.setTransactionSuccessful();
				return true;
			} else {
				return false;
			}

		} finally {
			if (inTransaction)
				db.endTransaction();

			c.close();
		}
	}

	@Override
	public boolean linkArticleToCategory(int articleID, int categoryID)
			throws DAOIOException {

		return linkArticleToCategoryInternal(articleID, categoryID, true);
	}

	@Override
	public boolean unlinkArticleToCategory(int articleID, int categoryID)
			throws DAOIOException {
		int numRows = db.delete("articles_categories",
				"category_id=? and article_id=?", new String[] { "" + categoryID,
						"" + articleID });
		return (numRows > 0);
	}

	@Override
	public void freeResources() {
		db.releaseMemory();
	}

	
	@Override
	public void markAllArticlesAsRead() {
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put("is_read", 1);
			db.update("articles", values,
					"is_read=0 AND is_removed=0", new String[] {});
			
			db.setTransactionSuccessful();
		} finally {
				db.endTransaction();
		}
	}
	
	@Override
	public void markAllArticlesAsRead(int categoryID) {
		db.beginTransaction();
		try {
			
			ContentValues values = new ContentValues();
			values.put("is_read", 1);
			
			String wherePart = "is_read = 0 AND is_removed = 0 AND " +
			"article_id IN (SELECT pa.article_id FROM articles_categories pa WHERE article_id = pa.article_id AND  "+
			"pa.category_id = "+categoryID+")";
			
			db.update("articles", values,
					wherePart, new String[] {});
			
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}
}
