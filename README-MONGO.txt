
QUICK MONGO REFERENCE:

The database is called "dc" as in "diccionario chimbo"

Here's an example of how to quickly query the DB using mongodb's command line

gubatron$ mongo
MongoDB shell version: 1.6.4
connecting to: test
> //use db database
> use dc
switched to db dc

> // the equivalent of SHOW TABLES in MySQL
> show collections
Definition
Tweep
Word
system.indexes

> //the equivalent of SELECT * FROM WORD;
> db.Word.find()
> db.Tweep.find()
{ "_id" : ObjectId("4df4d746bf2df2c8073ad79c"), "className" : "com.nolapeles.diccionariochimbo.indexer.models.Tweep", "screen_name" : "tweeper", "location" : "the location", "description" : "tweep description" }
{ "_id" : ObjectId("4df4d747bf2df2c8073ad79d"), "className" : "com.nolapeles.diccionariochimbo.indexer.models.Tweep", "screen_name" : "bob_sponge", "location" : "bottom of the sea", "description" : "another tweep" }
> db.Word.find()
{ "_id" : ObjectId("4df4d747bf2df2c8073ad7a0"), 
  "className" : "com.nolapeles.diccionariochimbo.indexer.models.Word", 
  "word" : "PABELLON", 
  "definitions" : [
		   { "$ref" : "Definition", "$id" : ObjectId("4df4d747bf2df2c8073ad79e")  },
		   { "$ref" : "Definition", "$id" : ObjectId("4df4d747bf2df2c8073ad79f")  }
   ] }

// Let's read one of the definitions
> db.Definition.find({"_id":ObjectId("4df4d747bf2df2c8073ad79e")})
{ "_id" : ObjectId("4df4d747bf2df2c8073ad79e"), 
  "className" : "com.nolapeles.diccionariochimbo.indexer.models.Definition", 
  "author" : { "$ref" : "Tweep", "$id" : ObjectId("4df4d746bf2df2c8073ad79c") }, 
  "definition" : "Lo que le dijo Paul a John", 
  "indexed_date" : NumberLong("1307891526844"), 
  "score" : 0, 
  "numRetweets" : 0, 
  "numWins" : 5, 
  "numFails" : 0 }

Some useful commands:

// SHOW DATABASES
> show dbs

// USE DB
> use myDatabase

// DROP DATABASE (after you're using it invoke)
> use dc
> db.dropDatabase()

> db.dropDatabase //will show the javascript code of that method.

// DROP TABLE
> db.Tweep.drop()

For more info http://www.mongodb.org/display/DOCS/SQL+to+Mongo+Mapping+Chart
