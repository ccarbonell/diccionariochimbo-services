package com.nolapeles.diccionariochimbo.indexer;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.nolapeles.diccionariochimbo.indexer.models.Definition;
import com.nolapeles.diccionariochimbo.indexer.models.Settings;
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

	private Mongo _mongo;
	
	private Morphia _morphia;

	private Datastore _dataStore;
	
	private MongoMapper(String host, int port) {
		try {
			_mongo = new Mongo(host, port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (MongoException e) {
			e.printStackTrace();
		}
		
		_morphia = new Morphia();
		
		mapModels();
		
		_dataStore = _morphia.createDatastore(_mongo, "dc");
	}
	
	private MongoMapper() {
		this("localhost",MONGO_PORT);
	}
	
	private void mapModels() {
		_morphia.map(Tweep.class);
		_morphia.map(Definition.class);
		_morphia.map(Word.class);		
		_morphia.map(Settings.class);
		

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
		return _morphia;
	}
	
	public Datastore getDatastore() {
		return _dataStore;
	}
	
	private static void testCreateTweeps() {
		MongoMapper mongoMapper = MongoMapper.instance();
		
		Tweep t1 = new Tweep();
		t1.location = "the location";
		t1.screen_name = "tweeper";
		
		Tweep t2 = new Tweep();
		t2.location = "bottom of the sea";
		t2.screen_name = "bob_sponge";
		
		Word w1 = new Word();
		w1.word = "PABELLON";
		
		Definition d1 = new Definition();
		d1.tweep = t1;
		d1.definition = "Lo que le dijo Paul a John";
		d1.indexed_date = System.currentTimeMillis();
		d1.numFails = 0;
		d1.numWins = 5;

		Definition d2 = new Definition();
		d2.tweep = t2;
		d2.definition = "Para alguien muy grande que es bello.";
		d2.indexed_date = System.currentTimeMillis();
		d2.numFails = 9;
		d2.numWins = 0;
		
		w1.definitions = new ArrayList<Definition>();
		w1.definitions.add(d1);
		w1.definitions.add(d2);
		
		Word w2 = new Word();
		w2.word = "VENTILADOR";
		
		Definition d3 = new Definition();
		d3.tweep = t2;
		d3.definition = "Lo que una prostituta le dice a un cliente cachondo.";
		
		w2.definitions = Arrays.asList(d3);
		
		Datastore ds = mongoMapper.getDatastore();
		
		
		ds.save(t1);
		ds.save(t2);
		ds.save(d1);
		ds.save(d2);
		
		ds.save(w1);
		
		ds.save(d3);
		ds.save(w2);

		
		List<Tweep> found = ds.find(Tweep.class).asList();
		for (Tweep t : found) {
			System.out.println(t);
		}
		
	}
	
	public static void main(String[] args) {
		testCreateTweeps();
	}
}
