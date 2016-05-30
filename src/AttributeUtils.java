import com.hp.hpl.jena.rdf.model.Model;

import java.util.*;

/**
 * Created by Rael on 30.05.2016.
 */
public class AttributeUtils {
    //class dataAttributes

    // string constants needed to create attributes
    private String roomTypeName = "pomieszczenie";
    private String attributeLineStart = "attribute(";
    private String attributeLineEnd = ").\n";
    private String nameSeparator = "_";
    private String elementSeparator = ",";
    private String booleanValues = "[true,false]";
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

    public  String createBooleanAttributeString(HashMap<Integer, String> rowValues)
    {
        int [] numbers =   {0, 1};  //variable values numbers from QueryUtils which are needed to create given attribute

        return attributeLineStart + rowValues.get(numbers[0]) + nameSeparator + rowValues.get(numbers[1]) + elementSeparator + booleanValues + attributeLineEnd;
    }

    public String createPropertyAttributeString(HashMap<Integer, String> rowValues)
    {
        int [] numbers =   {0,1,3}; //variable values numbers from QueryUtils which are needed to create given attribute

        return attributeLineStart + rowValues.get(numbers[0]) + nameSeparator + rowValues.get(numbers[1]) + nameSeparator + rowValues.get(numbers[2]) + elementSeparator + valueStart + valueMarker + valueEnd + attributeLineEnd;
    }

    public  String createRoomAttribute(HashMap<Integer, String> rowValues)
    {
        int [] numbers =   {1};
        String roomProperty = rowValues.get(numbers[0]); //variable values numbers from QueryUtils which are needed to create given attribute

        return attributeLineStart +
                "pomieszczenie" +
                nameSeparator +
                roomProperty +
                elementSeparator +
                valueStart +
                valueMarker +
                valueEnd +
                attributeLineEnd;
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

        //3. Create attributes with subattributes
        queryRows =  query.getQueryAsListHashMap(FileUtils.queryDescPath);
        resultVars = query.getResultVars(FileUtils.queryDescPath);
        List<String> allAttributes = new LinkedList<>();

        HashMap<String, HashSet<String>> subAtributeVals = new HashMap<>();

        for (HashMap<String, String> row: queryRows)
        {
            String subAttribute = row.get(resultVars.get(0));
            String attribute = row.get(resultVars.get(1));
            String subAttributeVal =  row.get(resultVars.get(2));
            for(String attr : (List<String>) allRoomAttributes)
            {
                if(attr.contains(attribute))
                {
                    String newAttribute = attr.replace(attribute, attribute + nameSeparator + subAttribute);

                    // delete everything after [ character
                    newAttribute = newAttribute.substring(0, newAttribute.indexOf("[") +1);

                    HashSet<String> set;
                    if(subAtributeVals.containsKey(subAttribute))
                    {
                        set = subAtributeVals.get(subAttribute);
                    }
                    else
                    {
                        set = new HashSet<>();
                    }
                    set.add(subAttributeVal);
                    subAtributeVals.put(subAttribute,set);
                    allAttributes.add(newAttribute);
                }
            }
        }
        // add values of subattribute
        for(String key : subAtributeVals.keySet())
        {
            for (String attribute: allAttributes
                    ) {
                if (attribute.contains(key))
                {
                    String vals = subAtributeVals.get(key).toString();
                    vals = vals.substring(1, vals.length() -1); // delete [] from string
                    vals = vals + valueEnd + attributeLineEnd;
                    String newAttribute = attribute.concat(vals);
                    allAttributes.set(allAttributes.indexOf(attribute), newAttribute);
                }
            }
        }
        allAttributes.addAll(allRoomAttributes);
        String output =  allAttributes.toString();
        output = output.replace("\n,", "\n"); // delete unnecessary commas created when using toString();
        output = output.substring(1, output.length() - 1);
        return output;
    }

    public  String createRoomKey(HashMap<Integer, String> rowValues)
    {
        String key = roomTypeName  + nameSeparator + rowValues.get(1) ;
        return key;

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

    public  String createSubAttributeKey(HashMap<Integer, String> rowValues)
    {
        return rowValues.get(0);
    }

    // iterate through QueryUtils rows and produce things
    public  List iterateThroughRows(String type, List queryRows, List resultVars, int[] variableNumbers, int[] numbers)
    {

        List attributeList = new LinkedList<>();
        HashMap<Integer, String> rowValues = new HashMap<>();
        HashMap<String, HashSet<String>> objectPropertyValues = new HashMap<>();
        for (Object hm : queryRows) {
            HashMap row = (HashMap) hm;
            for (int number: variableNumbers) {
                String key = (String) resultVars.get(number);
                String value = (String) row.get(key);
                rowValues.put(number, value);
            }

            String key = "";

            if(type.equals("RoomElementAttributes"))
            {
                key = createRoomElementKey(rowValues, numbers);
            }
            else if(type.equals("RoomAttributes"))
            {
                key = createRoomKey(rowValues);
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

            if(type.equals("RoomElementAttributes"))
            {
                attributeList.add(createBooleanAttributeString(rowValues));
                attributeList.add(createPropertyAttributeString(rowValues));
            }
            else if(type.equals("RoomAttributes"))
            {
                attributeList.add(createRoomAttribute(rowValues));
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
