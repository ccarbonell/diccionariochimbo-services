package com.nolapeles.diccionariochimbo.indexer.models;

/**
 * POJO representing a Twitter user.
 * 
 * Fields mimic the ones depicted on the twitter api
 * http://apiwiki.twitter.com/w/page/22554755/Twitter-REST-API-Method:-users%C2%A0show
 * 
 * @author gubatron
 *
 */
public class Tweep {
	public int id;
	public String name;
	public String screen_name;
	public String location;
	public String description;
	public String profile_image_url;
	public String url;
}
