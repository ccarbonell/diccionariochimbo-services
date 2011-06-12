package com.nolapeles.diccionariochimbo.indexer;

import java.net.UnknownHostException;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.nolapeles.diccionariochimbo.indexer.models.Definition;
import com.nolapeles.diccionariochimbo.indexer.models.Tweep;
import com.nolapeles.diccionariochimbo.indexer.models.Word;

/**
 * This class is used to bootstrap the MongoDB
 * connection and to map our POJOS.
 * @author gubatron
 *
 */
public class MongoMapper {
	
	private static final int MONGO_PORT = 27017;

	private static MongoMapper INSTANCE;

	private Mongo mongo;
	
	private Morphia morphia;

	private Datastore dataStore;
	
	private MongoMapper(String host, int port) {
		try {
			mongo = new Mongo(host, port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (MongoException e) {
			e.printStackTrace();
		}
		
		morphia = new Morphia();
		mapModels();
		
		dataStore = morphia.createDatastore(mongo, "dc");
	}
	
	private MongoMapper() {
		this("localhost",MONGO_PORT);
	}
	
	private void mapModels() {
		morphia.map(Word.class);
		morphia.map(Tweep.class);
		morphia.map(Definition.class);
	}
	
	public static MongoMapper instance() {
		return instance("localhost",MONGO_PORT);
	}
	
	public static MongoMapper instance(String host, int port) {
		if (INSTANCE == null) {
			INSTANCE = new MongoMapper(host, port);
		}
		
		return INSTANCE;
	}
	
	public Morphia getMorphia() {
		return morphia;
	}
	
	public Datastore getDatastore() {
		return dataStore;
	}
}
