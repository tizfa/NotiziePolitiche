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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class ArticleSearchParameters implements Serializable {
	
	private static final long serialVersionUID = 4324325435431L;

	public enum ArticleSortCriteria {
		DATE_ASCENDANT,
		DATE_DESCENDANT,
	}
	
	public enum ArticleReadState {
		READ, UNREAD, BOTH_READ_AND_UNREAD
	}
	
	public enum ArticleState {
		VALID,
		TRASHED,
		BOTH_VALID_AND_TRASHED
	}
	
	
	public class ArticlesOrder implements Serializable {
		
		private static final long serialVersionUID = -1569840276713851472L;
		private List<ArticleSortCriteria> orderCriteria;
		
		public ArticlesOrder() {
			orderCriteria = new ArrayList<ArticleSearchParameters.ArticleSortCriteria>();
			orderCriteria.add(ArticleSortCriteria.DATE_DESCENDANT);
		}
		
		public void setOrderCriteria(List<ArticleSearchParameters.ArticleSortCriteria> criteria) {
			orderCriteria = criteria;
		}
		
		public List<ArticleSearchParameters.ArticleSortCriteria> getOrderCriteria() {
			return orderCriteria;
		}
	}
	
	
	private ArticleState articleState;
	
	
	/**
	 * The initial date.
	 */
	private Date startDate;
	
	/**
	 * The final date.
	 */
	private Date endDate;
	
	
	
	/**
	 * Want only unreaded articles.
	 */
	private ArticleReadState readState;
	
	
	/**
	 * The text of the query we are interested on.
	 */
	private String queryText;

	
	/**
	 * The category ID which should own the article.
	 */
	private int categoryID;
	
	
	private final ArticlesOrder order;
	
	private boolean preferred;
	
	
	public ArticleSearchParameters() {
		GregorianCalendar gc = new GregorianCalendar(1970, 0, 1);
		startDate = gc.getTime();
		gc.set(2100, 11, 31);
		endDate = gc.getTime();
		readState = ArticleReadState.BOTH_READ_AND_UNREAD;
		queryText = "";
		articleState = ArticleState.VALID;
		order = new ArticlesOrder();
		setCategoryID(-1);
		setPreferred(false);
	}
	
	public ArticlesOrder getOrder() {
		return order;
	}

	public Date getStartDate() {
		return startDate;
	}


	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}


	public Date getEndDate() {
		return endDate;
	}


	

	public String getQueryText() {
		return queryText;
	}


	public void setQueryText(String queryText) {
		this.queryText = queryText;
	}



	public ArticleReadState getReadState() {
		return readState;
	}


	public void setReadState(ArticleReadState readState) {
		this.readState = readState;
	}

	public void setCategoryID(int categoryID) {
		this.categoryID = categoryID;
	}

	public int getCategoryID() {
		return categoryID;
	}

	public void setPreferred(boolean preferred) {
		this.preferred = preferred;
	}

	public boolean isPreferred() {
		return preferred;
	}
	
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public ArticleState getArticleState() {
		return articleState;
	}

	public void setArticleState(ArticleState articleState) {
		this.articleState = articleState;
	}
	
}
