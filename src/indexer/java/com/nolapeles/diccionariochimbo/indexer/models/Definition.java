package com.nolapeles.diccionariochimbo.indexer.models;

import org.bson.types.ObjectId;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.Reference;

@Entity
public class Definition {
	@Id 
	private ObjectId id;
	
	@Reference 
	public Tweep author;
	
	public String definition;
	
	@Indexed
	public long indexed_date;
	public int score;
	public int numRetweets;
	public int numWins;
	public int numFails;
	
	public int updateScore() {
		score = numRetweets*Scores.RT_POINTS + numWins*Scores.WIN_POINTS + numFails*Scores.FAIL_POINTS;
		return score;
	}
	
}