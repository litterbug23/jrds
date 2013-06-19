package jrds.webapp;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jrds.Probe;

import org.apache.log4j.Logger;
import org.rrd4j.ConsolFun;
import org.rrd4j.data.DataProcessor;

import jrds.store.ExtractInfo;
import jrds.store.Extractor;

/**
 * A servlet wich show the last update values and time
 * @author Fabrice Bacchella
 * @version $Revision: 236 $
 */
public final class ProbeSummary extends JrdsServlet {
    static final private Logger logger = Logger.getLogger(ProbeSummary.class);

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setContentType("text/plain");
        res.addHeader("Cache-Control", "no-cache");
        ServletOutputStream out = res.getOutputStream();

        ParamsBean params = getParamsBean(req);

        Probe<?,?> probe = params.getProbe();
        if(probe != null) {
            Date begin = params.getPeriod().getBegin();
            Date end = params.getPeriod().getEnd();

            Extractor fetched = probe.fetchData();
            ExtractInfo ei = ExtractInfo.get().make(begin, end);
            String names[] = fetched.getDsNames();
            DataProcessor dp = probe.extract(ei);
            for(int i= 0; i< names.length ; i++) {
                String dsName = names[i];
                try {
                    out.print(dsName + " ");
                    out.print(dp.getAggregate(dsName, ConsolFun.AVERAGE) + " ");
                    out.print(dp.getAggregate(dsName, ConsolFun.MIN) + " ");
                    out.println(dp.getAggregate(dsName, ConsolFun.MAX) );
                } catch (IOException e) {
                    logger.error("Probe " + probe + "unusable: " + e);
                }
            }
        }
        else {
            logger.error("Probe id provided " + params.getId() + " invalid");
        }
    }

}
