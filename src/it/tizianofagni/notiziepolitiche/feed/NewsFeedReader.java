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

package it.tizianofagni.notiziepolitiche.feed;

import it.tizianofagni.notiziepolitiche.dao.Article;
import it.tizianofagni.notiziepolitiche.utils.GenericUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

public class NewsFeedReader {

	public NewsFeedReader() {
	}

	/**
	 * Retrieve the list of news from the feed source.
	 * 
	 * @return The list of news from the feed source.
	 * @throws IOException
	 *             Raised if some I/O error occurs during the operation.
	 */
	public List<Article> getNews(URL feedURL) throws FeedReaderIOException {
		if (feedURL == null)
			throw new NullPointerException("The specified URL is 'null'");

		BufferedInputStream is = null;
		try {
			//XmlPullFeedParser parser = new XmlPullFeedParser(feedURL.toString());
			AndroidRss2FeedParser parser = new AndroidRss2FeedParser(feedURL.toString());
			List<Message> items = parser.parse();
			ArrayList<Article> articles = new ArrayList<Article>();
			for (int i = 0; i < items.size(); i++) {
				Message item = items.get(i);
				Article a = makeNews(item);
				articles.add(a);
			}

			return articles;
		} catch (Exception e) {
			throw new FeedReaderIOException("Reading articles from feed "
					+ feedURL.toString(), e);
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (Exception e2) {
				throw new FeedReaderIOException("Closing feed reader", e2);
			}
		}
	}

	private Article makeNews(Message entry) {
		try {

			Article a = new Article();
			if (entry.getTitle() != null)
				a.setTitle(entry.getTitle());
			if (entry.getDate() == null)
				a.setDate(new GregorianCalendar().getTime());
			else
				a.setDate(entry.getDate());
			
			if (entry.getDescription() != null)
				a.setDescription(Jsoup.clean(entry.getDescription(),
						Whitelist.basicWithImages()));
			a.setRead(false);
			a.setUrl(entry.getLink());

			if (entry.getGuid() == null)
				a.setFeedURLMd5(GenericUtils.md5(a.getUrl().toString()));
			else
				a.setFeedURLMd5(GenericUtils.md5(entry.getGuid()));

			return a;
		} catch (Exception e) {
			return null;
		}
	}

}
