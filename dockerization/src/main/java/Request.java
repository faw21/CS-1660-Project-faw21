import com.google.cloud.dataproc.v1.*;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Request {

  public static Job waitForJobCompletion(
      JobControllerClient jobControllerClient, String projectId, String region, String jobId) {
    while (true) {
      // Poll the service periodically until the Job is in a finished state.
      Job jobInfo = jobControllerClient.getJob(projectId, region, jobId);
      switch (jobInfo.getStatus().getState()) {
        case DONE:
        case CANCELLED:
        case ERROR:
          return jobInfo;
        default:
          try {
            // Wait a second in between polling attempts.
            TimeUnit.SECONDS.sleep(1);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
      }
    }
  }

  public static ArrayList<Blob> request(
      String projectId, String region, String clusterName, String jobFilePath)
      throws IOException, InterruptedException {
    String myEndpoint = String.format("%s-dataproc.googleapis.com:443", region);

    // Configure the settings for the cluster controller client.
    ClusterControllerSettings clusterControllerSettings =
            ClusterControllerSettings.newBuilder().setEndpoint(myEndpoint).build();

    // Configure the settings for the job controller client.
    JobControllerSettings jobControllerSettings =
        JobControllerSettings.newBuilder().setEndpoint(myEndpoint).build();

    // Create both a cluster controller client and job controller client with the
    // configured settings. The client only needs to be created once and can be reused for
    // multiple requests. Using a try-with-resources closes the client, but this can also be done
    // manually with the .close() method.
    try (ClusterControllerClient clusterControllerClient =
                 ClusterControllerClient.create(clusterControllerSettings);
         JobControllerClient jobControllerClient =
            JobControllerClient.create(jobControllerSettings)) {

      // Configure the settings for our job.
      JobPlacement jobPlacement = JobPlacement.newBuilder().setClusterName(clusterName).build();
      String[] args = new String[2];
      args[0] = jobFilePath + "/Data/";
      args[1] = jobFilePath + "/Output/";
      HadoopJob hadoopJob;
      hadoopJob = HadoopJob.newBuilder().setMainClass("InvertedIndex")
              .addJarFileUris(jobFilePath + "/JAR/invertedindex.jar")
              .addArgs(args[0])
              .addArgs(args[1])
              .build();
      Job job = Job.newBuilder().setPlacement(jobPlacement).setHadoopJob(hadoopJob).build();

      // Submit an asynchronous request to execute the job.
      Job request = jobControllerClient.submitJob(projectId, region, job);
      String jobId = request.getReference().getJobId();
      System.out.println(String.format("Submitted job \"%s\"", jobId));

      // Wait for the job to finish.
      CompletableFuture<Job> finishedJobFuture =
          CompletableFuture.supplyAsync(
              () -> waitForJobCompletion(jobControllerClient, projectId, region, jobId));
      int timeout = 10;
      try {
        Job jobInfo = finishedJobFuture.get(timeout, TimeUnit.MINUTES);

        // Cloud Dataproc job output gets saved to a GCS bucket allocated to it.
        Cluster clusterInfo = clusterControllerClient.getCluster(projectId, region, clusterName);
        Storage storage = StorageOptions.getDefaultInstance().getService();
        ArrayList<Blob> results= new ArrayList<Blob>();
        String resultPath = "Output/part-r-";
        try {

          for (int i = 0; i < 11; i++) {
            Blob blob;
            if(i!=10) {
              blob = storage.get(
                      clusterInfo.getConfig().getConfigBucket(),
                      resultPath + "0000" + i);
            }
            else {
              blob = storage.get(
                      clusterInfo.getConfig().getConfigBucket(),
                      resultPath + "000" + i);
            }
            results.add(blob);

          }
          System.out.println(String.format("Job \"%s\" finished with state %s!", jobId, jobInfo.getStatus().getState()));
        }

        catch(Exception e){}
        return results;

      } catch (TimeoutException e) {
        System.err.println(
            String.format("Job timed out after %d minutes: %s", timeout, e.getMessage()));
      }


    } catch (ExecutionException e) {
      System.err.println(String.format("Error executing quickstart: %s ", e.getMessage()));
    }
    return null;
  }

  public static void main(String... args) throws IOException, InterruptedException {

    String projectId = "corded-sunbeam-273920";// project-id of project to create the cluster in
    String region = "us-east4";  // region to create the cluster
    String clusterName = "cs1660-project-cluster"; // name of the cluster
    String jobFilePath = "gs://dataproc-staging-us-east4-9371974370-akh5vmt7"; // location in GCS of the PySpark job

    request(projectId, region, clusterName, jobFilePath);
  }
}