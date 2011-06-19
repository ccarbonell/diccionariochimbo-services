package com.nolapeles.diccionariochimbo.indexer;

import java.util.Iterator;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import com.nolapeles.diccionariochimbo.indexer.models.Tweet;

/*
 * Class to process stored tweets into Word, Definitions and scores for players.
 */
public class TweetProcessor {
	private Datastore _ds;
	
	public TweetProcessor() {
		_ds = MongoMapper.instance().getDatastore();
	}
	
	public void processTweets() {
		Query<Tweet> q = _ds.createQuery(Tweet.class).field("processed").equal(false);
		
		Iterator<Tweet> iterator = q.iterator();
		
		while (iterator.hasNext()) {
			processTweet(iterator.next());
		}
	}

	private void processTweet(Tweet tweet) {
		if (!hasKnownWordDelimiters(tweet.text)) {
			System.out.println("!! DELETED " + tweet.text);
			_ds.delete(tweet);
			return;
		}
		
		normalizeTweet(tweet);
		//System.out.println(tweet.text);
	}
	
	/**
	 * Try to convert the tweet to the preferred syntax WORD: Definition
	 * @param tweet
	 */
	private void normalizeTweet(Tweet tweet) {
		//make sure the delimitor is right next to the word
		
		tweet.text.replace(";", ":");
		tweet.text.replace("==",":");
		
		tweet.text = tweet.text.replace(" :", ":");
	}

	/**
	 * Make sure it has delimiters so that we can process or discard right away.
	 * 
	 * We call delimiters the separators between word and definitions
	 * common delimiters are:
	 * 
	 * :
	 * ==
	 * ;
	 * 
	 * @param text
	 * @return
	 */
	private boolean hasKnownWordDelimiters(String text) {
		return text.contains(":") || text.contains("==") || text.contains(";");
	}

	public static void main(String[] arg) {
		final TweetProcessor PROCESSOR  = new TweetProcessor();
		PROCESSOR.processTweets();
	}
}
