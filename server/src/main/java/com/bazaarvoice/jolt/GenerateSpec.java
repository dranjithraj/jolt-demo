package com.bazaarvoice.jolt;

import com.bazaarvoice.jolt.helper.Item;
import com.bazaarvoice.jolt.helper.ItemsList;
import com.bazaarvoice.jolt.helper.MainClass;
import freemarker.template.Configuration;
import freemarker.template.Template;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/generate")
public class GenerateSpec extends HttpServlet {

    private static final long serialVersionUID = 1L;
	public static final String UNDERSCORE = "_";
    public static final String PARAM_INPUT = "input";    
    public static final String MODEL_PROPERTIES = "model_properties";
    public static final String PAYLOAD = "payload";
    public static final String PARAM_TEMPLATE = "template";
	private static final String MODEL_NAME = "modelName";
    private final Logger logger = Logger.getLogger(getClass().getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        logger.info("===========/generate POST Request ===========");
        logger.log(Level.INFO, "input-> {0}", req.getParameter(PARAM_INPUT));
        logger.log(Level.INFO,"sort-> {0}", req.getParameter("sort"));
        logger.log(Level.INFO,"template-> {0}", req.getParameter(PARAM_TEMPLATE));
        String templateFileName = req.getParameter(PARAM_TEMPLATE);
        String modelName = req.getParameter(MODEL_NAME);
        logger.log(Level.INFO,"Model name-> {0}", modelName);
        
        String inputPayloadString;
        try {
            inputPayloadString = req.getParameter(PARAM_INPUT);
        } catch (Exception e) {
            response.getWriter().println("Could not url-decode the inputs.\n");
            return;
        }

        Object inputPayloadJSON;
        Writer writer = new StringWriter();
        Configuration configuration = new Configuration();
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        List<Item> items = new ArrayList<>();
        ItemsList itemsList = new ItemsList(items);  
        itemsList.setModelName(modelName);
        try {
            inputPayloadJSON = JsonUtils.jsonToObject(inputPayloadString);
            if (inputPayloadJSON instanceof LinkedHashMap) {
                linkedHashMap = (LinkedHashMap) inputPayloadJSON;
            } else if (inputPayloadJSON instanceof ArrayList){
                linkedHashMap = (LinkedHashMap) ((ArrayList)inputPayloadJSON).get(0);
            }
            LinkedHashMap payloadJson = (LinkedHashMap)linkedHashMap.get(PAYLOAD);
            Set<String> keys = ((LinkedHashMap) payloadJson.get(MODEL_PROPERTIES)).keySet();

            keys.forEach(key -> {
                if (!(((LinkedHashMap) payloadJson.get(MODEL_PROPERTIES)).get(key) instanceof ArrayList))
                    items.add(new Item(key, 0));
            });

            configuration.setDirectoryForTemplateLoading(new File(MainClass.class.getResource("/").toURI()));
            Template template = configuration.getTemplate(templateFileName);

            template.process(itemsList, writer);

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().println("Could not parse the 'input' JSON.\n");
            return;
        }

        String result = writer.toString();
        logger.log( Level.INFO, "Java GenerateSpec result=> {0}", result);

        // Add PR details in the response
        response.getWriter().println(result);
    }

}
