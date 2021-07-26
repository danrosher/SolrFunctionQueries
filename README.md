# Solr Misc Function Queries

---

## FieldValueSourceParser

This function query will take a field name, value to match against and a boost
If the field contains one or more matches for value, then we return the boost
otherwise we return 1.0. 

The aim is to multiply these boosts together. Imagine a ML model that from a query
provides a set of values & boosts, we should apply (positively or negatively) to docs accordingly.
 
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
    
---
---

        
## DecayFunctionSourceParser

## Installation
 
    ./gradlew clean jar
 
### Add to solrconfig.xml

    <config>
        ... 
        <lib dir="${solr.install.dir:../../../..}/dist/" regex="SolrNVector-1.0-SNAPSHOT.jar" />
        ....
        <valueSourceParser name="linear" class="com.github.danrosher.solr.search.decayfunction.LinearDecayFunctionValueSourceParser" />
        <valueSourceParser name="gauss" class="com.github.danrosher.solr.search.decayfunction.GaussDecayFunctionValueSourceParser" />
        <valueSourceParser name="exp" class="com.github.danrosher.solr.search.decayfunction.ExponentialDecayFunctionValueSourceParser" />
        
        # "linear","gauss","exp" can be any strings, but these make sense here.

### Add to schema.xml

    <fieldType name="pint" class="solr.IntPointField" docValues="true"/>
    <fieldType name="plong" class="solr.LongPointField" docValues="true"/>
    <fieldType name="pdouble" class="solr.DoublePointField" docValues="true"/>
    <fieldType name="pfloat" class="solr.FloatPointField" docValues="true"/>
    
    <fieldType name="pdate" class="solr.DatePointField" docValues="true"/>
    
    <fieldType name="location" class="solr.LatLonPointSpatialField" docValues="true"/>
    ...
    <field name="pdate" type="pdate" indexed="true" stored="true" multiValued="false" />
    <field name="location" type="location" indexed="true" stored="true" multiValued="false" />
    
    <dynamicField name="*_i"  type="pint"    indexed="true"  stored="true" multiValued="false"/>
    <dynamicField name="*_l"  type="plong"   indexed="true"  stored="true" multiValued="false"/>
    <dynamicField name="*_f"  type="pfloat"  indexed="true"  stored="true" multiValued="false"/>
    <dynamicField name="*_d"  type="pdouble" indexed="true"  stored="true" multiValued="false"/>
    
### e.g. decay boost score by numerical(salary),geo(location)

    b=mul(gauss("price",20,0),gauss("location","2km",11,12))
    &q={!boost b=$b v=$qq}
    &qq={!edismax }*:*
    &sort=score+desc
    &fl=*,score   


## Description
 

Decay functions score a document with a function that decays depending on the distance of a numeric field value of the 
document from a user given origin. This is similar to a range query, but with smooth edges instead of boxes.

To use distance scoring on a query that has numerical fields, the user has to define an origin and a scale for each 
field. The origin is needed to define the “central point” from which the distance is calculated, and the scale to 
define the rate of decay. The decay function is specified as     

    <decay_function>(<field-name>,scale,origin,offset,decay) for numerical/date field
    <decay_function>(<field-name>,scale,origin_lat,origin_lon,offset,decay) for geo fields
    
-  <decay_function>  should be one of 'linear', 'exp', or 'gauss'

-  The <field-name> must be a NumericFieldType, DatePointField, or LatLonPointSpatialField field, 
NOT multi-valued.

    
    e.g. linear("location","23km",52.0247, -0.490,"0km",0.5)

In the above example, the field is a geo_point and origin can be provided in geo format. scale and offset must be given 
with a unit in this case. If your field is a date field, you can set scale and offset as days, hours, as with 
[DateMath](https://solr.apache.org/guide/working-with-dates.html#date-math). 

    e.g. gauss(pdate,"+2DAY+6HOUR","2021-07-20T00:00:00Z","+3DAY",0.5) 

pdate: DatePointField
"+2DAY+6HOUR": range 
"2021-07-20T00:00:00Z: origin (defaults to NOW)
"+3DAY: offset (defaults to zero)
0.5: decay

- **origin**  The point of origin used for calculating distance. Must be given as a number for numeric field, date for date fields 
  and geo point for geo fields. Required for geo and numeric field. For date fields the default is NOW. 
  Date math (for example NOW-1h) is supported for origin.

- **scale** Required for all types. Defines the distance from origin + offset at which the computed score will equal 
decay parameter. For geo fields: Can be defined as number+unit (1km, 12m,...). Default unit is KM. 
For date fields: Can to be defined as a number+unit ("1h", "10d",…). For numeric field: Any number.

- **offset**  If an offset is defined, the decay function will only compute the decay function for documents with a 
distance greater than the defined offset. The default is 0.

- **decay** The decay parameter defines how documents are scored at the distance given at scale. 
If no decay is defined, documents at the distance scale will be scored 0.5.

In the first example, your documents might represents hotels and contain a geo location field. 
You want to compute a decay function depending on how far the hotel is from a given location. 
You might not immediately see what scale to choose for the gauss function, but you can say something like: 
"At a distance of 2km from the desired location, the score should be reduced to one third." 
The parameter "scale" will then be adjusted automatically to assure that the score function computes a score of 0.33 
for hotels that are 2km away from the desired location.

In the second example, documents with a field value between 2013-09-12 and 2013-09-22 would get a weight of 1.0 and 
documents which are 15 days from that date a weight of 0.5.

### Supported decay functions

The DECAY_FUNCTION determines the shape of the decay:

**gauss**
Normal decay, computed as:

score(doc) =  exp(- (max(0,|doc.val - origin| - offset)^2)/2sig^2)

where sig is computed to assure that the score takes the value decay at distance scale from origin+-offset

sig^2 = -scale^2/(2.ln(decay))

**exp**
Exponential decay, computed as:

score(doc) =  exp(lmda .  max(0,|doc.val - origin| - offset))

lmda = ln(decay)/scale

where again the parameter lambda is computed to assure that the score takes the value decay at distance scale
 from origin+-offset


**linear**
Linear decay, computed as:

score(doc) =  max((s-v)/s,0)

where:
v = max(0,|doc.val - origin| - offset)
s = scale(1.0-decay)) 

where again the parameter s is computed to assure that the score takes the value decay at distance 
scale from origin+-offset

In contrast to the normal and exponential decay, this function actually sets the score to 0 if the field value 
exceeds twice the user given scale value.

For single functions the three decay functions together with their parameters can be visualized like this 
(the field in this example called "age"):

![age(linear,gauss,exp)](https://www.elastic.co/guide/en/elasticsearch/reference/current/images/decay_2d.png 
"age(linear,gauss,exp)")


### Detailed example

Suppose you are searching for a hotel in a certain town. Your budget is limited. Also, you would like the hotel to be 
close to the town center, so the farther the hotel is from the desired location the less likely you are to check in.

You would like the query results that match your criterion (for example, "hotel, Nancy, non-smoker") to be scored with 
respect to distance to the town center and also the price.

Intuitively, you would like to define the town center as the origin and maybe you are willing to walk 2km to the town 
center from the hotel.In this case your origin for the location field is the town center and the scale is ~2km.

If your budget is low, you would probably prefer something cheap above something expensive. For the price field, the 
origin would be 0 Euros and the scale depends on how much you are willing to pay, for example 20 Euros.

In this example, the fields might be called "price" for the price of the hotel and "location" for the coordinates 
of this hotel.

The function for price in this case could be:    

    gauss("price",20,0) //or linear,exp
    
and for location:  

    gauss("location","2km",11,12) //or leanear,exp   
    
Suppose you want to multiply these two functions on the original score, the request would look like this:

    b=mul( gauss("price",20,0),gauss("location","2km",11,12))
    &q={!boost b=$b v=$qq}
    &qq={!edismax }*:*
    &sort=score+desc
    &fl=*,score
    
Next, we show how the computed score looks like for each of the three possible decay functions.

### Normal decay, function gaussed
When choosing gauss as the decay function in the above example, the contour and surface plot of the multiplier 
looks like this:

![Distance/Price](https://f.cloud.github.com/assets/4320215/768157/cd0e18a6-e898-11e2-9b3c-f0145078bd6f.png 
"Distance/Price")

![Distance/Price](https://f.cloud.github.com/assets/4320215/768160/ec43c928-e898-11e2-8e0d-f3c4519dbd89.png 
"Distance/Price")

Suppose your original search results matches three hotels :

- "Backback Nap"
- "Drink n Drive"
- "BnB Bellevue".

"Drink n Drive" is pretty far from your defined location (nearly 2 km) and is not too cheap (about 13 Euros) 
so it gets a low factor a factor of 0.56. "BnB Bellevue" and "Backback Nap" are both pretty close to the defined 
location but "BnB Bellevue" is cheaper, so it gets a multiplier of 0.86 whereas "Backpack Nap" gets a value of 0.66.

### Exponential decay, function exp
When choosing exp as the decay function in the above example, the contour and surface plot of the multiplier 
looks like this:

![Distance/Price](https://f.cloud.github.com/assets/4320215/768161/082975c0-e899-11e2-86f7-174c3a729d64.png
"Distance/Price")

![Distance/Price](https://f.cloud.github.com/assets/4320215/768162/0b606884-e899-11e2-907b-aefc77eefef6.png
"Distance/Price")

### Linear decay, function linear

When choosing linear as the decay function in the above example, the contour and surface plot of the multiplier 
looks like this:

![Distance/Price](https://f.cloud.github.com/assets/4320215/768164/1775b0ca-e899-11e2-9f4a-776b406305c6.png
"Distance/Price")

![Distance/Price](https://f.cloud.github.com/assets/4320215/768165/19d8b1aa-e899-11e2-91bc-6b0553e8d722.png
"Distance/Price")


    


        
    
    










  








       
    


    
    
      
 
 
 
 