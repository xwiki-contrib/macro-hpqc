package org.xwiki.hpqc.api;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HPQCRequirementsBean {

    private static final String APPLICATION_JSON = "application/json";
    private static final String MACRO_HYPERLINK = "macro-hyperlink";
    private static final String HPQC_JSON_MODEL_ID = "id";
    private static final String HPQC_JSON_MODEL_ENTITIES = "entities";
    private static final String HPQC_JSON_MODEL_TYPE = "Type";
    private static final String HPQC_JSON_MODEL_FIELDS = "Fields";
    private static final String HPQC_JSON_MODEL_VALUE = "value";
    private static final String HPQC_JSON_MODEL_VALUES = "values";
    private static final String HPQC_JSON_MODEL_NAME = "Name";
    private static final String HPQC_ISSUE_ERROR_ID = HPQC_JSON_MODEL_ID;
    private static final String HPQC_ISSUE_ERROR_NAME = "name";
    private static final String HPQC_ISSUE_ERROR_STATUS = "status";
    private static final String HPQC_PATH_AUTH = "qcbin/authentication-point/authenticate";

    private String hpqc_host_url = "";
    private String hpqc_hyperlink_url = "";
    private String hpqc_path_query_favorites = "";
    private String hpqc_path_query_service_type = "";
    /**
     * Query parameter: page-size value
     */
    private int row_size = 50;

    private CloseableHttpClient httpclient = null;
    private CredentialsProvider credsProvider = new BasicCredentialsProvider();
    private URI uri = null;
    private List<Header> headers = new ArrayList<Header>();
    private ResponseHandler<String> responseHandler = null;
	private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * TODO add documentation
     *
     * @param hpqc_host_url
     * @param hpqc_domain_name
     * @param hpqc_project_name
     * @param userName
     * @param password
     */
    public HPQCRequirementsBean(String hpqc_host_url, String hpqc_domain_name, String hpqc_project_name, String userName, String password) {
	this.hpqc_host_url = hpqc_host_url;
	this.hpqc_hyperlink_url = String.format("testdirector:%s/qcbin,%s,%s,[AnyUser]", hpqc_host_url.replaceAll("https://", "").replaceAll("http://", ""), hpqc_domain_name, hpqc_project_name);
	this.hpqc_path_query_favorites = String.format("qcbin/rest/domains/%s/projects/%s/favorites", hpqc_domain_name, hpqc_project_name);
	this.hpqc_path_query_service_type = String.format("qcbin/rest/domains/%s/projects/%s/requirements", hpqc_domain_name, hpqc_project_name);

	/**
	 * Default Headers
	 */
	this.headers.add(new BasicHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON));
	this.headers.add(new BasicHeader("accept", APPLICATION_JSON));
	this.headers.add(new BasicHeader("Content-Type", APPLICATION_JSON));
	this.headers.add(new BasicHeader("X-PrettyPrint", "1"));

	/**
	 * Creating a response handler.
	 */
	this.responseHandler = new ResponseHandler<String>() {

	    @Override
	    public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
		int status = response.getStatusLine().getStatusCode();
		if (status >= 200 && status < 300) {
		    HttpEntity entity = response.getEntity();
		    return entity != null ? EntityUtils.toString(entity, "UTF-8") : null;
		} else {
		    throw new ClientProtocolException("Unexpected response status: " + status);
		}
	    }

	};

	try {
	    this.uri = new URIBuilder(hpqc_host_url).build();
	    this.credsProvider.setCredentials(new AuthScope(uri.getHost(), uri.getPort(), AuthScope.ANY_REALM), new UsernamePasswordCredentials(userName, password));
	    this.httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).setDefaultHeaders(headers).build();
	    getAuthentification();
	} catch (IOException e) {
		logger.error("Network error, please check the stack trace.", e);
	} catch (URISyntaxException e) {
		logger.error("URI syntax error, please check the stack trace.", e);
	}
	}

    /**
     * TODO add documentation
     *
     * @throws org.apache.http.client.ClientProtocolException
     * @throws java.io.IOException
     * @throws java.net.URISyntaxException
     */
    private void getAuthentification() throws ClientProtocolException, IOException, URISyntaxException {

	String path = String.format("%s/%s", hpqc_host_url, HPQC_PATH_AUTH);
	HttpGet httpget = new HttpGet(path);

	/**
	 * Request config
	 */
	CloseableHttpResponse response = httpclient.execute(httpget);

	logRequest(response, httpget);
	EntityUtils.consume(response.getEntity());

    }

    /**
     * TODO add documentation
     * 
     * @param params
     * @return
     */
    public List<HashMap<String, String>> getIssuesList(String params) {

	List<HashMap<String, String>> issuesList = new ArrayList<HashMap<String, String>>();

	for (String param : params.split(",")) {

	    if (param.matches("[-+]?\\d*\\.?\\d+")) {
		HashMap<String, String> issue = getIssueByID(param);

		if (null == issue) {
		    issue = getErrorIssue(param);
		} else {
		    issuesList.add(issue);
		}
	    } else {

		List<HashMap<String, String>> issues = getIssuesByFavorite(param, this.row_size);
		if (null == issues) {
		    issuesList.add(getErrorIssue(param));
		} else {
		    issuesList.addAll(issues);
		}
	    }
	}

	return issuesList;
    }

    /**
     * TODO add documentation
     * 
     * @param param
     * @return
     */
    private List<HashMap<String, String>> getIssuesByFavorite(String param, int newRowSize) {

	List<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
	String favoriteQuery = getFavoriteQuery(param);
	String path = "";

	try {
	    String favoriteQueryEncoded = URLEncoderRFC1738.encode(String.format("{%s}", favoriteQuery), "UTF-8");
        path = String.format("%s/%s/?page-size=%d&query=%s", hpqc_host_url, hpqc_path_query_service_type, newRowSize, favoriteQueryEncoded);
	    HttpGet httpget = new HttpGet(path);

	    /**
	     * Creating request configuration.
	     */

	    CloseableHttpResponse response = httpclient.execute(httpget);
	    String json = httpclient.execute(httpget, responseHandler);
	    logRequest(response, httpget);
	    EntityUtils.consume(response.getEntity());

	    /**
	     * Creating JSON Objects.
	     */
	    JSONParser jsonParser = new JSONParser();
	    Object obj = jsonParser.parse(json);
	    JSONObject jsonObject = (JSONObject) obj;
	    JSONArray entities = (JSONArray) jsonObject.get(HPQC_JSON_MODEL_ENTITIES);

	    /**
	     * Iterating entities.
	     */

	    for (int j = 0; j < entities.size(); j++) {
		HashMap<String, String> issue = new HashMap<String, String>();
		JSONObject fieldsList = (JSONObject) entities.get(j);

		/**
		 * Adding Issue Type
		 */
		String issueTypeArray = fieldsList.get(HPQC_JSON_MODEL_TYPE).toString();
		issue.put(HPQC_JSON_MODEL_TYPE, issueTypeArray);

		JSONArray fields = (JSONArray) fieldsList.get(HPQC_JSON_MODEL_FIELDS);

		/**
		 * JSON-Simple does not offers type safety.
		 */
		@SuppressWarnings("unchecked")
		Iterator<JSONObject> iterator = fields.iterator();

		while (iterator.hasNext()) {
		    JSONObject issueEntry = (JSONObject) iterator.next();
		    JSONArray issueValues = (JSONArray) issueEntry.get(HPQC_JSON_MODEL_VALUES);

		    if (null != issueEntry.get(HPQC_JSON_MODEL_NAME)) {
			for (int i = 0; i < issueValues.size(); i++) {
			    JSONObject issueValue = (JSONObject) issueValues.get(i);
			    if (null != issueValue.get(HPQC_JSON_MODEL_VALUE)) {
				issue.put(issueEntry.get(HPQC_JSON_MODEL_NAME).toString(), issueValue.get(HPQC_JSON_MODEL_VALUE).toString());
			    }
			}
		    }

		}

		/**
		 * Adding hyper link.
		 */
		issue.put(MACRO_HYPERLINK, String.format("%s;%s:%s", hpqc_hyperlink_url, issue.get(HPQC_JSON_MODEL_TYPE), issue.get(HPQC_JSON_MODEL_ID)));

		/**
		 * Adding the issue to the list.
		 */
		result.add(issue);

	    }

	} catch (IOException e) {
		logger.error("Network error, please check the stack trace.", e);
	} catch (ParseException e) {
		logger.error("Parse error, please check the stack trace.", e);
	}

		return result;
    }

    /**
     * TODO add documentation
     * 
     * @param favoriteName
     * @return
     */
    private String getFavoriteQuery(String favoriteName) {
	String result = "";

	try {
	    String favoriteQuery = URLEncoderRFC1738.encode(String.format("{name[\"%s\"]}", favoriteName), "UTF-8");
	    String path = String.format("%s/%s/?query=%s", hpqc_host_url, hpqc_path_query_favorites, favoriteQuery);
	    HttpGet httpget = new HttpGet(path);

	    /**
	     * Request config
	     */

	    CloseableHttpResponse response = httpclient.execute(httpget);
	    String json = httpclient.execute(httpget, responseHandler);
	    logRequest(response, httpget);
	    EntityUtils.consume(response.getEntity());

	    /**
	     * JSON
	     */
	    JSONParser jsonParser = new JSONParser();
	    Object obj = jsonParser.parse(json);
	    JSONObject jsonObject = (JSONObject) obj;

	    JSONArray msg = (JSONArray) jsonObject.get("Favorites");

	    /**
	     * JSON-Simple does not offers type safety.
	     */

	    @SuppressWarnings("unchecked")
	    Iterator<JSONObject> iterator = msg.iterator();

	    while (iterator.hasNext()) {
		JSONObject favoriteEntry = (JSONObject) iterator.next();
		JSONObject favoriteFilter = (JSONObject) favoriteEntry.get("Filter");
		JSONObject favoriteValues = (JSONObject) favoriteFilter.get("Where");
		JSONArray favoriteField = (JSONArray) favoriteValues.get("Field");

		if (null != favoriteField) {
		    for (int i = 0; i < favoriteField.size(); i++) {
			JSONObject favoriteFilterValue = (JSONObject) favoriteField.get(i);
			if (null != favoriteFilterValue.get("Value")) {
			    result += String.format("%s[%s]", favoriteFilterValue.get(HPQC_JSON_MODEL_NAME), favoriteFilterValue.get("Value"));
			    if (i < favoriteField.size() - 1) {
				result += ";";
			    }
			}

		    }
		}

	    }

	} catch (ParseException e) {
		logger.error("Parse error, please check the stack trace.", e);
	} catch (IOException e) {
		logger.error("Network error, please check the stack trace.", e);
	}

		return result;
    }

    /**
     * TODO add documentation
     * 
     * @param id
     * @return
     */
    private HashMap<String, String> getIssueByID(String id) {
	HashMap<String, String> result = new HashMap<String, String>();

	try {
	    String path = String.format("%s/%s/%s", hpqc_host_url, hpqc_path_query_service_type, id);
	    HttpGet httpget = new HttpGet(path);

	    /**
	     * Request config
	     */

	    CloseableHttpResponse response = httpclient.execute(httpget);
	    String json = httpclient.execute(httpget, responseHandler);
	    logRequest(response, httpget);
	    EntityUtils.consume(response.getEntity());

	    /**
	     * JSON
	     */
	    JSONParser jsonParser = new JSONParser();
	    Object obj = jsonParser.parse(json);
	    JSONObject jsonObject = (JSONObject) obj;

	    /**
	     * Type
	     */
	    String issueTypeArray = jsonObject.get(HPQC_JSON_MODEL_TYPE).toString();
	    result.put(HPQC_JSON_MODEL_TYPE, issueTypeArray);

	    JSONArray msg = (JSONArray) jsonObject.get(HPQC_JSON_MODEL_FIELDS);

	    /**
	     * JSON-Simple does not offers type safety.
	     */
	    @SuppressWarnings("unchecked")
	    Iterator<JSONObject> iterator = msg.iterator();

	    while (iterator.hasNext()) {
		JSONObject issueEntry = (JSONObject) iterator.next();
		JSONArray issueValues = (JSONArray) issueEntry.get(HPQC_JSON_MODEL_VALUES);

		if (null != issueEntry.get(HPQC_JSON_MODEL_NAME)) {
		    for (int i = 0; i < issueValues.size(); i++) {
			JSONObject issueValue = (JSONObject) issueValues.get(i);
			if (null != issueValue.get(HPQC_JSON_MODEL_VALUE)) {
			    result.put(issueEntry.get(HPQC_JSON_MODEL_NAME).toString(), issueValue.get(HPQC_JSON_MODEL_VALUE).toString());
			}
		    }
		}

	    }

	    /**
	     * Adding hyper link.
	     */
	    
        String hyperlinkValue = String.format("%s;%s:%s", hpqc_hyperlink_url, result.get(HPQC_JSON_MODEL_TYPE), result.get(HPQC_JSON_MODEL_ID));
	    result.put(MACRO_HYPERLINK, hyperlinkValue);

	} catch (IOException e) {
		logger.error("Network error, please check the stack trace.", e);
	} catch (ParseException e) {
		logger.error("Parse error, please check the stack trace.", e);
	}

		return result;
    }

    /**
     * TODO add documentation
     * 
     * @param response
     * @param httpget
     */
    private void logRequest(CloseableHttpResponse response, HttpGet httpget) {
	// TODO add logging
		logger.debug("----------------------------------------");
		logger.debug("Executing request " + httpget.getRequestLine());
		logger.debug(response.getStatusLine().toString());
    }

    /**
     * Returning a default error object to inform the user.
     * 
     * @param id
     * @return
     */
    private HashMap<String, String> getErrorIssue(String id) {
	HashMap<String, String> result = new HashMap<String, String>();

	result.put(HPQC_ISSUE_ERROR_ID, id);
	result.put(HPQC_ISSUE_ERROR_NAME, id);
	result.put(HPQC_ISSUE_ERROR_STATUS, id);

	return result;
    }

    /**
     * This method will set the page size parameter.
     * @param newRowSize
     */
    public void setRowSize(final int newRowSize) {
        this.row_size = newRowSize;
    }
}
