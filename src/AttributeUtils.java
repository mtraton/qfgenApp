import com.hp.hpl.jena.rdf.model.Model;

import java.util.*;

/**
 * Created by Rael on 30.05.2016.
 */

//todo: posprzątać kod
//todo: ujednolicić nazwy zmiennych w zapytaniach
public class AttributeUtils {

    // string constants needed to create attributes
    private String roomTypeName = "pomieszczenie";
    private String attributeLineStart = "attribute(";
    private String attributeLineEnd = ").\n";
    private String nameSeparator = "_";
    private String elementSeparator = ", ";
    private String booleanValues = ", [true,false]";
    private String valueStart = "[";
    private String valueEnd = ", unspecified]";
    private String valueMarker = "$$$"; // string to mark place when we will put values of property

    static int[] roomElementsNumbers =  {0, 1, 3, 4};
    static int [] roomElementsValueNumbers = {1,3};
    static int[] roomNumbers = {1,2};

    Model model;

    public AttributeUtils(Model model)
    {
        this.model = model;
    }
    
    public String createAttributes()
    {
        // output - list of attributes
        LinkedList<String> attributeList = new LinkedList<>();

        // common variables
        List<HashMap<String, String>> queryRows;
        List<String> resultVars;
        List roomAttributes;
        HashMap<String, HashSet<String>> objectPropertyValues = new HashMap<>();
        QueryUtils query = new QueryUtils(model);

        //1. Create room element attributes
        queryRows =  query.getQueryAsListHashMap(FileUtils.queryPath);
        resultVars = query.getResultVars(FileUtils.queryPath);
        List roomElementAttributes  = iterateThroughRows("RoomElementAttributes",queryRows, resultVars, roomElementsNumbers, roomElementsValueNumbers);

        //2. Create room attributes
        queryRows =  query.getQueryAsListHashMap(FileUtils.roomQueryPath);
        resultVars = query.getResultVars(FileUtils.roomQueryPath);
        roomAttributes  = iterateThroughRows("RoomAttributes",queryRows, resultVars, roomNumbers, roomNumbers);
        List allRoomAttributes = new ArrayList<>(roomElementAttributes);
        allRoomAttributes.addAll(roomAttributes);
        
        //3. Convert list to string
        String output =   allRoomAttributes.toString();
        output = output.replace("\n,", "\n"); // delete unnecessary commas created when using toString();
        output = output.substring(1, output.length() - 1);
        return output;
    }
    
    public  String createRoomKey(HashMap<String, String> rowValues)
    {
        String s = roomTypeName + nameSeparator  + rowValues.get("rel");
        return s;
    }


    public  String createRoomElementKey2(HashMap<String, String> rowValues)
    {

        String s = rowValues.get("typPomieszczenia") + nameSeparator + rowValues.get("typ") + nameSeparator + rowValues.get("wlasnosc");
        return s;
    }

    public  String createBooleanKey(HashMap<String, String> rowValues)
    {
        String s = rowValues.get("typPomieszczenia") + nameSeparator + rowValues.get("typ");
        return s;
    }

    public  String createRoomElementKey(HashMap<Integer, String> rowValues, int[] numbers)
    {
        String key = "";
        for (int number: numbers
                ) {
            key = key + rowValues.get(number) + nameSeparator;
        }
        key = key.substring(0, key.length()-1);
        return key;
    }

    public  String createSubAttributeKey(HashMap<String, String> rowValues)
    {
        String s = roomTypeName + nameSeparator + rowValues.get("nad") + nameSeparator + rowValues.get("rel");
        return s;
    }

    public  String createElementSubAttributeKey(HashMap<String, String> rowValues)
    {
        String s = rowValues.get("typPomieszczenia") + nameSeparator + rowValues.get("typ") + nameSeparator + rowValues.get("nad") + nameSeparator  + rowValues.get("wlasnosc");
        return s;
    }


    public String createAttribute(String key)
    {
        String s = attributeLineStart + key +
           elementSeparator +
            valueStart +
            valueMarker +
            valueEnd +
            attributeLineEnd;
        return s;
    }

    public String createBooleanAttribute(String key)
    {
        String s =  attributeLineStart + key +
                booleanValues +
                attributeLineEnd;
        return s;
    }

    // iterate through QueryUtils rows and produce things
    public  List iterateThroughRows(String type, List queryRows, List resultVars, int[] variableNumbers, int[] numbers)
    {

        List attributeList = new LinkedList<>();
        HashMap<Integer, String> rowValues = new HashMap<>();
        HashMap<String, HashSet<String>> objectPropertyValues = new HashMap<>();
        HashMap row;
        for (Object hm : queryRows) {
            row = (HashMap) hm;
            for (int number: variableNumbers) {
                String key = (String) resultVars.get(number);
                String value = (String) row.get(key);
                rowValues.put(number, value);
            }

            String key = "";
            String booleanKey = "";
            // wykrywanie przypadku poatrybutu


            String s = (String) row.get("nad");


            if(type.equals("RoomElementAttributes"))
            {

                if((!s.isEmpty()))
                {
                    key = createElementSubAttributeKey(row);
                }
                else
                {
                    key = createRoomElementKey2(row);
                    booleanKey = createBooleanKey(row);
                }

            }
            else if(type.equals("RoomAttributes"))
            {
                if(!((String) row.get("nad")).isEmpty())
                {
                   key = createSubAttributeKey(row);

                }
                else
                {
                    key = createRoomKey(row);
                }

            }

            HashMap<String, HashSet<String>> subAtributeVals = new HashMap<>();

            // add property value to hashmap (we need to gather all possible values from every row)
            String value = rowValues.get(variableNumbers.length); // assumption - last variable represents value of subattribute
            HashSet<String> set;

            if(objectPropertyValues.containsKey(key))
            {
                set = objectPropertyValues.get(key);
            }
            else
            {
                set = new HashSet<>();
            }
            set.add(value);
            objectPropertyValues.put(key , set);
            // zamiast do listy dodawać te atrybuty do setu
            if(type.equals("RoomElementAttributes"))
            {

                    attributeList.add(createAttribute(key));
                if(!booleanKey.isEmpty()) {
                    attributeList.add(createBooleanAttribute(booleanKey));
                }

            }
            else if(type.equals("RoomAttributes"))
            {

                attributeList.add(createAttribute(key));
            }



        }

        List<String>  attributesWithoutDuplicates =  new ArrayList<>(new LinkedHashSet(attributeList));

        // add values of properties
        for(String propertyName : objectPropertyValues.keySet())
        {
            String values = objectPropertyValues.get(propertyName).toString();
            values =  values.substring(1, values.length() - 1);  // delete [ and ] from values string

            // iterate through list of attributes and put proper values of property
            for (String attr: attributesWithoutDuplicates) {
                if(attr.contains(propertyName))
                {
                    int elementIndex = attributesWithoutDuplicates.indexOf(attr);
                    String attributeWithValues = attr.replace(valueMarker,values );
                    attributesWithoutDuplicates.set(elementIndex, attributeWithValues);
                }
            }
        }
        return attributesWithoutDuplicates;
    }


}
