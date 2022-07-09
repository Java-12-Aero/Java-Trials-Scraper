import java.util.*;
import java.util.concurrent.*;

import org.apache.hc.client5.http.impl.classic.*;
import org.apache.hc.core5.http.io.entity.*;

import com.fasterxml.jackson.databind.*;

public class grabMatch implements Callable {
    final JsonNode match;
    final ArrayList<String> game;
    final HttpClientBuilder clientBuilder;
    public grabMatch(JsonNode match, HttpClientBuilder clientBuilder) throws Exception {
        this.match = match;
        this.game = new ArrayList<String>();
        this.clientBuilder = clientBuilder;
    }
    public ArrayList<String> call() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-API-KEY","<API Key Here>");
        ObjectMapper mapper = new ObjectMapper();
        String redScore = new String();
        String blueScore = match.get("values").get("teamScore").get("basic").get("displayValue").asText();
        String id = match.get("activityDetails").get("instanceId").asText();
        String timestamp = match.get("period").asText();
        game.add(blueScore);
        game.add(id);
        game.add(timestamp);
        redScore = "5";
        if (blueScore.equals("5")) { // if the player's team loses, we know that the opposing team's score is 5, no matter what. I actually encountered an error where the entire enemy team disconnected, and thus didn't actually exist according to the pgcr. doing this check saves time and network resources since the only reason I pull the pgcr is for the red team's score
            redScore = "0";
            CloseableHttpResponse response = http.requestGet("https://stats.bungie.net/Platform/Destiny2/Stats/PostGameCarnageReport/"+id+"/", headers, clientBuilder);
            String jsonString = EntityUtils.toString(response.getEntity());
            JsonNode json = mapper.readTree(jsonString);
            response.close();
            if (json.get("ErrorCode").asText().equals("1")) {
                JsonNode teams = json.get("Response").get("teams");
                if (teams.asText().length() > 1){ // there's no rhyme or reason to which team is which side, so I just check which one lost
                    if (teams.get(0).get("score").get("basic").get("displayValue").asText().equals("5")) {
                        redScore = teams.get(1).get("score").get("basic").get("displayValue").asText();
                    } else {
                        redScore = teams.get(0).get("score").get("basic").get("displayValue").asText();
                    }
                }
            }
            
            } else {
                redScore = "5";
            }
            game.add(redScore);
            String blowout = "no";
            String competitive = "no";
            if (Integer.valueOf(blueScore)+ Integer.valueOf(redScore) >= 8) { // yay math!
                competitive = "yes";
            } else if (Integer.valueOf(blueScore)+ Integer.valueOf(redScore) == 5) {
                blowout = "yes";
            }
            game.add(competitive);
            game.add(blowout);
        Thread.sleep(new Random().nextInt(1000)); // I can probably remove this, this was part of my attempts to prevent address already in use errors before I switched to threadsafe client creation
        return game;
    }
}