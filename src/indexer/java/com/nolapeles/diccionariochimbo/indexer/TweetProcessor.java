package com.nolapeles.diccionariochimbo.indexer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import com.nolapeles.diccionariochimbo.indexer.models.Definition;
import com.nolapeles.diccionariochimbo.indexer.models.Tweep;
import com.nolapeles.diccionariochimbo.indexer.models.Tweet;
import com.nolapeles.diccionariochimbo.indexer.models.Word;

/*
 * Class to process stored tweets into Word, Definitions and scores for players.
 */
public class TweetProcessor {
	
	public final static Pattern PATTERN_TWEEP = Pattern.compile("@\\w*[\\:|\\.]?\\s?");
	public final static Pattern PATTERN_HASHTAG = Pattern.compile("(#\\w*[\\:\\.]?\\s?)");
	private final static Pattern PATTERN_WORD_DEFINITION = Pattern.compile(".*?\\\"?([\\w]*)\\\"?\\:?\\s?(.*)");
	
	private final static Pattern PATTERN_TWEEP_AFTER_RT = Pattern.compile("(.*)?RT.*@(\\w*)");

	private Datastore _ds;
	
	private Twitter _twitter;
	
	@SuppressWarnings("unused")
	private Map<Long, Tweep> _seenTweeps;
	private int BOUNCED = 0;
	private String _before;
	
	/** Turn off debugging to start saving the processed data */
	private final static boolean DEBUG = true;
	
	public TweetProcessor() {
		_ds = MongoMapper.instance().getDatastore();
		_seenTweeps = new HashMap<Long, Tweep>();
		_twitter = new TwitterFactory().getInstance();;
	}
	
	public void processTweets() {
		Query<Tweet> q = _ds.createQuery(Tweet.class).field("processed").equal(false);
		
		Iterator<Tweet> iterator = q.iterator();
		
		int n=0;
		while (iterator.hasNext()) {
			processTweet(iterator.next());
			n++;
		}
		//System.out.println(n);
	}

	private void processTweet(Tweet tweet) {
		if (!hasKnownWordDelimiters(tweet.text)) {
			System.out.println("!! DELETED " + tweet.text);
			_ds.delete(tweet);
			return;
		}
		
		normalizeTweet(tweet);
		
		/** */
		String potentialAuthor = getPotentialAuthor(tweet);
		
		int rtCount = countRTs(tweet);
		
		String word_definition_text = getCleanTweetText(tweet);
		
		Word word = new Word();
		Definition definition = new Definition();
		
		extractWordAndDefinition(word_definition_text, word, definition);
		
		if (word.word == null) {
			BOUNCED++;
			return;
		}
		
		//search for the word
		Query<Word> wordQuery = _ds.createQuery(Word.class);
		List<Word> foundWords = wordQuery.filter("word", word.word).asList();

		//It's a new word.
		if (foundWords.size()==0) {
			saveNewWordAndDefinition(tweet, potentialAuthor, rtCount, word,
					definition);
		} else {
			saveDefinition(tweet,rtCount,word,definition);
		}
		
		//System.out.println(tweet.text);
	}

	private void saveDefinition(Tweet tweet, int rtCount, Word word,
			Definition definition) {
		
		trySavingTweep(tweet, null, rtCount);

		List<Definition> definitions = word.definitions;
		
		if (definitions==null || definitions.size()==0) {
			word.definitions = new ArrayList<Definition>();
			word.definitions.add(definition);
			
		} else {
			//new definition indeed
			if (!word.definitions.contains(definition)) {
				word.definitions.add(definition);
			} else {
				Definition wordFromDB = word.definitions.get(word.definitions.indexOf(definition));
				wordFromDB.numRetweets++;
			}
		}
		
		if (!DEBUG) {
			_ds.save(word);
		}
	}

	public void saveNewWordAndDefinition(Tweet tweet, String potentialAuthor,
			int rtCount, Word word, Definition definition) {
		//This looks like it's not the owner of the tweet.
		trySavingTweep(tweet, potentialAuthor, rtCount);
		
		if (tweet.tweep != null) {
			definition.tweep = tweet.tweep;
			definition.indexed_date = System.currentTimeMillis();
			definition.lastScoreUpdateTimestamp = System.currentTimeMillis();
			definition.numFails=0;
			definition.numRetweets = 0;
			definition.numWins = 0;
			definition.numFails = 0;
			definition.score = 0;
		}
		
		word.definitions = Arrays.asList(definition);
		
		if (!DEBUG) {
			_ds.save(word);
			saveDefinition(tweet, rtCount, word, definition);
		}
		
		
	}

	public void trySavingTweep(Tweet tweet, String potentialAuthor, int rtCount) {
		if (rtCount == 1 && potentialAuthor!=null && tweet.tweep == null) {
			System.out.println("(fetching) Tweep is potential author -> " + potentialAuthor);
			tweet.tweep = fetchTweepByName(potentialAuthor);
		}
		
		//save this guy if you have to
		saveTweep(tweet.tweep);

	}

	/**
	 * Saves it only if it doesn't exist.
	 * @param tweep
	 */
	private void saveTweep(Tweep tweep) {
		Query<Tweep> query = _ds.createQuery(Tweep.class);
		List<Tweep> foundTweepLikeThis = query.filter("screen_name",tweep.screen_name).asList();
		
		if (foundTweepLikeThis.size() == 0) { // && !DEBUG) {
		 //gotta save it
			_ds.save(tweep);
			System.out.println("Saving tweep @" + tweep.screen_name);
		}
		
	}

	private Tweep fetchTweepByName(String potentialAuthor) {
		Tweep tweep = null;
		
		try {
			User user = _twitter.showUser(potentialAuthor);
			tweep = new Tweep();
			tweep.location = user.getLocation();
			tweep.profile_image_url = user.getProfileImageURL().toString();
			tweep.screen_name = potentialAuthor;
			tweep.user_id = user.getId();
		} catch (TwitterException e) {
			e.printStackTrace();
		}
		return tweep;
	}

	/**
	 * Given a "clean" tweet, it'll extract the word and the definition,
	 * and update the given object references.
	 * 
	 * @param text - text to parse
	 * @param w - target word object
	 * @param d - target definition object
	 */
	private void extractWordAndDefinition(String text,
			Word w, Definition d) {
		
		//We extract now the WORD and the DEFINITION and see if we have ourselves a definition.
		Matcher matcher = PATTERN_WORD_DEFINITION.matcher(text);
		if (matcher.matches()) {
			w.word = matcher.group(1).toUpperCase().replaceAll("\\\"", "");
			
			if (w.word.equals("") || w.word.toLowerCase().indexOf("jaja") >= 0) {
				//System.out.println("FALSE POSITIVE ON -> ["+text+"]");
				w.word = null;
				return;
			}
			
			d.definition = matcher.group(2).toLowerCase();
			//System.out.println("AFTER ["+w.word+"] -> ["+d.definition+"]\n");
		} 
	
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

		_before = text;
		
		if (text.contains("<<")) {
			text = text.substring(0,text.indexOf("<<"));
		}

		if (text.contains(">>")) {
			text = text.substring(0,text.indexOf(">>"));
		}

		//Remove @users
		text = PATTERN_TWEEP.matcher(text).replaceAll("");
		
		//Let's remove RTs
		text = text.replaceAll("RT", "");
		
		//Remove "-->"
		text = text.replaceAll("-->","");
		
		text = PATTERN_HASHTAG.matcher(text).replaceAll("");
		text = text.trim();
		
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
		//If it's a RT, then the author is not tweet.tweep.
		if (tweet.text.contains("RT")) {
			//get the nickname right after RT
			Matcher matcher = PATTERN_TWEEP_AFTER_RT.matcher(tweet.text);
			if (matcher.matches()) {
				String group1 = matcher.group(1);
				String group2 = matcher.group(2);
			}
			
		}
		return null;
	}

	/**
	 * Try to convert the tweet to the preferred syntax WORD: Definition
	 * @param tweet
	 */
	private void normalizeTweet(Tweet tweet) {
		//make sure the delimitor is right next to the word
		tweet.text = tweet.text.replace(";", ":").replace("==",":").replace("=",":").replace("-",":").replace(" :", ":");
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
		//System.out.println("BOUNCED TWEETS: " + PROCESSOR.BOUNCED);
	}
}
