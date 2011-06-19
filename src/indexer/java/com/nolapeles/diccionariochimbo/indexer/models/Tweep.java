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
	
	@Indexed
	public String screen_name;
	
	@Indexed
	public long user_id;
	
	public String location;
	public String profile_image_url;
	
	public int social_score;
	public int content_score;
	public int bonus_score;
	
	@Override
	public String toString() {
		return "[Tweep] {user_id: "+user_id+
			",\n\tscreen_name: "+screen_name+
			",\n\tlocation: "+location+
			",\n\tprofile_image_url: "+profile_image_url+"}";
		
	}

}
