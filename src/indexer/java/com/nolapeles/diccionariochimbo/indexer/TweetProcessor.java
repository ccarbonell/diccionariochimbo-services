package com.nolapeles.diccionariochimbo.indexer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private final static Pattern PATTERN_WORD_DEFINITION = Pattern.compile(".*?\\\"?([\\S]*)\\\"?\\:\\s?(.*)");

	private Datastore _ds;
	private Map<Long, Tweep> _seenTweeps;
	
	public TweetProcessor() {
		_ds = MongoMapper.instance().getDatastore();
		_seenTweeps = new HashMap<Long, Tweep>();
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
		
		Word word = new Word();
		Definition definition = new Definition();
		
		extractWordAndDefinition(word_definition_text, word, definition);
		
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
		
		_ds.save(word);
	}

	public void saveNewWordAndDefinition(Tweet tweet, String potentialAuthor,
			int rtCount, Word word, Definition definition) {
		//This looks like it's not the owner of the tweet.
		if (rtCount == 1) {
			if (potentialAuthor!=null) {
				//TODO: Search for that Tweep and use it as the owner of the definition.
				//tweet.tweep = fetchTweepByName(potentialAuthor);
			} else {
				
			}
		}
		
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
		
		_ds.save(word);
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
			d.definition = matcher.group(2).toLowerCase();
			
			System.out.println("+ ["+w.word+"] -> ["+d.definition+"]");
		} else {
			System.out.println("! " + text);
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
		
		System.out.println("BEFORE: [" + text + "]");
		
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
