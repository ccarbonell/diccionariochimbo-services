package com.nolapeles.diccionariochimbo.indexer.models;

import java.util.List;

import org.bson.types.ObjectId;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.NotSaved;
import com.nolapeles.diccionariochimbo.services.MongoMapper;

/**
 * This will be saved as a single object which will basically serve as a map to save settings for the application.
 * @author gubatron
 *
 */
public class Settings {
	@Id
	public ObjectId id;

	public String SITE_NAME;
	
	/** The ID of the last Tweet we fetched */
	public long TWEET_FETCHER_LAST_TWEET_ID;

	/** THE hashtag we use for search - If we spin another website because this tag is
	 * too localized we can always configure TweetFetcher by just changing this value
	 * for that instance of the site.*/
	public String SEARCH_HASHTAG;

	///////////////////////////////////////////////////////////////////////////
	
	@NotSaved
	private static Settings INSTANCE;
	
	public static Settings instance() {
		load();
		return INSTANCE;
	}
	
	/**
	 * If the application is running for the first time ever, this is the method to initialize all the settings.
	 * @return
	 */
	private static Settings create() {
		INSTANCE = new Settings();

		INSTANCE.SITE_NAME = "DiccionarioChimbo.com";
		INSTANCE.TWEET_FETCHER_LAST_TWEET_ID = -1;
		INSTANCE.SEARCH_HASHTAG = "#diccionariochimbo";
		
		return INSTANCE;
	}
	
	
	/**
	 * Loads (or reloads) settings.
	 * If settings have not been created, it will create and store the settings.
	 */
	public static void load() {
		 Datastore ds = MongoMapper.instance().getDatastore();
		 List<Settings> found = ds.find(Settings.class).asList();
		 int size = found.size();
		 
		 if (size == 1) {
			 //the one and only
			 INSTANCE = found.get(0);
		 } else {
			 INSTANCE = Settings.create();
			 ds.save(INSTANCE);
		 }  
	}
	
	public void save() {
		//nothing to save.
		if (INSTANCE == null) {
			return;
		}
		
		MongoMapper.instance().getDatastore().save(INSTANCE);;
	}
}
