import java.io.IOException;
import java.util.StringTokenizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
public class InvertedIndex {
  public static class InvertedIndexMapper extends 
    Mapper<Object,Text,Object,Text>{
    private Text keyInfo = new Text();
    private  Text valueInfo = new Text();
    @Override
    public void map(Object key, Text value, Context context)
        throws IOException, InterruptedException {
      String filename = ((FileSplit)context.getInputSplit()).getPath().getName().toString();
      StringTokenizer itr = new StringTokenizer(value.toString());
      while(itr.hasMoreTokens()){
        keyInfo.set(itr.nextToken()+":"+filename);//key is the combination of term + ':' + filename;
                                                  //for example, "King:Hugo.tar.gz"
        valueInfo.set("1");                       //value is '1'
        context.write(keyInfo, valueInfo);        //emit ("King:Hugo.tar.gz", "1")
      }
    }
  }
  public static class InvertedIndexCombiner 
    extends Reducer<Text, Text, Text, Text>{
    private Text info = new Text();
    @Override
    protected void reduce(Text key, Iterable<Text> values,Context context)
        throws IOException, InterruptedException {
      int sum = 0;
      for(Text value : values){
        sum += Integer.parseInt(value.toString());//sum up all occurences of ("King:Hugo.tar.gz", "1")
                                                  //and assign the number of occurence to `sum`
      }
      
      //convert a bunch of ("King:Hugo.tar.gz", "1") to ("King", "Hugo.tar.gz:n")
      //now the key is term, and value is the combination of filename and number of occurence
      int splitIndex = key.toString().indexOf(":");
      info.set(key.toString().substring(splitIndex+1)+":"+sum);
      key.set(key.toString().substring(0,splitIndex));
      context.write(key, info);//emit ("King", "Hugo.tar.gz:n")
    }
  }
  public static class InvertedIndexReducer 
    extends Reducer<Text, Text, Text, Text>{
    
    private Text result = new Text();
    @Override
    protected void reduce(Text key, Iterable<Text> values,Context context)
        throws IOException, InterruptedException {
      String fileList = new String();
      for(Text value : values){
        fileList += value.toString()+";"; //Reduce number of occurences in different files to 1 record
                      //For example, ("King", "Hugo.tar.gz:n") + ("King", "Tolstoy.tar.gz:m") =>
                                                        //("King", "Hugo.tar.gz.n;Tolstoy.tar.gz:m")
      }
      result.set(fileList);
      context.write(key, result);         //emit ("King", "Hugo.tar.gz.n;Tolstoy.tar.gz:m")
    }
  }
  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
      String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
      if (otherArgs.length != 2) {
        System.err.println("Usage: InvertedIndex <in> <out>");
        System.exit(2);
      }
      Job job = new Job(conf, "InvertedIndex");
      job.setJarByClass(InvertedIndex.class);
      job.setMapperClass(InvertedIndexMapper.class);
      job.setCombinerClass(InvertedIndexCombiner.class);
      job.setReducerClass(InvertedIndexReducer.class);
      job.setOutputKeyClass(Text.class);
      job.setOutputValueClass(Text.class);
      FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
      FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
      System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}
