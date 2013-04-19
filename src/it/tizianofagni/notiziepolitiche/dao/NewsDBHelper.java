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

import java.io.File;
import java.lang.reflect.Method;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

public class NewsDBHelper {
	/**
	 * The prefix for the any method that upgrades DB to a new version.
	 */
	private static final String PREFIX_UPGRADE_METHOD = "upgradeDBToVersion";

	/**
	 * The name of the application DB.
	 */
	private static final String DATABASE_NAME = "newsdb.sqlite";

	/**
	 * The current DB version.
	 */
	private static final int DATABASE_VERSION = 1;

	/**
	 * The Android context.
	 */
	private Context ctx;

	private boolean storageOnSDCard;

	private SQLiteDatabase db;

	/**
	 * The DB storage path in case of use of SD card.
	 */
	private String storagePath;
	
	private boolean forceCreation;

	public NewsDBHelper(Context ctx) {
		this.ctx = ctx;
		storageOnSDCard = true;
		db = null;
		storagePath = "/sdcard";
		forceCreation = false;
	}

	
	
	public boolean isForceCreation() {
		return forceCreation;
	}



	public void setForceCreation(boolean forceCreation) {
		this.forceCreation = forceCreation;
	}



	public String getStoragePath() {
		return storagePath;
	}

	public void setStoragePath(String storagePath) {
		this.storagePath = storagePath;
	}

	public boolean isOpen() {
		return (db != null);
	}

	public void setStorageOnCard(boolean storageOnSDCard) {
		this.storageOnSDCard = storageOnSDCard;
	}

	public boolean isStorageOnSDCard() {
		return storageOnSDCard;
	}

	public void open() throws DAOIOException {
		if (isOpen())
			throw new DAOIOException("The DB is already open");

		File sdCardDir = null, dbFile = null;

		if (storageOnSDCard) {
			// Store DB on SD card.
			if (Environment.getExternalStorageState().equals(
					Environment.MEDIA_MOUNTED)) {
				sdCardDir = new File(storagePath);
				if (!sdCardDir.exists()) {
					if (!sdCardDir.mkdirs())
						throw new DAOIOException("Can not create directory <"
								+ storagePath.toString() + ">");
				}
				dbFile = new File(sdCardDir, DATABASE_NAME);
			} else
				throw new DAOIOException(
						"The SD card is not currently available in R/W mode");
		} else {
			// Store DB on application private storage.
			dbFile = ctx.getDatabasePath(DATABASE_NAME);
		}

		if (dbFile.exists() && !forceCreation) {
			Log.i("DBHelper", "Opening database at " + dbFile);
			db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
			if (db.getVersion() > 0 && DATABASE_VERSION > db.getVersion())
				upgradeDB(db.getVersion(), DATABASE_VERSION);
			else if (db.getVersion() == 0) {
				createDB();
			}
		} else {
			// Delete old db file.
			Log.i("DBHelper", "Deleting old database at " + dbFile);
			dbFile.delete();
			
			Log.i("DBHelper", "Creating new database at " + dbFile);
			db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
			createDB();
		}
		db.setLockingEnabled(true);
	}

	

	public void close() {
		if (isOpen()) {
			db.close();
			db = null;
		}
	}

	protected void createDB() {
		if (DATABASE_VERSION == 1)
			createDBVersion1();
	}

	private void createDBVersion1() {
		Log.i("" + this, "Creating Database " + db.getPath());

		db.beginTransaction();
		try {
			db.setVersion(DATABASE_VERSION);

			// Create the table categories.
			final String CREATE_TABLE_CATEGORIES = "CREATE TABLE categories (category_id integer PRIMARY KEY,"
					+ "name text not null,"
					+ "img_id integer not null, feeds text not null);";
			db.execSQL(CREATE_TABLE_CATEGORIES);

			// Create the table articles.
			final String CREATE_TABLE_ARTICLES = "CREATE TABLE articles (article_id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ "title TEXT NOT NULL, description TEXT NOT NULL, url TEXT NOT NULL, feed_url_md5 TEXT NOT NULL, "
					+ "published_date INTEGER NOT NULL, is_read INTEGER NOT NULL, is_removed INTEGER NOT NULL, preferred INTEGER NOT NULL"
					+ ");";
			/*final String CREATE_TABLE_ARTICLES = "CREATE VIRTUAL TABLE articles USING FTS3 (article_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "title TEXT NOT NULL, description TEXT NOT NULL, url TEXT NOT NULL, feed_url_md5 TEXT NOT NULL, "
				+ "published_date INTEGER NOT NULL, is_read INTEGER NOT NULL, is_removed INTEGER NOT NULL, preferred INTEGER NOT NULL"
				+ ");";*/
			db.execSQL(CREATE_TABLE_ARTICLES);

			// Create table keeping relations between articles and categories.
			final String CREATE_TABLE_ARTICLES_CATEGORIES = "CREATE TABLE articles_categories ("
					+ "id INTEGER PRIMARY KEY AUTOINCREMENT, article_id INTEGER NOT NULL, category_id INTEGER NOT NULL"
					+ ");";
			db.execSQL(CREATE_TABLE_ARTICLES_CATEGORIES);

			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	protected void upgradeDB(int oldVersion, int newVersion) {
		Log.i("" + this, "Upgrading database " + db.getPath()
				+ " from version " + db.getVersion() + " to "
				+ DATABASE_VERSION + ", which will destroy all old data");

		try {
			// Call any upgrade method necessary to update the DB schema and to
			// migrate the old
			// data.
			for (int version = oldVersion + 1; version <= newVersion; version++) {
				Method m = getClass().getDeclaredMethod(
						PREFIX_UPGRADE_METHOD + version,
						new Class[] { SQLiteDatabase.class });
				m.invoke(this, db);
			}
		} catch (Exception e) {
			throw new RuntimeException("Upgrading db", e);
		}
	}

	public SQLiteDatabase getWritableDatabase() throws DAOIOException {
		if (!isOpen())
			open();
		return db;
	}
}
