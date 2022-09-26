package twitter;

import io.github.redouane59.twitter.TwitterClient;
import io.github.redouane59.twitter.dto.endpoints.AdditionalParameters;
import io.github.redouane59.twitter.dto.tweet.TweetList;
import io.github.redouane59.twitter.dto.tweet.TweetV2;
import io.github.redouane59.twitter.dto.tweet.TweetV2.TweetData;
import io.github.redouane59.twitter.helpers.ConverterHelper;
import io.github.redouane59.twitter.signature.TwitterCredentials;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.UUID;
import java.text.SimpleDateFormat;

public class KenyanTweetsProducer {

	private KafkaProducer<String, String> producer;
	private final TwitterClient twitterClient;
	private final String keyword = "kenya";

	public KenyanTweetsProducer() {
		twitterClient = new TwitterClient(TwitterCredentials.builder()
				.bearerToken(TwitterConfig.BEARERTOKEN)
				.build());
	}

	//Kafka Producer
	private KafkaProducer<String, String> createKafkaProducer() {
		// Create producer properties
		Properties prop = new Properties();
		prop.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfig.BOOTSTRAPSERVERS);
		prop.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		prop.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

		// create safe Producer
		prop.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
		prop.setProperty(ProducerConfig.ACKS_CONFIG, KafkaConfig.ACKS_CONFIG);
		prop.setProperty(ProducerConfig.RETRIES_CONFIG, KafkaConfig.RETRIES_CONFIG);
		prop.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, KafkaConfig.MAX_IN_FLIGHT_CONN);

		// Additional settings for high throughput producer
		prop.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, KafkaConfig.COMPRESSION_TYPE);
		prop.setProperty(ProducerConfig.LINGER_MS_CONFIG, KafkaConfig.LINGER_CONFIG);
		prop.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, KafkaConfig.BATCH_SIZE);

		// Create producer
		return new KafkaProducer<String, String>(prop);
	}

	public void run() throws ExecutionException, InterruptedException {
		// Kafka Producer
		producer = createKafkaProducer();

		AdditionalParameters params = AdditionalParameters.builder()
				.startTime(ConverterHelper.minutesBeforeNow(15))
				.recursiveCall(false)
				.build();

		TweetList result = twitterClient.searchTweets(keyword, params);
		StringBuilder sb = new StringBuilder();
		String delimiter = "º¿";

		Locale dateLocale = Locale.US;
		SimpleDateFormat outFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", dateLocale);

		List<TweetV2.TweetData> tweetDataList = result.getData();

		for (Iterator<TweetData> i = tweetDataList.iterator(); i.hasNext();){
			TweetV2.TweetData tweet = (TweetV2.TweetData)i.next();

			sb.append(tweet.getText()).append(delimiter);

			if(tweet.getUser() != null) {
				sb.append(tweet.getUser().getCreatedAt()).append(delimiter);
				sb.append(tweet.getUser().getName()).append(delimiter);
			}else {
				sb.append("").append(delimiter).append(delimiter);
				sb.append("").append(delimiter).append(delimiter);
			}

			sb.append(tweet.getLikeCount()).append(delimiter);
			sb.append(tweet.getRetweetCount()).append(delimiter);
			sb.append(tweet.getReplyCount()).append(delimiter);

			sb.append("").append(outFormat.format(new Date()));	


			System.out.println("Data: " + sb.toString());

			producer.send(new ProducerRecord<>(KafkaConfig.TOPIC, UUID.randomUUID().toString(), sb.toString())).get();
		}
	}


	public static void main(String[] args) throws ExecutionException, InterruptedException {
		new KenyanTweetsProducer().run();
	}
}
