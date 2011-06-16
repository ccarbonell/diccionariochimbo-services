package com.nolapeles.diccionariochimbo.indexer;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Tweet;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

import com.google.code.morphia.Datastore;
import com.nolapeles.diccionariochimbo.indexer.models.Settings;

/**
 * Fetches all tweets tagged #DiccionarioChimbo and stores them.
 * 
 * @author gubatron
 * 
 */
public class TweetFetcher {

	private Twitter _twitter;
	private Datastore _ds;

	private Settings _settings;

	public TweetFetcher() {
		_twitter = new TwitterFactory().getInstance();
		_ds = MongoMapper.instance().getDatastore();

		_settings = Settings.instance();
	}

	/**
	 * Search and store the newest tweets.
	 */
	public void fetchTweets() {
		Query query = new Query(_settings.SEARCH_HASHTAG);
		prepareQuery(query, -1);


		List<Tweet> tweets = new ArrayList<Tweet>();
		int page = 0;

		// search until you can't search no more
		while (searchTweetPage(query, tweets, page++)) { }

		System.out.println("///////////////////////////////////////////////////////////\n\n");
		
		System.out.println(tweets.size() + " tweets found. Page: "
				+ query.getPage());

		for (Tweet tweet : tweets) {
			System.out.println(tweet.getId() + " - @" + tweet.getFromUser() + ": \"" + tweet.getText() + "\"");
		}
	}

	/**
	 * Results are stored on the list.
	 * 
	 * This method checks the search result
	 * 
	 * @param query  - The Query we configured.
	 * @param tweets - The list where we keep adding the results.
	 * @param page
	 * 
	 * return true if it found anything else. False if it found 0 tweets for this page.
	 */
	private boolean searchTweetPage(Query query, List<Tweet> tweets, int page) {
		boolean foundTweets = false;
		
		QueryResult result = null;
		try {
			//turn the page
			query.setPage(page);
			
			//Search!
			result = _twitter.search(query);
			
			//Update get max id
			if (result.getMaxId() > _settings.TWEET_FETCHER_LAST_TWEET_ID) {
				_settings.TWEET_FETCHER_LAST_TWEET_ID = result.getMaxId();
				_settings.save();
			}
			
			//add results to the list we'll persist at the end.
			List<Tweet> pageTweets = result.getTweets();			
			tweets.addAll(pageTweets);
			
			//found anything?
			foundTweets = pageTweets.size() > 0;
		} catch (TwitterException e) {
			e.printStackTrace();
		}
		
		return foundTweets;
		
	}

	private void prepareQuery(Query query, int pageNumber) {
		//query.setSinceId(_settings.TWEET_FETCHER_LAST_TWEET_ID);
		//query.setMaxId(_settings.TWEET_FETCHER_LAST_TWEET_ID);
		query.setResultType(Query.MIXED);
		query.setRpp(IndexerConstants.SEARCH_RESULTS_PER_PAGE);
		query.setPage(pageNumber);
	}

	public static void main(String[] args) {
		TweetFetcher fetcher = new TweetFetcher();
		fetcher.fetchTweets();
	}
}