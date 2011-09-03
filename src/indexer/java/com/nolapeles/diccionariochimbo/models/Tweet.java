package com.nolapeles.diccionariochimbo.models;

import org.bson.types.ObjectId;

import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.Reference;

/**
 * POJO Representing a raw tweet.
 * 
 * @author gubatron
 *
 */
public class Tweet {
	@Id
	public ObjectId id;
	
	@Indexed
	public long tweet_id;
	
	public String text;

	public boolean processed;
	
	@Reference
	public Tweep tweep;
	
	@Override
	public String toString() {
		String tweep_str = (tweep == null) ? "null" : tweep.toString();
		
		return "[Tweet] {id: "+tweet_id+",\n\ttext: "+text+",\n\ttweep: "+tweep_str+"}";
	}
}
