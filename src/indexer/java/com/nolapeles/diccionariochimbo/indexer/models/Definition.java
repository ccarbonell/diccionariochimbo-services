package com.nolapeles.diccionariochimbo.indexer.models;

import org.bson.types.ObjectId;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Reference;

@Entity
public class Definition {
	@Id 
	private ObjectId id;
	
	@Reference 
	public Tweep author;
	
	public String definition;
	public long indexed_date;
	public int score;
	public int numRetweets;
	public int numWins;
	public int numFails;
}
