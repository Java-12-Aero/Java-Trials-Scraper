import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.hc.client5.http.impl.classic.*;
import org.apache.hc.core5.http.io.entity.*;

import com.fasterxml.jackson.databind.*;

public class getCharacters implements Callable {
    final Map.Entry<String, String> member;
    final HttpClientBuilder clientBuilder;
    public getCharacters(Map.Entry<String,String> member,HttpClientBuilder clientBuilder) {
        this.member = member;
        this.clientBuilder = clientBuilder;
    }
    public List<String> call() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-API-KEY","<API key here>");
        ObjectMapper mapper = new ObjectMapper();
        String membershipId = member.getKey().toString();
        String membershipType = member.getValue().toString();
        String url = String.format("https://www.bungie.net/Platform/Destiny2/%s/Profile/%s/",membershipType,membershipId);
        Map<String, String> parameters = new HashMap<String,String>();
        parameters.put("components","200"); // This is a filter parameter that requests the characters from a player
        CloseableHttpResponse response = http.requestGet(url, parameters, headers, clientBuilder);
        String jsonString = EntityUtils.toString(response.getEntity());
        JsonNode json = mapper.readTree(jsonString).get("Response").get("characters").get("data");
        List<String> player = new ArrayList<String>();
        player.add(membershipId);
        player.add(membershipType);
        json.fieldNames().forEachRemaining(player::add); // builds list of character IDs
        response.close();
        return player;
    }
}