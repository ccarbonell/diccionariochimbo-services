package com.nolapeles.diccionariochimbo.indexer;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import com.nolapeles.diccionariochimbo.indexer.models.Tweet;

/*
 * Class to process stored tweets into Word, Definitions and scores for players.
 */
public class TweetProcessor {
	
	public final static Pattern PATTERN_TWEEP = Pattern.compile("@\\w*[\\:|\\.]?\\s?");
	public final static Pattern PATTERN_HASHTAG = Pattern.compile("(#\\w*[\\:\\.]?\\s?)");
	
	private Datastore _ds;
	
	public TweetProcessor() {
		_ds = MongoMapper.instance().getDatastore();
	}
	
	public void processTweets() {
		Query<Tweet> q = _ds.createQuery(Tweet.class).field("processed").equal(false);
		
		Iterator<Tweet> iterator = q.iterator();
		
		int n=0;
		while (iterator.hasNext()) {
			processTweet(iterator.next());
			n++;
		}
		System.out.println(n);
	}

	private void processTweet(Tweet tweet) {
		if (!hasKnownWordDelimiters(tweet.text)) {
			System.out.println("!! DELETED " + tweet.text);
			_ds.delete(tweet);
			return;
		}
		
		normalizeTweet(tweet);
		
		String potentialAuthor = getPotentialAuthor(tweet);
		
		int rtCount = countRTs(tweet);
		
		String word_definition_text = getCleanTweetText(tweet);
		
		//We have got ourselves an attempt of a definition
		if (rtCount == 0) {
			//extract word and definition
			
		} if (rtCount == 1) {
			//it's possible that it's a RT of a definition we don't yet have
			//the @name on the tweet will be the author if that's the case.
			//if we don't have @name in our db, we'll have to fetch it.
		} else {
			//add points for the user that sent this RT
		}
		
		//System.out.println(tweet.text);
	}
	
	/**
	 * Remove everything that has nothing to do with the word or definition.
	 * 
	 * @param tweet
	 * @return The clean version of the tweet.text
	 */
	private String getCleanTweetText(Tweet tweet) {
		//Remove whatever reply text there might be
		String text = new String(tweet.text);
		if (text.contains("<<")) {
			text = text.substring(0,text.indexOf("<<"));
		}

		if (text.contains(">>")) {
			text = text.substring(0,text.indexOf(">>"));
		}

		System.out.println("BEFORE: [" + text + "]");

		//Remove @users
		text = PATTERN_TWEEP.matcher(text).replaceAll("");
		
		//Let's remove RTs
		text = text.replaceAll("RT", "");
		
		//Remove -->
		text = text.replaceAll("-->","");
		
		text = PATTERN_HASHTAG.matcher(text).replaceAll("");
		text = text.trim();

		
		//We extract now the WORD and the DEFINITION and see if we have ourselves a definition.
		Pattern p = Pattern.compile("(\\w*)\\:\\s?(.*)");
		Matcher matcher = p.matcher(text);
		if (matcher.matches()) {
			text = matcher.group(1).toUpperCase()+": "+matcher.group(2).toLowerCase();
		} else {
			System.out.print("!");
		}
		
		System.out.println("AFTER: [" + text + "]\n");
		return text;
	}

	private int countRTs(Tweet tweet) {
		int numRt = 0;
		int offset = 0;
		
		while ((offset = tweet.text.indexOf("RT",offset))!=-1) {
			numRt++;
			offset++;
		}
		
		return numRt;
	}

	private String getPotentialAuthor(Tweet tweet) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Try to convert the tweet to the preferred syntax WORD: Definition
	 * @param tweet
	 */
	private void normalizeTweet(Tweet tweet) {
		//make sure the delimitor is right next to the word
		
		tweet.text = tweet.text.replace(";", ":").replace("==",":").replace(" :", ":");
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
