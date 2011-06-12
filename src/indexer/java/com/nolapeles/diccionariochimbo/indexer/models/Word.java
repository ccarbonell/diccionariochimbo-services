package com.nolapeles.diccionariochimbo.indexer.models;

import java.util.List;

public class Word {
	public int id;
	public String word;
	public List<Definition> definitions;
	public Definition best_definition;
}
