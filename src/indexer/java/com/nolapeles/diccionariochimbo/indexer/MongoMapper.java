package com.nolapeles.diccionariochimbo.indexer;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

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
		morphia.map(Tweep.class);
		morphia.map(Word.class);
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
	
	private static void testCreateTweeps() {
		MongoMapper mongoMapper = MongoMapper.instance();
		
		Tweep t1 = new Tweep();
		t1.description = "tweep description";
		t1.location = "the location";
		t1.screen_name = "tweeper";
		
		Word w1 = new Word();
		w1.word = "PABELLON";
		
		Definition d1 = new Definition();
		d1.author = t1;
		d1.definition = "Lo que le dijo Paul a John";
		d1.indexed_date = System.currentTimeMillis();
		d1.numFails = 0;
		d1.numWins = 5;
		
		w1.definitions = new ArrayList<Definition>();
		w1.definitions.add(d1);
		
		Datastore ds = mongoMapper.getDatastore();
		
		
		ds.save(t1);
		ds.save(d1);
		ds.save(w1);
				
		
		
		
		List<Tweep> found = ds.find(Tweep.class).asList();
		for (Tweep t : found) {
			System.out.println(t);
		}
		
	}
	
	public static void main(String[] args) {
		testCreateTweeps();
	}
}
