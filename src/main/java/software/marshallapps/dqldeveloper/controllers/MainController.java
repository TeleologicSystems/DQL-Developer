package software.marshallapps.dqldeveloper.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

import software.marshallapps.dqldeveloper.config.DQLDeveloperConfig;
import software.marshallapps.dqldeveloper.domain.TableMetaData;
import software.marshallapps.dqldeveloper.utils.Constants;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/resources")
public class MainController {

	private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);
	private static final Map<Integer, List<Map<String, String>>> RESPONSE_RESULTS = new ConcurrentHashMap();

	public static Object getValue(AttributeValue val) {
		Object o = val.isNULL();
		if (o == null) {
			if (val.getS() != null)
				o = val.getS();
			if (val.getN() != null)
				o = val.getN();
			if (val.getM() != null)
				o = val.getM();
			if (val.getL() != null)
				o = val.getL();
			if (val.getSS() != null)
				o = val.getSS();
			if (val.getBS() != null)
				o = val.getBS();
			if (val.getBOOL() != null)
				o = val.getB();
			if (val.getNS() != null)
				o = val.getNS();
		}
		return o;
	}

	@GetMapping("/selectAll/{tableName}")
	public List<Map<String, String>> selectAll(@PathVariable("tableName") String tableName) {
		List<Map<String, String>> content = Collections.EMPTY_LIST;

		TableMetaData metaData = DQLDeveloperConfig.getMetaData(DQLDeveloperConfig.createEndpoint(), tableName);

		System.out.println("Metadata null: " + metaData == null);

		if (metaData != null) {
			final int threadId = (int) Thread.currentThread().getId();
			RESPONSE_RESULTS.put(threadId, content);
			List<String> fields = metaData.fields;
			Map<String, AttributeValue> exclusiveStartKey = null;
			ExecutorService es = Executors.newFixedThreadPool(8);

			do {
				ScanRequest scanRequest = new ScanRequest().withTableName(tableName).withExclusiveStartKey(exclusiveStartKey);
				ScanResult scanResult = DQLDeveloperConfig.DDB_CLIENT.scan(scanRequest);
				exclusiveStartKey = scanResult.getLastEvaluatedKey();

				es.submit(new Runnable() {
					public void run() {
						List<Map<String, AttributeValue>> items = scanResult.getItems();
						for (Map<String, AttributeValue> item : items) {
							Map<String, String> displayItem = new HashMap();
							for (String field : fields) {
								String value = String.valueOf(
										// item.get(field));
										getValue(item.get(field)));

								displayItem.put(field, value);
							}
							List<Map<String, String>> results = RESPONSE_RESULTS.get(threadId);

							if (results == Collections.EMPTY_LIST) {
								results = new ArrayList(scanResult.getCount());
								RESPONSE_RESULTS.put(threadId, results);
							}
							results.add(displayItem);
						}
					}
				});

			} while (exclusiveStartKey != null);

			es.shutdown();

			try {
				if (!es.awaitTermination(60, TimeUnit.SECONDS)) {
					LOGGER.debug("Timeout elapsed for table: " + tableName);
				}
			} catch (InterruptedException ie) {
				LOGGER.debug(ie.getMessage());
			}

			content = RESPONSE_RESULTS.get(threadId);
			RESPONSE_RESULTS.remove(threadId);
		}

		return content;
	}

	@PostMapping("/config")
	public void updateConfig(String region) {
		DQLDeveloperConfig.setRegion(region);
	}

	@GetMapping("/init")
	public Map<String, TableMetaData> getTableMetaData() {
		DQLDeveloperConfig.init();
		Map<String, TableMetaData> tmpMetaData = DQLDeveloperConfig.getMetaData(DQLDeveloperConfig.createEndpoint());
		final Map<String, TableMetaData> tableMetaData;

		if (tmpMetaData == null) {
			tableMetaData = new HashMap();
			ExecutorService es = Executors.newFixedThreadPool(8);

			ListTablesResult ltr = DQLDeveloperConfig.DDB_CLIENT.listTables();
			List<String> tableNames = ltr.getTableNames();

			for (String tableName : tableNames) {
				es.submit(new Runnable() {
					public void run() {
						TableMetaData metaData = new TableMetaData();
						metaData.name = tableName;
						tableMetaData.put(tableName, metaData);

						Table table = DQLDeveloperConfig.DDB.getTable(tableName);
						TableDescription description = table.describe();
						List<KeySchemaElement> primaryKey = description.getKeySchema();

						metaData.primaryKey = primaryKey.get(0);

						ScanResult result = DQLDeveloperConfig.DDB_CLIENT.scan(new ScanRequest().withTableName(tableName));

						Set<String> fields = new HashSet();
						for (Map<String, AttributeValue> item : result.getItems()) {
							fields.addAll(item.keySet());
						}

						metaData.fields = List.copyOf(fields);
					}
				});
			}

			es.shutdown();
			try {
				if (!es.awaitTermination(1, TimeUnit.MINUTES)) {
					LOGGER.warn("App initialization failed due to timeout");
				}
			} catch (InterruptedException ie) {
				LOGGER.warn(ie.getMessage());
			}

			DQLDeveloperConfig.setMetaData(DQLDeveloperConfig.createEndpoint(), tableMetaData);
		} else {
			tableMetaData = tmpMetaData;
		}
		return tableMetaData;
	}
}
