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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

public class XmlPullFeedParser extends BaseFeedParser {

	public XmlPullFeedParser(String feedUrl) {
		super(feedUrl);
	}

	public List<Message> parse() {
		List<Message> messages = null;
		InputStream is = null;
		try {
			
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			XmlPullParser parser = factory.newPullParser();
			
			// auto-detect the encoding from the stream
			is = this.getInputStream();
			parser.setInput(is, null);
			int eventType = parser.getEventType();
			Message currentMessage = null;
			boolean done = false;
			boolean ignoreItem = false;
			while (eventType != XmlPullParser.END_DOCUMENT && !done){
				String name = null;
				switch (eventType){
					case XmlPullParser.START_DOCUMENT:
						messages = new ArrayList<Message>();
						break;
					case XmlPullParser.START_TAG:
						name = parser.getName();
						if (name.equalsIgnoreCase(IMAGE)) {
							ignoreItem = true;
						} else if (name.equalsIgnoreCase(ITEM)){
							if (!ignoreItem) {
								currentMessage = new Message();
							}
						} else if (currentMessage != null){
							if (name.equalsIgnoreCase(LINK)){
								currentMessage.setLink(parser.nextText());
							} else if (name.equalsIgnoreCase(DESCRIPTION)){
								currentMessage.setDescription(parser.nextText());
							} else if (name.equalsIgnoreCase(PUB_DATE)){
								currentMessage.setDate(parser.nextText());
							} else if (name.equalsIgnoreCase(TITLE)){
								currentMessage.setTitle(parser.nextText());
							}	
						}
						break;
					case XmlPullParser.END_TAG:
						name = parser.getName();
						if (name.equalsIgnoreCase(ITEM) && currentMessage != null){
							messages.add(currentMessage);
							currentMessage = null;
						} else if (name.equalsIgnoreCase(IMAGE)) {
							ignoreItem = false;
						}
						else if (name.equalsIgnoreCase(CHANNEL)){
							done = true;
						}
						break;
				}
				
				eventType = parser.next();
			}
		} catch (Exception e) {
			Log.e(XmlPullFeedParser.class.getName(), e.getMessage(), e);
			throw new RuntimeException(e);
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (Exception e2) {
				throw new RuntimeException(e2);
			}
		}
		return messages;
	}
}
