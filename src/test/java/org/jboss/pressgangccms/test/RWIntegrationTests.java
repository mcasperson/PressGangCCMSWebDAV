package org.jboss.pressgangccms.test;

import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.pressgangccms.rest.v1.entities.RESTTagV1;
import org.jboss.pressgangccms.utils.common.StringUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.annotation.AxisRange;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkHistoryChart;
import com.carrotsearch.junitbenchmarks.annotation.LabelType;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;

import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.matcher.RestAssuredMatchers.*;
import static org.hamcrest.Matchers.*;

@AxisRange(min = 0, max = 20)
@BenchmarkHistoryChart(filePrefix = "rwbenchmarks", labelWith = LabelType.TIMESTAMP, maxRuns = 356)
public class RWIntegrationTests extends AbstractBenchmark implements TestBase
{
	/** Jackson object mapper */
	private final ObjectMapper mapper = new ObjectMapper();

	/**
	 * This method will run through a create, update and delete cycle on a entity.
	 * 
	 * @param getURL
	 * The REST endpoint to get the entity
	 * @param createURL
	 *            The REST endpoint to create the entity
	 * @param createJson
	 *            The created entity's JSON
	 * @param updateURL
	 *            The REST endpoint to update the entity
	 * @param updateJson
	 *            The updated entity's JSON
	 * @param deleteURL
	 *            The REST endpoint to delete the entity
	 */
	private void createUpdateDelete(final String getURL, final String createURL, final String createJson, final String updateURL, final String updateJson, final String deleteURL)
	{
		final Map<String, String> env = System.getenv();
		if (env.containsKey(RESTPASS) && env.containsKey(RESTUSER))
		{
			final String restUser = env.get(RESTUSER);
			final String restPass = env.get(RESTPASS);

			RestAssured.reset();
			RestAssured.baseURI = "http://devrest-pressgangccms.rhcloud.com";
			RestAssured.authentication = basic(restUser, restPass);
			RestAssured.urlEncodingEnabled = true;
			RestAssured.port = 80;

			/* Attempt to create an entity */
			final Response createResponse = given().body(createJson).contentType(JSON_CONTENT_TYPE).post(createURL);

			assertEquals(HTTP_OK, createResponse.getStatusCode());
			assertEquals(JSON_CONTENT_TYPE, createResponse.getContentType());

			final String createJsonResponse = createResponse.asString();
			final JsonPath createJsonPath = new JsonPath(createJsonResponse);

			/* All entities should have an ID */
			assertNotNull(createJsonPath.get("id"));

			final int id = createJsonPath.getInt("id");

			/* Attempt to update the entity */
			final String fixedUpdateJson = updateJson.replace(ID_MARKER, id + "");

			final Response updateResponse = given().body(fixedUpdateJson).contentType(JSON_CONTENT_TYPE).put(updateURL);

			assertEquals(HTTP_OK, updateResponse.getStatusCode());
			assertEquals(JSON_CONTENT_TYPE, updateResponse.getContentType());

			final String updateJsonResponse = updateResponse.asString();
			final JsonPath updateJsonPath = new JsonPath(updateJsonResponse);

			assertNotNull(updateJsonPath.get("id"));
			assertEquals(updateJsonPath.get("id"), createJsonPath.get("id"));

			/* Attempt to delete the entity */
			final Response deleteResponse = delete(deleteURL + "/" + id);

			assertEquals(HTTP_OK, deleteResponse.getStatusCode());
			assertEquals(JSON_CONTENT_TYPE, deleteResponse.getContentType());
			
			/* Attempt to get the deleted entity. This should fail. */
			final Response getResponse = get(getURL + "/" + id);

			assertEquals(HTTP_CUSTOM_ERROR, getResponse.getStatusCode());
		}
	}

	@Test
	public void crudTag()
	{
		/* This ID will be replaced with a marker, so the number just has to be something that won't be found anywhere other than the ID field */
		final Integer updateID = 1234;

		final String getPath = "/1/tag/get/json";
		final String createPath = "/1/tag/post/json";
		final String updatePath = "/1/tag/put/json";
		final String deletePath = "/1/tag/delete/json";

		final RESTTagV1 createTag = new RESTTagV1();
		createTag.explicitSetName(StringUtilities.generateRandomString(10));
		createTag.explicitSetDescription(StringUtilities.generateRandomString(10));

		final RESTTagV1 updateTag = new RESTTagV1();
		updateTag.setId(updateID);
		updateTag.explicitSetName(StringUtilities.generateRandomString(10));
		updateTag.explicitSetDescription(StringUtilities.generateRandomString(10));

		try
		{
			final String createJson = mapper.writeValueAsString(createTag);
			final String updateJson = mapper.writeValueAsString(updateTag).replace(updateID.toString(), ID_MARKER);

			createUpdateDelete(getPath, createPath, createJson, updatePath, updateJson, deletePath);
		}
		catch (final Exception ex)
		{
			fail();
		}
	}
}