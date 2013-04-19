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


import java.net.URL;
import java.util.Date;
import java.util.GregorianCalendar;

public class Article {
	public static final double UNKNOWN_LATITUDE = -Double.MAX_VALUE;
    public static final double UNKNOWN_LONGITUDE = -Double.MAX_VALUE;
	
	private int articleID;
	private String title;
	private String description;
	private URL url;
	private Date date;
	private boolean isRead;
	private String feedURLmd5;
	private boolean preferred;
	private boolean isRemoved;

	public Article() {
		articleID = -1;
		title = "";
		description = "";
		url = null;
		date = new GregorianCalendar().getTime();
		isRead = false;
		feedURLmd5 = null;
		isRemoved = false;
	}
	
	public int getArticleID() {
		return articleID;
	}
	public void setArticleID(int articleID) {
		this.articleID = articleID;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public URL getUrl() {
		return url;
	}
	public void setUrl(URL url) {
		this.url = url;
	}


	public Date getDate() {
		return date;
	}


	public void setDate(Date date) {
		this.date = date;
	}
	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("articleID="+articleID);
		sb.append(",title=<<"+title+">>");
		sb.append(",description=<<"+description+">>");
		sb.append(",url="+url.toString());
		sb.append(",modification_time=<<"+date.toString()+">>");
		
		return sb.toString();
	}


	public boolean isRead() {
		return isRead;
	}


	public void setRead(boolean isRead) {
		this.isRead = isRead;
	}
	
	
	public void setFeedURLMd5(String md5FeedUrl) {
		this.feedURLmd5 = md5FeedUrl;
	}
	
	public String getFeedURLMd5() {
		return this.feedURLmd5;
	}

	public void setPreferred(boolean preferred) {
		this.preferred = preferred;
	}

	public boolean isPreferred() {
		return preferred;
	}

	public void setRemoved(boolean isRemoved) {
		this.isRemoved = isRemoved;
	}

	public boolean isRemoved() {
		return isRemoved;
	}
}
