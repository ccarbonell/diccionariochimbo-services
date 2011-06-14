package com.nolapeles.diccionariochimbo.indexer.models;

import org.bson.types.ObjectId;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.Reference;
import com.nolapeles.diccionariochimbo.indexer.IndexerConstants;

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
	public long lastScoreUpdateTimestamp;
	public int numRetweets;
	public int numWins;
	public int numFails;
	
	/**
	 * If the score has not been updated within the {@link IndexerConstants}.DEFINITION_SCORE_EXPIRATION_INTERVAL
	 * it will calculate the score for this definition.
	 * @return true if the score was updated.
	 */
	public boolean updateScore() {
		/** We don't want to be updating scores unnecesarily. */
		if (System.currentTimeMillis() - lastScoreUpdateTimestamp > IndexerConstants.DEFINITION_SCORE_EXPIRATION_INTERVAL) { 
			score = numRetweets*Scores.RT_POINTS + numWins*Scores.WIN_POINTS + numFails*Scores.FAIL_POINTS;
			lastScoreUpdateTimestamp = System.currentTimeMillis();
			return true;
		}
		return false;
	}
	
}