package com.nolapeles.diccionariochimbo.indexer.models;

import org.bson.types.ObjectId;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Transient;
import com.nolapeles.diccionariochimbo.services.IndexerConstants;

@Entity
public class Definition {
	@Id 
	private ObjectId id;
	
	@Indexed
	public String definition;
	
	public long indexed_date;
	public int score;
	public long lastScoreUpdateTimestamp;
	public int numRetweets;
	public int numWins;
	public int numFails;

	@Reference
	public Tweep tweepAuthor;

	@Transient
	public final static int RT_POINTS = 10;
	
	/** TODO I'm thinking of removing this */
	@Transient
	public final static int WIN_POINTS = 5;
	
	/** TODO I'm thinking of removing this */
	@Transient
	public final static int FAIL_POINTS = -4;

	
	/**
	 * If the score has not been updated within the {@link IndexerConstants}.DEFINITION_SCORE_EXPIRATION_INTERVAL
	 * it will calculate the score for this definition.
	 * @return true if the score was updated.
	 */

	public boolean updateScore() {
		// We don't want to be updating scores unnecesarily.
		if (System.currentTimeMillis() - lastScoreUpdateTimestamp > IndexerConstants.DEFINITION_SCORE_EXPIRATION_INTERVAL) { 
			score = numRetweets*RT_POINTS + numWins*WIN_POINTS + numFails*FAIL_POINTS;
			lastScoreUpdateTimestamp = System.currentTimeMillis();
			return true;
		}
		return false;
	}

	
	@Override
	public boolean equals(Object obj) {
		Definition other = (Definition) obj; 
		return definition.equals(other.definition);
	}
	
}