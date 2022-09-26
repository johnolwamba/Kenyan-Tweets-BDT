package twitter;

import java.util.concurrent.ExecutionException;

import org.apache.spark.SparkConf;
import org.apache.spark.TaskContext;
import org.apache.spark.api.java.*;
import org.apache.spark.api.java.function.*;
import org.apache.spark.streaming.api.java.*;

import kafka.serializer.StringDecoder;

import org.apache.spark.sql.SparkSession;

import java.util.UUID;

import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.Seconds;
import org.apache.spark.streaming.StreamingContext;
import org.apache.spark.streaming.kafka010.*;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.io.compress.Compression.Algorithm;

import scala.Tuple2;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KenyanTweetsConsumer {
	public static JavaSparkContext sparkContext;
	private static final String TABLE_NAME = "tblTweet";

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws ExecutionException, InterruptedException {

		Map<String, Object> kafkaParams = new HashMap<>();
		kafkaParams.put("bootstrap.servers", "localhost:9092");
		kafkaParams.put("key.deserializer", StringDeserializer.class);
		kafkaParams.put("value.deserializer", StringDeserializer.class);
		kafkaParams.put("group.id", "use_a_separate_group_id_for_each_stream");
		kafkaParams.put("auto.offset.reset", "latest");
		kafkaParams.put("enable.auto.commit", false);

		Collection<String> topics = Arrays.asList("Kenyan-Tweets");

		Configuration hConf = HBaseConfiguration.create();
		
		try (Connection connection = ConnectionFactory.createConnection(hConf);
				Admin admin = connection.getAdmin())
		{
			HTableDescriptor table = new HTableDescriptor(TableName.valueOf(TABLE_NAME));
			table.addFamily(new HColumnDescriptor("tweet"));
			
			if (admin.tableExists(table.getTableName()))
			{
				admin.disableTable(table.getTableName());
				admin.deleteTable(table.getTableName());
			}
			
			admin.createTable(table);
			
			String delimiter = "º¿";
			HTable hTable = new HTable(hConf, TABLE_NAME);

			SparkConf sparkConf = new SparkConf();
			sparkConf.setMaster("local[2]");
			sparkConf.setAppName("KenyanTweets");

			JavaStreamingContext streamingContext = new JavaStreamingContext(sparkConf, Durations.seconds(1));
			JavaInputDStream<ConsumerRecord<String, String>> messages = KafkaUtils.createDirectStream(streamingContext, LocationStrategies.PreferConsistent(), ConsumerStrategies.<String, String> Subscribe(topics, kafkaParams));
			JavaPairDStream<String, String> results = messages.mapToPair(record -> new Tuple2<>(record.key(), record.value()));
			JavaDStream<String> lines = results.map(tuple2 -> tuple2._2());
			
			lines.print();  

			
			lines.foreachRDD(row -> {
				System.out.println("**** "+row.isEmpty());
				List<String> tweetDetails = row.collect();
				tweetDetails.forEach(singleRow -> {
					String[] data = singleRow.split(delimiter);
					String tweet = data[0];
					String userCreationDate = data[1];
					String userName = data[2];
					String likeCount = data[3];
					String retweetCount = data[4];
					String replyCount = data[5];
					String date = data[6];

					Put put = new Put(Bytes.toBytes(userName));
					put.add(Bytes.toBytes("tweet"),Bytes.toBytes("Text"),Bytes.toBytes(tweet));
					put.add(Bytes.toBytes("tweet"),Bytes.toBytes("Date"),Bytes.toBytes(date));
					put.add(Bytes.toBytes("tweet"),Bytes.toBytes("Likes"),Bytes.toBytes(likeCount));
					put.add(Bytes.toBytes("tweet"),Bytes.toBytes("Retweets"),Bytes.toBytes(retweetCount));
					put.add(Bytes.toBytes("tweet"),Bytes.toBytes("Replies"),Bytes.toBytes(replyCount));
					put.add(Bytes.toBytes("tweet"),Bytes.toBytes("User"),Bytes.toBytes(userName));

					try {
						hTable.put(put);
					} catch (Exception e) {
						e.printStackTrace();
					}

					System.out.println("Db Updated");
				});
			});

			streamingContext.start();
			streamingContext.awaitTermination();

		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
