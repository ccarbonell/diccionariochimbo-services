package com.nolapeles.diccionariochimbo.indexer.models;

import org.bson.types.ObjectId;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexed;

/**
 * POJO representing a Twitter user.
 * 
 * Fields mimic the ones depicted on the twitter api
 * http://apiwiki.twitter.com/w/page/22554755/Twitter-REST-API-Method:-users%C2%A0show
 * 
 * @author gubatron
 *
 */

@Entity
public class Tweep {
	@Id 
	private ObjectId id;
	
	public String name;

	@Indexed
	public String screen_name;
	
	@Indexed
	public int user_id;
	
	public String location;
	public String description;
	public String profile_image_url;
	public String url;
}
