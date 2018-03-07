package io.findify.s3mock

import java.util

import better.files.File
import com.amazonaws.auth.{AWSStaticCredentialsProvider, AnonymousAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.typesafe.config.{Config, ConfigFactory}
import io.findify.s3mock.provider.FileProvider

/**
  * Created by shutty on 8/9/16.
  */
object Main {

  private val options = ConfigOptions(ConfigFactory.load().getConfig("s3mock"))

  def main(args: Array[String]): Unit = {

    println(s"JAVA_ARGS: ${System.getenv("JAVA_ARGS")}")
    println(s"Options: $options")

    val server = new S3Mock(options.port, new FileProvider(options.dir))

    server.start

    createBuckets(buildClient(options.host, options.port, options.region), options.initBuckets)
  }

  private def createBuckets(client: AmazonS3, buckets: Set[String]): Unit = {
    try {
      buckets.foreach(b => {
        client.createBucket(b)
        println(s"Created bucket: $b")
      })
    } finally {
      client.shutdown()
    }
  }

  private def buildClient(host: String, port: Int, region: String): AmazonS3 = {
    val endpoint = new EndpointConfiguration(s"http://$host:$port", region)
    AmazonS3ClientBuilder.standard()
      .withPathStyleAccessEnabled(true)
      .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
      .withEndpointConfiguration(endpoint)
      .build()
  }
}


case class ConfigOptions(dir: String, region: String, host: String, port: Int, initBuckets: Set[String])

object ConfigOptions {

  def apply(config: Config): ConfigOptions = {

    def getWithDefault[T](path: String, default: T): T = {
      if (config.hasPath(path)) config.getAnyRef(path).asInstanceOf[T] else default
    }

    import scala.collection.JavaConverters._

    ConfigOptions(
      getWithDefault("dir", File.newTemporaryDirectory(prefix = "s3mock").pathAsString),
      getWithDefault("region", "eu-west-1"),
      getWithDefault("host", "localhost"),
      getWithDefault("port", 8001),
      getWithDefault("initBuckets", new util.HashMap[String, String]()).asScala.values.toSet
    )
  }

}
