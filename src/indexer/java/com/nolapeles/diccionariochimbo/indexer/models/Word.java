package com.nolapeles.diccionariochimbo.indexer.models;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bson.types.ObjectId;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.Transient;

@Entity
public class Word {
	@Id 
	private ObjectId id;
	
	@Indexed
	public String word;
	
	@Embedded
	public List<Definition> definitions;
	
	@Transient
	private Comparator<Definition> BEST_DEFINITION_COMPARATOR = new Comparator<Definition>() {

		@Override
		public int compare(Definition a, Definition b) {
			return a.score - b.score;
		}
	};
	
	public Definition getBestDefinition() {
		if (definitions.size()==1) {
			return definitions.get(0);
		}
		
		for (Definition d : definitions) {
			d.updateScore();
		}
		
		Collections.sort(definitions, BEST_DEFINITION_COMPARATOR);
		
		return definitions.get(0);
	}
	
}
