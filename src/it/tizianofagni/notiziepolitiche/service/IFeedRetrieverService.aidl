// Include your fully-qualified package statement.
package it.tizianofagni.notiziepolitiche.service;

// See the list above for which classes need
// import statement.
import it.tizianofagni.notiziepolitiche.service.IFeedRetrieverServiceCallback;

// Declare the interface.
interface IFeedRetrieverService {
    
    
    /* Add a callback handler for the service.*/
    void addCallback(IFeedRetrieverServiceCallback callback);
    
    /* Remove a registered callback handler for the service.*/
    void removeCallback(IFeedRetrieverServiceCallback callback);
    
    /* Refresh all data sources. Return true if the network connection was available,
    otherwise false. */
    boolean refreshAll();
    
    /* Refresh data for party identified by partyID. Return true if the network connection was available, 
    otherwise false. */
    boolean refresh(int partyID);
    
    /* Remove all the expired data. */
  	void removeExpiredData();
    
    /* Update the configuration because the specified parameter has changed value. */
    void updateConfiguration(String parameter);
    
    /* Tell the service to terminate itself. */
    void quit();
}