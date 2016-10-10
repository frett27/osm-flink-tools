
#Tool output file format


This flink job create a bunch of CSV files containing the consolidated OSM datas.

4 output files (or folders if chunked) are created :

	nodes.csv -> contains all the nodes
	ways.csv -> contains all the ways, with geometry recontructed
    rels.csv -> contains all the relations between objects
    polygons -> contains the constructed polygons :
			ie : relations with a multipolygon type tags, or ways with area="true" tag


#Geometry Encoding :

Several benchmark and experience return show that binary with Base64 format. the binary form use the ShapeFile Geometry format [ShapeFile Specification White Paper](https://www.esri.com/library/whitepapers/pdfs/shapefile.pdf)

This format is used for several reasons :

 - encoding is about 20% more efficient than JSON encoding (processing time)
 - we usually don't need an exhaustive description of geometry composition in reading the file with a text editor.
 - this save also **a lot on space on disk**, so save IO and enhance performance.

#Attributes encoding

as OSM has a schemaless attribute encoding, all the attributes are encoded in a single string, separated by a pipe for the tags couples.

in the following exmaple, two tags are defined : (**source** and **addr**)

	source=cadastre-dgi-fr source : Direction Générale des Finances Publiques - Cadastre. Mise à jour : 2014|addr:housenumber=1370



##Nodes.csv

this file contains the nodes, the schema is :

	[nodeID],[Longitude],[Lattitude],[AttributesEncodedString]


for example :

	1494807670,4.0791015,45.6086534,source=cadastre-dgi-fr source : Direction Générale des Impôts - Cadastre. Mise à jour : 2011|emergency=fire_hydrant
	1495262595,4.6469399000000005,44.369202400000006,name=Mairie de Bourg St Andéol|amenity=townhall
	1495262598,4.646618,44.368504400000006,tourism=hotel
	1681815651,5.795038300000001,45.247683300000006,mtb:scale=5|intermittent=yes|ford=yes
	1681815802,5.7976033000000005,45.249447700000005,intermittent=yes|ford=yes
	1681884394,5.770713000000001,45.1925438,source=cadastre-dgi-fr source : Direction Générale des Impôts - Cadastre. Mise à jour : 2009|amenity=atm|operator=Société Générale
	1681967592,4.8801848,44.960183400000005,highway=crossing
	1682061427,5.8887139,45.270307700000004,parking=surface|wheelchair_toilet=unknown|wheelchair=yes|capacity=10|amenity=parking
	1682073222,6.3165981,46.1460798,source=Bing|natural=tree
	1682073229,6.316142,46.146152900000004,source=Bing|natural=tree


the nodes geometry are not encoded as a binary geometry as it is more space saving and efficient.


#Ways.csv

this file contains the ways the file format is the following :


	[WayId], [ShapeFileBinaryAndBase64EncodedString],[AttributsEncodedString]


example :

	4256483,AwAAAN+hzRa10BdApL52eu8aR0DvIHam0NEXQPvVd9kKG0dAAQAAAAMAAAAAAAAA36HNFrXQF0CkvnZ67xpHQLI9xFCY0RdAGp9uRQcbR0DvIHam0NEXQPvVd9kKG0dA,created_by=JOSM|noexit=yes|highway=residential|name=Rue de la Gare
	4277439,AwAAAAtYvKKlpBNAiVHGrqNzRkDd9ML4xKUTQOh5Juq7c0ZAAQAAABAAAAAAAAAAC1i8oqWkE0CoX32XrXNGQKS3Luu0pBNAx1aGJ6pzRkCV+g1uxqQTQHurCf2nc0ZAs7IiD+mkE0Dnr+typXNGQCVBuAIKpRNAKdPDK6RzRkCgP8gNGqUTQNx9M9ajc0ZACtFr/T6lE0CJUcauo3NGQGXbs/hepRNANQu0O6RzRkCr3isEg6UTQM/JQb+lc0ZAYQybt6ClE0Az5Z4JqHNGQEHFJ1O7pRNAzLFh8qtzRkDd9ML4xKUTQCOjA5Kwc0ZApSIEmr6lE0BjPmpdtHNGQC4U+qqmpRNAnDAGe7hzRkD41KAekaUTQK3x43m6c0ZAnpeKjXmlE0DoeSbqu3NGQA==,junction=roundabout|highway=primary
	4279298,AwAAADvVEAtruRNAGO3xQjp1RkAwaYzWUb0TQCkzlMlfdUZAAQAAAAMAAAAAAAAAO9UQC2u5E0ApM5TJX3VGQIIznUJTuxNACFirdk11RkAwaYzWUb0TQBjt8UI6dUZA,highway=unclassified|name=Rue Robert Schuman
	4376147,AwAAAHA7frlgAhdAlHoFWJqWRkAr+kMzTwYXQI8g8Bu6lkZAAQAAAA0AAAAAAAAAcDt+uWACF0CUegVYmpZGQAPeuHtvAhdAyUUQQpuWRkCg2oWLgQIXQDmzAuiclkZAE0B7P38DF0ALKqp+pZZGQISJmkOtBBdAg8XhzK+WRkCciY6JOQUXQFdinpW0lkZApyTrcHQFF0C7l6ArtpZGQKco8n+iBRdAYUyfwraWRkDEo0OWuwUXQOTOlr22lkZA5NXeTPEFF0BEM/T0tpZGQFpUelwKBhdAiw3MwLeWRkCcDUP1IAYXQCvr7YW4lkZAK/pDM08GF0CPIPAbupZGQA==,highway=secondary|name=Avenue du Serment de Buchenwald|zone:maxspeed=FR:30|source:maxspeed=FR:zone30|maxspeed=30|bicycle=use_sidepath


#Polygons.csv

polygons are encoded in the same manner of ways 

example :

	14437,BQAAACRcbWpF2BNA/m/brRvmRkDtTtJX698TQPe/bbdu5kZAAgAAACsAAAAAAAAAHgAAAESwBRjx2BNAVXawtELmRkAkXG1qRdgTQJet9UVC5kZAtaEH40zYE0D1qb5OReZGQPozFxP22BNABZN2/kjmRkArhHqfT9kTQAMr4s9L5kZAKA8LtabZE0BCkv4DUeZGQIv1V9mI2RNA4sUMnlPmRkB+IT92ctkTQF3v7UxX5kZABkYDHVLZE0BJCzycZeZGQGlqI+h52RNA+o+WFGnmRkDsVioDYt0TQPe/bbdu5kZAcURSZt7dE0B3IQzyaeZGQNoU2RiF3hNAsHd/vFfmRkCQmOoTit4TQIC8V61M5kZAOlT42ALfE0B3HQXjO+ZGQPVLxFvn3xNAhy5NtibmRkDtTtJX698TQGB5kJ4i5kZACS6gYXvfE0DOieL6HOZGQNqQf2YQ3xNA/m/brRvmRkDdS3Vc6N0TQP5v260b5kZAOQq1S8DcE0Agqt87IOZGQMoRfB2C3BNA26JxTSbmRkBeClkneNwTQJlAWxgz5kZAaqCe40jcE0AIjnD/NuZGQG3XOies2xNAeUMaFTjmRkDtV0tL9tkTQARWs3M/5kZANOMjGL3ZE0Bzo8haQ+ZGQFyWCoVj2RNABvsKLUbmRkBEsAUY8dgTQFV2sLRC5kZARLAFGPHYE0BVdrC0QuZGQNK/ydu23BNAi5qeZjfmRkC90ujE1NwTQNZt9UE75kZAXLnNCvvcE0AXQ94XPOZGQIebnVYb3RNA0qKIVjjmRkD+qadFM90TQAeIOIw45kZAAbScOkXdE0DPSCmNPeZGQGUUcHQ63RNA6Jmo70bmRkDBGaFE8NwTQBZajHBJ5kZAU14robvcE0DRuTavReZGQHfvTXiu3BNAmPvkKEDmRkC0d0ZbldwTQF5LyAc95kZA0r/J27bcE0CLmp5mN+ZGQNK/ydu23BNAi5qeZjfmRkA=,type=multipolygon



#Rels.csv

relations are encoded inlined in a single line

the format is :

	[relid],[StringEncodedAttributes],[RelationEncodedString]


RelationEncodedString is a "||" separated list of relation description.


for example :

	3699399,name=Rue de Faramans|ref:FR:FANTOIR=010540111H|type=associatedStreet,role=house|type=node|relid=2827636318||role=house|type=node|relid=2827637310||role=house|type=node|relid=2827637416||role=house|type=node|relid=2827637458||role=house|type=node|relid=2827637467||role=house|type=node|relid=2827636331||role=house|type=node|relid=2827636329||role=house|type=node|relid=2827636330||role=house|type=node|relid=2827636360||role=house|type=node|relid=2827636397||role=house|type=node|relid=2827637220||role=house|type=node|relid=2827637227||role=house|type=node|relid=2827637229||role=house|type=node|relid=2827637243||role=house|type=node|relid=2827637252||role=house|type=node|relid=2827637255||role=house|type=node|relid=2827637267||role=house|type=node|relid=2827637277||role=house|type=node|relid=2827637278||role=house|type=node|relid=2827637281||role=house|type=node|relid=2827637280||role=house|type=node|relid=2827637302||role=house|type=node|relid=2827637304||role=house|type=node|relid=2827637313||role=house|type=node|relid=2827637316||role=house|type=node|relid=2827637339||role=house|type=node|relid=2827637350||role=house|type=node|relid=2827637358||role=house|type=node|relid=2827637365||role=house|type=node|relid=2827637372||role=house|type=node|relid=2827637369||role=house|type=node|relid=2827637370||role=house|type=node|relid=2827637371||role=house|type=node|relid=2827637373||role=house|type=node|relid=2827637398||role=house|type=node|relid=2827637410||role=house|type=node|relid=2827637413||role=house|type=node|relid=2827637417||role=house|type=node|relid=2827637424||role=house|type=node|relid=2827637423||role=street|type=way|relid=63535004
