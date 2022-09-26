# Kenyan-Tweets-BDT
This project involves data streaming from Twitter API, using Kafka and store the results in Hbase for Big Data Technology CS523 2022.

# Describe
Due to the distance from my home country, I decided to make an API call that will help me fetch verified information from Kenya. In this digital age, fake news spreads as fast as wildfire. With this API, we can easily get the specific keyword that we want and check the tweets, likes and retweets of the keyword.

# Project Requirements:
- Create your own project for Spark Streaming
- Integrate HBase and/or Hive with Part 1
- Create a simple demo project for any of the following tools: Kafka
- Record a demo of your Presentation of all the above 3 parts


# Tools Used
- Twitter deleoper portal
- Kafka
- HBase
- Spark

# How it works
1.  We have to ensure the following components are running first:
- HDFS
- HBase
- Zookeeper
- Kafka

2. Create HBase table

`$ hbase shell`

`hbase> create 'tweet'`

3.  Run producer then consumer
Producer:

`spark-submit --class "twitter.KenyanTweetsProducer" --master local[2] KenyanTweetsProducer.jar`

# Consumer

`spark-submit --class "twitter.KenyanTweetsConsumer" --master local[2] KenyanTweetsConsumer.jar`

Demo Video
https://web.microsoftstream.com/video/ee153b57-0beb-4a03-9cc6-5e3bd76771f1
