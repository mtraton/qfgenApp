import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Create entry elements of dataset
 */
public class EntryUtils {

    /*
    createEntries - creates entries based on QueryUtils (about rooms)

    ex:
    entry(data{classroom_color:yellow, classroom_color_intensity:unspecified, classroom_name:"Laboratorium 316", classroom_size:small, id:lab316, place:classroom}).
    entry(data{classroom_computer:true, classroom_computer_color:white, classroom_computer_model:unspecified, id:lab318}).
     */

    Model model;

    public EntryUtils(Model model)
    {
        this.model = model;
    }

    public String createEntries() {

        QueryUtils query = new QueryUtils(model);
        ArrayList queryList = (ArrayList) query.getQueryAsListHashMap(FileUtils.queryPath);


        HashMap describesQuery = describes();


        //todo: hard-code keys (?)
        String type = "typ";
        String property = "wlasnosc";
        String value = "wartosc";
        String roomID = "pomieszczenie";
        String roomType = "typPomieszczenia";
        String obj = "obj";



        StringBuilder entry = new StringBuilder();

        boolean newEntry = true;

        //Loop for every line of QueryUtils
        for(int i=0; i<queryList.size(); i++) {

            HashMap h = (HashMap) queryList.get(i);

            String dataRoomType = (String)h.get(roomType);
            String dataType = (String)h.get(type);
            String dataProperty = (String)h.get(property);
            String dataValue = (String)h.get(value);
            String dataID = (String)h.get(roomID);

            //APPEND:
            if(newEntry) {
                entry.append( "entry(data{" );
                entry.append( dataRoomType );
                entry.append("_");
                entry.append( dataType );
                entry.append(":true");
                entry.append(", ");
            }

            entry.append( dataRoomType );
            entry.append("_");
            entry.append( dataType );
            entry.append("_");

            if(describesQuery.containsKey(dataProperty)) {
                String valDescribed = (String)describesQuery.get(dataProperty);

                entry.append( valDescribed.trim() );// delete carriage returns
                entry.append("_");
            }

            entry.append( dataProperty );
            entry.append(":");
            entry.append( dataValue );
            entry.append(", ");


            if(i<queryList.size()-1) {
                HashMap h1 = (HashMap) queryList.get(i+1);

                String dataObj = (String)h.get(obj);
                String dataNEXTObj = (String)h1.get(obj);

                if( dataObj.equals( dataNEXTObj ) ) {
                    newEntry = false;
                } else {
                    newEntry = true;
                }
            }


            if(newEntry) {
                entry.append("id:");
                entry.append( dataID.trim() );  //REMEMBER trim() !!
                entry.append("}).");
                entry.append("\n");
            }
        }


        return entry.toString();
    }


    public String createEntriesRooms() {

        QueryUtils query = new QueryUtils(model);
        ArrayList queryList = (ArrayList) query.getQueryAsListHashMap(FileUtils.roomAttrQueryPath);


        HashMap describesQuery = describes();


        //todo: hard-code keys (?)
        String roomID = "id";
        String rel = "rel";
        String value = "val";
        String roomType = "roomType";


        StringBuilder entry = new StringBuilder();

        boolean newEntry = true;

        //Loop for every line of QueryUtils
        for(int i=0; i<queryList.size(); i++) {

            HashMap h = (HashMap) queryList.get(i);

            String dataID = (String)h.get(roomID);
            String dataRel = (String)h.get(rel);
            String dataValue = (String)h.get(value);
            String dataRoomType = (String)h.get(roomType);

            //APPEND:
            if(newEntry) {
                entry.append( "entry(data{" );
            }

            if(!dataRel.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                entry.append( dataRoomType );
                entry.append("_");

                if(describesQuery.containsKey(dataRel)) {
                    String valDescribed = (String)describesQuery.get(dataRel);

                    entry.append( valDescribed.trim() );// delete carriage returns
                    entry.append("_");
                }

                entry.append( dataRel );
                entry.append(":");
                entry.append( dataValue );
                entry.append(", ");
            }

            if(i<queryList.size()-1) {
                HashMap h1 = (HashMap) queryList.get(i+1);

                String dataObj = (String)h.get(roomID);
                String dataNEXTObj = (String)h1.get(roomID);

                if( dataObj.equals( dataNEXTObj ) ) {
                    newEntry = false;
                } else {
                    newEntry = true;
                }
            }


            if(newEntry) {
                entry.append("id:");
                entry.append( dataID.trim() );  //REMEMBER trim() !!

                entry.append(", ");
                entry.append("place:");
                entry.append( dataRoomType );

                entry.append("}).");
                entry.append("\n");
            }
        }


        return entry.toString();
    }



    /*
    Function to create HashMap for what describes what(target)
    example:
    room_tv_intensity:dark
    vs
    room_tv_color_intensity:dark
     */
    public  HashMap describes() {


        String queryDescPath = FileUtils.queryDescPath;
        //String queryDescPath = "..\\descQuery";
        //String queryDescPath = "C:\\Users\\Rael\\Dropbox\\Uczelnia\\Workshop\\descQuery";

        String queryString = FileUtils.readFile(queryDescPath, StandardCharsets.UTF_8);

        com.hp.hpl.jena.query.Query query = QueryFactory.create(queryString);

        QueryExecution qe = QueryExecutionFactory.create(query, model);
        ResultSet results = qe.execSelect();

        List<String> resultVars = query.getResultVars();

        // Output QueryUtils results
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ResultSetFormatter.outputAsCSV(baos, results);
        String queryResult = "";
        //save result to String from outputStream
        try {
            queryResult = baos.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // delete ontology URI from QueryUtils results since they're irrelevant in this context
        queryResult = queryResult.replace(FileUtils.ontologyIRI, "");
        queryResult = queryResult.replace(FileUtils.ontologyIRI, "");
        // turn into lowercase
        queryResult = queryResult.toLowerCase();
        // split into lines
        String[] queryLines = queryResult.split("\n", -1);


        // TO HashMap
        int rowNumber = queryLines.length - 1;  // we are ommiting first line since it only contains var names
        HashMap describesQuery = new HashMap(rowNumber);

        // split each line into array of seperate values and put them into hashmap
        for (int lineCounter = 1; lineCounter < rowNumber; lineCounter++) // we are ommiting first line since it only contains var names
        {
            String[] explodedRow = queryLines[lineCounter].split(",", -1);

            describesQuery.put(explodedRow[0].trim(),explodedRow[1].trim());

            //System.out.println("key:"+explodedRow[0].trim() +", value:"+explodedRow[1].trim()+",");
            //System.out.println("key:"+describesQuery.containsKey("intensywność") +", value:"+describesQuery.get("intensywność")+",");
        }


        return describesQuery;
    }


}


