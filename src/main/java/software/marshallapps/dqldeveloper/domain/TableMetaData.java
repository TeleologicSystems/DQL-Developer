package software.marshallapps.dqldeveloper.domain;

import java.util.List;

import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;

public class TableMetaData {
	public String name;
	public KeySchemaElement primaryKey;
	public String description;
	public List<String> fields;
	public String throughput;
	public String tableArn;
	public String billingMode;
}
