package twitter;

public class KafkaConfig {
	
    public static final String BOOTSTRAPSERVERS = "127.0.0.1:9092";
    public static final String TOPIC = "Kenyan-Tweets";
    public static final String ACKS_CONFIG = "all";
    public static final String MAX_IN_FLIGHT_CONN = "5";

    public static final String COMPRESSION_TYPE = "snappy";
    public static final String RETRIES_CONFIG = Integer.toString(Integer.MAX_VALUE);
    public static final String LINGER_CONFIG = "20";
    public static final String BATCH_SIZE = Integer.toString(32*1024);
    
    public static final String SERVERS = "bigdata-1:9092, bigdata-2:9092, bigdata-3:9092";
}
