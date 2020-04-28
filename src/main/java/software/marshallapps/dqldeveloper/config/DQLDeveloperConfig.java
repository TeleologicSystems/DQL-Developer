package software.marshallapps.dqldeveloper.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.s3.model.Region;

import software.marshallapps.dqldeveloper.domain.TableMetaData;

public class DQLDeveloperConfig {

	public static AmazonDynamoDB DDB_CLIENT;
	public static DynamoDB DDB;
	private static Map<String, Map<String, TableMetaData>> META_DATA_FOR_ENDPOINTS = new ConcurrentHashMap();

	public static void init() {
		DDB_CLIENT = AmazonDynamoDBClientBuilder.standard()
				.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(createEndpoint(), getRegion())).build();
		DDB = new DynamoDB(DDB_CLIENT);
	}

	public static String createEndpoint() {
		String region = getRegion();
		StringBuilder sb = new StringBuilder("https://dynamodb.").append(region == null ? Region.US_West : region).append(".amazonaws.com");
		return sb.toString();
	}

	public static String getRegion() {
		return System.getProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY);
	}

	public static void setRegion(String region) {
		System.setProperty(SDKGlobalConfiguration.AWS_REGION_ENV_VAR, region);
	}

	public static synchronized Map<String, Map<String, TableMetaData>> getMetaData() {
		return META_DATA_FOR_ENDPOINTS;
	}

	public static synchronized Map<String, TableMetaData> getMetaData(String endpoint) {
		return META_DATA_FOR_ENDPOINTS.get(endpoint);
	}

	public static synchronized TableMetaData getMetaData(String endpoint, String tableName) {
		Map<String, TableMetaData> dataForEndpoint = META_DATA_FOR_ENDPOINTS.get(endpoint);
		TableMetaData metaData = null;
		if (dataForEndpoint != null) {
			metaData = dataForEndpoint.get(tableName);
		}
		return metaData;
	}

	public static synchronized void setMetaData(Map<String, Map<String, TableMetaData>> metaData) {
		META_DATA_FOR_ENDPOINTS = metaData;
	}

	public static synchronized void setMetaData(String endpoint, Map<String, TableMetaData> metaData) {
		META_DATA_FOR_ENDPOINTS.put(endpoint, metaData);
	}

	public static void main(String... args) {
		System.out.println(Region.US_West);
	}

}
