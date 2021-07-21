# Solr Misc Function Queries

## FieldValueSourceParser

This function query will take a field name, value to match against and a boost
If the field contains one or more matches for value, then we return the boost
otherwise we return 1.0. 

The aim is to multiple these boosts together. Imagine a ML model that 
given a set of values, we should boost (positively or negatively) the doc accordingly.
 
 e.g. Given a query that 'looks' like a senior java dev, we may have a model that returns
 features and boosts like the following:
 
    senior_java_dev^1.003  # +ve boost
    java_dev^1.002         # +ve boost
    sales_dev^0.1          # -ve boost
 
 With this function query we can boost in features field 'feat' like the following:
 
    boost=mul(fvboost(feat,'senior_java_dev',1.003),fvboost(feat,'java_dev',1.002),fvboost(feat,'sales_dev',0.1))
    
    
This is faster and easier to read that doing this with existing function queries , e.g.

    boost=mul(if(termfreq(feature,'senior_java_dev'),1.003,1),if(termfreq(feature,'java_dev'),1.002,1),
          if(termfreq(feature,'sales_dev'),0.1,1))) 
  
 
 ## Installation
 
 ./gradlew clean jar
 
 ### Add to solrconfig.xml
 
 e.g 
 
    <config>
    ... 
    <lib dir="${solr.install.dir:../../../..}/dist/" regex="SolrFunctionQueries-1.0-SNAPSHOT.jar" />
    ....
    <valueSourceParser name="fvboost" class="com.github.danrosher.solr.search.FieldValueSourceParser" />

    
    # "fvboost" can be any string
   
 ### Add to schema.xml
 
 e.g.
 
    <field name="features" type="string" indexed="true" stored="true" multiValued="true" />
    
 ## Usage
 
    #Here 52.01966071979866, -0.4983083573742952 is the query point
 
 ### Get boost in fl, if doc has f1 in features field, return 2 else 1 
    q=*:*&fl=*,id,f1:fvboost(features,"f1",2)        
    
 ### Boost score by features
 
    b=mul(fvboost(features,"f1",2),fvboost(features,"f2",3),fvboost(features,"f3",5)) # max boost=2*3*5=15
    &q={!boost b=$b v=$qq}
    &qq={!edismax }*:*
    &sort=score desc
 
 ### Sort by FQ
    q=*:*&sort=mul(fvboost(features,"f1",2),fvboost(features,"f2",3),fvboost(features,"f3",5))
    
### Filter by FQ
    q={!frange l=6}mul(fvboost(features,"f1",2),fvboost(features,"f2",3),fvboost(features,"f3",5))
    

        
    


    
    
      
 
 
 
 