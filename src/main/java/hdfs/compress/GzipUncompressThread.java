package hdfs.compress;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionInputStream;
import org.apache.hadoop.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

/**
 * @Auther: huxuhui
 * @Date: 2019/10/11 09:46
 * @Description:
 */
public class GzipUncompressThread implements Runnable {

    private Logger logger = LoggerFactory.getLogger(GzipUncompressThread.class);

    private final static String codecClassName = "org.apache.hadoop.io.compress.GzipCodec";

    private String defaultFS;

    //需要压缩的文件
    private String sourceFile;

    //配置
    private Configuration configuration;

    public GzipUncompressThread(String defaultFS, String path, Configuration configuration) {
        this.sourceFile = path;
        this.defaultFS = defaultFS;
        this.configuration = configuration;
    }

    @Override
    public void run() {
        if (!sourceFile.contains(".gz")) {
            logger.error("file:{} is not gz file.", sourceFile);
            return;
        }
        String goal_dir = sourceFile.replace(".gz", "");
        logger.info("start uncompress file:{},target file:{}", sourceFile, goal_dir);
        //压缩文件
        Class<?> codecClass = null;
        FileSystem fs = null;
        FSDataInputStream input = null;
        OutputStream output = null;
        try {
            fs = FileSystem.get(URI.create(defaultFS), configuration);
            codecClass = Class.forName(codecClassName);
            CompressionCodec codec = (CompressionCodec) ReflectionUtils.newInstance(codecClass, configuration);
            //指定压缩文件输出路径
            input = fs.open(new Path(sourceFile));
            CompressionInputStream codec_input = codec.createInputStream(input);
            output = fs.create(new Path(goal_dir));
            IOUtils.copyBytes(codec_input, output, configuration);
            fs.delete(new Path(sourceFile), true);
            logger.info("compress success delete:{}", sourceFile);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("compress file fail,roll back....");
            if (fs != null) {
                try {
                    fs.delete(new Path(goal_dir), true);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            try {
                if (fs != null) {
                    fs.close();
                }
                IOUtils.closeStream(input);
                IOUtils.closeStream(output);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
