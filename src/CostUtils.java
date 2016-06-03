import com.hp.hpl.jena.rdf.model.Model;
import javafx.util.Pair;

import java.util.*;

/**
 * Created by Rael on 02.06.2016.
 */
public class CostUtils {

    String costBegin = "cost(";
    String costEnd = ")";
    String elementSeparator = "_";
    String valueSeparator = "";

    public Model model;

    public CostUtils(Model model) {
        this.model = model;
    }

    public List<String> getAttributeNames() {
        AttributeUtils attributeUtils = new AttributeUtils(model);
        String outputData = attributeUtils.createAttributes();
        String lines[] = outputData.split("\\r?\\n");
        List<String> attributeNames = Arrays.asList(lines);
        for (String attributeName : attributeNames) {
            // delete obsolete data
            //attribute(pomieszczenie_telewizor_kolor_intensywność,[ciemny, unspecified]).
            // (pomieszczenie_telewizor_kolor_intensywność,
            int startIndex = attributeName.indexOf("(");
            int endIndex = attributeName.indexOf(",") + 1;
            int listElementIndex = attributeNames.indexOf(attributeName);
            String shortenedName = attributeName.substring(startIndex, endIndex);
            attributeNames.set(listElementIndex, shortenedName);
        }

        return attributeNames;
    }

    public String createCosts() {
        //cost(classroom_desk_color_intensity,40).

        //1. Wykonaj zapytanie pytające o koszta
        List<HashMap<String, String>> queryRows;
        List<String> resultVars;
        List roomAttributes;
        HashMap<String, HashSet<String>> objectPropertyValues = new HashMap<>();
        QueryUtils query = new QueryUtils(model);

        //1. Create room element attributes
        queryRows = query.getQueryAsListHashMap(FileUtils.costQueryPath);
        resultVars = query.getResultVars(FileUtils.costQueryPath);
        //pomieszczenie_intensywność
        //2. Na podstawie wierszy zapytania zbuduj listę kosztów
        //  a. wyciągnij z listy atrybutów wszystkie nazwy atrybutów
        List<String> attributeNames = getAttributeNames();
        Pair<String, String> classIdentifier = null;

        String key;
        String value;

        for (Object hm : queryRows) {
            HashMap row = (HashMap) hm;
            {
                // get row elements
                String indQueryName = resultVars.get(0);
                String indId = (String) row.get(indQueryName); // koszt_Biurko

                String attrQueryName = resultVars.get(1);
                String attrName = (String) row.get(attrQueryName); // Intensywność

                String attrCostName = resultVars.get(2);
                String attrCost = (String) row.get(attrCostName); // 10

                String overAttrName =  resultVars.get(3);
                String overAttr = (String) row.get(overAttrName); // Kolor
                boolean test = overAttr.isEmpty();
                String namePart = "";

                // znajdź nazwę klasy
                if (attrName.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) // here we get the name of class from ontology of which costs we are operating on
                {
                    // e.g. : biurko
                    value = indId;
                    key = attrCost;
                    classIdentifier = new Pair<>(key, value); // e.g. biurko, koszt_Biurko
                    continue; // this line does not have cost value associated
                }
                // stwórz string wyszukania atrybutu


                //todo: popatrzeć na klasę ResultFormatter;
                // koszt klasy
                if (attrName.equals("koszt") ) {
                    namePart = classIdentifier.getKey() + ","; //biurko,
                    //cost(biurko, 10)
                    // atrybuty klasy
                    // podatrybuty klasy
                }
                else if(overAttr.isEmpty() == false) {
                        namePart = classIdentifier.getKey() + "_" + overAttr + "_" + attrName;
                        //cost(pomieszczenie_biurko_kolor_intensywność,10)
                        // dlaczego nie jest 40
                        // dlaczego się powtarza
                } else if (indId.equals(classIdentifier.getValue())) { // tu jest błąd
                    namePart = classIdentifier.getKey() + "_" + attrName + ",";
                }

                if(!namePart.isEmpty())
                {
                    for (String attributeName : attributeNames
                            ) {
                        if(attributeName.contains(namePart))
                        {
                            int attributeNameIndex = attributeNames.indexOf(attributeName);
                            String costString = "cost" + attributeName + attrCost + ")\n";
                            attributeNames.set(attributeNameIndex, costString);
                        }
                    }
                }
            }
        }
        List<String> costList = new LinkedList<>();
        // delete attributes without costs:
        for (String name :attributeNames) {
            // Do something
            if(name.contains("cost"))
            {
                costList.add(name);
            }

        }

        String costs = "";
        for (String s : costList
                ) {
            costs += s;
        }
        //costs = costs.substring(1, costs.length() -1); // delete [] braces


        return costs;
    }

}


