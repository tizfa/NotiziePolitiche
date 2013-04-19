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



import java.util.List;

public interface NewsDAO {
	
	/**
	 * Init the storage.
	 * 
	 * @param forceCreation If true, force the creation of a new storage.
	 * @throws DAOIOException Raised if some error occurs during the methdo call.
	 */
	public void init(boolean forceCreation) throws DAOIOException;
	
	/**
	 * Release all resources allocated within the storage. After this call
	 * the storage manager is no more valid.
	 * 
	 * @throws DAOIOException Raised if some error occurs during the methdo call.
	 */
	public void release() throws DAOIOException;
	
	
	/////////////////////////////////////////////
	
	
	/**
	 * Add a category to application storage.
	 * 
	 * @param category The category to add.
	 * @return The category ID of the added category.
	 * @throws DAOIOException Raised if some I/O error occurs.
	 */
	public int addCategory(Category category) throws DAOIOException;
	
	/**
	 * Get the category associated with specified ID.
	 * 
	 * @param categoryID The ID of the category to retrieve.
	 * @return The category data or 'null' if the category ID can not be found.
	 * @throws DAOIOException Raised if some I/O error occurs.
	 */
	public Category getCategory(int categoryID) throws DAOIOException;
	
	
	
	/**
	 * Update the category specified by the given ID with the new category data. 
	 * 
	 * @param categoryID The category ID to update.
	 * @param category The new category data.
	 * @return True if the category has been updated, false otherwise.
	 * @throws DAOIOException Raised if some I/O error occurs.
	 */
	public boolean setCategory(int categoryID, Category category) throws DAOIOException;
	
	
	/**
	 * Remove the category specified by the given ID.
	 * 
	 * @param categoryID The category ID to remove.
	 * @return True if specified category has been removed, false otherwise.
	 * @throws DAOIOException Raised if some I/O error occurs.
	 */
	public boolean removeCategory(int categoryID) throws DAOIOException;
	
	
	
	/**
	 * Remove all the categories stored.
	 * 
	 * @return True if all the categories have been removed, false otherwise.
	 * @throws DAOIOException Raised if some I/O error occurs.
	 */
	public boolean removeAllCategories() throws DAOIOException;
	
	
	
	
	
	/**
	 * Get a number of catehgories given by numcats and starting at position startIdex.
	 * 
	 * @param startIndex The 0-based start index of the results. 
	 * @param numcats The number of partys to retrieve.
	 * @return The list of categories.
	 * @throws DAOIOException Raised if some I/O error occurs.
	 */
	public List<Category> getCategories(int startIndex, int numcats) throws DAOIOException;
	
	/**
	 * Get the number of categories stored.
	 * 
	 * @return The number of categories stored on the storage.
	 * @throws DAOIOException Raised if some I/O error occurs.
	 */
	public int getCategoriesSize() throws DAOIOException;
	
	
	
	
	
	
	//////////////////////////////////////////////
	
	

	/**
	 * Add a series of articles to application storage.
	 * 
	 * @param article The list of articles to add.
	 * @param allowDuplicates True if you want also to store duplicates articles (which
	 * have the same value of {@link Article#getFeedURLMd5()}, false if you want to
	 * store only distinct articles.
	 * @return The IDs of the added articles.
	 * @throws DAOIOException Raised if some I/O error occurs.
	 */
	public List<Integer> addArticles(List<Article> articles, boolean allowDuplicates) throws DAOIOException;
	
	
	/**
	 * Add an article to application storage.
	 * 
	 * @param article The article to add.
	 * @return The ID of the added article.
	 * @throws DAOIOException Raised if some I/O error occurs.
	 */
	public int addArticle(Article article) throws DAOIOException;
	
	
	/**
	 * Get the article data of the specified article ID.
	 * 
	 * @param articleID The ID of the article to retrieve.
	 * @return The article data or 'null' if the article ID can not be found.
	 * @throws DAOIOException Raised if some I/O error occurs.
	 */
	public Article getArticle(int articleID) throws DAOIOException;

	

	/**
	 * Mark all unread articles in the DB as read (except those trashed).
	 */
	public void markAllArticlesAsRead();
	
	
	/**
	 * Mark all articles belonging to party partyID as read (except those trashed). 
	 * 
	 * @param partyID The party ID.
	 */
	public void markAllArticlesAsRead(int partyID);
	
	
	
	
	/**
	 * Get the article from md5 URI string.
	 * 
	 * @param articleURIMd5 The article md5 URI to find.
	 * @return The corresponding article or 'null' if the article can not be found.
	 */
	public Article getArticle(String articleURIMd5);
	
	
	/**
	 * Update the article specified by the given ID with the new article data. 
	 * 
	 * @param articleID The article ID to update.
	 * @param article The new article data.
	 * @return True if the article has been updated, false otherwise.
	 * @throws DAOIOException Raised if some I/O error occurs.
	 */
	public boolean setArticle(int articleID, Article article) throws DAOIOException;
	
	
	/**
	 * Update the specified articles.
	 * 
	 * @param articleIDs The IDs of the articles to update.
	 * @param articles The articles to set.
	 * @return A list of boolean result, one for each article considered in the
	 * operation. 
	 * @throws DAOIOException Raised if some I/O error occurs.
	 */
	public List<Boolean> setArticles(List<Integer> articleIDs, List<Article> articles) throws DAOIOException;
	
	
	
	/**
	 * Remove the article specified by the given ID.
	 * 
	 * @param articleID The article ID to remove.
	 * @return True if specified article has been removed, false otherwise.
	 * @throws DAOIOException Raised if some I/O error occurs.
	 */
	public boolean removeArticle(int articleID) throws DAOIOException;
	
	
	/**
	 * Remove all specified articles.
	 * 
	 * @param articleIDs The IDs of the articles to remove.
	 * @return The list of result of deletion of each article.
	 * @throws DAOIOException Raised if some I/O error occurs.
	 */
	public List<Boolean> removeArticles(List<Integer> articleIDs) throws DAOIOException;
	
	
	
	/**
	 * Get a number of articles given by numArticles and starting at position startIdex.
	 * 
	 * @param searchParms The search parameters to use.
	 * @param startIndex The 0-based start index of the results. 
	 * @param numArticles The number of articles to retrieve.
	 * @return The list of articles matched.
	 * @throws DAOIOException Raised if some I/O error occurs.
	 */
	public List<Article> getArticles(ArticleSearchParameters searchParms, int startIndex, int numArticles) throws DAOIOException;
	
	/**
	 * Get the number of articles stored that math the given search parameters.
	 * 
	 * @param searchParms The search parameters.
	 * @return The number of articles stored on the storage.
	 * @throws DAOIOException Raised if some I/O error occurs.
	 */
	public int getArticlesSize(ArticleSearchParameters searchParms) throws DAOIOException;
	
	
	/**
	 * Remove all the articles stored in the storage.
	 * 
	 * @return True if all the articles have been removed, false otherwise.
	 * @throws DAOIOException Raised if some I/O error occurs.
	 */
	public boolean removeAllArticles() throws DAOIOException;
	
	
	/**
	 * Make a link between the specified article ID and category ID.
	 * 
	 * @return True if the link has been made successfully, false otherwise.
	 * @throws DAOIOException Raised if some I/O error occurs.
	 */
	public boolean linkArticleToCategory(int articleID, int categoryID) throws DAOIOException;
	
	
	/**
	 * Drop a link between the specified article ID and category ID.
	 * 
	 * @return True if the link has been made successfully, false otherwise.
	 * @throws DAOIOException Raised if some I/O error occurs.
	 */
	public boolean unlinkArticleToCategory(int articleID, int categoryID) throws DAOIOException;
	
	
	/**
	 * Free any in-memory temporary resources hold by the DAO layer.
	 * 
	 */
	public void freeResources();
}
