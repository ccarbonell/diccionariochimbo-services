package com.nolapeles.diccionariochimbo.indexer;

import com.google.code.morphia.Datastore;

/*
 * Class to process stored tweets into Word, Definitions and scores for players.
 */
public class TweetProcessor {
	private Datastore _ds;
	
	public TweetProcessor() {
		_ds = MongoMapper.instance().getDatastore();
	}

}
