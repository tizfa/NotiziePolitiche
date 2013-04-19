// Include your fully-qualified package statement.
package it.tizianofagni.notiziepolitiche.service;


// Declare the interface.
interface IFeedRetrieverServiceCallback {
    
   	/**
	 * Signal that the service has been retrieved "numNewArticles" articles after a call
	 * to refreshAll() method.
	 * 
	 * @param numNewArticles The number of new articles retrieved from source feeds.
	 */
	void newArticlesRetrieved(int numNewArticles);

	
	void startRetrievingArticles(int partyID);
	
	void endRetrievingArticles(int partyID);
	
	/**
	 * Signal that the service has been retrieved "numNewArticles" articles for the
	 * specified political party.
	 * 
	 * @param category The category for which new articles has been retrieved.
	 * @param numNewArticles The number of new articles retrived.
	 */
	void newArticlesRetrievedForCategory(int categoryID, int numNewArticles);
    
}