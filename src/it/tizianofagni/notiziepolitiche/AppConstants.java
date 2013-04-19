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

public class AppConstants {
	
	
	
	// GENERAL
	public static final String PREF_NAME = "it.tizianofagni.notiziepolitiche.preferences";
	
	
	// NOTIFICATION
	
	/**
	 * The value of msg which notify new articles to read.
	 */
	public static final int NOTIFICATION_NEW_ARTICLES = 1;
	
	
	/**
	 * Value to pass to main activity to signal that it should start
	 * by showing only the new articles.
	 */
	public static final String NOTIFICATION_SHOW_NEW_ARTICLES = "NOTIFICATION_SHOW_NEW_ARTICLES";
	
	
	//////////////////////// TOPIC /////////////////////////
	
	
	
	////////////////////////////////////////////////////////
	
	
	
	
	// PREFERENCES
	
	
	/**
	 * Current application version.
	 */
	public static final SoftwareVersion applicationVersion = new SoftwareVersion(1,0,6);
	

	
	
	
	// DEFAULT VALUES
	
	/**
	 * Default Interval in minutes between feeds update.
	 */
	public static final int DEFAULT_RETRIEVE_FEEDS_INTERVAL = 120;
	
	
	/**
	 * Default Interval in minutes between the deletion of expired data.
	 */
	public static final int DEFAULT_REMOVE_EXPIRED_INTERVAL = 180;
	
	
	/**
	 * Default number of days after which the data old than (currentDate minus
	 * numberOfDays) will be deleted.
	 */
	public static final int DEFAULT_REMOVE_EXPIRED_DATA = 2;
	
	/**
	 * Default number of days after the cached data old than (currentDate minus
	 * numberOfDays) will be deleted.
	 */
	public static final int DEFAULT_REMOVE_EXPIRED_CACHED_DATA = 1;
	
	
	
	public static final int DEFAULT_ARTICLE_TITLE_FONT_SIZE = 13;
	public static final int DEFAULT_ARTICLE_DESCRIPTION_FONT_SIZE = 13;
	
	// INTENT KEYS VALUES
	
	/**
	 * The key used to retrieve the articles in party details view.
	 */
	public static final String INTENT_SEARCH_PARMS_KEY = "INTENT_SEARCH_PARMS_KEY";
	
	
	
	/**
	 * Used to browse to a specified article details.
	 */
	public static final String INTENT_BROWSE_URL_KEY = "INTENT_BROWSE_URL_KEY";
	
	
	/**
	 * Used to signal that the application should be start minimized.
	 */
	public static final String INTENT_START_MINIMIZED_KEY = "INTENT_START_MINIMIZED_KEY";
}
