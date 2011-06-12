package com.nolapeles.diccionariochimbo.indexer.models;

import java.util.List;

import org.bson.types.ObjectId;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.Reference;

@Entity
public class Word {
	@Id 
	private ObjectId id;
	
	@Indexed
	public String word;
	
	@Reference
	public List<Definition> definitions;
	
	@Reference
	public Definition best_definition;
}
