package io.github.vlovric.dazaikindle;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import io.github.vlovric.dazaikindle.pipeline.Pipeline;

public class Main {
    /**
     * Entry point. Parses CLI arguments, then runs the processing pipeline.
      * Exits with nonzero status on any error, printing a message to stderr.
      * Exits with zero status on success.
     * @param args
     */
    public static void main(String[] args){
        AppArgs appArgs;

        try{
            appArgs = new ArgsParser().parse(args);
        }catch(CmdLineException e){
            System.err.println(e.getMessage());
            System.err.println("Usage: java -jar dazai-kindle.jar [options...]");

            new CmdLineParser(new ArgsParser()).printUsage(System.err);
            System.exit(1);
            return;
        }catch(IllegalArgumentException e){
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }

        try{
            new Pipeline(appArgs).run();
        }catch(Exception e){
            System.err.println("[DazaiKindle] ❌ Pipeline Error:");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
}
