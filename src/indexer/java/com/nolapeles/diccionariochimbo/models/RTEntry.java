package com.nolapeles.diccionariochimbo.models;

import org.bson.types.ObjectId;

import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Reference;

public class RTEntry {
	@Id 
	public ObjectId id;
	
	@Reference
	public Tweet tweet;

	public RTEntry(Tweet t) {
		this.tweet = t;
	}

	@Override
	public boolean equals(Object obj) {
		RTEntry other = (RTEntry) obj;
		return other.tweet.tweep.equals(this.tweet.tweep);
	}
	
	@Override
	public int hashCode() {
		return this.tweet.tweep.hashCode();
	}
}