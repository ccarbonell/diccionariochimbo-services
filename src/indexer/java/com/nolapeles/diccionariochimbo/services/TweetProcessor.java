package com.nolapeles.diccionariochimbo.services;

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
import com.nolapeles.diccionariochimbo.models.Definition;
import com.nolapeles.diccionariochimbo.models.Tweep;
import com.nolapeles.diccionariochimbo.models.Tweet;
import com.nolapeles.diccionariochimbo.models.Word;

/*
 * Class to process stored tweets into Word, Definitions and scores for players.
 */
public class TweetProcessor {

	public final static Pattern PATTERN_TWEEP = Pattern
			.compile("@\\w*[\\:|\\.]?\\s?");
	public final static Pattern PATTERN_HASHTAG = Pattern
			.compile("(#\\w*[\\:\\.]?\\s?)");
	private final static Pattern PATTERN_WORD_DEFINITION = Pattern
			.compile(".*?\\\"?([\\w]*)\\\"?\\:?\\s?(.*)");

	private final static Pattern PATTERN_COUNT_RTS = Pattern.compile("RT ");
	private final static Pattern PATTERN_TWEEP_AFTER_RT = Pattern
			.compile("^RT.@(\\w*).*");

	private Datastore _ds;

	private Twitter _twitter;

	/** For in Memory Access to Tweeps */
	private Map<Long, Tweep> _seenTweepsById;
	
	/** For in Memory Access to Tweeps */
	private Map<String, Tweep> _seenTweepsByName;

	private int BOUNCED = 0;


	/** Turn off debugging to start saving the processed data */
	private final static boolean DEBUG = false;

	public TweetProcessor() {
		_ds = MongoMapper.instance().getDatastore();
		_seenTweepsById = new HashMap<Long, Tweep>();
		_seenTweepsByName = new HashMap<String, Tweep>();
		_twitter = new TwitterFactory().getInstance();
	}

	public void processTweets() {
		Query<Tweet> q = _ds.createQuery(Tweet.class).field("processed")
				.equal(false);

		Iterator<Tweet> iterator = q.iterator();

		while (iterator.hasNext()) {
			processTweet(iterator.next());
		}
	}

	/**
	 * Deletes all the Words and marks all fetched Tweets as unprocessed.
	 */
	public void resetProcessedTweets() {
		// delete all words.
		_ds.delete(_ds.createQuery(Word.class));

		// make all tweets unprocessed.
		Query<Tweet> q = _ds.createQuery(Tweet.class);

		Iterator<Tweet> iterator = q.iterator();

		while (iterator.hasNext()) {
			try {
				Tweet tweet = iterator.next();
				tweet.processed = false;
				_ds.save(tweet);
			} catch (Exception e) {
				// keep going.
			}
		}
	}

	/**
	 * Makes sure Tweet has a Tweep.
	 * 
	 * @param t
	 * @param potentialAuthor
	 */
	private void ensureTweetHasTweep(Tweet t, String potentialAuthor) {
		// It has a tweep, let's make sure it all looks good.

		// everything is great already
		if (t.tweep != null) {
			return;
		}

		// it's a RT and for some reason the tweet had no author
		if (t.tweep == null && potentialAuthor != null) {

			// System.out.println("==");
			System.out.println("Tweep for " + t.tweet_id + " is null ["
					+ t.text + "]");

			if (potentialAuthor != null) {
				System.out.println("> Potential author: " + potentialAuthor
						+ "\n");

				if (t.tweep == null) {
					// try to find the author in our local database and Twitter
					t.tweep = getTweepByName(potentialAuthor);
				}
			}
		}
	}

	private void processTweet(Tweet tweet) {
		if (!hasKnownWordDelimiters(tweet.text)) {
			System.out.println("!! DELETED " + tweet.text);
			_ds.delete(tweet);
			return;
		}

		normalizeTweetText(tweet);

		/**
		 * Could be of use in case we missed an original tweet, or the RTer was
		 * kind enough to add the #DiccionarioChimbo hashtag so we could process
		 * it.
		 */
		String definitionAuthorScreenName = getDefinitionAuthor(tweet);

		// make sure the tweet has a tweep.
		ensureTweetHasTweep(tweet, definitionAuthorScreenName);

		// count how many 'RT' are present in the text.
		int rtCount = countRTs(tweet);

		String word_definition_text = getCleanTweetText(tweet);

		Word word = new Word();
		Definition definition = new Definition();

		extractWordAndDefinition(word_definition_text, word, definition);

		// No word, no go.
		if (word.word == null) {
			BOUNCED++;
			return;
		}

		saveDefinition(word, definition, rtCount, tweet,
				definitionAuthorScreenName);
	}

	/**
	 * Words have one or more Definitions.
	 * 
	 * If a Word didn't exist in the DB, then we know for sure this is a new
	 * definition.
	 * 
	 * If the word exists, the definition given may or may not exist already.
	 * 
	 * If it exists already, exact repeated definitions for the first author
	 * will count as RTs.
	 * 
	 * @param word
	 * @param definition
	 * @param rtCount
	 * @param tweet
	 */
	private void saveDefinition(Word word, Definition definition, int rtCount,
			Tweet tweet, String definitionAuthorScreenName) {

		// search for the word
		Word wordInDB = findOneOf(Word.class, "word", word.word);
		boolean wordExisted = wordInDB != null;
		
		//decide who the author is
		decideDefinitionAuthor(definition,tweet, rtCount, definitionAuthorScreenName);

		if (wordExisted) {
			// we gotta add a definition.
			List<Definition> definitions = wordInDB.definitions;

			// Word existed and had some definitions already.
			if (definitions != null && definitions.size() > 0) {
				// maybe this definition has been seen already and we'll just
				// add a RT to it.
				if (definitions.contains(definition)) {
					Definition oldDefinition = definitions.get(definitions
							.indexOf(definition));

					// OPEN TO CHEATING HERE...(add an if() later)
					// to avoid an user from tweeting the same definition over
					// and over...
					// we only count the retweet if the author != tweep
					oldDefinition.numRetweets++;
					
					//TODO: Not sure if adding here
					//social points for the person that RTed.

				} else {
					definitions.add(definition);
					
					//TODO: Might have to add content points for the author here.
					
				}
			}
			
			saveWord(tweet, wordInDB);
		} else {
			// easy case, new word, new definition.
			definition.indexed_date = System.currentTimeMillis();
			definition.numFails = 0;
			definition.numWins = 1;
			definition.numRetweets = Math.max(rtCount,1);
			definition.updateScore();
			
			word.definitions = Arrays.asList(definition);
			
			saveWord(tweet, word);

		}

		//TODO: Update user score with what just happened.
		//I have doubts though of when to do this
		//if doing it little by little as we process tweets
		//or if we should just have a User Score processing class
		//aside.
		//
		//something like:
		//updateUserScore(tweep, isAuthor, isNewDefinition, isRtweetedDefinition, isNewWord)
	}

	private void decideDefinitionAuthor(Definition definition, Tweet tweet,
			int rtCount, String definitionAuthorScreenName) {
		
		if (rtCount == 0) {
			definition.tweepAuthor = tweet.tweep;
		} else if (rtCount > 0) {
			// we have a new word that we've only seen in the form of a RT.
			if (tweet.tweep==null ||
				!tweet.tweep.screen_name.equals(definitionAuthorScreenName)) {
				definition.tweepAuthor=getTweepByName(definitionAuthorScreenName);
				
				if (tweet.tweep == null && definition.tweepAuthor!=null) {
					tweet.tweep = definition.tweepAuthor;
				}
			}
		}
	}

	/** Generic way of searching for a single Object in Mongo */
	public <T> T findOneOf(Class clazz, String fieldName, Object fieldValue) {
		@SuppressWarnings("unchecked")
		List<T> foundResults = _ds.createQuery(clazz)
				.filter(fieldName, fieldValue).asList();
		if (foundResults.size() > 0) {
			return foundResults.get(0);
		} else {
			return null;
		}
	}


	/**
	 * Marks the tweet as processed and saves the word (and the tweet so we
	 * don't process it again)
	 * 
	 * @param tweet
	 * @param word
	 */
	private void saveWord(Tweet tweet, Word word) {
		if (!DEBUG && !tweet.processed && word.word != null) {
			// saves recursively
			tweet.processed = true;
			_ds.save(tweet);
			_ds.save(word);
		}
	}

	private Tweep getTweepById(long user_id) {
		Tweep result = _seenTweepsById.get(user_id);

		if (result != null) {
			return result;
		}

		return findOneOf(Tweep.class, "user_id", user_id);
	}

	/**
	 * Fetches Tweep from Twitter.com
	 * 
	 * @param potentialAuthor
	 * @return
	 */
	private Tweep getTweepFromTwitter(String potentialAuthor) {
		Tweep tweep = null;

		try {
			User user = _twitter.showUser(potentialAuthor);

			tweep = new Tweep();
			tweep.location = user.getLocation();
			tweep.profile_image_url = user.getProfileImageURL().toString();
			tweep.screen_name = potentialAuthor;
			tweep.user_id = user.getId();
			cacheTweep(tweep);
			
		} catch (TwitterException e) {
			//e.printStackTrace();
		}

		return tweep;
	}

	private void cacheTweep(Tweep tweep) {
		//cache it by id
		if (!_seenTweepsById.containsKey(tweep.user_id)) {
			_seenTweepsById.put(tweep.user_id, tweep);
		}
		
		//cache it by name
		if (!_seenTweepsByName.containsKey(tweep.screen_name)) {
			_seenTweepsByName.put(tweep.screen_name, tweep);
		}
	}

	/**
	 * Tries to fetch a Tweep from the DB using the screen_name. 
	 * 
	 * If it can't get it from the DB, it will try to get it from Twitter.com,
	 * in which case it'll persist it to our DB.
	 * 
	 * Invoking this method and getting a non null result means that
	 * the Tweep will be cached for constant access via ID or Screen name.
	 * 
	 */
	private Tweep getTweepByName(String potentialAuthor) {
		//1. Check the memory cache
		Tweep result = _seenTweepsByName.get(potentialAuthor);
		
		//2. Check DB
		if (result == null) {
			result = findOneOf(Tweep.class, "screen_name", potentialAuthor);
		}

		//3. Get it from Twitter.com
		if (result == null) {
			result = getTweepFromTwitter(potentialAuthor);
			
			//save it in DB because we just checked and he wasn't there.
			if (result != null) {
				saveTweep(result);
			}
		}
		
		//4. Whatever you got try to cache it for next time.
		if (result != null ) {
			cacheTweep(result);
		}

		return result;
	}

	/**
	 * Saves it only if it doesn't exist.
	 * 
	 * @param tweep
	 */
	private void saveTweep(Tweep tweep) {
		Tweep foundTweep = findOneOf(Tweep.class, "screen_name",
				tweep.screen_name);

		if (foundTweep == null) {
			_ds.save(tweep);
			cacheTweep(tweep);
			System.out.println("Saving tweep @" + tweep.screen_name);
		}
	}

	/**
	 * Given a "clean" tweet, it'll extract the word and the definition, and
	 * update the given object references.
	 * 
	 * @param text
	 *            - text to parse
	 * @param w
	 *            - target word object
	 * @param d
	 *            - target definition object
	 */
	private void extractWordAndDefinition(String text, Word w, Definition d) {

		// We extract now the WORD and the DEFINITION and see if we have
		// ourselves a definition.
		Matcher matcher = PATTERN_WORD_DEFINITION.matcher(text);
		if (matcher.matches()) {
			w.word = matcher.group(1).toUpperCase().replaceAll("\\\"", "");

			if (w.word.equals("") || w.word.toLowerCase().indexOf("jaja") >= 0) {
				// System.out.println("FALSE POSITIVE ON -> ["+text+"]");
				w.word = null;
				return;
			}

			d.definition = matcher.group(2).toLowerCase();
			// System.out.println("AFTER ["+w.word+"] -> ["+d.definition+"]\n");
		}
	}

	/**
	 * Remove everything that has nothing to do with the word or definition.
	 * 
	 * @param tweet
	 * @return The clean version of the tweet.text
	 */
	private String getCleanTweetText(Tweet tweet) {
		// Remove whatever reply text there might be

		String text = new String(tweet.text);

		if (text.contains("<<")) {
			text = text.substring(0, text.indexOf("<<"));
		}

		if (text.contains(">>")) {
			text = text.substring(0, text.indexOf(">>"));
		}

		// Remove @users
		text = PATTERN_TWEEP.matcher(text).replaceAll("");

		// Let's remove RTs
		text = text.replaceAll("RT", "");

		// Remove "-->"
		text = text.replaceAll("-->", "");

		text = PATTERN_HASHTAG.matcher(text).replaceAll("");
		text = text.trim();

		return text;
	}

	private int countRTs(Tweet tweet) {
		int numRt = 0;
		int offset = 0;

		while ((offset = tweet.text.indexOf("RT", offset)) != -1) {
			numRt++;
			offset++;
		}

		return numRt;
	}

	private String getDefinitionAuthor(Tweet tweet) {
		// If it's a RT, then the author is not tweet.tweep.
		if (tweet.text.contains("RT")) {

			// count how many RTs there are
			Matcher rtCountMatcher = PATTERN_COUNT_RTS.matcher(tweet.text);
			int rtCount = 0;

			while (rtCountMatcher.find()) {
				rtCount++;
			}

			// get the nickname right after RT
			Matcher matcher = PATTERN_TWEEP_AFTER_RT.matcher(tweet.text);
			if (matcher.matches() && rtCount == 1) {
				String author = matcher.group(1);

				return author;
			} else {
				return null;
			}
		} else {
			if (tweet.tweep == null) {
				return null;
			}

			return tweet.tweep.screen_name;
		}
	}

	/**
	 * Try to convert the tweet to the preferred syntax WORD: Definition
	 * 
	 * @param tweet
	 */
	private void normalizeTweetText(Tweet tweet) {
		// make sure the delimiter is right next to the word
		tweet.text = tweet.text.replace(";", ":").replace("==", ":")
				.replace("=", ":").replace("-", ":").replace(" :", ":");
	}

	/**
	 * Make sure it has delimiters so that we can process or discard right away.
	 * 
	 * We call delimiters the separators between word and definitions common
	 * delimiters are:
	 * 
	 * : == ;
	 * 
	 * @param text
	 * @return
	 */
	private boolean hasKnownWordDelimiters(String text) {
		return text.contains(":") || text.contains("==") || text.contains(";");
	}

	public static void main(String[] arg) {
		final TweetProcessor PROCESSOR = new TweetProcessor();
		PROCESSOR.resetProcessedTweets();
		PROCESSOR.processTweets();

		// System.out.println("BOUNCED TWEETS: " + PROCESSOR.BOUNCED);
	}
}
