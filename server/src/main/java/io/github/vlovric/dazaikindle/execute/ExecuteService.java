package io.github.vlovric.dazaikindle.execute;

import org.springframework.stereotype.Service;

import io.github.vlovric.dazaikindle.execute.dto.ExecuteResponse;
import io.github.vlovric.dazaikindle.execute.dto.FullRunRequest;

@Service
public class ExecuteService {

    public ExecuteResponse executeFullRun(FullRunRequest request){
        //TODO
        //execute pipeline with custom locations for .epub book artifact and output of whole run
        //I also need to write the run.json file after the run is finished
    }
}
