import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.hc.client5.http.impl.classic.*;
import org.apache.hc.client5.http.impl.io.*;
import org.apache.hc.core5.http.io.entity.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

public class App {
    public static void main(String[] args) throws Exception {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(); // this is for threadsafe client creation so that I can do 1k requests simultaneously without getting "address already in use"
        connectionManager.setMaxTotal(1000);
        connectionManager.setDefaultMaxPerRoute(1000);
        HttpClientBuilder clientBuilder = HttpClients.custom().setConnectionManager(connectionManager);
        System.out.println("Started Trials Scraper With Clan");
        // Sets up threads for later performance
        ExecutorService executorService = Executors.newFixedThreadPool(1000);
        // required headers for all requests
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-API-KEY","<API key here>");
        // Setting up necessary JSON parsing tools, and creating a JSON object of the root player, comments include some test case users
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode toJson = mapper.createObjectNode();
        toJson.put("displayName","Java-12");
        toJson.put("displayNameCode","1924");
        System.out.println("User input, searching");
        String url = "https://www.bungie.net/Platform/Destiny2/SearchDestinyPlayerByBungieName/-1/";
        CloseableHttpResponse response = http.requestPost(url, toJson, headers, clientBuilder); // Sends a POST request with json body
        // Reads the response and gets info
        String jsonString = EntityUtils.toString(response.getEntity());
        JsonNode json = mapper.readTree(jsonString);
        JsonNode result = json.get("Response").get(0);
        String membershipId = result.get("membershipId").asText();
        String membershipType = result.get("membershipType").asText();
        url = String.format("https://www.bungie.net/Platform/GroupV2/User/%s/%s/0/1/",membershipType, membershipId); // Pulls the bungie profile
        response = http.requestGet(url, headers, clientBuilder);
        System.out.println("User found, pulling clan");
        // Grabs the group ID from the user and searches the group
        jsonString = EntityUtils.toString(response.getEntity());
        json = mapper.readTree(jsonString);
        String clanId = json.get("Response").get("results").get(0).get("group").get("groupId").asText();
        url = String.format("https://www.bungie.net/Platform/GroupV2/%s/Members/",clanId);
        response = http.requestGet(url, headers, clientBuilder);
        jsonString = EntityUtils.toString(response.getEntity());
        json = mapper.readTree(jsonString);
        List<JsonNode> players = new ArrayList<JsonNode>(); // The sole purpose of this list is to get turned into another list, I could forego it in the interest of code golf but it makes for a readability nightmare
        json.get("Response").get("results").elements().forEachRemaining(players::add);
        Map<String, String> members = new HashMap<String, String>();
        long startTime = System.currentTimeMillis();
        for (JsonNode i : players) {
            membershipType = i.get("destinyUserInfo").get("membershipType").asText();
            membershipId = i.get("destinyUserInfo").get("membershipId").asText();
            members.put(membershipId, membershipType);
        }
        // now you see why it would be horribly unreadable to do this directly into a map
        long endTime = System.currentTimeMillis();
        System.out.println("Clan Members Grabbed, pulling characters");
        System.out.println(endTime-startTime);
        startTime = System.currentTimeMillis();
        // grabs each member id, then grabs every character
        List calls = new ArrayList<>();
        for (Map.Entry<String, String> entry : members.entrySet()) {
            Callable task = new getCharacters(entry, clientBuilder);
            calls.add(task);
        }
        List<FutureTask> results = executorService.invokeAll(calls);
        List<ArrayList> characters = new ArrayList<>();
        results.forEach(f -> {
            try {
                characters.add((ArrayList) f.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
        // each character is paired with the membershipId + membershipType for future requests
        endTime = System.currentTimeMillis();
        System.out.println("Characters Grabbed, pulling Activity History");
        System.out.println(endTime-startTime);
        startTime = System.currentTimeMillis();
        calls.clear();
        // pulls every page of a filtered activity list from each character, this is info that can be set private on a per-account basis
        for (ArrayList<String> j : characters) {
            membershipId = j.get(0);
            membershipType = j.get(1);
            for (int k = 2; k != j.size(); k++) {
                Callable task = new activityHistory(membershipId, membershipType, j.get(k), clientBuilder);
                calls.add(task);
            }
        }
        results.clear();
        results = executorService.invokeAll(calls);
        // unifies everything
        ArrayList<ObjectNode> history = new ArrayList<ObjectNode>();
        for (FutureTask l : results) {
            Collection<? extends ObjectNode> matchList = (ArrayList) l.get();
            history.addAll(matchList);
        }
        endTime = System.currentTimeMillis();
        System.out.println("Match history built, grabbing games");
        System.out.println(endTime-startTime);
        startTime = System.currentTimeMillis();
        calls.clear();
        // goes through each individual game and grabs the relevant data
        for (ObjectNode i : history) {
            Callable task = new grabMatch(i, clientBuilder);
            calls.add(task);
        }
        results.clear();
        results = executorService.invokeAll(calls);
        ArrayList<ArrayList<String>> matches = new ArrayList<ArrayList<String>>();
        for (FutureTask k : results) {
            ArrayList activity = (ArrayList) k.get();
            matches.add(activity);
        }
        // yeah I'm too lazy to do a more robust filtering but this works well enough- it keeps matches with multiple clan members on the same fireteam from being duplicated in the dataset
        HashSet<ArrayList> filteredMatches = new HashSet();
        filteredMatches.addAll(matches);
        endTime = System.currentTimeMillis();
        System.out.println("matches grabbed, building file");
        System.out.println(endTime-startTime);
        // makes the .csv file where I can then import to google sheets or whatever and putz around with
        int fileNum = 0;
        String fileName = toJson.get("displayName").asText() + "#" + toJson.get("displayNameCode").asText();
        String extension = ".csv";
        String path = "output/";
        String finalPath = path + fileName + "-" + fileNum + extension;
        Path filePath = Paths.get(finalPath);
        while (Files.exists(filePath)) {
            fileNum++;
            finalPath = path + fileName +"-"+ fileNum + extension;
            filePath = Paths.get(finalPath);
        }
        Files.createFile(filePath);
        String headline = "day,month,year,bluescore,redscore,competitive?,blowout?";
        Files.write(filePath, headline.getBytes());
        for (List i : filteredMatches) {
            String timeStamp = i.get(2).toString();
            String day = ""+timeStamp.charAt(8) + timeStamp.charAt(9);
            String month = ""+timeStamp.charAt(5)+timeStamp.charAt(6);
            String year = ""+timeStamp.charAt(0)+timeStamp.charAt(1)+timeStamp.charAt(2)+timeStamp.charAt(3);
            String line = "\n"+day+","+month+","+year+","+i.get(0)+","+i.get(3)+","+i.get(4)+","+i.get(5);
            Files.write(filePath, line.getBytes(), StandardOpenOption.APPEND);
            //formatted bluescore id timestamp redscore competitive blowout - timestamp was used in a different iteration, and I plan to bring it back to investigate flawless queue
        } 
        System.out.println("File built, task finished");
        executorService.shutdown();
    }
}
