package com.nolapeles.diccionariochimbo.indexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Tweet;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

import com.google.code.morphia.Datastore;
import com.nolapeles.diccionariochimbo.indexer.models.Settings;
import com.nolapeles.diccionariochimbo.indexer.models.Tweep;

/**
 * Fetches all tweets tagged #DiccionarioChimbo and stores them.
 * 
 * @author gubatron
 * 
 */
public class TweetFetcher {

	private Twitter _twitter;
	private Datastore _ds;
	
	/** Where we'll keep the tweets (models) that'll be saved */
	private List<com.nolapeles.diccionariochimbo.indexer.models.Tweet> _modelTweets;
	
	/** We'll keep a cache of users that we have already in the database. If we don't have them we'll insert them */
	private Map<Long,Tweep> _seenTweeps;

	private Settings _settings;
	private Query _query;

	public TweetFetcher() {
		_modelTweets = new ArrayList<com.nolapeles.diccionariochimbo.indexer.models.Tweet>();
		_seenTweeps = new HashMap<Long,Tweep>();
		_twitter = new TwitterFactory().getInstance();
		_ds = MongoMapper.instance().getDatastore();
		_settings = Settings.instance();
	}

	/**
	 * Search and store the newest tweets.
	 */
	public void fetchTweets() {
		_query = new Query(_settings.SEARCH_HASHTAG);

		int page = 1;
		
		// search until you can't search no more
		while (searchTweetPage(page++)) {  }
		
		if (_modelTweets.size() > 0) {
			_ds.save(_modelTweets);
		}
	}

	/**
	 * Results are stored on the list.
	 * 
	 * This method checks the search result
	 * 
	 * @param _query  - The Query we configured.
	 * @param _modelTweets - The list where we keep adding the results.
	 * @param page
	 * 
	 * return true if it found anything else. False if it found 0 tweets for this page.
	 */
	private boolean searchTweetPage(int page) {
		boolean foundTweets = false;
		
		QueryResult result = null;
		try {
			//turn the page
			prepareQuery(page);
			
			//Search!
			result = _twitter.search(_query);
			
			//Update get max id
			if (result.getMaxId() > _settings.TWEET_FETCHER_LAST_TWEET_ID) {
				_settings.TWEET_FETCHER_LAST_TWEET_ID = result.getMaxId();
				_settings.save();
				System.out.println("new max id: " + result.getMaxId());
			}
			
			//add results to the list we'll persist at the end.
			List<Tweet> pageTweets = result.getTweets();
			for (Tweet t : pageTweets) {
				com.nolapeles.diccionariochimbo.indexer.models.Tweet tweet = new com.nolapeles.diccionariochimbo.indexer.models.Tweet();
				tweet.text = t.getText();
				tweet.tweet_id = t.getId();
				
				//user's already in memory
				if (_seenTweeps.containsKey(t.getFromUserId())) {
					tweet.tweep = _seenTweeps.get(t.getFromUserId());
				} else {
					//try to search it in the database first.
					Tweep persistedTweep = _ds.find(Tweep.class, "user_id", t.getFromUserId()).get();
					
					//don't have it yet, let's persist it
					if (persistedTweep == null) {
						persistedTweep = new Tweep();
						persistedTweep.user_id = t.getFromUserId();
						persistedTweep.location = t.getLocation();
						persistedTweep.profile_image_url = t.getProfileImageUrl();
						persistedTweep.screen_name = t.getFromUser();
						
						_ds.save(persistedTweep);
					}
					
					//once found we add it to the _seenTweetps structure
					_seenTweeps.put(t.getFromUserId(), persistedTweep);
				}
				
				_modelTweets.add(tweet);
			}
			
			
			//found anything?
			foundTweets = pageTweets.size() > 0;
		} catch (TwitterException e) {
			e.printStackTrace();
		}
		
		return foundTweets;
		
	}

	private void prepareQuery(int pageNumber) {
		_query.setSinceId(_settings.TWEET_FETCHER_LAST_TWEET_ID);
		//query.setMaxId(_settings.TWEET_FETCHER_LAST_TWEET_ID);
		_query.setResultType(Query.MIXED);
		_query.setRpp(IndexerConstants.SEARCH_RESULTS_PER_PAGE);
		_query.setPage(pageNumber);
	}

	public static void main(String[] args) {
		TweetFetcher fetcher = new TweetFetcher();
		fetcher.fetchTweets();
	}
}
