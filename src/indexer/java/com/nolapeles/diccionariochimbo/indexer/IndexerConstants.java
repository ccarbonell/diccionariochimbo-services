package com.nolapeles.diccionariochimbo.indexer;

public class IndexerConstants {

	/** Amount of time a Definition score will last before we recalculate. (1 HOUR)*/
	public static final long DEFINITION_SCORE_EXPIRATION_INTERVAL = 3600000;
	
	/**
	 * Set the maximum number of search results per query. Currently twitter will only send 100 search results.
	 */
	public static final int SEARCH_RESULTS_PER_PAGE = 100;

}
