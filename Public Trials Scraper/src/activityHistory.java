import java.util.*;
import java.util.concurrent.*;

import org.apache.hc.client5.http.impl.classic.*;
import org.apache.hc.core5.http.io.entity.*;

import com.fasterxml.jackson.databind.*;

public class activityHistory implements Callable {
    final String membershipId;
    final String membershipType;
    final String characterId;
    final HttpClientBuilder clientBuilder;
    public activityHistory(String membershipId, String membershipType, String characterId, HttpClientBuilder clientBuilder) {
        this.membershipId = membershipId;
        this.membershipType = membershipType;
        this.characterId = characterId;
        this.clientBuilder = clientBuilder;
    }
    public ArrayList<JsonNode> call() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-API-KEY","<API key here>");
        ObjectMapper mapper = new ObjectMapper();
        ArrayList<JsonNode> activities = new ArrayList<JsonNode>();
        Map<String, String> search = new HashMap<String, String>();
        search.put("count","250"); // max page length is 250 activities
        search.put("mode","84"); // filters to just trials of osiris
        search.put("page","0"); // 0-index page
        int more;
        int page;
        page = 0;
        more = 1;
        search.replace("page",String.valueOf(page));
        String url = "https://www.bungie.net/Platform/Destiny2/"+membershipType+"/Account/"+membershipId+"/Character/"+characterId+"/Stats/Activities";
        while (more != 0) {
            search.replace("page",String.valueOf(page));
            CloseableHttpResponse response = http.requestGet(url, search, headers, clientBuilder);
            String jsonString = EntityUtils.toString(response.getEntity());
            JsonNode json = mapper.readTree(jsonString).get("Response");
            if (json != null && json.size() > 0) {
                if (json.get("activities").size() == 250 ) {
                    page++;
                } else {
                    more = 0;
                }
                for (JsonNode jsonNode : json.get("activities")) {
                    activities.add(jsonNode); // individually goes through the history and adds to list. I tried addall() but it didn't play nice with json nodes
                }
            } else {
                more = 0;
            }
            if (json == null && page == 0) {
                System.out.println("User probably has data set to private"); // I could technically leave this out but it's kind of nice to know
            }
            response.close();
        }
        return activities;
    }
}